/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.gui.widgets.containers;

import nl.oxod.nekoclient.gui.renderer.GuiRenderer;
import nl.oxod.nekoclient.gui.utils.Cell;
import nl.oxod.nekoclient.gui.utils.WindowConfig;
import nl.oxod.nekoclient.gui.widgets.WWidget;
import nl.oxod.nekoclient.gui.widgets.pressable.WTriangle;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.Mth;
import nl.oxod.nekoclient.gui.engine.UiMotion;

import java.util.function.Consumer;

import static com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT;
import static nl.oxod.nekoclient.utils.Utils.getWindowHeight;
import static nl.oxod.nekoclient.utils.Utils.getWindowWidth;

public abstract class WWindow extends WVerticalList {
  public double padding = 8;
  /** Optional engine-level panel constraints. A negative value keeps content sizing. */
  public double panelWidth = -1;
  public double maxViewHeight = -1;
  public Consumer<WContainer> beforeHeaderInit;
  public String id;

  public final WWidget icon;
  protected final String title;

  protected WHeader header;
  public WView view;

  protected boolean dragging;
  protected boolean expanded = true;
  protected boolean dragged;

  protected double animProgress = 1;

  protected boolean moved = false;
  protected double movedX, movedY;

  public WWindow(WWidget icon, String title) {
    this.icon = icon;
    this.title = title;
  }

  @Override
  public void init() {
    header = header(icon);
    header.theme = theme;
    super.add(header).expandWidgetX().widget();

    view = super.add(theme.view()).expandX().pad(padding).widget();
    if (maxViewHeight >= 0) view.maxHeight = Math.min(view.maxHeight, maxViewHeight);

    if (id != null) {
      expanded = theme.getWindowConfig(id).expanded;
      animProgress = expanded ? 1 : 0;
      view.restoreScroll(theme.getWindowConfig(id).scroll);
      view.onScroll = value -> theme.getWindowConfig(id).scroll = value;
    }
  }

  protected abstract WHeader header(WWidget icon);

  @Override
  protected void onCalculateSize() {
    super.onCalculateSize();
    if (panelWidth >= 0) width = Math.min(panelWidth, availableWidth);
    height = header.height + (height - header.height) * animProgress;
  }

  @Override
  public double contentWidth(double available) {
    return panelWidth >= 0 ? Math.min(panelWidth, available) : available;
  }

  @Override
  public boolean clipsChildren() { return true; }

  @Override
  public boolean isStackingContext() { return true; }

  public boolean isDragging() { return dragging; }

  @Override
  public <T extends WWidget> Cell<T> add(T widget) {
    return view.add(widget);
  }

  @Override
  public void clear() {
    view.clear();
  }

  public void setExpanded(boolean expanded) {
    this.expanded = expanded;
    invalidate();

    if (id != null) {
      WindowConfig config = theme.getWindowConfig(id);
      config.expanded = expanded;
    }
  }

  @Override
  protected void onCalculateWidgetPositions() {
    if (id != null) {
      WindowConfig config = theme.getWindowConfig(id);

      if (config.x != -1) {
        x = config.x;

        if (x + width > getWindowWidth()) {
          x = Math.max(0, getWindowWidth() - width);
        }
      }

      if (config.y != -1) {
        y = config.y;

        if (y + height > getWindowHeight()) {
          y = Math.max(0, getWindowHeight() - height);
        }
      }
    }

    super.onCalculateWidgetPositions();

    if (moved) {
      movedX = Mth.clamp(movedX, 0, Math.max(0, getWindowWidth() - width));
      movedY = Mth.clamp(movedY, 0, Math.max(0, getWindowHeight() - height));
      move(movedX - x, movedY - y);
    }
  }

  @Override
  public boolean render(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
    if (!visible) return true;

    double previous = animProgress;
    animProgress = UiMotion.approach(animProgress, expanded ? 1 : 0, 20, delta);
    if (previous != animProgress) invalidate();

    boolean scissor = (animProgress != 0 && animProgress != 1) || (expanded && animProgress != 1);
    if (scissor) renderer.scissorStart(x, y, width, height);
    boolean toReturn = super.render(renderer, mouseX, mouseY, delta);
    if (scissor) renderer.scissorEnd();

    return toReturn;
  }

