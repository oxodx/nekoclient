package nl.oxod.nekoclient.systems.hud.elements;

import nl.oxod.nekoclient.NekoClient;
import nl.oxod.nekoclient.settings.*;
import nl.oxod.nekoclient.systems.hud.*;
import nl.oxod.nekoclient.utils.render.color.SettingColor;

public class LiquidWatermarkHud extends HudElement {
  public static final HudElementInfo<LiquidWatermarkHud> INFO = new HudElementInfo<>(Hud.GROUP,
    "liquid-watermark", "Compact branded watermark inspired by LiquidBounce.", LiquidWatermarkHud::new);

  private final SettingGroup sgGeneral = settings.getDefaultGroup();
  private final Setting<SettingColor> background = sgGeneral.add(new ColorSetting.Builder()
    .name("background-color").defaultValue(new SettingColor(0, 0, 0, 165)).build());
  private final Setting<SettingColor> accent = sgGeneral.add(new ColorSetting.Builder()
    .name("accent-color").defaultValue(new SettingColor(70, 119, 255)).build());
  private final Setting<SettingColor> text = sgGeneral.add(new ColorSetting.Builder()
    .name("text-color").defaultValue(new SettingColor(255, 255, 255)).build());

  public LiquidWatermarkHud() {
    super(INFO);
  }

  @Override
  public void tick(HudRenderer renderer) {
    String version = NekoClient.VERSION.toString();
    setSize(renderer.textWidth("NEKO", true) + renderer.textWidth(version, false) + 25,
      renderer.textHeight(true) + 10);
  }

  @Override
  public void render(HudRenderer renderer) {
    String version = NekoClient.VERSION.toString();
    renderer.roundedQuad(x, y, getWidth(), getHeight(), 5, background.get());
    renderer.roundedQuad(x, y, 6, getHeight(), 5, accent.get());
    double nameX = x + 11;
    double textY = y + 5;
    double end = renderer.text("NEKO", nameX, textY, text.get(), true);
    renderer.text(version, end + 6, textY, accent.get(), false);
  }
}
