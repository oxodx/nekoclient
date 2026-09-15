/*
 * Copyright (c) NekoClient.
 */

package nl.oxod.nekoclient.gui.themes.catppuccin.widgets;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.widgets.WTooltip;
import nl.oxod.nekoclient.gui.themes.catppuccin.CatppuccinWidget;

public class WCatppuccinTooltip extends WTooltip implements CatppuccinWidget {
    public WCatppuccinTooltip(String text) {
        super(text);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        renderer.quad(this, theme().backgroundColor.get());
    }
}