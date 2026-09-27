/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.gui.themes.neko.widgets;

import nl.oxod.nekoclient.gui.renderer.GuiRenderer;
import nl.oxod.nekoclient.gui.themes.neko.NekoGuiTheme;
import nl.oxod.nekoclient.gui.themes.neko.NekoWidget;
import nl.oxod.nekoclient.gui.widgets.WVerticalSeparator;
import nl.oxod.nekoclient.utils.render.color.Color;

public class WNekoVerticalSeparator extends WVerticalSeparator implements NekoWidget {
  @Override
  protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
    NekoGuiTheme theme = theme();
    Color colorEdges = theme.separatorEdges.get();
    Color colorCenter = theme.separatorCenter.get();

    double s = theme.scale(1);
    double offsetX = Math.round(width / 2.0);

    renderer.quad(x + offsetX, y, s, height / 2, colorEdges, colorEdges, colorCenter, colorCenter);
    renderer.quad(x + offsetX, y + height / 2, s, height / 2, colorCenter, colorCenter, colorEdges, colorEdges);
  }
}
