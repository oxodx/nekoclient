/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import nl.oxod.nekoclient.systems.modules.Modules;
import nl.oxod.nekoclient.systems.modules.movement.Jesus;
import net.minecraft.world.level.block.PowderSnowBlock;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import static nl.oxod.nekoclient.NekoClient.mc;

@Mixin(PowderSnowBlock.class)
public abstract class PowderSnowBlockMixin {
  @ModifyReturnValue(method = "canEntityWalkOnPowderSnow", at = @At("RETURN"))
  private static boolean onCanWalkOnPowderSnow(boolean original, Entity entity) {
    if (entity == mc.player && Modules.get().get(Jesus.class).canWalkOnPowderSnow()) return true;
    return original;
  }
}
