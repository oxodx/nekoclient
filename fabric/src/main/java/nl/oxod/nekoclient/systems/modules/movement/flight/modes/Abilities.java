package nl.oxod.nekoclient.systems.modules.movement.flight.modes;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import nl.oxod.nekoclient.systems.modules.movement.flight.FlyMode;
import nl.oxod.nekoclient.systems.modules.movement.flight.FlyModes;
import nl.oxod.nekoclient.systems.modules.movement.flight.Flight;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;

public class Abilities extends FlyMode {
  public Abilities(Flight settings) {
    super(FlyModes.Abilities, settings);
  }

  @Override
  public void onActivate() {
    if (mc.player.isSpectator()) return;
    mc.player.getAbilities().flying = true;
    if (mc.player.getAbilities().instabuild) return;
    mc.player.getAbilities().mayfly = true;
  }

  @Override
  public void onDeactivate() {
    settings.abilitiesOff();
  }

  @Override
  public void onTickEventPost(TickEvent.Post event) {
    if (mc.player.isSpectator()) return;
    mc.player.getAbilities().setFlyingSpeed(settings.general.speed.get().floatValue());
    mc.player.getAbilities().flying = true;
    if (mc.player.getAbilities().instabuild) return;
    mc.player.getAbilities().mayfly = true;
  }

  @Override
  public void onReceivePacket(PacketEvent.Receive event) {
    if (!(event.packet instanceof ClientboundPlayerAbilitiesPacket packet)) return;
    event.cancel();

    mc.player.getAbilities().invulnerable = packet.isInvulnerable();
    mc.player.getAbilities().instabuild = packet.canInstabuild();
    mc.player.getAbilities().setWalkingSpeed(packet.getWalkingSpeed());
  }
}
