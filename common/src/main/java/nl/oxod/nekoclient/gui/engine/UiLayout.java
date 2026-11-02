package nl.oxod.nekoclient.gui.engine;

import nl.oxod.nekoclient.gui.widgets.WWidget;
import nl.oxod.nekoclient.gui.widgets.containers.WContainer;

/** Constraint propagation and measure/arrange passes for every native UI screen. */
public final class UiLayout {
  private UiLayout() {}

  public static void measure(WWidget node, double availableWidth) {
    node.availableWidth = Math.max(0, availableWidth);
    if (node instanceof WContainer container) {
      double contentWidth = node.contentWidth(node.availableWidth);
      for (var cell : container.cells) {
        measure(cell.widget(), Math.max(0, contentWidth - cell.padLeft() - cell.padRight()));
      }
    }
    node.measureSelf();
  }

  public static void arrange(WWidget node) {
    node.arrangeSelf();
    if (node instanceof WContainer container) {
      for (var cell : container.cells) arrange(cell.widget());
    }
  }
}
