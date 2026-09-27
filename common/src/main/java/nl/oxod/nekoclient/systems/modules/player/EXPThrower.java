/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.systems.modules.player;

import nl.oxod.nekoclient.events.world.TickEvent;
import nl.oxod.nekoclient.systems.modules.Categories;
import nl.oxod.nekoclient.systems.modules.Module;
import nl.oxod.nekoclient.utils.player.FindItemResult;
import nl.oxod.nekoclient.utils.player.InvUtils;
import nl.oxod.nekoclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.item.Items;

public class EXPThrower extends Module {
  public EXPThrower() {
    super(Categories.Player, "exp-thrower", "Automatically throws XP bottles from your hotbar.");
  }

  @EventHandler
  private void onTick(TickEvent.Pre event) {
    FindItemResult exp = InvUtils.findInHotbar(Items.EXPERIENCE_BOTTLE);
    if (!exp.found()) return;

    Rotations.rotate(mc.player.getYRot(), 90, () -> {
      if (exp.getHand() != null) {
        mc.gameMode.useItem(mc.player, exp.getHand());
      } else {
        InvUtils.swap(exp.slot(), true);
        mc.gameMode.useItem(mc.player, exp.getHand());
        InvUtils.swapBack();
      }
    });
  }
}
