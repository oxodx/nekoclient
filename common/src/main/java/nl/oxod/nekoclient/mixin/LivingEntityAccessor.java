/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.tags.TagKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
    @Invoker("jumpInLiquid")
    void neko$swimUpwards(TagKey<Fluid> fluid);

    @Accessor("jumping")
    boolean neko$isJumping();

    @Accessor("noJumpDelay")
    int neko$getJumpCooldown();

    @Accessor("noJumpDelay")
    void neko$setJumpCooldown(int cooldown);
}
