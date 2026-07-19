package meteordevelopment.meteorclient.systems.modules.combat.killaura;

import java.util.ArrayList;
import java.util.List;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.CrystalAura;
import meteordevelopment.meteorclient.systems.modules.combat.killaura.KillAura.AttackItems;
import meteordevelopment.meteorclient.systems.modules.combat.killaura.KillAura.RotationMode;
import meteordevelopment.meteorclient.systems.modules.combat.killaura.KillAura.ShieldMode;
import meteordevelopment.meteorclient.utils.entity.Target;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.world.TickRate;
import net.minecraft.client.Minecraft;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
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
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;

public class KillAuraMode {
  protected final Minecraft mc;
  protected final KillAura settings;
  private final KillAuraModes type;

  protected final List<Entity> targets = new ArrayList<>();
  protected boolean wasPathing = false;
  protected int switchTimer, hitTimer;
  protected boolean attacking, swapped;
  protected static int previousSlot;

  public KillAuraMode(KillAuraModes type) {
    this.settings = Modules.get().get(KillAura.class);
    this.mc = Minecraft.getInstance();
    this.type = type;
  }

  protected void isAllowedToAttack() {
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
      stopAttacking();
      return;
    }
  }

  protected void autoSwitch() {
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
  }

  protected void attack(Entity target) {
    if (settings.rotation.get() == RotationMode.OnHit)
      Rotations.rotate(Rotations.getYaw(target), Rotations.getPitch(target, Target.Body));

    mc.gameMode.attack(mc.player, target);
    mc.player.swing(InteractionHand.MAIN_HAND);

    hitTimer = 0;
  }

  protected boolean shouldShieldBreak() {
    for (Entity target : targets) {
      if (target instanceof Player player) {
        if (player.isBlocking() && settings.shieldMode.get() == ShieldMode.Break) {
          return true;
        }
      }
    }

    return false;
  }

  protected boolean acceptableWeapon(ItemStack stack) {
    if (shouldShieldBreak()) return stack.getItem() instanceof AxeItem;
    if (settings.attackWhenHolding.get() == AttackItems.All) return true;

    if (settings.weapons.get().contains(Items.DIAMOND_SWORD) && stack.is(ItemTags.SWORDS)) return true;
    if (settings.weapons.get().contains(Items.DIAMOND_AXE) && stack.is(ItemTags.AXES)) return true;
    if (settings.weapons.get().contains(Items.DIAMOND_PICKAXE) && stack.is(ItemTags.PICKAXES)) return true;
    if (settings.weapons.get().contains(Items.DIAMOND_SHOVEL) && stack.is(ItemTags.SHOVELS)) return true;
    if (settings.weapons.get().contains(Items.DIAMOND_HOE) && stack.is(ItemTags.HOES)) return true;
    if (settings.weapons.get().contains(Items.MACE) && stack.getItem() instanceof MaceItem) return true;
    if (settings.weapons.get().contains(Items.DIAMOND_SPEAR) && stack.is(ItemTags.SPEARS)) return true;
    return settings.weapons.get().contains(Items.TRIDENT) && stack.getItem() instanceof TridentItem;
  }

  protected void stopAttacking() {
    if (!attacking) return;

    attacking = false;
    if (wasPathing) {
      PathManagers.get().resume();
      wasPathing = false;
    }
    if (settings.swapBack.get() && swapped) {
      InvUtils.swap(previousSlot, false);
      swapped = false;
    }
  }

  protected boolean delayCheck() {
    if (switchTimer > 0) {
      switchTimer--;
      return false;
    }

    float delay = (settings.customDelay.get()) ? settings.hitDelay.get() : 0.5f;
    if (settings.tpsSync.get()) delay /= (TickRate.INSTANCE.getTickRate() / 20);

    if (settings.customDelay.get()) {
      if (hitTimer < delay) {
        hitTimer++;
        return false;
      } else return true;
    } else return mc.player.getAttackStrengthScale(delay) >= 1;
  }

  protected boolean entityCheck(Entity entity) {
    if (entity.equals(mc.player) || entity.equals(mc.getCameraEntity())) return false;
    if ((entity instanceof LivingEntity livingEntity && livingEntity.isDeadOrDying()) || !entity.isAlive())
      return false;

    AABB hitbox = entity.getBoundingBox();
    if (!PlayerUtils.isWithin(
      Mth.clamp(mc.player.getX(), hitbox.minX, hitbox.maxX),
      Mth.clamp(mc.player.getY(), hitbox.minY, hitbox.maxY),
      Mth.clamp(mc.player.getZ(), hitbox.minZ, hitbox.maxZ),
      settings.range.get()
    )) return false;

    if (!settings.entities.get().contains(entity.getType())) return false;
    if (settings.ignoreNamed.get() && entity.hasCustomName()) return false;
    if (!PlayerUtils.canSeeEntity(entity) && !PlayerUtils.isWithin(entity, settings.wallsRange.get()))
      return false;
    if (settings.ignoreTamed.get()) {
      if (entity instanceof OwnableEntity tameable
        && tameable.getOwner() != null
        && tameable.getOwner().equals(mc.player)
      ) return false;
    }
    if (settings.ignorePassive.get()) {
      if (entity instanceof EnderMan enderman && !enderman.isCreepy()) return false;
      if ((entity instanceof Piglin || entity instanceof ZombifiedPiglin || entity instanceof Wolf) && !((Mob) entity).isAggressive())
        return false;
    }
    if (entity instanceof Player player) {
      if (player.isCreative()) return false;
      if (!Friends.get().shouldAttack(player)) return false;
      if (settings.shieldMode.get() == ShieldMode.Ignore && player.isBlocking()) return false;
      if (player instanceof FakePlayerEntity fakePlayer && fakePlayer.noHit) return false;
    }
    if (entity instanceof LivingEntity livingEntity) {
      // Hostile mobs with baby variants (zombies, piglins, hoglins, zoglins)
      if (entity instanceof Zombie || entity instanceof Piglin
        || entity instanceof Hoglin || entity instanceof Zoglin) {
        return switch (settings.hostileMobAgeFilter.get()) {
          case Baby -> livingEntity.isBaby();
          case Adult -> !livingEntity.isBaby();
          case Both -> true;
        };
      }
      // Passive mobs with baby variants (animals, villagers)
      if (entity instanceof AgeableMob && (!(entity instanceof Frog || entity instanceof Parrot))) {
        return switch (settings.passiveMobAgeFilter.get()) {
          case Baby -> livingEntity.isBaby();
          case Adult -> !livingEntity.isBaby();
          case Both -> true;
        };
      }
    }
    return true;
  }

  public void onActivate() {
	}

	public void onDeactivate() {
	}

	public void onTickPre(TickEvent.Pre event, Entity target) {
	}

	public void onTickPost(TickEvent.Post event) {
	}

	public void onSendPacket(PacketEvent.Send event) {
	}

	public String getInfoString() {
		return "";
	}
}
