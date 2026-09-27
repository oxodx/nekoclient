/*
 * Copyright (c) NekoClient.
 */

package nl.oxod.nekoclient.gui.themes.catppuccin;

import meteordevelopment.meteorclient.utils.render.color.SettingColor;

public enum CatppuccinAccent {
    Rosewater,
    Flamingo,
    Pink,
    Mauve,
    Red,
    Maroon,
    Peach,
    Yellow,
    Green,
    Teal,
    Sky,
    Sapphire,
    Blue,
    Lavender;

    public SettingColor get(CatppuccinFlavor flavor) {
        return flavor.get(CatppuccinColor.valueOf(name()));
    }
}