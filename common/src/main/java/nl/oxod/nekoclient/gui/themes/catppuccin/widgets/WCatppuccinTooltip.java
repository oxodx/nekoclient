/*
 * Copyright (c) NekoClient.
 */

package nl.oxod.nekoclient.gui.themes.catppuccin.widgets;

import nl.oxod.nekoclient.gui.renderer.GuiRenderer;
import nl.oxod.nekoclient.gui.widgets.WTooltip;
import nl.oxod.nekoclient.gui.themes.catppuccin.CatppuccinWidget;
import nl.oxod.nekoclient.renderer.text.TextRenderer;
import nl.oxod.nekoclient.renderer.text.VanillaTextRenderer;

public class WCatppuccinTooltip extends WTooltip implements CatppuccinWidget {
  public WCatppuccinTooltip(String text) {
    super(text);
  }

  @Override
  protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
    if (TextRenderer.get() == VanillaTextRenderer.INSTANCE) {
      renderer.fill(this, theme().backgroundColor.get());
    } else {
      renderer.quad(this, theme().backgroundColor.get());
    }
  }
}
