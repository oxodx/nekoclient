/*
 * Copyright (c) NekoClient.
 */

package nl.oxod.nekoclient.gui.themes.catppuccin.widgets;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.widgets.WHorizontalSeparator;
import nl.oxod.nekoclient.gui.themes.catppuccin.CatppuccinGuiTheme;
import nl.oxod.nekoclient.gui.themes.catppuccin.CatppuccinWidget;

public class WCatppuccinHorizontalSeparator extends WHorizontalSeparator implements CatppuccinWidget {
    public WCatppuccinHorizontalSeparator(String text) {
        super(text);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        if (text == null) renderWithoutText(renderer);
        else renderWithText(renderer);
    }

    private void renderWithoutText(GuiRenderer renderer) {
        CatppuccinGuiTheme theme = theme();
        double s = theme.scale(1);
        double w = width / 2;

        renderer.quad(x, y + s, w, s, theme.overlay0Color(), theme.textColor());
        renderer.quad(x + w, y + s, w, s, theme.textColor(), theme.overlay0Color());
    }

    private void renderWithText(GuiRenderer renderer) {
        CatppuccinGuiTheme theme = theme();
        double s = theme.scale(2);
        double h = theme.scale(1);

        double textStart = Math.round(width / 2.0 - textWidth / 2.0 - s);
        double textEnd = s + textStart + textWidth + s;

        double offsetY = Math.round(height / 2.0);

        renderer.quad(x, y + offsetY, textStart, h, theme.overlay0Color(), theme.overlay2Color());
        renderer.text(text, x + textStart + s, y, theme.textColor(), false);
        renderer.quad(x + textEnd, y + offsetY, width - textEnd, h, theme.overlay2Color(), theme.overlay0Color());
    }
}