/*
 * Copyright (c) NekoClient.
 */

package nl.oxod.nekoclient.gui.themes.catppuccin.widgets;

import nl.oxod.nekoclient.gui.renderer.GuiRenderer;
import nl.oxod.nekoclient.gui.widgets.WLabel;
import nl.oxod.nekoclient.gui.themes.catppuccin.CatppuccinWidget;

public class WCatppuccinLabel extends WLabel implements CatppuccinWidget {
  public WCatppuccinLabel(String text, boolean title) {
    super(text, title);
  }

  @Override
  protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
    if (!text.isEmpty()) {
      renderer.text(text, x, y, color != null ? color : theme().textColor(), title);
    }
  }
}
