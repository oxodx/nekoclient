package nl.oxod.nekoclient.systems.hud.elements;

import java.util.ArrayList;
import java.util.List;
import nl.oxod.nekoclient.settings.*;
import nl.oxod.nekoclient.systems.hud.*;
import nl.oxod.nekoclient.systems.modules.Module;
import nl.oxod.nekoclient.systems.modules.Modules;
import nl.oxod.nekoclient.utils.render.color.SettingColor;

public class BindsHud extends HudElement {
  public static final HudElementInfo<BindsHud> INFO = new HudElementInfo<>(Hud.GROUP,
    "binds", "Displays module key bindings in a LiquidBounce-style panel.", BindsHud::new);

  private final SettingGroup sgGeneral = settings.getDefaultGroup();
  private final Setting<SettingColor> header = sgGeneral.add(new ColorSetting.Builder()
    .name("header-color").defaultValue(new SettingColor(0, 0, 0, 175)).build());
  private final Setting<SettingColor> body = sgGeneral.add(new ColorSetting.Builder()
    .name("body-color").defaultValue(new SettingColor(0, 0, 0, 130)).build());
  private final Setting<SettingColor> accent = sgGeneral.add(new ColorSetting.Builder()
    .name("accent-color").defaultValue(new SettingColor(70, 119, 255)).build());
  private final Setting<SettingColor> text = sgGeneral.add(new ColorSetting.Builder()
    .name("text-color").defaultValue(new SettingColor(255, 255, 255)).build());

  private final List<Module> bound = new ArrayList<>();
  private double rowHeight;

  public BindsHud() {
    super(INFO);
  }

  @Override
  public void tick(HudRenderer renderer) {
    bound.clear();
    double width = 75;
    rowHeight = renderer.textHeight(false) + 4;
    for (Module module : Modules.get().getAll()) {
      if (!module.keybind.isSet()) continue;
      bound.add(module);
      width = Math.max(width, renderer.textWidth(module.title, false) + renderer.textWidth("[" + module.keybind + "]", false) + 24);
    }
    setSize(width, rowHeight + 8 + Math.max(1, bound.size()) * rowHeight + 6);
  }

  @Override
  public void render(HudRenderer renderer) {
    double headerHeight = rowHeight + 8;
    renderer.roundedQuad(x, y, getWidth(), getHeight(), 5, body.get());
    renderer.roundedQuad(x, y, getWidth(), headerHeight, 5, header.get());
    renderer.text("Binds", x + 8, y + 4, text.get(), true);

    double rowY = y + headerHeight + 3;
    if (bound.isEmpty()) {
      renderer.text("No key bindings", x + 8, rowY, text.get().copy().a(150), false);
      return;
    }

    for (Module module : bound) {
      renderer.text(module.title, x + 8, rowY, module.isActive() ? accent.get() : text.get(), false);
      String key = "[" + module.keybind + "]";
      renderer.text(key, x + getWidth() - 8 - renderer.textWidth(key, false), rowY,
        module.isActive() ? accent.get() : text.get().copy().a(150), false);
      rowY += rowHeight;
    }
  }
}