  @Override
  protected void renderWidget(WWidget widget, GuiRenderer renderer, double mouseX, double mouseY, double delta) {
    if (expanded || animProgress > 0 || widget instanceof WHeader) {
      widget.render(renderer, mouseX, mouseY, delta);
    }

  }

  @Override
  protected boolean propagateEvents(WWidget widget) {
    return widget instanceof WHeader || expanded;
  }

  @Override
  public boolean isChildActive(WWidget widget) {
    return widget.visible && (widget instanceof WHeader || expanded);
  }

  protected abstract class WHeader extends WContainer {
    private final WWidget icon;
    private WTriangle triangle;
    private WHorizontalList list;

    public WHeader(WWidget icon) {
      this.icon = icon;
    }

    @Override
    public void init() {
      if (icon != null) {
        createList();
        add(icon).centerY();
      }

      if (beforeHeaderInit != null) {
        createList();
        beforeHeaderInit.accept(this);
      }

      add(theme.label(title, true)).expandCellX().center().pad(4);

      triangle = add(theme.triangle()).pad(4).right().centerY().widget();
      triangle.action = () -> setExpanded(!expanded);
    }

    private void createList() {
      list = add(theme.horizontalList()).expandX().widget();
      list.spacing = 0;
    }

    @Override
    public <T extends WWidget> Cell<T> add(T widget) {
      if (list != null) return list.add(widget);
      return super.add(widget);
    }

    @Override
    protected void onCalculateSize() {
      width = 0;
      height = 0;

      for (Cell<?> cell : cells) {
        double w = cell.padLeft() + cell.widget().width + cell.padRight();
        if (cell.widget() instanceof WTriangle) w *= 2;

        width += w;
        height = Math.max(height, cell.padTop() + cell.widget().height + cell.padBottom());
      }
    }

    @Override
    public boolean onMouseClicked(MouseButtonEvent click, boolean doubled) {
      if (mouseOver && !doubled) {
        if (WWindow.this.parent instanceof WContainer container) container.bringToFront(WWindow.this);
        if (click.button() == MOUSE_BUTTON_RIGHT) setExpanded(!expanded);
        else {
          dragging = true;
          dragged = false;
        }

        return true;
      }

      return false;
    }

    @Override
    public boolean onMouseReleased(MouseButtonEvent click) {
      if (dragging) {
        dragging = false;

        if (!dragged) setExpanded(!expanded);
        else {
          double snappedX = Mth.clamp(theme.snapWindowPosition(x), 0, Math.max(0, getWindowWidth() - width));
          double snappedY = Mth.clamp(theme.snapWindowPosition(y), 0, Math.max(0, getWindowHeight() - WWindow.this.height));
          WWindow.this.move(snappedX - x, snappedY - y);
          movedX = WWindow.this.x;
          movedY = WWindow.this.y;

          if (id != null) {
            WindowConfig config = theme.getWindowConfig(id);
            config.x = movedX;
            config.y = movedY;
          }
        }
      }

      return false;
    }

    @Override
    public void onMouseMoved(double mouseX, double mouseY, double lastMouseX, double lastMouseY) {
      if (dragging && (mouseX != lastMouseX || mouseY != lastMouseY)) {
        WWindow.this.move(mouseX - lastMouseX, mouseY - lastMouseY);

        moved = true;
        movedX = x;
        movedY = y;

        if (id != null) {
          WindowConfig config = theme.getWindowConfig(id);

          config.x = x;
          config.y = y;
        }

        dragged = true;
      }
    }

    @Override
    public boolean render(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
      triangle.rotation = (1 - animProgress) * -90;

      return super.render(renderer, mouseX, mouseY, delta);
    }

    @Override
    public void cancelPointer() {
      dragging = false;
      super.cancelPointer();
    }
  }
}
