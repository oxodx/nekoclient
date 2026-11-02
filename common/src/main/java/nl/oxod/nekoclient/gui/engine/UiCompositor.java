package nl.oxod.nekoclient.gui.engine;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/** Stable paint order shared by widget scenes and HUD scenes. */
public final class UiCompositor {
  private UiCompositor() {}

  public static <T> void paint(List<T> layers, Predicate<T> visible, Consumer<T> paint,
      Predicate<T> stackingContext, Runnable flush) {
    for (T layer : List.copyOf(layers)) {
      if (!visible.test(layer)) continue;
      boolean isolated = stackingContext.test(layer);
      if (isolated) flush.run();
      paint.accept(layer);
      if (isolated) flush.run();
    }
  }

  public static <T> T pick(List<T> layers, Function<T, UiRect> bounds, double x, double y) {
    for (int i = layers.size() - 1; i >= 0; i--) {
      T layer = layers.get(i);
      if (bounds.apply(layer).contains(x, y)) return layer;
    }
    return null;
  }
}
