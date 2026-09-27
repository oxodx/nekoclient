/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.Velocity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityFluidInteraction;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Predicate;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/**
 * NeoForge half of the {@link EntityFluidInteractionMixin} pair.
 *
 * <p>NeoForge rewrites {@code update(Entity, boolean)} into a one-line delegate and moves the real
 * body into a new {@code update(Entity, Predicate<FluidType>)} overload, so the injection point
 * lives in a different method on each loader. Gated to NeoForge by MixinPlugin; the two must never
 * both apply, and the method name must always be given with its descriptor.
 *
 * <p>The predicate parameter is declared erased on purpose: this module compiles against
 * Mojang mappings, where {@code FluidType} does not exist, and the value is never used.
 */
@Mixin(EntityFluidInteraction.class)
public abstract class EntityFluidInteractionNeoForgeMixin {
    @ModifyExpressionValue(
        method = "update(Lnet/minecraft/world/entity/Entity;Ljava/util/function/Predicate;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/material/FluidState;getFlow(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/Vec3;")
    )
    private Vec3 modifyFluidFlow(Vec3 flow, final Entity entity, final Predicate<?> typePushPredicate) {
        if (entity != mc.player) return flow;

        Velocity velocity = Modules.get().get(Velocity.class);
        if (velocity.isActive() && velocity.liquids.get()) {
            double h = velocity.getHorizontal(velocity.liquidsHorizontal);
            double v = velocity.getVertical(velocity.liquidsVertical);
            flow = flow.multiply(h, v, h);
        }

        return flow;
    }
}
