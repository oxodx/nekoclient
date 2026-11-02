/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.gui.themes.neko.widgets;

import nl.oxod.nekoclient.gui.renderer.GuiRenderer;
import nl.oxod.nekoclient.gui.themes.neko.NekoWidget;
import nl.oxod.nekoclient.gui.widgets.WWidget;
import nl.oxod.nekoclient.gui.widgets.WTopBar;
import nl.oxod.nekoclient.utils.render.color.Color;

public class WNekoTopBar extends WTopBar implements NekoWidget {
  @Override
  protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
    double inset = theme().scale(5);
    double radius = theme().scale(theme().cornerRadius.get() + 3);
    double shadow = theme().scale(theme().shadowSize.get());
    if (shadow > 0) renderer.shadow(x - inset, y - inset, width + inset * 2, height + inset * 2,
      radius, shadow, theme().shadowColor.get());
    renderer.roundedOutline(x - inset, y - inset, width + inset * 2, height + inset * 2,
      radius, theme().scale(1), theme().outlineColor.get(), theme().backgroundColor.get());
  }

  @Override
  protected void renderButton(GuiRenderer renderer, WWidget widget, boolean active, boolean pressed,
                              boolean hovered, double pad, String name) {
    double radius = theme().scale(theme().cornerRadius.get());
    if (active) renderer.roundedOutline(widget.x, widget.y, widget.width, widget.height,
      widget.height / 2, theme().scale(1), theme().accentColor.get(), theme().activeSurfaceColor.get());
    else if (pressed || hovered) renderer.roundedQuad(widget.x, widget.y, widget.width, widget.height,
      widget.height / 2, getButtonColor(pressed, hovered));

    Color text = active ? theme().titleTextColor.get() : theme().textSecondaryColor.get();
    renderer.text(name, widget.x + pad, widget.y + pad, text, false);
  }

  @Override
  protected Color getButtonColor(boolean pressed, boolean hovered) {
    if (pressed) return theme().activeSurfaceColor.get();
    if (hovered) return theme().surfaceHighColor.get();
    return theme().surfaceColor.get();
  }

  @Override
  protected Color getNameColor() {
    return theme().textColor.get();
  }
}
