/*
* This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/nekoclient).
* Copyright (c) Meteor Development.
*/

package nl.oxod.nekoclient.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.pipeline.PipelineCache;
import nl.oxod.nekoclient.renderer.NekoRenderPipelines;
import net.minecraft.client.renderer.ShaderManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShaderManager.class)
public abstract class ShaderManagerMixin {
  @Inject(method = "apply", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setCurrentPipelineCache(Lcom/mojang/blaze3d/pipeline/PipelineCache;)Lcom/mojang/blaze3d/pipeline/PipelineCache;"))
  private void neko$reloadPipelines(CallbackInfo ci, @Local(name = "pipelineCache") PipelineCache pipelineCache) {
    NekoRenderPipelines.precompile(pipelineCache);
  }
}
