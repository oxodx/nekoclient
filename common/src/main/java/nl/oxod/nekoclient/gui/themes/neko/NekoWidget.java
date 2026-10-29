/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.gui.themes.neko;

import nl.oxod.nekoclient.gui.renderer.GuiRenderer;
import nl.oxod.nekoclient.gui.utils.BaseWidget;
import nl.oxod.nekoclient.gui.widgets.WWidget;
import nl.oxod.nekoclient.utils.render.color.Color;

public interface NekoWidget extends BaseWidget {
  default NekoGuiTheme theme() {
    return (NekoGuiTheme) getTheme();
  }

  default void renderBackground(GuiRenderer renderer, WWidget widget, Color outlineColor, Color backgroundColor) {
    NekoGuiTheme theme = theme();
    double s = theme.scale(1);
    double radius = theme.scale(theme.cornerRadius.get());
    renderer.roundedOutline(widget.x, widget.y, widget.width, widget.height, radius, s,
      outlineColor, backgroundColor);
  }

  default void renderBackground(GuiRenderer renderer, WWidget widget, boolean pressed, boolean mouseOver) {
    NekoGuiTheme theme = theme();
    Color surface = pressed || mouseOver ? theme.surfaceHighColor.get() : theme.surfaceColor.get();
    renderBackground(renderer, widget, theme.outlineColor.get(pressed, mouseOver), surface);
  }
}
