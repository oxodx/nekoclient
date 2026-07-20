package nl.oxod.nekoclient.systems.modules.combat.killaura;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.CrystalAura;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.utils.GameSensitivityUtils;
import meteordevelopment.meteorclient.utils.entity.Target;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.world.TickRate;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Zoglin;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.oxod.nekoclient.systems.modules.combat.killaura.settings.General;
import nl.oxod.nekoclient.systems.modules.combat.killaura.settings.Targeting;
import nl.oxod.nekoclient.systems.modules.combat.killaura.settings.Timing;

import java.util.ArrayList;
import java.util.List;

import static net.minecraft.util.Mth.*;

public class KillAura extends Module {
  public KillAura() {
    super(Categories.Neko_Combat, "kill-aura", "Attacks specified entities around you.");
  }

  public final General general = new General(settings.getDefaultGroup(), this);
  public final Targeting targeting = new Targeting(settings.createGroup("Targeting"), this);
  public final Timing timing = new Timing(settings.createGroup("Timing"), this);

  public final static ArrayList<Item> FILTER = new ArrayList<>(List.of(
      Items.DIAMOND_SWORD,
      Items.DIAMOND_AXE,
      Items.DIAMOND_PICKAXE,
      Items.DIAMOND_SHOVEL,
      Items.DIAMOND_HOE,
      Items.MACE,
      Items.DIAMOND_SPEAR,
      Items.TRIDENT));

  protected final List<Entity> targets = new ArrayList<>();
  protected boolean wasPathing = false;
  protected int switchTimer, hitTimer;
  public boolean attacking, swapped;
  protected static int previousSlot;

  private UVPair rotateVector = new UVPair(0, 0);
  private LivingEntity primary;
  private Entity selected;

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
  public void onTickPre(TickEvent.Pre event) {
    isAllowedToAttack();

    Entity target = getTarget();
    if (target == null)
      return;

    autoSwitch();

    if (!acceptableWeapon(mc.player.getMainHandItem())) {
      stopAttacking();
      return;
    }

    attacking = true;

    if (target instanceof LivingEntity livingTarget) {
      this.primary = livingTarget;
    } else {
      this.primary = null;
      stopAttacking();
      return;
    }

    if (general.pauseOnCombat.get() && PathManagers.get().isPathing() && !wasPathing) {
      PathManagers.get().pause();
      wasPathing = true;
    }

    if (general.rotation.get() == RotationMode.Always) {
      if (general.rotationType.get() == RotationType.Fast) {
        Rotations.rotate(Rotations.getYaw(primary), Rotations.getPitch(primary, Target.Body));
      } else {
        updateRotation(false, 80, 35);
        Rotations.rotate(rotateVector.u(), rotateVector.v());
      }
    }

    if (delayCheck())
      targets.forEach(this::attack);
  }

  @EventHandler
  public void onTickPost(TickEvent.Post event) {
  }

  @EventHandler
  public void onSendPacket(PacketEvent.Send event) {
    if (event.packet instanceof ServerboundSetCarriedItemPacket) {
      switchTimer = timing.switchDelay.get();
    }
  }

  @Override
  public String getInfoString() {
    return "";
  }

  public Entity getTarget() {
    if (!targets.isEmpty())
      return targets.getFirst();
    return null;
  }

  private void isAllowedToAttack() {
    if (!mc.player.isAlive() || PlayerUtils.getGameMode() == GameType.SPECTATOR) {
      stopAttacking();
      return;
    }
    if (timing.pauseOnUse.get() && (mc.gameMode.isDestroying() || mc.player.isUsingItem())) {
      stopAttacking();
      return;
    }
    if (general.onlyOnClick.get() && !mc.options.keyAttack.isDown()) {
      stopAttacking();
      return;
    }
    if (TickRate.INSTANCE.getTimeSinceLastTick() >= 1f && timing.pauseOnLag.get()) {
      stopAttacking();
      return;
    }
    if (timing.pauseOnCA.get() && Modules.get().get(CrystalAura.class).isActive()
        && Modules.get().get(CrystalAura.class).kaTimer > 0) {
      stopAttacking();
      return;
    }
    if (general.onlyOnLook.get()) {
      Entity targeted = mc.crosshairPickEntity;

      if (targeted == null || !entityCheck(targeted)) {
        stopAttacking();
        return;
      }

      targets.clear();
      targets.add(mc.crosshairPickEntity);
    } else {
      targets.clear();
      TargetUtils.getList(targets, this::entityCheck, targeting.priority.get(),
          targeting.maxTargets.get());
    }

    if (targets.isEmpty()) {
      stopAttacking();
      return;
    }
  }

