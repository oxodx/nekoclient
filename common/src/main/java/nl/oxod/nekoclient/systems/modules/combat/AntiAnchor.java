/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.systems.modules.combat;

import nl.oxod.nekoclient.events.world.TickEvent;
import nl.oxod.nekoclient.settings.BoolSetting;
import nl.oxod.nekoclient.settings.Setting;
import nl.oxod.nekoclient.settings.SettingGroup;
import nl.oxod.nekoclient.systems.modules.Categories;
import nl.oxod.nekoclient.systems.modules.Module;
import nl.oxod.nekoclient.utils.player.InvUtils;
import nl.oxod.nekoclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;

public class AntiAnchor extends Module {
  private final SettingGroup sgGeneral = settings.getDefaultGroup();

  private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
    .name("rotate")
    .description("Makes you rotate when placing.")
    .defaultValue(true)
    .build()
  );

  private final Setting<Boolean> swing = sgGeneral.add(new BoolSetting.Builder()
    .name("swing")
    .description("Swings your hand when placing.")
    .defaultValue(true)
    .build()
  );

  public AntiAnchor() {
    super(Categories.Combat, "anti-anchor", "Automatically prevents Anchor Aura by placing a slab on your head.");
  }

  @EventHandler
  private void onTick(TickEvent.Pre event) {
    if (mc.level.getBlockState(mc.player.blockPosition().above(2)).getBlock() == Blocks.RESPAWN_ANCHOR
      && mc.level.getBlockState(mc.player.blockPosition().above()).getBlock() == Blocks.AIR) {

      BlockUtils.place(
        mc.player.blockPosition().offset(0, 1, 0),
        InvUtils.findInHotbar(itemStack -> Block.byItem(itemStack.getItem()) instanceof SlabBlock),
        rotate.get(),
        15,
        swing.get(),
        false,
        true
      );
    }
  }
}
