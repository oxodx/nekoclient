/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.gui.themes.neko.widgets;

import nl.oxod.nekoclient.gui.renderer.GuiRenderer;
import nl.oxod.nekoclient.gui.themes.neko.NekoWidget;
import nl.oxod.nekoclient.gui.widgets.WTooltip;

public class WNekoTooltip extends WTooltip implements NekoWidget {
  public WNekoTooltip(String text) {
    super(text);
  }

  @Override
  protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
    renderer.roundedQuad(this, theme().scale(theme().cornerRadius.get()), theme().surfaceHighColor.get());
    double marker = theme().scale(2);
    renderer.roundedQuad(x + marker, y + marker, marker, height - marker * 2,
      marker / 2, theme().accentColor.get());
  }
}
