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
    double s = theme.scale(2);

    renderer.quad(widget.x + s, widget.y + s, widget.width - s * 2, widget.height - s * 2, backgroundColor);

    renderer.quad(widget.x, widget.y, widget.width, s, outlineColor);
    renderer.quad(widget.x, widget.y + widget.height - s, widget.width, s, outlineColor);
    renderer.quad(widget.x, widget.y + s, s, widget.height - s * 2, outlineColor);
    renderer.quad(widget.x + widget.width - s, widget.y + s, s, widget.height - s * 2, outlineColor);
  }

  default void renderBackground(GuiRenderer renderer, WWidget widget, boolean pressed, boolean mouseOver) {
    NekoGuiTheme theme = theme();
    renderBackground(renderer, widget, theme.outlineColor.get(pressed, mouseOver), theme.backgroundColor.get(pressed, mouseOver));
  }
}
