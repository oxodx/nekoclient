package nl.oxod.nekoclient.gui.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

/** Shared front-to-back picking with the same clip intersections as the compositor. */
public final class UiHitTest {
  private UiHitTest() {}

  public static <T> List<T> path(T node, UiRect clip, double x, double y,
      Function<T, UiRect> bounds, Function<T, List<T>> children,
      Predicate<T> visible, Predicate<T> clips, BiPredicate<T, T> accepts) {
    if (!visible.test(node) || !clip.contains(x, y)) return List.of();
    if (clips.test(node)) clip = clip.intersect(bounds.apply(node));
    if (!clip.contains(x, y)) return List.of();
    List<T> descendants = children.apply(node);
    for (int i = descendants.size() - 1; i >= 0; i--) {
      T child = descendants.get(i);
      if (!accepts.test(node, child)) continue;
      List<T> path = path(child, clip, x, y, bounds, children, visible, clips, accepts);
      if (!path.isEmpty()) {
        path.add(node);
        return path;
      }
    }
    if (!bounds.apply(node).contains(x, y)) return List.of();
    var result = new ArrayList<T>();
    result.add(node);
    return result;
  }
}
