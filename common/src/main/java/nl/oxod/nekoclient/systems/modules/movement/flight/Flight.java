package nl.oxod.nekoclient.systems.modules.movement.flight;

import nl.oxod.nekoclient.events.entity.player.CanWalkOnFluidEvent;
import nl.oxod.nekoclient.events.entity.player.PlayerMoveEvent;
import nl.oxod.nekoclient.events.entity.player.SendMovementPacketsEvent;
import nl.oxod.nekoclient.events.packets.PacketEvent;
import nl.oxod.nekoclient.events.world.CollisionShapeEvent;
import nl.oxod.nekoclient.events.world.TickEvent;
import nl.oxod.nekoclient.gui.GuiTheme;
import nl.oxod.nekoclient.gui.widgets.WWidget;
import nl.oxod.nekoclient.mixin.LocalPlayerAccessor;
import nl.oxod.nekoclient.mixin.ServerboundMovePlayerPacketAccessor;
import nl.oxod.nekoclient.systems.modules.Categories;
import nl.oxod.nekoclient.systems.modules.Module;
import nl.oxod.nekoclient.utils.entity.EntityUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import nl.oxod.nekoclient.systems.modules.movement.flight.modes.Abilities;
import nl.oxod.nekoclient.systems.modules.movement.flight.modes.MatrixExploit;
import nl.oxod.nekoclient.systems.modules.movement.flight.modes.MatrixExploit2;
import nl.oxod.nekoclient.systems.modules.movement.flight.modes.Velocity;
import nl.oxod.nekoclient.systems.modules.movement.flight.modes.VulcanClip;
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

  private FlyMode currentMode;

  public Flight() {
    super(Categories.Movement, "flight", "FLYYYY! No Fall is recommended with this module.");
    onFlyModeChanged(general.flyMode.get());
  }

  @Override
  public WWidget getWidget(GuiTheme theme) {
    WWidget widget = super.getWidget(theme);
    if (general.flyMode.get() == FlyModes.Vulcan_Clip) {
      return theme.label("This mode works only on 1.8.9 servers");
    }
    return widget;
  }

  @Override
  public void onActivate() {
    currentMode.onActivate();
  }

  @Override
  public void onDeactivate() {
    currentMode.onDeactivate();
  }

  @EventHandler
  private void onPreTick(TickEvent.Pre event) {
    float currentYaw = mc.player.getYRot();
    if (mc.player.fallDistance >= 3f && currentYaw == lastYaw && mc.player.getDeltaMovement().length() < 0.003d) {
      mc.player.setYRot(currentYaw + (flip ? 1 : -1));
      flip = !flip;
    }
    lastYaw = currentYaw;

    currentMode.onTickEventPre(event);
  }

  @EventHandler
  private void onPostTick(TickEvent.Post event) {
    if (delayLeft > 0)
      delayLeft--;

    if (offLeft <= 0 && delayLeft <= 0) {
      delayLeft = antiKick.delay.get();
      offLeft = antiKick.offTime.get();

      if (antiKick.antiKickMode.get() == AntiKickMode.Packet) {
        ((LocalPlayerAccessor) mc.player).neko$setPositionReminder(20);
      }
    } else if (delayLeft <= 0) {
      boolean shouldReturn = false;

      if (antiKick.antiKickMode.get() == AntiKickMode.Normal) {
        if (general.flyMode.get() == FlyModes.Abilities) {
          abilitiesOff();
          shouldReturn = true;
        }
      } else if (antiKick.antiKickMode.get() == AntiKickMode.Packet && offLeft == antiKick.offTime.get()) {
        ((LocalPlayerAccessor) mc.player).neko$setPositionReminder(20);
      }

      offLeft--;

      if (shouldReturn)
        return;
    }

    if (mc.player.getYRot() != lastYaw)
      mc.player.setYRot(lastYaw);

    currentMode.onTickEventPost(event);
  }

  private void antiKickPacket(ServerboundMovePlayerPacket packet, double currentY) {
    if (this.delayLeft <= 0 && this.lastPacketY != Double.MAX_VALUE &&
      shouldFlyDown(currentY, this.lastPacketY) && EntityUtils.isOnAir(mc.player)) {
      ((ServerboundMovePlayerPacketAccessor) packet).neko$setY(lastPacketY - 0.03130D);
    } else {
      lastPacketY = currentY;
    }
  }

  @EventHandler
  private void onSendPacket(PacketEvent.Send event) {
    currentMode.onSendPacket(event);

    if (!(event.packet instanceof ServerboundMovePlayerPacket packet)
      || antiKick.antiKickMode.get() != AntiKickMode.Packet)
      return;

    double currentY = packet.getY(Double.MAX_VALUE);
    if (currentY != Double.MAX_VALUE) {
      antiKickPacket(packet, currentY);
    } else {
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
  public void onSentPacket(PacketEvent.Sent event) {
    currentMode.onSentPacket(event);
  }

  @EventHandler
  private void onReceivePacket(PacketEvent.Receive event) {
    currentMode.onReceivePacket(event);
  }

  @EventHandler
  public void onCanWalkOnFluid(CanWalkOnFluidEvent event) {
    currentMode.onCanWalkOnFluid(event);
  }

  @EventHandler
  public void onCollisionShape(CollisionShapeEvent event) {
    currentMode.onCollisionShape(event);
  }

  @EventHandler
  private void onPlayerMoveEvent(PlayerMoveEvent event) {
    currentMode.onPlayerMoveEvent(event);
  }

  @EventHandler
  private void onPlayerMoveSendPre(SendMovementPacketsEvent.Pre event) {
    currentMode.onPlayerMoveSendPre(event);
  }

  public void onFlyModeChanged(FlyModes mode) {
    switch (mode) {
      case Abilities -> currentMode = new Abilities(this);
      case Velocity -> currentMode = new Velocity(this);
      case Matrix_Exploit -> currentMode = new MatrixExploit(this);
      case Matrix_Exploit_2 -> currentMode = new MatrixExploit2(this);
      case Vulcan_Clip -> {
        if (general.showInfo.get()) {
          info("Vulcan fly works on 1.8.9 servers");
        }
        currentMode = new VulcanClip(this);
      }
    }
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
    if (!isActive() || general.flyMode.get() != FlyModes.Velocity)
      return -1;
    return general.speed.get().floatValue() * (mc.player.isSprinting() ? 15f : 10f);
  }

  public boolean noSneak() {
    return isActive() && general.flyMode.get() == FlyModes.Velocity && general.noSneak.get();
  }

  public enum AntiKickMode {
    Normal,
    Packet,
    None
  }
}
