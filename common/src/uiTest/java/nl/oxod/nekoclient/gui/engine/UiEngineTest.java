package nl.oxod.nekoclient.gui.engine;

import java.util.ArrayList;
import java.util.List;

/** Runs without Minecraft/native initialization or an external test framework. */
public final class UiEngineTest {
  private static int checks;

  public static void main(String[] args) {
    clips();
    picking();
    capture();
    stacking();
    motion();
    System.out.println("UI engine: " + checks + " checks passed");
  }

  private static void check(boolean condition, String message) {
    checks++;
    if (!condition) throw new AssertionError(message);
  }

  private static void clips() {
    UiRect parent = new UiRect(10, 10, 20, 20);
    check(parent.intersect(new UiRect(0, 0, 50, 50)).equals(parent), "Clip all four edges, not just one edge per axis");
    check(parent.intersect(new UiRect(0, 0, 15, 15)).equals(new UiRect(10, 10, 5, 5)), "Keep original right/bottom when shifting left/top");
    check(!parent.intersect(new UiRect(40, 40, 10, 10)).contains(40, 40), "Disjoint clips cannot receive clicks");
    check(!parent.contains(30, 20), "Adjacent half-open hit regions must not overlap");
    check(!new UiRect(0, 0, -10, -1).contains(0, 0), "Negative layout space is empty");
  }

  private record Node(String name, UiRect bounds, boolean visible, boolean clip, List<Node> children) {}

  private static Node node(String name, double x, double y, double width, double height, Node... children) {
    return new Node(name, new UiRect(x, y, width, height), true, true, List.of(children));
  }

  private static List<Node> pick(Node root, double x, double y) {
    return UiHitTest.path(root, root.bounds, x, y, Node::bounds, Node::children,
      Node::visible, Node::clip, (parent, child) -> true);
  }

  private static void picking() {
    Node behind = node("behind", 0, 0, 100, 100);
    Node overflow = node("overflow", 0, 0, 200, 200);
    Node front = node("front", 20, 20, 40, 40, overflow);
    Node root = node("scene", 0, 0, 300, 300, behind, front);
    check(pick(root, 25, 25).equals(List.of(overflow, front, root)), "Pick and bubble only the front branch");
    check(pick(root, 80, 80).equals(List.of(behind, root)), "Child outside viewport cannot steal input");
    Node hidden = new Node("hidden", behind.bounds, false, true, List.of());
    check(pick(node("scene", 0, 0, 300, 300, behind, hidden), 10, 10).getFirst() == behind, "Ignore invisible layers");
    check(pick(root, -1, 0).isEmpty(), "Outside viewport has no target");
    var collapsed = UiHitTest.path(root, root.bounds, 25, 25, Node::bounds, Node::children,
      Node::visible, Node::clip, (parent, child) -> child != front);
    check(collapsed.getFirst() == behind, "Collapsed branches are excluded before descent");
  }

  private static void capture() {
    var pointer = new UiPointerCapture<String>();
    pointer.acquire("slider", 0);
    check(pointer.release(1) == null && "slider".equals(pointer.target()), "Other buttons cannot end a drag");
    check("slider".equals(pointer.release(0)), "Release reaches captured control after leaving its viewport");
    check(pointer.release(0) == null, "Release is delivered once");
    pointer.acquire("panel", 0);
    check("panel".equals(pointer.cancel()) && pointer.target() == null, "Screen removal cancels capture");
  }

  private static void stacking() {
    var layers = new ArrayList<>(List.of("back", "front"));
    var painted = new ArrayList<String>();
    UiCompositor.paint(layers, layer -> true, layer -> {
      painted.add(layer);
      if (layer.equals("back")) layers.add("new");
    }, layer -> true, () -> painted.add("flush"));
    check(painted.equals(List.of("flush", "back", "flush", "flush", "front", "flush")),
      "Paint stable snapshot and flush both sides of each stacking context");
    check("new".equals(UiCompositor.pick(layers, layer -> new UiRect(0, 0, 100, 100), 50, 50)), "HUD picks last painted element");
  }

  private static void motion() {
    double at30 = 0, at120 = 0;
    for (int i = 0; i < 6; i++) at30 = UiMotion.approach(at30, 1, 12, 1.0 / 30);
    for (int i = 0; i < 24; i++) at120 = UiMotion.approach(at120, 1, 12, 1.0 / 120);
    check(Math.abs(at30 - at120) < 1e-9, "Animation must not depend on frame rate");
    check(UiMotion.approach(0, 1, 12, 30) < 1, "Long pause has bounded motion step");
    check(UiMotion.approach(0.5, 1, 12, -1) == 0.5, "Negative time cannot reverse animation");
  }
}
