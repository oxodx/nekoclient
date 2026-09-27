/*
 * Copyright (c) NekoClient.
 */

package nl.oxod.nekoclient.gui.themes.catppuccin.widgets;

import nl.oxod.nekoclient.gui.widgets.WTopBar;
import nl.oxod.nekoclient.utils.render.color.Color;
import nl.oxod.nekoclient.gui.themes.catppuccin.CatppuccinWidget;

public class WCatppuccinTopBar extends WTopBar implements CatppuccinWidget {
  @Override
  protected Color getButtonColor(boolean pressed, boolean hovered) {
    return theme().backgroundColor.get(pressed, hovered);
  }

  @Override
  protected Color getNameColor() {
    return theme().textColor();
  }
}
