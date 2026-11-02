package nl.oxod.nekoclient.gui.engine;

/** Frame-rate independent motion, with bounded time steps after a stalled frame. */
public final class UiMotion {
  private UiMotion() {}

  public static double approach(double value, double target, double speed, double dt) {
    double result = target + (value - target) * Math.exp(-speed * Math.max(0, Math.min(dt, 0.1)));
    return Math.abs(result - target) < 0.001 ? target : result;
  }
}
