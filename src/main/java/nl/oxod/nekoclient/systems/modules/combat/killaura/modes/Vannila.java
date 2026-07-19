package nl.oxod.nekoclient.systems.modules.combat.killaura.modes;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.pathing.PathManagers;
import meteordevelopment.meteorclient.utils.entity.Target;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.entity.Entity;
import nl.oxod.nekoclient.systems.modules.combat.killaura.KillAuraMode;
import nl.oxod.nekoclient.systems.modules.combat.killaura.KillAura.RotationMode;

public class Vannila extends KillAuraMode {
  public Vannila() {
    super();
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
      switchTimer = settings.timing.switchDelay.get();
    }
  }

  @Override
  public void onTickPre(TickEvent.Pre event, Entity target) {
    if (settings.general.rotation.get() == RotationMode.Always)
      Rotations.rotate(Rotations.getYaw(target), Rotations.getPitch(target, Target.Body));
    if (settings.general.pauseOnCombat.get() && PathManagers.get().isPathing() && !wasPathing) {
      PathManagers.get().pause();
      wasPathing = true;
    }

    if (delayCheck()) targets.forEach(this::attack);
  }
}