  private void autoSwitch() {
    if (general.autoSwitch.get()) {
      FindItemResult weaponResult = new FindItemResult(mc.player.getInventory().getSelectedSlot(), -1);
      if (general.attackWhenHolding.get() == AttackItems.Weapons)
        weaponResult = InvUtils.find(this::acceptableWeapon, 0, 8);

      if (shouldShieldBreak()) {
        FindItemResult axeResult = InvUtils.find(itemStack -> itemStack.getItem() instanceof AxeItem, 0, 8);
        if (axeResult.found())
          weaponResult = axeResult;
      }

      if (!swapped) {
        previousSlot = mc.player.getInventory().getSelectedSlot();
        swapped = true;
      }

      InvUtils.swap(weaponResult.slot(), false);
    }
  }

  private void attack(Entity target) {
    if (general.rotation.get() == RotationMode.OnHit)
      Rotations.rotate(Rotations.getYaw(target), Rotations.getPitch(target, Target.Body));

    mc.gameMode.attack(mc.player, target);
    mc.player.swing(InteractionHand.MAIN_HAND);

    hitTimer = 0;
  }

  private boolean shouldShieldBreak() {
    for (Entity target : targets) {
      if (target instanceof Player player) {
        if (player.isBlocking() && general.shieldMode.get() == ShieldMode.Break) {
          return true;
        }
      }
    }

    return false;
  }

  private boolean acceptableWeapon(ItemStack stack) {
    if (shouldShieldBreak())
      return stack.getItem() instanceof AxeItem;
    if (general.attackWhenHolding.get() == AttackItems.All)
      return true;

    if (general.weapons.get().contains(Items.DIAMOND_SWORD)
        && stack.is(ItemTags.SWORDS))
      return true;
    if (general.weapons.get().contains(Items.DIAMOND_AXE)
        && stack.is(ItemTags.AXES))
      return true;
    if (general.weapons.get().contains(Items.DIAMOND_PICKAXE)
        && stack.is(ItemTags.PICKAXES))
      return true;
    if (general.weapons.get().contains(Items.DIAMOND_SHOVEL)
        && stack.is(ItemTags.SHOVELS))
      return true;
    if (general.weapons.get().contains(Items.DIAMOND_HOE)
        && stack.is(ItemTags.HOES))
      return true;
    if (general.weapons.get().contains(Items.MACE)
        && stack.getItem() instanceof MaceItem)
      return true;
    if (general.weapons.get().contains(Items.DIAMOND_SPEAR)
        && stack.is(ItemTags.SPEARS))
      return true;
    return general.weapons.get().contains(Items.TRIDENT)
        && stack.getItem() instanceof TridentItem;
  }

  private void stopAttacking() {
    if (!attacking)
      return;

    attacking = false;
    if (wasPathing) {
      PathManagers.get().resume();
      wasPathing = false;
    }
    if (general.swapBack.get() && swapped) {
      InvUtils.swap(previousSlot, false);
      swapped = false;
    }
  }

  private boolean delayCheck() {
    if (switchTimer > 0) {
      switchTimer--;
      return false;
    }

    float delay = (timing.customDelay.get()) ? timing.hitDelay.get() : 0.5f;
    if (timing.tpsSync.get())
      delay /= (TickRate.INSTANCE.getTickRate() / 20);

    if (timing.customDelay.get()) {
      if (hitTimer < delay) {
        hitTimer++;
        return false;
      } else
        return true;
    } else
      return mc.player.getAttackStrengthScale(delay) >= 1;
  }

