package nl.oxod.nekoclient.gui.engine;

/** Button-specific capture shared by pointer-driven controls. */
public final class UiPointerCapture<T> {
  private T target;
  private int button = -1;

  public T target() { return target; }

  public void acquire(T target, int button) {
    this.target = target;
    this.button = button;
  }

  public T release(int button) {
    if (this.button != button) return null;
    return cancel();
  }

  public T cancel() {
    T previous = target;
    target = null;
    button = -1;
    return previous;
  }
}
