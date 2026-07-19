package meteordevelopment.meteorclient.systems.modules.combat.killaura.modes;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.CrystalAura;
import meteordevelopment.meteorclient.systems.modules.combat.killaura.KillAura.AttackItems;
import meteordevelopment.meteorclient.systems.modules.combat.killaura.KillAura.RotationMode;
import meteordevelopment.meteorclient.systems.modules.combat.killaura.KillAura.RotationType;
import meteordevelopment.meteorclient.systems.modules.combat.killaura.KillAuraMode;
import meteordevelopment.meteorclient.systems.modules.combat.killaura.KillAuraModes;
import meteordevelopment.meteorclient.utils.GameSensitivityUtils;
import meteordevelopment.meteorclient.utils.entity.Target;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.world.TickRate;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import static net.minecraft.util.Mth.*;

public class Matrix extends KillAuraMode {
  private UVPair rotateVector = new UVPair(0, 0);
  private LivingEntity primary;

  public Matrix() {
    super(KillAuraModes.Matrix);
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
    stopAttacking();
  }

  @EventHandler
  public void onSendPacket(PacketEvent.Send event) {
    if (event.packet instanceof ServerboundSetCarriedItemPacket) {
      switchTimer = settings.switchDelay.get();
    }
  }

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
      primary = null;
      stopAttacking();
      return;
    }

    Entity target = targets.getFirst();

    if (!(target instanceof LivingEntity livingTarget)) {
      primary = null;
      stopAttacking();
      return;
    }

    this.primary = livingTarget;

    if (settings.autoSwitch.get()) {
      FindItemResult weaponResult = new FindItemResult(mc.player.getInventory().getSelectedSlot(), -1);
      if (settings.attackWhenHolding.get() == AttackItems.Weapons)
        weaponResult = InvUtils.find(this::acceptableWeapon, 0, 8);

      if (shouldShieldBreak()) {
        FindItemResult axeResult = InvUtils.find(itemStack -> itemStack.getItem() instanceof AxeItem, 0, 8);
        if (axeResult.found()) weaponResult = axeResult;
      }

      if (!swapped) {
        previousSlot = mc.player.getInventory().getSelectedSlot();
        swapped = true;
      }

      InvUtils.swap(weaponResult.slot(), false);
    }

    if (!acceptableWeapon(mc.player.getMainHandItem())) {
      stopAttacking();
      return;
    }

    attacking = true;

    if (settings.pauseOnCombat.get() && PathManagers.get().isPathing() && !wasPathing) {
      PathManagers.get().pause();
      wasPathing = true;
    }

    updateRotation();
    Rotations.rotate(rotateVector.u(), rotateVector.v());

    if (delayCheck()) targets.forEach(this::attack);
  }

  private void updateRotation() {
    if (primary == null) return;

    Vec3 vec = primary.position().add(0, clamp(mc.player.getEyeHeight(mc.player.getPose()) - primary.getY(),
        0, primary.getBbHeight() * (mc.player.distanceTo(primary) / settings.range.get())), 0)
        .subtract(mc.player.getEyePosition());

    float yawToTarget = (float) wrapDegrees(Math.toDegrees(Math.atan2(vec.z, vec.x)) - 90);
    float pitchToTarget = (float) (-Math.toDegrees(Math.atan2(vec.y, length(vec.x, vec.z))));

    float yawDelta = wrapDegrees(yawToTarget - rotateVector.u());
    float pitchDelta = wrapDegrees(pitchToTarget - rotateVector.v());

    switch (settings.rotationType.get()) {
      case Smooth -> {
        float clampedYaw = Math.min(Math.max(Math.abs(yawDelta), 1.0f), 80.0f);
        float clampedPitch = Math.min(Math.max(Math.abs(pitchDelta), 1.0f), 35.0f) / 3.0f;

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
