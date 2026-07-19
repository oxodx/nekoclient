package meteordevelopment.meteorclient.systems.modules.combat.killaura.modes;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.systems.modules.combat.killaura.KillAura.RotationType;
import meteordevelopment.meteorclient.systems.modules.combat.killaura.KillAuraMode;
import meteordevelopment.meteorclient.utils.GameSensitivityUtils;
import meteordevelopment.meteorclient.utils.entity.Target;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import static net.minecraft.util.Mth.*;

public class Matrix extends KillAuraMode {
  private UVPair rotateVector = new UVPair(0, 0);
  private LivingEntity primary;
  private Entity selected;

  public Matrix() {
    super();
  }

  @Override
  public void onActivate() {
    previousSlot = -1;
    swapped = false;
    rotateVector = new UVPair(mc.player.getYRot(), mc.player.getXRot());
  }

  @Override
  public void onDeactivate() {
    targets.clear();
    primary = null;
    selected = null;
    stopAttacking();
  }

  @EventHandler
  public void onSendPacket(PacketEvent.Send event) {
    if (event.packet instanceof ServerboundSetCarriedItemPacket) {
      switchTimer = settings.timing.switchDelay.get();
    }
  }

  @Override
  public void onTickPre(TickEvent.Pre event, Entity target) {
    if (target instanceof LivingEntity livingTarget) {
      this.primary = livingTarget;
    } else {
      this.primary = null;
      stopAttacking();
      return;
    }

    if (settings.general.pauseOnCombat.get() && PathManagers.get().isPathing() && !wasPathing) {
      PathManagers.get().pause();
      wasPathing = true;
    }

    if (settings.general.rotationType.get() == RotationType.Fast) {
      Rotations.rotate(Rotations.getYaw(primary), Rotations.getPitch(primary, Target.Body));
    } else {
      updateRotation(false, 80, 35);
      Rotations.rotate(rotateVector.u(), rotateVector.v());
    }

    if (delayCheck()) targets.forEach(this::attack);
  }

  private void updateRotation(boolean attack, float rotationYawSpeed, float rotationPitchSpeed) {
    if (primary == null) return;

    double distance = mc.player.distanceTo(primary);
    double lookHeight = clamp(distance / settings.targeting.range.get(), 0, 1) * primary.getBbHeight();
    Vec3 vec = primary.position().add(0, lookHeight, 0).subtract(mc.player.getEyePosition());

    float yawToTarget = (float) wrapDegrees(Math.toDegrees(Math.atan2(vec.z, vec.x)) - 90);
    float pitchToTarget = (float) (-Math.toDegrees(Math.atan2(vec.y, length(vec.x, vec.z))));

    float yawDelta = wrapDegrees(yawToTarget - rotateVector.u());
    float pitchDelta = wrapDegrees(pitchToTarget - rotateVector.v());

    switch (settings.general.rotationType.get()) {
      case Smooth -> {
        float absYawDelta = Math.abs(yawDelta);
        float absPitchDelta = Math.abs(pitchDelta);
        float clampedYaw = Math.min(absYawDelta, rotationYawSpeed);
        float clampedPitch = Math.min(absPitchDelta, rotationPitchSpeed);

        if (attack && selected != primary && settings.general.speedUpRotationWhenAttacking.get()) {
          clampedPitch = absPitchDelta;
        } else {
          clampedPitch /= 3f;
        }

        float yaw = rotateVector.u() + (yawDelta > 0 ? clampedYaw : -clampedYaw);
        float pitch = clamp(rotateVector.v() + (pitchDelta > 0 ? clampedPitch : -clampedPitch), -89.0F, 89.0F);

        float gcd = GameSensitivityUtils.getGCDValue();
        yaw -= (yaw - rotateVector.u()) % gcd;
        pitch -= (pitch - rotateVector.v()) % gcd;

        rotateVector = new UVPair(yaw, pitch);
      }
      case Fast -> {
        float yaw = rotateVector.u() + yawDelta;
        float pitch = clamp(rotateVector.v() + pitchDelta, -90, 90);

        float gcd = GameSensitivityUtils.getGCDValue();
        yaw -= (yaw - rotateVector.u()) % gcd;
        pitch -= (pitch - rotateVector.v()) % gcd;

        rotateVector = new UVPair(yaw, pitch);
      }
    }
  }
}
