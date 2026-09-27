/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.mixin;

import nl.oxod.nekoclient.mixininterface.IComponent;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.MutableComponent;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(MutableComponent.class)
public abstract class MutableComponentMixin implements IComponent {
    @Shadow
    private @Nullable Language decomposedWith;

    @Override
    public void neko$invalidateCache() {
        this.decomposedWith = null;
    }
}
