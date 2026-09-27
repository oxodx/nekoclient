/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.mixin;

import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.GpuDeviceBackend;
import com.mojang.blaze3d.systems.RenderPassBackend;
import nl.oxod.nekoclient.mixininterface.IGpuDevice;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(GpuDevice.class)
public abstract class GpuDeviceMixin implements IGpuDevice {
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
