/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.gui.widgets;

import nl.oxod.nekoclient.gui.GuiTheme;
import nl.oxod.nekoclient.gui.renderer.GuiRenderer;
import nl.oxod.nekoclient.gui.utils.BaseWidget;
import nl.oxod.nekoclient.gui.widgets.containers.WView;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import nl.oxod.nekoclient.gui.engine.UiLayout;
import nl.oxod.nekoclient.gui.engine.UiRect;

public abstract class WWidget implements BaseWidget {
  public boolean visible = true;
  public GuiTheme theme;

  public double x, y;
  public double width, height;
  public double minWidth;
  public double availableWidth = Double.POSITIVE_INFINITY;

  public WWidget parent;
  public String tooltip;

  public boolean mouseOver;
  public boolean focused;
  protected boolean instantTooltips;
  protected double mouseOverTimer;

  public void init() {
  }

  public void move(double deltaX, double deltaY) {
    x = Math.round(x + deltaX);
    y = Math.round(y + deltaY);
  }

  @Override
  public GuiTheme getTheme() {
    return theme;
  }

  public double pad() {
    return theme.pad();
  }

  // Layout

  public void calculateSize() {
    UiLayout.measure(this, availableWidth);
  }

  public final void measureSelf() {
    onCalculateSize();

    double minWidth = theme.scale(this.minWidth);
    if (width < minWidth) width = minWidth;

    width = Math.max(0, Math.round(Math.min(width, availableWidth)));
    height = Math.max(0, Math.round(height));
  }

  public double contentWidth(double available) { return available; }

  public UiRect bounds() { return new UiRect(x, y, width, height); }

  public boolean clipsChildren() { return false; }

  public boolean isStackingContext() { return false; }

  public int layer() { return 0; }

  /** Detached overlay content, painted above the ordinary scene and picked first. */
  public WWidget popup() { return null; }

  public void cancelPointer() { setFocused(false); }

  protected void onCalculateSize() {

  }

  public void calculateWidgetPositions() {
    UiLayout.arrange(this);
  }

  public final void arrangeSelf() {
    x = Math.round(x);
    y = Math.round(y);

    onCalculateWidgetPositions();
  }

  protected void onCalculateWidgetPositions() {

  }

  // Rendering

  public boolean render(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
    if (!visible) return true;

    if (mouseOver) {
      mouseOverTimer += delta;

      if ((instantTooltips || mouseOverTimer >= 1) && tooltip != null) {
        WView view = getView();
        if (view == null || view.mouseOver) renderer.tooltip(tooltip);
      }
    } else {
      mouseOverTimer = 0;
    }

    onRender(renderer, mouseX, mouseY, delta);
    return false;
  }

  protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
  }

  // Events

  public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
    return onMouseClicked(click, doubled);
  }

  public boolean onMouseClicked(MouseButtonEvent click, boolean doubled) {
    return false;
  }

  public boolean mouseReleased(MouseButtonEvent click) {
    return onMouseReleased(click);
  }

  public boolean onMouseReleased(MouseButtonEvent click) {
    return false;
  }

  public void mouseMoved(double mouseX, double mouseY, double lastMouseX, double lastMouseY) {
    mouseOver = isOver(mouseX, mouseY);
    onMouseMoved(mouseX, mouseY, lastMouseX, lastMouseY);
  }

  public void onMouseMoved(double mouseX, double mouseY, double lastMouseX, double lastMouseY) {
  }

  public boolean mouseScrolled(double amount) {
    return onMouseScrolled(amount);
  }

  public boolean onMouseScrolled(double amount) {
    return false;
  }

  public boolean keyPressed(KeyEvent input) {
    return onKeyPressed(input);
  }

  public boolean onKeyPressed(KeyEvent input) {
    return false;
  }

  public boolean keyRepeated(KeyEvent input) {
    return onKeyRepeated(input);
  }

  public boolean onKeyRepeated(KeyEvent input) {
    return false;
  }

  public boolean charTyped(CharacterEvent input) {
    return onCharTyped(input);
  }

  public boolean onCharTyped(CharacterEvent input) {
    return false;
  }

  // Other

  public void invalidate() {
    WWidget root = getRoot();
    if (root != null) root.invalidate();
  }

  protected WWidget getRoot() {
    return parent != null ? parent.getRoot() : (this instanceof WRoot ? this : null);
  }

  public WView getView() {
    if (this instanceof WView view) return view;
    return parent != null ? parent.getView() : null;
  }

  public boolean isOver(double x, double y) {
    return x >= this.x && x <= this.x + width && y >= this.y && y <= this.y + height;
  }

  public boolean isFocused() {
    return focused;
  }

  public void setFocused(boolean focused) {
    if (this.focused != focused) this.focused = focused;
  }
}
