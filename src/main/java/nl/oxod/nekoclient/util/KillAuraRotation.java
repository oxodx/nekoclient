package nl.oxod.nekoclient.util;

import net.minecraft.client.player.LocalPlayer;

public final class KillAuraRotation {
  public static final float LINEAR_TURN_SPEED = 180.0f;
  public static final float RESET_THRESHOLD = 2.0f;
  public static final int TICKS_UNTIL_RESET = 5;

  private static RotationUtil.Rotation currentRotation = null;
  private static RotationUtil.Rotation targetRotation = null;

  private static int resetTicks = 0;

  private KillAuraRotation() {
  }

  public static void setTarget(RotationUtil.Rotation rotation) {
    targetRotation = rotation;
    resetTicks = TICKS_UNTIL_RESET;
  }

  public static void reset() {
    currentRotation = null;
    targetRotation = null;
    resetTicks = 0;
  }

  public static boolean hasCurrentRotation() {
    return currentRotation != null;
  }

  public static RotationUtil.Rotation getCurrentRotation() {
    return currentRotation;
  }

  public static void update(LocalPlayer player) {
    if (player == null) {
      reset();
      return;
    }

    RotationUtil.Rotation playerRotation = RotationUtil.playerRotation(player);
    if (targetRotation == null || resetTicks <= 0) {
      targetRotation = null;
      if (currentRotation == null) {
        return;
      }

      RotationUtil.Rotation next = step(currentRotation, playerRotation);
      next = RotationUtil.normalizeToSensitivity(next, currentRotation);

      if (RotationUtil.rotationAngleTo(next, playerRotation) <= RESET_THRESHOLD) {
        float fixedYaw = currentRotation.yaw()
          + RotationUtil.angleDifference(player.getYRot(), currentRotation.yaw());
        player.setYRot(fixedYaw);
        player.yBob = fixedYaw;
        player.yBobO = fixedYaw;
        currentRotation = null;
      } else {
        currentRotation = next;
      }
      return;
    }

    RotationUtil.Rotation from = currentRotation != null ? currentRotation : playerRotation;
    RotationUtil.Rotation next = step(from, targetRotation);
    next = RotationUtil.normalizeToSensitivity(next, from);
    currentRotation = next;

    resetTicks--;
  }

  private static RotationUtil.Rotation step(RotationUtil.Rotation from, RotationUtil.Rotation to) {
    return RotationUtil.towardsLinear(from, to, LINEAR_TURN_SPEED, LINEAR_TURN_SPEED);
  }
}
