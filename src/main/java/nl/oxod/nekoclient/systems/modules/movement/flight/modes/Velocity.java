package nl.oxod.nekoclient.systems.modules.movement.flight.modes;

import meteordevelopment.meteorclient.events.world.TickEvent;
import nl.oxod.nekoclient.systems.modules.movement.flight.FlyMode;
import nl.oxod.nekoclient.systems.modules.movement.flight.FlyModes;
import nl.oxod.nekoclient.systems.modules.movement.flight.Flight;
import net.minecraft.world.phys.Vec3;

public class Velocity extends FlyMode {
  public Velocity(Flight settings) {
    super(FlyModes.Velocity, settings);
  }

  @Override
  public void onTickEventPost(TickEvent.Post event) {
    mc.player.getAbilities().flying = false;
    mc.player.setDeltaMovement(0, 0, 0);
    Vec3 playerVelocity = mc.player.getDeltaMovement();
    if (mc.options.keyJump.isDown())
      playerVelocity = playerVelocity.add(0, settings.general.speed.get() * (settings.general.verticalSpeedMatch.get()
          ? 10f
          : 5f), 0);
    if (mc.options.keyShift.isDown())
      playerVelocity = playerVelocity.subtract(0, settings.general.speed.get() * (settings.general.verticalSpeedMatch.get()
          ? 10f
          : 5f), 0);
    mc.player.setDeltaMovement(playerVelocity);
    if (settings.general.noSneak.get()) {
      mc.player.setOnGround(false);
    }
  }
}
