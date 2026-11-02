package nl.oxod.nekoclient.gui.engine;

import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import nl.oxod.nekoclient.gui.widgets.WWidget;
import nl.oxod.nekoclient.gui.widgets.containers.WContainer;
import nl.oxod.nekoclient.gui.widgets.input.WTextBox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** Hit-test once, bubble one branch, and capture releases even outside a scroll viewport. */
public final class UiInput {
  private final UiScene scene;
  private final UiPointerCapture<WWidget> pointer = new UiPointerCapture<>();
  private double mouseX, mouseY;
  private List<WWidget> hovered = List.of();

  public UiInput(UiScene scene) {
    this.scene = scene;
  }

  private List<WWidget> nodes() {
    var nodes = new ArrayList<WWidget>();
    collect(scene, nodes);
    return nodes;
  }

  private void collect(WWidget node, List<WWidget> result) {
    result.add(node);
    if (node instanceof WContainer container) {
      for (var cell : List.copyOf(container.cells)) collect(cell.widget(), result);
    }
    if (node.popup() != null) collect(node.popup(), result);
  }

  private boolean exposed(WWidget node) {
    UiRect bounds = node.bounds();
    for (WWidget ancestor = node; ancestor != null; ancestor = ancestor.parent) {
      if (!ancestor.visible) return false;
      if (ancestor.parent instanceof WContainer container && !container.isChildActive(ancestor)) return false;
      if (ancestor != node && ancestor.clipsChildren()) bounds = bounds.intersect(ancestor.bounds());
    }
    return bounds.width() > 0 && bounds.height() > 0;
  }

  private List<WWidget> hit(WWidget node, UiRect clip) {
    return UiHitTest.path(node, clip, mouseX, mouseY, WWidget::bounds,
      widget -> widget instanceof WContainer container
        ? container.cells.stream().map(cell -> (WWidget) cell.widget()).toList() : List.of(),
      widget -> widget.visible, WWidget::clipsChildren,
      (parent, child) -> !(parent instanceof WContainer container) || container.acceptsInput(child));
  }

  public void move(double x, double y) {
    scene.layout();
    double previousX = mouseX, previousY = mouseY;
    mouseX = x;
    mouseY = y;
    var all = nodes();
    WWidget capture = pointer.target();
    if (capture != null && (!all.contains(capture) || !capture.visible)) {
      capture.cancelPointer();
      pointer.cancel();
      capture = null;
    }
    // Viewport gates need geometric hover during picking, before occlusion is known.
    for (WWidget node : all) node.mouseOver = node.visible && node.isOver(x, y);
    List<WWidget> path = List.of();
    for (int i = all.size() - 1; i >= 0; i--) {
      WWidget owner = all.get(i);
      if (owner.popup() != null && exposed(owner)) {
        path = hit(owner.popup(), scene.bounds());
        if (!path.isEmpty()) {
          path.add(owner);
          break;
        }
      }
    }
    if (path.isEmpty()) path = hit(scene, scene.bounds());
    for (WWidget node : all) node.mouseOver = path.contains(node);
    for (WWidget node : hovered) {
      if (!path.contains(node)) node.onMouseMoved(x, y, previousX, previousY);
    }
    for (WWidget node : path) node.onMouseMoved(x, y, previousX, previousY);
    if (capture != null && !path.contains(capture) && !hovered.contains(capture)) {
      capture.onMouseMoved(x, y, previousX, previousY);
    }
    for (WWidget node : all) {
      if (node.focused && node != capture && !(node instanceof WContainer)
          && !path.contains(node) && !hovered.contains(node)) {
        node.onMouseMoved(x, y, previousX, previousY);
      }
    }
    // Controls can refine hit testing, but may not re-enable hover behind a panel.
    for (WWidget node : all) if (!path.contains(node)) node.mouseOver = false;
    hovered = path;
  }

  public boolean click(MouseButtonEvent event, boolean doubled) {
    move(event.x(), event.y());
    var all = nodes();
    // Focused popup controls get first refusal, including their detached popup trees.
    for (int i = all.size() - 1; i >= 0; i--) {
      WWidget node = all.get(i);
      if (node.focused && exposed(node) && !(node instanceof WContainer) && !hovered.contains(node)) {
        if (node.onMouseClicked(event, doubled)) return capture(node, event.button());
      }
    }
    for (WWidget node : all) {
      if (node instanceof WTextBox && !hovered.contains(node)) node.setFocused(false);
    }
    for (WWidget node : List.copyOf(hovered)) {
      if (node.onMouseClicked(event, doubled)) return capture(node, event.button());
    }
    return hovered.size() > 1;
  }

  private boolean capture(WWidget node, int button) {
    if (pointer.target() != null && pointer.target() != node) pointer.target().cancelPointer();
    pointer.acquire(node, button);
    for (WWidget ancestor = node; ancestor.parent != null; ancestor = ancestor.parent) {
      if (ancestor.isStackingContext() && ancestor.parent instanceof WContainer container) {
        container.bringToFront(ancestor);
      }
    }
    return true;
  }

  public boolean release(MouseButtonEvent event) {
    move(event.x(), event.y());
    WWidget target = pointer.release(event.button());
    if (target != null) {
      target.onMouseReleased(event);
      return true;
    }
    return false;
  }

  public boolean scroll(double x, double y, double amount) {
    move(x, y);
    for (WWidget node : List.copyOf(hovered)) if (node.onMouseScrolled(amount)) return true;
    return hovered.size() > 1;
  }

  public boolean key(KeyEvent event, boolean repeat) {
    return focused(node -> repeat ? node.onKeyRepeated(event) : node.onKeyPressed(event));
  }

  public boolean character(CharacterEvent event) {
    return focused(node -> node.onCharTyped(event));
  }

  private boolean focused(Predicate<WWidget> event) {
    for (WWidget node : nodes()) {
      if (exposed(node) && node.focused && event.test(node)) return true;
    }
    return false;
  }

  public boolean focusNext(boolean backwards) {
    scene.layout();
    List<WTextBox> fields = nodes().stream().filter(node -> node instanceof WTextBox && exposed(node))
      .map(node -> (WTextBox) node).toList();
    if (fields.isEmpty()) return false;
    int current = -1;
    for (int i = 0; i < fields.size(); i++) {
      if (fields.get(i).focused) current = i;
      fields.get(i).setFocused(false);
    }
    int next = current < 0 ? (backwards ? fields.size() - 1 : 0)
      : Math.floorMod(current + (backwards ? -1 : 1), fields.size());
    fields.get(next).setFocused(true);
    fields.get(next).setCursorMax();
    return true;
  }

  public void reset() {
    WWidget capture = pointer.cancel();
    if (capture != null) capture.cancelPointer();
    for (WWidget node : nodes()) {
      node.setFocused(false);
      node.mouseOver = false;
    }
    hovered = List.of();
  }

  public boolean draggingPanel() {
    for (WWidget node = pointer.target(); node != null; node = node.parent) {
      if (node instanceof nl.oxod.nekoclient.gui.widgets.containers.WWindow window) return window.isDragging();
    }
    return false;
  }
}
