/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.gui.themes.neko.widgets;

import nl.oxod.nekoclient.gui.renderer.GuiRenderer;
import nl.oxod.nekoclient.gui.themes.neko.NekoWidget;
import nl.oxod.nekoclient.gui.widgets.WMultiLabel;
import nl.oxod.nekoclient.utils.render.color.Color;

public class WNekoMultiLabel extends WMultiLabel implements NekoWidget {
  public WNekoMultiLabel(String text, boolean title, double maxWidth) {
    super(text, title, maxWidth);
  }

  @Override
  protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
    double h = theme.textHeight(title);
    Color defaultColor = theme().textColor.get();

    for (int i = 0; i < lines.size(); i++) {
      renderer.text(lines.get(i), x, y + h * i, color != null ? color : defaultColor, false);
    }
  }
}
