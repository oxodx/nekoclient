/*
* This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
* Copyright (c) Meteor Development.
*/

package nl.oxod.nekoclient.mixin;

import com.mojang.renderpearl.backend.api.RenderPassBackend;
import com.mojang.renderpearl.backend.opengl.GlCommandEncoder;
import com.mojang.renderpearl.backend.opengl.GlDevice;
import nl.oxod.nekoclient.mixininterface.IGpuDevice;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GlCommandEncoder.class)
public abstract class GlCommandEncoderMixin {
  @Shadow
  @Final
  private GlDevice device;

  @SuppressWarnings("deprecation")
  @Inject(method = "createRenderPass", at = @At("RETURN"))
  private void createRenderPass$iGpuDevice(CallbackInfoReturnable<RenderPassBackend> cir) {
    ((IGpuDevice) device).neko$onCreateRenderPass(cir.getReturnValue());
  }
}
