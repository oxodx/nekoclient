package meteordevelopment.meteorclient.systems.modules.combat.killaura.modes;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.CrystalAura;
import meteordevelopment.meteorclient.systems.modules.combat.killaura.KillAura.AttackItems;
import meteordevelopment.meteorclient.systems.modules.combat.killaura.KillAura.RotationMode;
import meteordevelopment.meteorclient.systems.modules.combat.killaura.KillAuraMode;
import meteordevelopment.meteorclient.systems.modules.combat.killaura.KillAuraModes;
import meteordevelopment.meteorclient.utils.entity.Target;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.world.TickRate;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.level.GameType;

public class Vannila extends KillAuraMode {
  public Vannila() {
    super(KillAuraModes.Vannila);
  }

  @Override
  public void onActivate() {
    previousSlot = -1;
    swapped = false;
  }

  @Override
  public void onDeactivate() {
    targets.clear();
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
    isAllowedToAttack();

    Entity primary = targets.getFirst();

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
    if (settings.rotation.get() == RotationMode.Always)
      Rotations.rotate(Rotations.getYaw(primary), Rotations.getPitch(primary, Target.Body));
    if (settings.pauseOnCombat.get() && PathManagers.get().isPathing() && !wasPathing) {
      PathManagers.get().pause();
      wasPathing = true;
    }

    if (delayCheck()) targets.forEach(this::attack);
  }
}
