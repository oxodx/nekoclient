/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.gui.widgets;

import nl.oxod.nekoclient.gui.widgets.pressable.WPressable;
import nl.oxod.nekoclient.utils.render.color.Color;
import net.minecraft.client.input.MouseButtonEvent;

public abstract class WLabel extends WPressable {
  public Color color;

  protected String text;
  protected boolean title;
  private String fitted;
  private String fittedSource;
  private double fittedWidth = -1;

  public WLabel(String text, boolean title) {
    this.text = text;
    this.title = title;
  }

  @Override
  protected void onCalculateSize() {
    width = theme.textWidth(text, text.length(), title);
    height = theme.textHeight(title);
  }

  @Override
  public boolean onMouseClicked(MouseButtonEvent click, boolean doubled) {
    if (action != null) return super.onMouseClicked(click, doubled);
    return false;
  }

  @Override
  public boolean onMouseReleased(MouseButtonEvent click) {
    if (action != null) return super.onMouseReleased(click);
    return false;
  }

  public void set(String text) {
    if (Math.round(theme.textWidth(text, text.length(), title)) != width) invalidate();

    this.text = text;
  }

  public String get() {
    return text;
  }

  protected String fittedText() {
    if (text.equals(fittedSource) && fittedWidth == width) return fitted;
    fittedSource = text;
    fittedWidth = width;
    if (theme.textWidth(text, text.length(), title) <= width) return fitted = text;
    String suffix = "...";
    double remaining = width - theme.textWidth(suffix, suffix.length(), title);
    if (remaining <= 0) return fitted = "";
    int low = 0, high = text.codePointCount(0, text.length());
    while (low < high) {
      int middle = (low + high + 1) / 2;
      int end = text.offsetByCodePoints(0, middle);
      if (theme.textWidth(text, end, title) <= remaining) low = middle;
      else high = middle - 1;
    }
    return fitted = text.substring(0, text.offsetByCodePoints(0, low)) + suffix;
  }

  public WLabel color(Color color) {
    this.color = color;
    return this;
  }
}
