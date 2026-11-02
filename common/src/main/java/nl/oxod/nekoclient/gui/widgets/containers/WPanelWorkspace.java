/*
 * This file is part of the NekoClient distribution.
 */

package nl.oxod.nekoclient.gui.widgets.containers;

import nl.oxod.nekoclient.gui.utils.Cell;
import nl.oxod.nekoclient.gui.widgets.WWidget;

import java.util.ArrayList;
import java.util.List;

import static nl.oxod.nekoclient.utils.Utils.getWindowWidth;

/**
 * Desktop-style panel workspace used by click GUIs. It owns panel stacking and
 * free-form geometry independently of a visual theme; overlays are laid out by
 * anchors and never participate in panel sizing.
 */
public abstract class WPanelWorkspace extends WContainer {
  public enum Anchor { FREE, TOP_CENTER }

  protected final List<WWindow> panels = new ArrayList<>();
  private final List<Overlay> overlays = new ArrayList<>();

  public <T extends WWindow> T addPanel(T panel) {
    panels.add(panel);
    add(panel);
    sortLayers();
    return panel;
  }

  public <T extends WWidget> T addOverlay(T widget, Anchor anchor, double offsetY) {
    Cell<T> cell = add(widget);
    overlays.add(new Overlay(cell, anchor, offsetY));
    return widget;
  }

  protected void removePanel(WWindow panel) {
    panels.remove(panel);
    cells.stream().filter(cell -> cell.widget() == panel).findFirst().ifPresent(this::remove);
  }

  @Override
  protected void onCalculateWidgetPositions() {
    sortLayers();
    for (Cell<?> cell : cells) {
      cell.width = cell.widget().width;
      cell.height = cell.widget().height;

      Overlay overlay = overlay(cell);
      if (overlay != null && overlay.anchor == Anchor.TOP_CENTER) {
        cell.x = (getWindowWidth() - cell.width) / 2;
        cell.y = y + overlay.offsetY;
      } else {
        cell.x = cell.widget().x;
        cell.y = cell.widget().y;
      }

      cell.alignWidget();
    }
  }

  @Override
  public void bringToFront(WWidget widget) {
    if (widget instanceof WWindow window && window.id != null) {
      long highest = panels.stream().filter(panel -> panel.id != null)
        .mapToLong(panel -> theme.getWindowConfig(panel.id).zIndex).max().orElse(0);
      theme.getWindowConfig(window.id).zIndex = highest + 1;
    }
    super.bringToFront(widget);
    sortLayers();
  }

  private void sortLayers() {
    cells.sort(java.util.Comparator.comparingLong(cell -> {
      if (overlay(cell) != null) return Long.MAX_VALUE;
      if (cell.widget() instanceof WWindow window && window.id != null) return theme.getWindowConfig(window.id).zIndex;
      return 0;
    }));
  }

  private Overlay overlay(Cell<?> cell) {
    for (Overlay overlay : overlays) if (overlay.cell == cell) return overlay;
    return null;
  }

  private record Overlay(Cell<?> cell, Anchor anchor, double offsetY) {}
}
