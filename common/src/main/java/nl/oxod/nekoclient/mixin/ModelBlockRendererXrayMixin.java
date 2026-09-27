/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import nl.oxod.nekoclient.systems.modules.Modules;
import nl.oxod.nekoclient.systems.modules.render.Xray;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Fabric half of the Xray side-visibility hook.
 *
 * <p>NeoForge adds a five-argument {@code shouldRenderFace(BlockAndTintGetter, BlockPos,
 * BlockState, Direction, BlockPos)} and keeps the vanilla four-argument form as deprecated, so an
 * unqualified method name resolves to both and the handler signature only fits one -- which makes
 * Mixin throw {@code InvalidInjectionException}. The descriptor below pins the vanilla overload.
 * See {@link ModelBlockRendererXrayNeoForgeMixin} for the NeoForge half.
 */
@Mixin(ModelBlockRenderer.class)
public abstract class ModelBlockRendererXrayMixin {
    @ModifyReturnValue(
        method = "shouldRenderFace(Lnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;Lnet/minecraft/core/BlockPos;)Z",
        at = @At("RETURN"))
    private static boolean shouldRenderFace$xray(boolean original, BlockAndTintGetter level, BlockState state, Direction direction, BlockPos neighborPos) {
        Xray xray = Modules.get().get(Xray.class);

        if (xray.isActive()) {
            return xray.modifyDrawSide(state, level, neighborPos.relative(direction.getOpposite()), direction, original);
        }

        return original;
    }
}
