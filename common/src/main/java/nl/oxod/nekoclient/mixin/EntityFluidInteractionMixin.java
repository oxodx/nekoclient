/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import nl.oxod.nekoclient.systems.modules.Modules;
import nl.oxod.nekoclient.systems.modules.movement.Velocity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityFluidInteraction;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import static nl.oxod.nekoclient.NekoClient.mc;

@Mixin(EntityFluidInteraction.class)
public abstract class EntityFluidInteractionMixin {
    @ModifyExpressionValue(
        // The descriptor is required: NeoForge adds an update(Entity, Predicate) overload, and an
        // unqualified "update" would match both, fail to find the getFlow call in the boolean
        // delegate, and abort the boot. Gated to Fabric by MixinPlugin; see
        // EntityFluidInteractionNeoForgeMixin for the NeoForge half.
        method = "update(Lnet/minecraft/world/entity/Entity;Z)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/material/FluidState;getFlow(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/Vec3;")
    )
    private Vec3 modifyFluidFlow(Vec3 flow, final Entity entity, final boolean ignoreCurrent) {
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
