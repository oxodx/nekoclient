/*
 * Copyright (c) NekoClient.
 */

package nl.oxod.nekoclient.gui.themes.catppuccin.widgets;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.widgets.WVerticalSeparator;
import meteordevelopment.meteorclient.utils.render.color.Color;
import nl.oxod.nekoclient.gui.themes.catppuccin.CatppuccinGuiTheme;
import nl.oxod.nekoclient.gui.themes.catppuccin.CatppuccinWidget;

public class WCatppuccinVerticalSeparator extends WVerticalSeparator implements CatppuccinWidget {
    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        CatppuccinGuiTheme theme = theme();
        Color colorEdges = theme.overlay0Color();
        Color colorCenter = theme.overlay2Color();

        double s = theme.scale(1);
        double offsetX = Math.round(width / 2.0);

        renderer.quad(x + offsetX, y, s, height / 2, colorEdges, colorEdges, colorCenter, colorCenter);
        renderer.quad(x + offsetX, y + height / 2, s, height / 2, colorCenter, colorCenter, colorEdges, colorEdges);
    }
}