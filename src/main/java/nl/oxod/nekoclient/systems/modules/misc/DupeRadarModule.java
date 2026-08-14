package nl.oxod.nekoclient.systems.modules.misc;

import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import nl.oxod.nekoclient.gui.DupeRadarScreen;
import nl.oxod.nekoclient.util.DupeRadar;

import java.util.List;

public class DupeRadarModule extends Module {
  private final SettingGroup sgGeneral = settings.getDefaultGroup();

  public final Setting<Boolean> autoCheckOnJoin = sgGeneral.add(new BoolSetting.Builder()
    .name("auto-check-on-join")
    .description("Automatically check the server you join against DupeDB.")
    .defaultValue(true)
    .build()
  );

  public DupeRadarModule() {
    super(Categories.Misc, "dupe-radar", "Checks your server and detected plugins against DupeDB for known dupes.");
    runInMainMenu = true;
  }

  @Override
  public void onActivate() {
    mc.gui.setScreen(new DupeRadarScreen(GuiThemes.get(), this));
  }

  @Override
  public void onDeactivate() {
    if (mc.gui.screen() instanceof DupeRadarScreen) {
      mc.gui.setScreen(null);
    }
  }

  @EventHandler
  private void onGameJoined(GameJoinedEvent event) {
    if (!autoCheckOnJoin.get() || mc.player == null || mc.getCurrentServer() == null) return;
    String ip = mc.getCurrentServer().ip;
    if (ip == null || ip.isBlank()) return;
    ServerAddress serverAddress = ServerAddress.parseString(ip);
    if (serverAddress == null || serverAddress.getHost() == null || serverAddress.getHost().isBlank()) return;
    String host = serverAddress.getHost().toLowerCase();
    boolean isIp = host.matches("\\d{1,3}(?:\\.\\d{1,3}){3}");
    DupeRadar.RadarServerSnapshot server = new DupeRadar.RadarServerSnapshot(
      List.of(new DupeRadar.RadarServerIdentity(host, isIp)));
    DupeRadar.checkServer(List.of(), server, false);
  }
}
