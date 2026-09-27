/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.mixin;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MultiPlayerGameMode.class)
public interface MultiPlayerGameModeAccessor {
  @Accessor("destroyProgress")
  float neko$getBreakingProgress();

  @Accessor("destroyProgress")
  void neko$setDestroyProgress(float progress);

  @Accessor("destroyBlockPos")
  BlockPos neko$getCurrentBreakingBlockPos();
}
