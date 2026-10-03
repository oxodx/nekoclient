package nl.oxod.nekoclient.systems.modules.movement;

import nl.oxod.nekoclient.utils.RotationUtil;

/** Standalone regression checks; no client world or test framework required. */
public final class ScaffoldRotationTest {
  public static void main(String[] args) {
    // Rounding mouse steps must not increase the requested turn budget.
    assertTurn(new RotationUtil.Rotation(0, 0), new RotationUtil.Rotation(180, 0), 11, 3);
    assertTurn(new RotationUtil.Rotation(0, 0), new RotationUtil.Rotation(90, 90), 10, 4);
    assertTurn(new RotationUtil.Rotation(179, 0), new RotationUtil.Rotation(-179, 0), 10, 0.15);
    assertTurn(new RotationUtil.Rotation(0, 89), new RotationUtil.Rotation(10, 90), 5, 0.15);
    assertTurn(new RotationUtil.Rotation(0, 0), new RotationUtil.Rotation(90, 0), 0, 0.15);
    assertTurn(new RotationUtil.Rotation(0, 89.92F), new RotationUtil.Rotation(10, 90), 5, 0.15);
    assertTurn(new RotationUtil.Rotation(0, -89.92F), new RotationUtil.Rotation(-10, -90), 5, 0.15);
    for (int yaw = -180; yaw <= 180; yaw += 15) {
      for (int pitch = -90; pitch <= 90; pitch += 15) {
        assertTurn(new RotationUtil.Rotation(12.5F, 8.5F),
          new RotationUtil.Rotation(yaw, pitch), 75, 0.15);
      }
    }
    System.out.println("Scaffold rotation regression checks passed.");
  }

  private static void assertTurn(RotationUtil.Rotation from, RotationUtil.Rotation goal,
                                 float cap, double gcd) {
    RotationUtil.Rotation actual = Scaffold.stepTellyRotation(from, goal, cap, gcd);
    double yaw = RotationUtil.angleDifference(actual.yaw(), from.yaw());
    double pitch = actual.pitch() - from.pitch();
    if (Math.hypot(yaw, pitch) > cap + 0.0001) {
      throw new AssertionError("Turn exceeded " + cap + " degrees: " + actual);
    }
    if (actual.pitch() < -90 || actual.pitch() > 90) throw new AssertionError("Invalid pitch");
    if (gcd > 0 && (Math.abs(yaw / gcd - Math.rint(yaw / gcd)) > 0.0002
      || Math.abs(pitch / gcd - Math.rint(pitch / gcd)) > 0.0002)) {
      throw new AssertionError("Turn is not on the mouse sensitivity grid: " + actual);
    }
    if (cap > 0 && Math.abs(yaw) + Math.abs(pitch) < 0.0001) {
      throw new AssertionError("Turn made no progress");
    }
  }
}
