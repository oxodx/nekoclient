/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.systems.modules.combat;

import nl.oxod.nekoclient.events.game.OpenScreenEvent;
import nl.oxod.nekoclient.events.world.TickEvent;
import nl.oxod.nekoclient.systems.modules.Categories;
import nl.oxod.nekoclient.systems.modules.Module;
import nl.oxod.nekoclient.utils.player.InvUtils;
import nl.oxod.nekoclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Block;

public class SelfAnvil extends Module {
  public SelfAnvil() {
    super(Categories.Combat, "self-anvil", "Automatically places an anvil on you to prevent other players from going into your hole.");
  }

  @EventHandler
  private void onOpenScreen(OpenScreenEvent event) {
    if (event.screen instanceof AnvilScreen) event.cancel();
  }

  @EventHandler
  private void onTick(TickEvent.Pre event) {
    if (BlockUtils.place(mc.player.blockPosition().offset(0, 2, 0), InvUtils.findInHotbar(itemStack -> Block.byItem(itemStack.getItem()) instanceof AnvilBlock), 0)) {
      toggle();
    }
  }
}
