package meteordevelopment.meteorclient.systems.modules.combat.killaura.modes;

import java.util.ArrayList;
import java.util.List;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.CrystalAura;
import meteordevelopment.meteorclient.systems.modules.combat.killaura.KillAura.RotationType;
import meteordevelopment.meteorclient.systems.modules.combat.killaura.KillAuraMode;
import meteordevelopment.meteorclient.systems.modules.combat.killaura.KillAuraModes;
import meteordevelopment.meteorclient.utils.GameSensitivityUtils;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.world.TickRate;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import static meteordevelopment.meteorclient.utils.RaycastUtils.raycastEntity;
import static net.minecraft.util.Mth.*;

public class Matrix extends KillAuraMode {
  public Matrix() {
    super(KillAuraModes.Matrix);
  }

  protected final List<Entity> targets = new ArrayList<>();

  @Override
  public void onTickPre(TickEvent.Pre event) {
    if (!mc.player.isAlive() || PlayerUtils.getGameMode() == GameType.SPECTATOR) {
      stopAttacking();
      return;
    }
    if (settings.pauseOnUse.get() && (mc.gameMode.isDestroying() || mc.player.isUsingItem())) {
      stopAttacking();
      return;
    }
    if (settings.onlyOnClick.get() && !mc.options.keyAttack.isDown()) {
      stopAttacking();
      return;
    }
    if (TickRate.INSTANCE.getTimeSinceLastTick() >= 1f && settings.pauseOnLag.get()) {
      stopAttacking();
      return;
    }
    if (settings.pauseOnCA.get() && Modules.get().get(CrystalAura.class).isActive()
        && Modules.get().get(CrystalAura.class).kaTimer > 0) {
      stopAttacking();
      return;
    }
    if (settings.onlyOnLook.get()) {
      Entity targeted = mc.crosshairPickEntity;

      if (targeted == null || !entityCheck(targeted)) {
        stopAttacking();
        return;
      }

      targets.clear();
      targets.add(mc.crosshairPickEntity);
    } else {
      targets.clear();
      TargetUtils.getList(targets, this::entityCheck, settings.priority.get(), settings.maxTargets.get());
    }

    if (targets.isEmpty()) {
      this.primary = null;
      stopAttacking();
      return;
    }

    Entity target = targets.getFirst();

    if (target != null && target.isAlive() && target instanceof LivingEntity livingTarget) {
      this.primary = livingTarget;
      isRotated = false;

      EntityHitResult result = raycastEntity(settings.range.get(), rotateVector.u(), rotateVector.v(), 0f);
      if (result != null) {
        ChatUtils.info(result.getType().name());
      }
      if (delayCheck() && result != null && result.getType() == net.minecraft.world.phys.HitResult.Type.ENTITY) {
        attack(target);
        selected = target;
        ticks = 2;
      }

      if (settings.rotationType.get() == RotationType.Fast) {
        if (ticks > 0) {
          updateRotation(true, 180, 90);
          Rotations.rotate(rotateVector.u(), rotateVector.v());
          ticks--;
        } else {
          reset();
        }
      } else {
        if (!isRotated) {
          updateRotation(false, 80, 35);
          Rotations.rotate(rotateVector.u(), rotateVector.v());
        }
      }
    } else {
      this.primary = null;
      reset();
    }
  }

  private UVPair rotateVector = new UVPair(0, 0);
  private LivingEntity primary;
  private Entity selected;
  float lastYaw, lastPitch;
  int ticks = 0;
  boolean isRotated;

  private void updateRotation(boolean attack, float rotationYawSpeed, float rotationPitchSpeed) {
    Vec3 vec = primary.position().add(0, clamp(mc.player.getEyeHeight(mc.player.getPose()) - primary.getY(),
        0, primary.getBbHeight() * (mc.player.distanceTo(primary) / settings.range.get())), 0)
        .subtract(mc.player.getEyePosition());

    isRotated = true;

    float yawToTarget = (float) wrapDegrees(Math.toDegrees(Math.atan2(vec.z, vec.x)) - 90);
    float pitchToTarget = (float) (-Math.toDegrees(Math.atan2(vec.y, length(vec.x, vec.z))));

    float yawDelta = (wrapDegrees(yawToTarget - rotateVector.u()));
    float pitchDelta = (wrapDegrees(pitchToTarget - rotateVector.v()));
    int roundedYaw = (int) yawDelta;

    switch (settings.rotationType.get()) {
      case Smooth -> {
        float clampedYaw = Math.min(Math.max(Math.abs(yawDelta), 1.0f), rotationYawSpeed);
        float clampedPitch = Math.min(Math.max(Math.abs(pitchDelta), 1.0f), rotationPitchSpeed);

        if (attack && selected != primary && settings.speedUpRotationWhenAttacking.get()) {
          clampedPitch = Math.max(Math.abs(pitchDelta), 1.0f);
        } else {
          clampedPitch /= 3f;
        }

        if (Math.abs(clampedYaw - this.lastYaw) <= 3.0f) {
          clampedYaw = this.lastYaw + 3.1f;
        }

        float yaw = rotateVector.u() + (yawDelta > 0 ? clampedYaw : -clampedYaw);
        float pitch = clamp(rotateVector.v() + (pitchDelta > 0 ? clampedPitch : -clampedPitch), -89.0F, 89.0F);

        float gcd = GameSensitivityUtils.getGCDValue();
        yaw -= (yaw - rotateVector.u()) % gcd;
        pitch -= (pitch - rotateVector.v()) % gcd;

        rotateVector = new UVPair(yaw, pitch);
        lastYaw = clampedYaw;
        lastPitch = clampedPitch;
      }
      case Fast -> {
        float yaw = rotateVector.u() + roundedYaw;
        float pitch = clamp(rotateVector.v() + pitchDelta, -90, 90);

        float gcd = GameSensitivityUtils.getGCDValue();
        yaw -= (yaw - rotateVector.u()) % gcd;
        pitch -= (pitch - rotateVector.v()) % gcd;

        rotateVector = new UVPair(yaw, pitch);
      }
    }
  }

  private void reset() {
    rotateVector = new UVPair(mc.player.getYRot(), mc.player.getXRot());
  }
}
