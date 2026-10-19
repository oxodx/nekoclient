/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.systems.modules.render;

import java.util.Map;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.world.clock.ClockNetworkState;
import nl.oxod.nekoclient.events.packets.PacketEvent;
import nl.oxod.nekoclient.events.world.TickEvent;
import nl.oxod.nekoclient.settings.DoubleSetting;
import nl.oxod.nekoclient.settings.Setting;
import nl.oxod.nekoclient.settings.SettingGroup;
import nl.oxod.nekoclient.systems.modules.Categories;
import nl.oxod.nekoclient.systems.modules.Module;

public class TimeChanger extends Module {

  private final SettingGroup sgGeneral = settings.getDefaultGroup();

  private final Setting<Double> time = sgGeneral.add(
    new DoubleSetting.Builder()
      .name("time")
      .description("The specified time to be set.")
      .defaultValue(0)
      .sliderRange(-20000, 20000)
      .build()
  );

  long oldTime;

  public TimeChanger() {
    super(
      Categories.Render,
      "time-changer",
      "Makes you able to set a custom time."
    );
  }

  @Override
  public void onActivate() {
    if (mc.level != null) oldTime = mc.level.getDefaultClockTime();
  }

  @Override
  public void onDeactivate() {
    if (mc.level == null) return;

    mc.level
      .dimensionType()
      .defaultClock()
      .ifPresent(clock -> {
        mc.level
          .clockManager()
          .handleUpdates(
            mc.level.getGameTime(),
            Map.of(clock, new ClockNetworkState(oldTime, 0f, 1f))
          );
        mc.level.environmentAttributes().invalidateTickCache();
      });
  }

  @EventHandler
  private void onPacketReceive(PacketEvent.Receive event) {
    if (event.packet instanceof ClientboundSetTimePacket packet) {
      if (mc.level == null) return;

      mc.execute(() -> {
        if (mc.level != null) {
          mc.level.setTimeFromServer(packet.gameTime());
          mc.level
            .dimensionType()
            .defaultClock()
            .ifPresent(clock -> {
              ClockNetworkState state = packet.clockUpdates().get(clock);
              if (state != null) oldTime = state.totalTicks();
            });
        }
      });
      event.cancel();
    }
  }

  @EventHandler
  private void onTick(TickEvent.Post event) {
    if (mc.level == null) return;

    long customTime = time.get().longValue();
    mc.level
      .dimensionType()
      .defaultClock()
      .ifPresent(clock -> {
        mc.level
          .clockManager()
          .handleUpdates(
            mc.level.getGameTime(),
            Map.of(clock, new ClockNetworkState(customTime, 0f, 0f))
          );
        mc.level.environmentAttributes().invalidateTickCache();
      });
  }
}
