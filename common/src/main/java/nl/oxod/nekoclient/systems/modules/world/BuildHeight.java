/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.systems.modules.world;

import nl.oxod.nekoclient.events.packets.PacketEvent;
import nl.oxod.nekoclient.mixin.BlockHitResultAccessor;
import nl.oxod.nekoclient.systems.modules.Categories;
import nl.oxod.nekoclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;

public class BuildHeight extends Module {
  public BuildHeight() {
    super(Categories.World, "build-height", "Allows you to interact with objects at the build limit.");
  }

  @EventHandler
  private void onSendPacket(PacketEvent.Send event) {
    if (!(event.packet instanceof ServerboundUseItemOnPacket p)) return;
    if (mc.level == null) return;
    if (p.getHitResult().getLocation().y >= mc.level.getHeight() && p.getHitResult().getDirection() == Direction.UP) {
      ((BlockHitResultAccessor) p.getHitResult()).neko$setDirection(Direction.DOWN);
    }
  }
}
