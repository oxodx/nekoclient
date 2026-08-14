package nl.oxod.nekoclient.systems.modules.misc;

import com.mojang.brigadier.suggestion.Suggestion;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.mixin.ClientPacketListenerAccessor;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.network.protocol.game.ClientboundCommandSuggestionsPacket;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;
import net.minecraft.network.protocol.game.ServerboundCommandSuggestionPacket;
import nl.oxod.nekoclient.gui.DupeRadarScreen;
import nl.oxod.nekoclient.util.DupeRadar;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class DupeRadarModule extends Module {
  private static final int AUTO_CHECK_DELAY_TICKS = 20;
  private static final int PROBE_WAIT_TICKS = 15;
  private static final Set<String> VERSION_ALIASES = Set.of("version", "ver", "about", "bukkit:version", "bukkit:ver", "bukkit:about");
  private static final Random RANDOM = new Random();

  private final SettingGroup sgGeneral = settings.getDefaultGroup();

  public final Setting<Boolean> autoCheckOnJoin = sgGeneral.add(new BoolSetting.Builder()
    .name("auto-check-on-join")
    .description("Automatically check the server you join against DupeDB using the detected plugins.")
    .defaultValue(true)
    .build()
  );

  private boolean pendingOpen;
  private int checkTicksLeft;
  private boolean probing;
  private final List<String> treePlugins = new ArrayList<>();
  private final List<String> probePlugins = new ArrayList<>();
  private String versionAlias;

  public DupeRadarModule() {
    super(Categories.Misc, "dupe-radar", "Checks your server and detected plugins against DupeDB for known dupes.");
    runInMainMenu = true;
  }

  @Override
  public void onActivate() {
    if (GuiThemes.get() != null) {
      openScreen();
    } else {
      pendingOpen = true;
    }
  }

  @Override
  public void onDeactivate() {
    pendingOpen = false;
    checkTicksLeft = 0;
    probing = false;
    if (mc.gui.screen() instanceof DupeRadarScreen) {
      mc.gui.setScreen(null);
    }
  }

  @EventHandler
  private void onTick(TickEvent.Post event) {
    if (pendingOpen && GuiThemes.get() != null) {
      pendingOpen = false;
      openScreen();
    }
    if (checkTicksLeft > 0 && --checkTicksLeft == 0) {
      if (probing) {
        runAutoCheck();
      } else {
        beginProbe();
      }
    }
  }

  @EventHandler
  private void onGameJoined(GameJoinedEvent event) {
    if (!autoCheckOnJoin.get() || mc.player == null || mc.getCurrentServer() == null) return;
    probing = false;
    treePlugins.clear();
    probePlugins.clear();
    versionAlias = null;
    // Give the server a moment to send the command tree, which reveals its plugins.
    checkTicksLeft = AUTO_CHECK_DELAY_TICKS;
  }

  @EventHandler
  private void onReadPacket(PacketEvent.Receive event) {
    // Plugin detection part 1: the command tree's namespaces ("pluginname:command")
    // are the actual server's plugins. Passive and universal, even behind proxies.
    if (event.packet instanceof ClientboundCommandsPacket packet) {
      if (!(event.connection.getPacketListener() instanceof ClientPacketListenerAccessor handler)) return;
      treePlugins.clear();
      versionAlias = null;
      packet.getRoot(
        CommandBuildContext.simple(handler.meteor$getRegistryAccess(), handler.meteor$getEnabledFeatures()),
        ClientPacketListenerAccessor.meteor$getCommandNodeFactory()
      ).getChildren().forEach(node -> {
        String[] split = node.getName().split(":");
        if (split.length > 1 && !treePlugins.contains(split[0])) {
          treePlugins.add(split[0]);
        }
        if (versionAlias == null && VERSION_ALIASES.contains(node.getName())) {
          versionAlias = node.getName();
        }
      });
      return;
    }

    // Plugin detection part 2: ask for "/version " tab completions, which Bukkit
    // answers with the installed plugin names. Same probe as Meteor's .server plugins.
    if (probing && event.packet instanceof ClientboundCommandSuggestionsPacket suggestions) {
      try {
        for (Suggestion suggestion : suggestions.toSuggestions().getList()) {
          String pluginName = suggestion.getText();
          if (pluginName != null && !pluginName.isBlank() && !probePlugins.contains(pluginName)) {
            probePlugins.add(pluginName);
          }
        }
      } catch (Exception ignored) {
      }
      probing = false;
      checkTicksLeft = 0;
      runAutoCheck();
    }
  }

  @EventHandler
  private void onSendPacket(PacketEvent.Send event) {
    // Don't interfere with the user's own tab completion while our probe is pending.
    if (probing && event.packet instanceof ServerboundCommandSuggestionPacket) {
      event.cancel();
    }
  }

  private void beginProbe() {
    if (versionAlias == null || mc.getConnection() == null) {
      runAutoCheck();
      return;
    }
    mc.getConnection().send(new ServerboundCommandSuggestionPacket(RANDOM.nextInt(200), versionAlias + " "));
    probing = true;
    checkTicksLeft = PROBE_WAIT_TICKS;
  }

  private void runAutoCheck() {
    if (!autoCheckOnJoin.get() || mc.player == null || mc.getConnection() == null || mc.getCurrentServer() == null)
      return;
    String ip = mc.getCurrentServer().ip;
    if (ip == null || ip.isBlank()) return;
    ServerAddress serverAddress = ServerAddress.parseString(ip);
    if (serverAddress == null || serverAddress.getHost() == null || serverAddress.getHost().isBlank()) return;
    String host = serverAddress.getHost().toLowerCase();
    boolean isIp = host.matches("\\d{1,3}(?:\\.\\d{1,3}){3}");
    DupeRadar.RadarServerSnapshot server = new DupeRadar.RadarServerSnapshot(
      List.of(new DupeRadar.RadarServerIdentity(host, isIp)));
    List<String> combined = new ArrayList<>();
    for (String plugin : treePlugins) {
      if (!combined.contains(plugin)) combined.add(plugin);
    }
    for (String plugin : probePlugins) {
      if (!combined.contains(plugin)) combined.add(plugin);
    }
    List<DupeRadar.RadarPluginSnapshot> snapshots = combined.stream()
      .map(name -> new DupeRadar.RadarPluginSnapshot(name, null, "Exact", 0, List.of(), List.of(), false))
      .toList();
    if (snapshots.isEmpty()) {
      info("Dupe radar auto-check started (no server plugins detected).");
    } else {
      info("Dupe radar auto-check started with %d detected plugin(s).", snapshots.size());
    }
    DupeRadar.checkServer(snapshots, server, false);
  }

  private void openScreen() {
    mc.gui.setScreen(new DupeRadarScreen(GuiThemes.get(), this));
  }
}
