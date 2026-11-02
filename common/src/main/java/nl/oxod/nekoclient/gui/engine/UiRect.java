package nl.oxod.nekoclient.gui.engine;

/** Logical bounds shared by layout, hit testing and GPU clipping. */
public record UiRect(double x, double y, double width, double height) {
  public UiRect {
    width = Math.max(0, width);
    height = Math.max(0, height);
  }

  public boolean contains(double px, double py) {
    return width > 0 && height > 0 && px >= x && py >= y && px < x + width && py < y + height;
  }

  public UiRect intersect(UiRect other) {
    double left = Math.max(x, other.x);
    double top = Math.max(y, other.y);
    return new UiRect(left, top, Math.min(x + width, other.x + other.width) - left,
      Math.min(y + height, other.y + other.height) - top);
  }
}
