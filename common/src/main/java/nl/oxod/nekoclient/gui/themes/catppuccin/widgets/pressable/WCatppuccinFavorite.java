/*
 * Copyright (c) NekoClient.
 */

package nl.oxod.nekoclient.gui.themes.catppuccin.widgets.pressable;

import nl.oxod.nekoclient.gui.widgets.pressable.WFavorite;
import nl.oxod.nekoclient.utils.render.color.Color;
import nl.oxod.nekoclient.gui.themes.catppuccin.CatppuccinWidget;

public class WCatppuccinFavorite extends WFavorite implements CatppuccinWidget {
    public WCatppuccinFavorite(boolean checked) {
        super(checked);
    }

    @Override
    protected Color getColor() {
        return theme().yellowColor();
    }
}