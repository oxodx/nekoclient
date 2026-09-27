/*
 * Copyright (c) NekoClient.
 */

package nl.oxod.nekoclient.gui.themes.catppuccin.widgets;

import nl.oxod.nekoclient.gui.renderer.GuiRenderer;
import nl.oxod.nekoclient.gui.widgets.WQuad;
import nl.oxod.nekoclient.utils.render.color.Color;

public class WCatppuccinQuad extends WQuad {
  public WCatppuccinQuad(Color color) {
    super(color);
  }

  @Override
  protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
    renderer.quad(x, y, width, height, color);
  }
}
