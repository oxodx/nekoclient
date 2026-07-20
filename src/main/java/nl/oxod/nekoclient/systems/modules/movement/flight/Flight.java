package nl.oxod.nekoclient.systems.modules.movement.flight;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.LocalPlayerAccessor;
import meteordevelopment.meteorclient.mixin.ServerboundMovePlayerPacketAccessor;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.Vec3;
import nl.oxod.nekoclient.systems.modules.movement.flight.settings.AntiKick;
import nl.oxod.nekoclient.systems.modules.movement.flight.settings.General;

public class Flight extends Module {
  public final General general = new General(settings.getDefaultGroup(), this);
  public final AntiKick antiKick = new AntiKick(settings.createGroup("Anti Kick"), this);

  private int delayLeft = antiKick.delay.get();
  private int offLeft = antiKick.offTime.get();
  private boolean flip;
  private float lastYaw;
  private double lastPacketY = Double.MAX_VALUE;

  public Flight() {
    super(Categories.Neko_Movement, "flight", "FLYYYY! No Fall is recommended with this module.");
  }

  @Override
  public void onActivate() {
    if (general.mode.get() == Mode.Abilities && !mc.player.isSpectator()) {
      mc.player.getAbilities().flying = true;
      if (mc.player.getAbilities().instabuild)
        return;
      mc.player.getAbilities().mayfly = true;
    }
  }

  @Override
  public void onDeactivate() {
    if (general.mode.get() == Mode.Abilities && !mc.player.isSpectator()) {
      abilitiesOff();
    }
  }

  @EventHandler
  private void onPreTick(TickEvent.Pre event) {
    float currentYaw = mc.player.getYRot();
    if (mc.player.fallDistance >= 3f && currentYaw == lastYaw && mc.player.getDeltaMovement().length() < 0.003d) {
      mc.player.setYRot(currentYaw + (flip ? 1 : -1));
      flip = !flip;
    }
    lastYaw = currentYaw;
  }

  @EventHandler
  private void onPostTick(TickEvent.Post event) {
    if (delayLeft > 0)
      delayLeft--;

    if (offLeft <= 0 && delayLeft <= 0) {
      delayLeft = antiKick.delay.get();
      offLeft = antiKick.offTime.get();

      if (antiKick.antiKickMode.get() == AntiKickMode.Packet) {
        // Resend movement packets
        ((LocalPlayerAccessor) mc.player).meteor$setPositionReminder(20);
      }
    } else if (delayLeft <= 0) {
      boolean shouldReturn = false;

      if (antiKick.antiKickMode.get() == AntiKickMode.Normal) {
        if (general.mode.get() == Mode.Abilities) {
          abilitiesOff();
          shouldReturn = true;
        }
      } else if (antiKick.antiKickMode.get() == AntiKickMode.Packet && offLeft == antiKick.offTime.get()) {
        // Resend movement packets
        ((LocalPlayerAccessor) mc.player).meteor$setPositionReminder(20);
      }

      offLeft--;

      if (shouldReturn)
        return;
    }

    if (mc.player.getYRot() != lastYaw)
      mc.player.setYRot(lastYaw);

    switch (general.mode.get()) {
      case Velocity -> {
        mc.player.getAbilities().flying = false;
        mc.player.setDeltaMovement(0, 0, 0);
        Vec3 playerVelocity = mc.player.getDeltaMovement();
        if (mc.options.keyJump.isDown())
          playerVelocity = playerVelocity.add(0, general.speed.get() * (general.verticalSpeedMatch.get()
              ? 10f
              : 5f), 0);
        if (mc.options.keyShift.isDown())
          playerVelocity = playerVelocity.subtract(0, general.speed.get() * (general.verticalSpeedMatch.get()
              ? 10f
              : 5f), 0);
        mc.player.setDeltaMovement(playerVelocity);
        if (general.noSneak.get()) {
          mc.player.setOnGround(false);
        }
      }
      case Abilities -> {
        if (mc.player.isSpectator())
          return;
        mc.player.getAbilities().setFlyingSpeed(general.speed.get().floatValue());
        mc.player.getAbilities().flying = true;
        if (mc.player.getAbilities().instabuild)
          return;
        mc.player.getAbilities().mayfly = true;
      }
    }
  }

