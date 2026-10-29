/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.gui.themes.neko.widgets;

import nl.oxod.nekoclient.gui.renderer.GuiRenderer;
import nl.oxod.nekoclient.gui.themes.neko.NekoWidget;
import nl.oxod.nekoclient.gui.widgets.WWidget;
import nl.oxod.nekoclient.gui.widgets.containers.WWindow;

public class WNekoWindow extends WWindow implements NekoWidget {
  public WNekoWindow(WWidget icon, String title) {
    super(icon, title);
  }

  @Override
  protected WHeader header(WWidget icon) {
    return new WNekoHeader(icon);
  }

  @Override
  protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
    if (expanded || animProgress > 0) {
      double radius = theme().scale(theme().cornerRadius.get() + 2);
      double shadow = theme().scale(theme().shadowSize.get());
      if (shadow > 0) renderer.shadow(x, y, width, height, radius, shadow, theme().shadowColor.get());
      renderer.roundedQuad(x, y, width, height, radius, theme().backgroundColor.get());
    }
  }

  private class WNekoHeader extends WHeader {
    public WNekoHeader(WWidget icon) {
      super(icon);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
      renderer.roundedQuad(this, theme().scale(theme().cornerRadius.get() + 2), theme().surfaceHighColor.get());
      double accentWidth = theme().scale(3);
      renderer.roundedQuad(x + theme().scale(2), y + theme().scale(3), accentWidth,
        height - theme().scale(6), accentWidth / 2, theme().accentColor.get());
      renderer.quad(x, y + height - theme().scale(1), width, theme().scale(1), theme().outlineColor.get());
    }
  }
}
