/*
* This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
* Copyright (c) Meteor Development.
*/

package nl.oxod.nekoclient.mixin;

import com.mojang.renderpearl.backend.api.GpuDeviceBackend;
import com.mojang.renderpearl.backend.api.RenderPassBackend;
import com.mojang.renderpearl.frontend.FrontendGpuDevice;
import nl.oxod.nekoclient.mixininterface.IGpuDevice;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(FrontendGpuDevice.class)
public abstract class FrontendGpuDeviceMixin implements IGpuDevice {
 @Shadow
 @Final
 private GpuDeviceBackend backend;

 @Override
 public void neko$pushScissor(int x, int y, int width, int height) {
  ((IGpuDevice) backend).neko$pushScissor(x, y, width, height);
 }

 @Override
 public void neko$popScissor() {
  ((IGpuDevice) backend).neko$popScissor();
 }

 @SuppressWarnings("deprecation")
 @Override
 public void neko$onCreateRenderPass(RenderPassBackend backend) {
  ((IGpuDevice) this.backend).neko$onCreateRenderPass(backend);
 }
}
