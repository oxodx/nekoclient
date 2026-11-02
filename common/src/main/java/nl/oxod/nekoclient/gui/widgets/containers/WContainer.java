/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.gui.widgets.containers;

import nl.oxod.nekoclient.gui.renderer.GuiRenderer;
import nl.oxod.nekoclient.gui.utils.Cell;
import nl.oxod.nekoclient.gui.widgets.WWidget;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import nl.oxod.nekoclient.gui.engine.UiCompositor;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;

import static nl.oxod.nekoclient.utils.Utils.getWindowHeight;

public abstract class WContainer extends WWidget {
  public final List<Cell<?>> cells = new ArrayList<>();

  public <T extends WWidget> Cell<T> add(T widget) {
    widget.parent = this;
    widget.theme = theme;

    Cell<T> cell = new Cell<>(widget).centerY();
    cells.add(cell);

    widget.init();
    invalidate();

    return cell;
  }

  public void clear() {
    if (!cells.isEmpty()) {
      cells.clear();
      invalidate();
    }
  }

  public void remove(Cell<?> cell) {
    if (cells.remove(cell)) invalidate();
  }

  /** Moves a child to the visual and input front, matching desktop-style panels. */
  public void bringToFront(WWidget widget) {
    for (int i = 0; i < cells.size(); i++) {
      Cell<?> cell = cells.get(i);
      if (cell.widget() != widget) continue;
      cells.remove(i);
      cells.add(cell);
      return;
    }
  }

  @Override
  public void move(double deltaX, double deltaY) {
    super.move(deltaX, deltaY);
    for (Cell<?> cell : cells) cell.move(deltaX, deltaY);
  }

  public void moveCells(double deltaX, double deltaY) {
    for (Cell<?> cell : cells) {
      cell.move(deltaX, deltaY);
    }
  }

  @Override
  public boolean isFocused() {
    if (focused) return true;

    for (Cell<?> cell : cells) {
      if (cell.widget().isFocused())
        return true;
    }

    return false;
  }

  // Layout

  @Override
  protected void onCalculateSize() {
    width = 0;
    height = 0;

    for (Cell<?> cell : cells) {
      width = Math.max(width, cell.padLeft() + cell.widget().width + cell.padRight());
      height = Math.max(height, cell.padTop() + cell.widget().height + cell.padBottom());
    }
  }

  @Override
  protected void onCalculateWidgetPositions() {
    for (Cell<?> cell : cells) {
      cell.x = x + cell.padLeft();
      cell.y = y + cell.padTop();

      cell.width = width - cell.padLeft() - cell.padRight();
      cell.height = height - cell.padTop() - cell.padBottom();

      cell.alignWidget();
    }
  }

  // Rendering

  @Override
  public boolean render(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
    if (super.render(renderer, mouseX, mouseY, delta)) return true;

    WView view = getView();
    double windowHeight = getWindowHeight();

    UiCompositor.paint(cells,
      cell -> cell.widget().visible && cell.widget().y <= windowHeight
        && cell.widget().y + cell.widget().height > 0 && shouldRenderWidget(cell.widget(), view),
      cell -> renderWidget(cell.widget(), renderer, mouseX, mouseY, delta),
      cell -> cell.widget().isStackingContext(), renderer::flush);

    return false;
  }

  protected void renderWidget(WWidget widget, GuiRenderer renderer, double mouseX, double mouseY, double delta) {
    widget.render(renderer, mouseX, mouseY, delta);
  }

  private boolean shouldRenderWidget(WWidget widget, WView view) {
    if (view == null) return true;
    if (!view.isWidgetInView(widget)) return false;

    if (widget.mouseOver && !view.mouseOver) {
      widget.mouseOver = false;
    }

    return true;
  }

  // Events

  protected boolean propagateEvents(WWidget widget) {
    return true;
  }

  public final boolean acceptsInput(WWidget widget) {
    return widget.visible && propagateEvents(widget);
  }

  public boolean isChildActive(WWidget widget) { return widget.visible; }

  @Override
  public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
    try {
      for (int i = cells.size() - 1; i >= 0; i--) {
        Cell<?> cell = cells.get(i);
        if (propagateEvents(cell.widget()) && cell.widget().mouseClicked(click, doubled)) return true;
      }
    } catch (ConcurrentModificationException _) {
    }

    return super.mouseClicked(click, doubled);
  }

  @Override
  public boolean mouseReleased(MouseButtonEvent click) {
    try {
      for (int i = cells.size() - 1; i >= 0; i--) {
        Cell<?> cell = cells.get(i);
        if (propagateEvents(cell.widget()) && cell.widget().mouseReleased(click)) return true;
      }
    } catch (ConcurrentModificationException _) {
    }

    return super.mouseReleased(click);
  }

  @Override
  public void mouseMoved(double mouseX, double mouseY, double lastMouseX, double lastMouseY) {
    try {
      for (Cell<?> cell : cells) {
        if (propagateEvents(cell.widget())) cell.widget().mouseMoved(mouseX, mouseY, lastMouseX, lastMouseY);
      }
    } catch (ConcurrentModificationException _) {
    }

    super.mouseMoved(mouseX, mouseY, lastMouseX, lastMouseY);
  }

  @Override
  public boolean mouseScrolled(double amount) {
    try {
      for (Cell<?> cell : cells) {
        if (propagateEvents(cell.widget()) && cell.widget().mouseScrolled(amount)) return true;
      }
    } catch (ConcurrentModificationException _) {
    }

    return super.mouseScrolled(amount);
  }

  @Override
  public boolean keyPressed(KeyEvent input) {
    try {
      for (Cell<?> cell : cells) {
        if (propagateEvents(cell.widget()) && cell.widget().keyPressed(input)) return true;
      }
    } catch (ConcurrentModificationException _) {
    }

    return onKeyPressed(input);
  }

  @Override
  public boolean keyRepeated(KeyEvent input) {
    try {
      for (Cell<?> cell : cells) {
        if (propagateEvents(cell.widget()) && cell.widget().keyRepeated(input)) return true;
      }
    } catch (ConcurrentModificationException _) {
    }

    return onKeyRepeated(input);
  }

  @Override
  public boolean charTyped(CharacterEvent input) {
    try {
      for (Cell<?> cell : cells) {
        if (propagateEvents(cell.widget()) && cell.widget().charTyped(input)) return true;
      }
    } catch (ConcurrentModificationException _) {
    }

    return super.charTyped(input);
  }
}
