package nl.oxod.nekoclient.systems.modules.misc;

import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.commands.commands.ServerCommand;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;
import nl.oxod.nekoclient.gui.DupeRadarScreen;
import nl.oxod.nekoclient.util.DupeRadar;

import java.util.List;

public class DupeRadarModule extends Module {
  // Max time to wait for the server's command tree after joining before giving up.
  private static final int TREE_WAIT_TICKS = 200;
  // Brief settle after the tree arrives so the server can finish updating it and
  // ServerCommand's own tree capture is populated, then probe it.
  private static final int TREE_SETTLE_TICKS = 15;

  private final SettingGroup sgGeneral = settings.getDefaultGroup();

  public final Setting<Boolean> autoCheckOnJoin = sgGeneral.add(new BoolSetting.Builder()
    .name("auto-check-on-join")
    .description("Automatically check the server you join against DupeDB using the detected plugins.")
    .defaultValue(true)
    .build()
  );

  private boolean pendingOpen;
  private int checkTicksLeft;
  private boolean treeReceived;
  private boolean waitingForTree;

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
    waitingForTree = false;
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
    if (!waitingForTree) return;
    if (--checkTicksLeft <= 0) {
      waitingForTree = false;
      runAutoCheck();
    }
  }

  @EventHandler
  private void onGameJoined(GameJoinedEvent event) {
    if (!autoCheckOnJoin.get() || mc.player == null || mc.getCurrentServer() == null) return;
    treeReceived = false;
    waitingForTree = true;
    checkTicksLeft = TREE_WAIT_TICKS;
  }

  @EventHandler
  private void onReadPacket(PacketEvent.Receive event) {
    // Wait for the server's command tree before probing, so we don't check with an
    // empty plugin list on slow servers. The actual detection is delegated to
    // ServerCommand so it is identical to ".server plugins".
    if (event.packet instanceof ClientboundCommandsPacket) {
      treeReceived = true;
      if (waitingForTree) {
        checkTicksLeft = TREE_SETTLE_TICKS;
      }
    }
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

    ServerCommand serverCommand = (ServerCommand) Commands.get("server");
    if (serverCommand == null) {
      DupeRadar.checkServer(List.of(), server, false);
      return;
    }
    serverCommand.findPlugins(plugins -> {
      if (!isActive() || mc.player == null || mc.getConnection() == null || mc.getCurrentServer() == null) return;
      List<DupeRadar.RadarPluginSnapshot> snapshots = plugins.stream()
        .filter(name -> name != null && !name.isBlank())
        .distinct()
        .map(DupeRadar::pluginSnapshot)
        .toList();
      if (snapshots.isEmpty()) {
        info("Dupe radar auto-check started (no server plugins detected).");
      } else {
        info("Dupe radar auto-check started with %d detected plugin(s).", snapshots.size());
      }
      DupeRadar.checkServer(snapshots, server, false);
    });
  }

  private void openScreen() {
    mc.gui.setScreen(new DupeRadarScreen(GuiThemes.get(), this));
  }
}
