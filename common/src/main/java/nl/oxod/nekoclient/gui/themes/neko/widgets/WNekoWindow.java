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
      renderer.quad(x, y + header.height, width, height - header.height, theme().backgroundColor.get());
      double s = theme().scale(1);
      renderer.quad(x, y + header.height, s, height - header.height, theme().outlineColor.get());
      renderer.quad(x + width - s, y + header.height, s, height - header.height, theme().outlineColor.get());
    }
  }

  private class WNekoHeader extends WHeader {
    public WNekoHeader(WWidget icon) {
      super(icon);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
      renderer.quad(this, theme().surfaceHighColor.get());
      renderer.quad(x, y, theme().scale(3), height, theme().accentColor.get());
      renderer.quad(x, y + height - theme().scale(1), width, theme().scale(1), theme().outlineColor.get());
    }
  }
}
