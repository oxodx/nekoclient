/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.gui.themes.neko.widgets;

import nl.oxod.nekoclient.gui.renderer.GuiRenderer;
import nl.oxod.nekoclient.gui.themes.neko.NekoWidget;
import nl.oxod.nekoclient.gui.widgets.WTooltip;
import nl.oxod.nekoclient.renderer.text.TextRenderer;
import nl.oxod.nekoclient.renderer.text.VanillaTextRenderer;

public class WNekoTooltip extends WTooltip implements NekoWidget {
  public WNekoTooltip(String text) {
    super(text);
  }

  @Override
  protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
    if (TextRenderer.get() == VanillaTextRenderer.INSTANCE) {
      renderer.fill(this, theme().surfaceHighColor.get());
    } else {
      renderer.quad(this, theme().surfaceHighColor.get());
    }
    renderer.quad(x, y, theme().scale(2), height, theme().accentColor.get());
  }
}
