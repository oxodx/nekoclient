/*
 * Copyright (c) NekoClient.
 */

package nl.oxod.nekoclient.gui.themes.catppuccin.widgets.pressable;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import nl.oxod.nekoclient.gui.themes.catppuccin.CatppuccinWidget;

public class WCatppuccinMinus extends WMinus implements CatppuccinWidget {
    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        double pad = pad();
        double s = theme.scale(3);

        renderBackground(renderer, this, pressed, mouseOver);
        renderer.quad(x + pad, y + height / 2 - s / 2, width - pad * 2, s, theme().redColor());
    }
}