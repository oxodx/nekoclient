/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.gui.themes.neko.widgets;

import nl.oxod.nekoclient.gui.renderer.GuiRenderer;
import nl.oxod.nekoclient.gui.widgets.WQuad;
import nl.oxod.nekoclient.utils.render.color.Color;

public class WNekoQuad extends WQuad {
  public WNekoQuad(Color color) {
    super(color);
  }

  @Override
  protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
    renderer.quad(x, y, width, height, color);
  }
}
