/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.mixin;

import nl.oxod.nekoclient.mixininterface.IServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ServerboundMovePlayerPacket.class)
public abstract class ServerboundMovePlayerPacketMixin implements IServerboundMovePlayerPacket {
    @Unique
    private int tag;

    @Override
    public void neko$setTag(int tag) {
        this.tag = tag;
    }

    @Override
    public int neko$getTag() {
        return this.tag;
    }
}
