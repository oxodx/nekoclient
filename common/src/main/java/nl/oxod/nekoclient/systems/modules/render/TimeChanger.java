/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.systems.modules.render;

import nl.oxod.nekoclient.events.packets.PacketEvent;
import nl.oxod.nekoclient.events.world.TickEvent;
import nl.oxod.nekoclient.settings.DoubleSetting;
import nl.oxod.nekoclient.settings.Setting;
import nl.oxod.nekoclient.settings.SettingGroup;
import nl.oxod.nekoclient.systems.modules.Categories;
import nl.oxod.nekoclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;

public class TimeChanger extends Module {
  private final SettingGroup sgGeneral = settings.getDefaultGroup();

  private final Setting<Double> time = sgGeneral.add(new DoubleSetting.Builder()
    .name("time")
    .description("The specified time to be set.")
    .defaultValue(0)
    .sliderRange(-20000, 20000)
    .build()
  );

  long oldTime;

  public TimeChanger() {
    super(Categories.Render, "time-changer", "Makes you able to set a custom time.");
  }

  @Override
  public void onActivate() {
    oldTime = mc.level.getGameTime();
  }

  @Override
  public void onDeactivate() {
    mc.level.getLevelData().setGameTime(oldTime);
  }

  @EventHandler
  private void onPacketReceive(PacketEvent.Receive event) {
    if (event.packet instanceof ClientboundSetTimePacket packet) {
      oldTime = packet.gameTime();
      event.cancel();
    }
  }

  @EventHandler
  private void onTick(TickEvent.Post event) {
    mc.level.getLevelData().setGameTime(time.get().longValue());
  }
}
