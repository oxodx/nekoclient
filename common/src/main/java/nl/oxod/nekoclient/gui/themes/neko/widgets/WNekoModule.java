/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.gui.themes.neko.widgets;

import static com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT;
import static com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT;

import net.minecraft.util.Mth;
import nl.oxod.nekoclient.gui.renderer.GuiRenderer;
import nl.oxod.nekoclient.gui.themes.neko.NekoGuiTheme;
import nl.oxod.nekoclient.gui.themes.neko.NekoWidget;
import nl.oxod.nekoclient.gui.utils.Cell;
import nl.oxod.nekoclient.gui.widgets.containers.WVerticalList;
import nl.oxod.nekoclient.gui.widgets.pressable.WPressable;
import nl.oxod.nekoclient.systems.modules.Module;

/** LiquidBounce-style module row with settings expanding inside its category panel. */
public class WNekoModule extends WVerticalList implements NekoWidget {
  private final Module module;
  private final String title;
  private WModuleHeader header;
  private WVerticalList settingsRoot;
  private Cell<WVerticalList> settingsCell;
  private boolean settingsExpanded;

  public WNekoModule(Module module, String title) {
    this.module = module;
    this.title = title;
    spacing = 0;
    tooltip = module.description;
  }

  @Override
  public void init() {
    header = super.add(new WModuleHeader()).expandX().widget();
  }

  private void toggleSettings() {
    settingsExpanded = !settingsExpanded;
    if (settingsExpanded) {
      settingsRoot = new WVerticalList();
      settingsRoot.spacing = 0;
      settingsCell = super.add(settingsRoot).expandX().pad(4);
      settingsRoot.add(theme.settings(module.settings)).expandX();
    } else if (settingsCell != null) {
      remove(settingsCell);
      settingsCell = null;
      settingsRoot = null;
    }
    invalidate();
  }

  @Override
  protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
    if (!settingsExpanded || settingsRoot == null) return;
    NekoGuiTheme theme = (NekoGuiTheme) this.theme;
    double headerHeight = header.height;
    renderer.quad(x, y + headerHeight, width, height - headerHeight, theme.backgroundColor.get());
    renderer.quad(x, y + headerHeight, theme.scale(2), height - headerHeight, theme.accentColor.get());
    module.settings.tick(settingsRoot, theme);
  }

  private class WModuleHeader extends WPressable implements NekoWidget {
    private double titleWidth;
    private double hoverProgress;
    private double activeProgress;

    WModuleHeader() {
      tooltip = module.description;
      activeProgress = module.isActive() ? 1 : 0;
    }

    @Override
    public double pad() {
      return theme.scale(5);
    }

    @Override
    protected void onCalculateSize() {
      if (titleWidth == 0) titleWidth = theme.textWidth(title);
      double pad = pad();
      width = Math.max(theme.scale(125), pad * 2 + titleWidth + theme.scale(20));
      height = pad * 2 + theme.textHeight();
    }

    @Override
    protected void onPressed(int button) {
      if (button == MOUSE_BUTTON_LEFT) module.toggle();
      else if (button == MOUSE_BUTTON_RIGHT) toggleSettings();
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
      NekoGuiTheme theme = theme();
      hoverProgress = Mth.clamp(hoverProgress + delta * 8 * (mouseOver ? 1 : -1), 0, 1);
      activeProgress = Mth.clamp(activeProgress + delta * 8 * (module.isActive() ? 1 : -1), 0, 1);

      renderer.roundedQuad(this, theme.scale(2), theme.surfaceColor.get());
      if (hoverProgress > 0) {
        renderer.setAlpha(hoverProgress);
        renderer.roundedQuad(this, theme.scale(2), theme.surfaceHighColor.get());
        renderer.setAlpha(1);
      }

      renderer.text(title, x + (width - titleWidth) / 2, y + pad(),
        activeProgress > 0.5 ? theme.accentColor.get() : theme.textSecondaryColor.get(), false);

      String arrow = settingsExpanded ? "v" : ">";
      renderer.text(arrow, x + width - pad() - theme.textWidth(arrow), y + pad(),
        settingsExpanded ? theme.textColor.get() : theme.textSecondaryColor.get(), false);
    }
  }
}
