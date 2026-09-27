/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.utils;

import nl.oxod.nekoclient.mixin.KillAuraMultiPlayerGameModeAccessor;
import net.minecraft.client.Minecraft;

public final class InventoryHelper {
  private InventoryHelper() {
  }

  public static void selectHotbarSlot(Minecraft mc, int slot) {
    if (mc == null || mc.player == null || mc.gameMode == null || mc.getConnection() == null) return;

    int clampedSlot = Math.max(0, Math.min(8, slot));
    if (mc.player.getInventory().getSelectedSlot() == clampedSlot) return;

    mc.player.getInventory().setSelectedSlot(clampedSlot);
    ((KillAuraMultiPlayerGameModeAccessor) mc.gameMode).killAura$ensureHasSentCarriedItem();
  }
}
