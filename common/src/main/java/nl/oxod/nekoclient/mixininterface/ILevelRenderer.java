/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.mixininterface;

import com.mojang.blaze3d.pipeline.RenderTarget;

public interface ILevelRenderer {
    void neko$pushEntityOutlineFramebuffer(RenderTarget framebuffer);

    void neko$popEntityOutlineFramebuffer();
}
