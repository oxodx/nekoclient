package nl.oxod.nekoclient.util;

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