  private boolean entityCheck(Entity entity) {
    if (entity.equals(mc.player) || entity.equals(mc.getCameraEntity()))
      return false;
    if ((entity instanceof LivingEntity livingEntity && livingEntity.isDeadOrDying()) || !entity.isAlive())
      return false;

    AABB hitbox = entity.getBoundingBox();
    if (!PlayerUtils.isWithin(
        Mth.clamp(mc.player.getX(), hitbox.minX, hitbox.maxX),
        Mth.clamp(mc.player.getY(), hitbox.minY, hitbox.maxY),
        Mth.clamp(mc.player.getZ(), hitbox.minZ, hitbox.maxZ),
        targeting.range.get()))
      return false;

    if (!targeting.entities.get().contains(entity.getType()))
      return false;
    if (targeting.ignoreNamed.get() && entity.hasCustomName())
      return false;
    if (!PlayerUtils.canSeeEntity(entity) && !PlayerUtils.isWithin(entity, targeting.wallsRange.get()))
      return false;
    if (targeting.ignoreTamed.get()) {
      if (entity instanceof OwnableEntity tameable
          && tameable.getOwner() != null
          && tameable.getOwner().equals(mc.player))
        return false;
    }
    if (targeting.ignorePassive.get()) {
      if (entity instanceof EnderMan enderman && !enderman.isCreepy())
        return false;
      if ((entity instanceof Piglin || entity instanceof ZombifiedPiglin || entity instanceof Wolf)
          && !((Mob) entity).isAggressive())
        return false;
    }
    if (entity instanceof Player player) {
      if (player.isCreative())
        return false;
      if (!Friends.get().shouldAttack(player))
        return false;
      if (general.shieldMode.get() == ShieldMode.Ignore && player.isBlocking())
        return false;
      if (player instanceof FakePlayerEntity fakePlayer && fakePlayer.noHit)
        return false;
    }
    if (entity instanceof LivingEntity livingEntity) {
      if (entity instanceof Zombie || entity instanceof Piglin
          || entity instanceof Hoglin || entity instanceof Zoglin) {
        return switch (targeting.hostileMobAgeFilter.get()) {
          case Baby -> livingEntity.isBaby();
          case Adult -> !livingEntity.isBaby();
          case Both -> true;
        };
      }
      if (entity instanceof AgeableMob && (!(entity instanceof Frog || entity instanceof Parrot))) {
        return switch (targeting.passiveMobAgeFilter.get()) {
          case Baby -> livingEntity.isBaby();
          case Adult -> !livingEntity.isBaby();
          case Both -> true;
        };
      }
    }
    return true;
  }

  private void updateRotation(boolean attack, float rotationYawSpeed, float rotationPitchSpeed) {
    if (primary == null)
      return;

    double distance = mc.player.distanceTo(primary);
    double lookHeight = clamp(distance / targeting.range.get(), 0, 1) * primary.getBbHeight();
    Vec3 vec = primary.position().add(0, lookHeight, 0).subtract(mc.player.getEyePosition());

    float yawToTarget = (float) wrapDegrees(Math.toDegrees(Math.atan2(vec.z, vec.x)) - 90);
    float pitchToTarget = (float) (-Math.toDegrees(Math.atan2(vec.y, length(vec.x, vec.z))));

    float yawDelta = wrapDegrees(yawToTarget - rotateVector.u());
    float pitchDelta = wrapDegrees(pitchToTarget - rotateVector.v());

    float absYawDelta = Math.abs(yawDelta);
    float absPitchDelta = Math.abs(pitchDelta);
    float clampedYaw = Math.min(absYawDelta, rotationYawSpeed);
    float clampedPitch = Math.min(absPitchDelta, rotationPitchSpeed);

    if (attack && selected != primary && general.speedUpRotationWhenAttacking.get()) {
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

  public enum AttackItems {
    Weapons,
    All
  }

  public enum RotationMode {
    Always,
    OnHit,
    None
  }

  public enum RotationType {
    Smooth,
    Fast
  }

  public enum ShieldMode {
    Ignore,
    Break,
    None
  }

  public enum EntityAge {
    Baby,
    Adult,
    Both
  }
}
