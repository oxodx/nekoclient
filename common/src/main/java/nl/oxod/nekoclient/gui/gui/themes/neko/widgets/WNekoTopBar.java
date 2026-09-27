/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.gui.themes.neko.widgets;

import nl.oxod.nekoclient.gui.themes.neko.NekoWidget;
import nl.oxod.nekoclient.gui.widgets.WTopBar;
import nl.oxod.nekoclient.utils.render.color.Color;

public class WNekoTopBar extends WTopBar implements NekoWidget {
    @Override
    protected Color getButtonColor(boolean pressed, boolean hovered) {
        return theme().backgroundColor.get(pressed, hovered);
    }

    @Override
    protected Color getNameColor() {
        return theme().textColor.get();
    }
}
