/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.mixininterface;

import com.mojang.blaze3d.systems.RenderPassBackend;

public interface IGpuDevice {
    /**
     * Currently there can only be a single scissor pushed at once.
     */
    void neko$pushScissor(int x, int y, int width, int height);

    void neko$popScissor();

    /**
     * This is an *INTERNAL* method, it shouldn't be called.
     */
    @Deprecated
    @SuppressWarnings("DeprecatedIsStillUsed")
    void neko$onCreateRenderPass(RenderPassBackend backend);
}