  private void antiKickPacket(ServerboundMovePlayerPacket packet, double currentY) {
    // maximum time we can be "floating" is 80 ticks, so 4 seconds max
    if (this.delayLeft <= 0 && this.lastPacketY != Double.MAX_VALUE &&
        shouldFlyDown(currentY, this.lastPacketY) && EntityUtils.isOnAir(mc.player)) {
      // actual check is for >= -0.03125D, but we have to do a bit more than that
      // due to the fact that it's a bigger or *equal* to, and not just a bigger than
      ((ServerboundMovePlayerPacketAccessor) packet).meteor$setY(lastPacketY - 0.03130D);
    } else {
      lastPacketY = currentY;
    }
  }

  /**
   * @see net.minecraft.network.protocol.game.ServerGamePacketListener#handleMovePlayer(ServerboundMovePlayerPacket)
   */
  @EventHandler
  private void onSendPacket(PacketEvent.Send event) {
    if (!(event.packet instanceof ServerboundMovePlayerPacket packet)
        || antiKick.antiKickMode.get() != AntiKickMode.Packet)
      return;

    double currentY = packet.getY(Double.MAX_VALUE);
    if (currentY != Double.MAX_VALUE) {
      antiKickPacket(packet, currentY);
    } else {
      // if the packet is a Rot packet or an StatusOnly packet then we need to
      // make it a PosRot packet or a Pos packet respectively, so it has a Y value
      ServerboundMovePlayerPacket fullPacket;
      if (packet.hasRotation()) {
        fullPacket = new ServerboundMovePlayerPacket.PosRot(
            mc.player.getX(),
            mc.player.getY(),
            mc.player.getZ(),
            packet.getYRot(0),
            packet.getXRot(0),
            packet.isOnGround(),
            mc.player.horizontalCollision);
      } else {
        fullPacket = new ServerboundMovePlayerPacket.Pos(
            mc.player.getX(),
            mc.player.getY(),
            mc.player.getZ(),
            packet.isOnGround(),
            mc.player.horizontalCollision);
      }
      event.cancel();
      antiKickPacket(fullPacket, mc.player.getY());
      mc.getConnection().send(fullPacket);
    }
  }

  @EventHandler
  private void onReceivePacket(PacketEvent.Receive event) {
    if (!(event.packet instanceof ClientboundPlayerAbilitiesPacket packet) || general.mode.get() != Mode.Abilities)
      return;
    event.cancel(); // Cancel packet, so fly won't be toggled

    mc.player.getAbilities().invulnerable = packet.isInvulnerable();
    mc.player.getAbilities().instabuild = packet.canInstabuild();
    mc.player.getAbilities().setWalkingSpeed(packet.getWalkingSpeed());
  }

  private boolean shouldFlyDown(double currentY, double lastY) {
    if (currentY >= lastY) {
      return true;
    } else
      return lastY - currentY < 0.03130D;
  }

  public void abilitiesOff() {
    mc.player.getAbilities().flying = false;
    mc.player.getAbilities().setFlyingSpeed(0.05f);
    if (mc.player.getAbilities().instabuild)
      return;
    mc.player.getAbilities().mayfly = false;
  }

  public float getFlyingSpeed() {
    // All the multiplication below is to get the speed to roughly match the speed
    // you get when using vanilla fly

    if (!isActive() || general.mode.get() != Mode.Velocity)
      return -1;
    return general.speed.get().floatValue() * (mc.player.isSprinting() ? 15f : 10f);
  }

  public boolean noSneak() {
    return isActive() && general.mode.get() == Mode.Velocity && general.noSneak.get();
  }

  public enum Mode {
    Abilities,
    Velocity
  }

  public enum AntiKickMode {
    Normal,
    Packet,
    None
  }
}
