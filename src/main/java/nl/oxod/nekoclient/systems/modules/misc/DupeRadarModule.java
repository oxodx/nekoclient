package nl.oxod.nekoclient.systems.modules.misc;

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
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;
import nl.oxod.nekoclient.gui.DupeRadarScreen;
import nl.oxod.nekoclient.util.DupeRadar;

import java.util.ArrayList;
import java.util.List;

public class DupeRadarModule extends Module {
  private static final int AUTO_CHECK_DELAY_TICKS = 20;

  private final SettingGroup sgGeneral = settings.getDefaultGroup();

  public final Setting<Boolean> autoCheckOnJoin = sgGeneral.add(new BoolSetting.Builder()
    .name("auto-check-on-join")
    .description("Automatically check the server you join against DupeDB using the detected plugins.")
    .defaultValue(true)
    .build()
  );

  private boolean pendingOpen;
  private int checkTicksLeft;
  private final List<String> detectedPlugins = new ArrayList<>();
  private boolean pluginTreeReceived;

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
      runAutoCheck();
    }
  }

  @EventHandler
  private void onGameJoined(GameJoinedEvent event) {
    if (!autoCheckOnJoin.get() || mc.player == null || mc.getCurrentServer() == null) return;
    pluginTreeReceived = false;
    // Give the server a moment to send the command tree, which reveals its plugins.
    checkTicksLeft = AUTO_CHECK_DELAY_TICKS;
  }

  @EventHandler
  private void onReadPacket(PacketEvent.Receive event) {
    // Plugin detection: the command tree's namespaces ("pluginname:command") are the
    // actual server's plugins. This is passive and universal, even behind proxies like Minehut.
    if (!(event.packet instanceof ClientboundCommandsPacket packet)) return;
    if (!(event.connection.getPacketListener() instanceof ClientPacketListenerAccessor handler)) return;
    detectedPlugins.clear();
    packet.getRoot(
      CommandBuildContext.simple(handler.meteor$getRegistryAccess(), handler.meteor$getEnabledFeatures()),
      ClientPacketListenerAccessor.meteor$getCommandNodeFactory()
    ).getChildren().forEach(node -> {
      String[] split = node.getName().split(":");
      if (split.length > 1 && !detectedPlugins.contains(split[0])) {
        detectedPlugins.add(split[0]);
      }
    });
    pluginTreeReceived = true;
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
    List<DupeRadar.RadarPluginSnapshot> snapshots = pluginTreeReceived ? detectedPlugins.stream()
      .map(name -> new DupeRadar.RadarPluginSnapshot(name, null, "Exact", 0, List.of(), List.of(), false))
      .toList() : List.of();
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
