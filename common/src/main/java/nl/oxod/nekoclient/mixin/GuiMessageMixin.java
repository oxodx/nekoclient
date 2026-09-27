/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.mixin;

import com.mojang.authlib.GameProfile;
import nl.oxod.nekoclient.mixininterface.IGuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(GuiMessage.class)
public abstract class GuiMessageMixin implements IGuiMessage {
    @Shadow
    @Final
    private Component content;
    @Unique
    private int id;
    @Unique
    private GameProfile sender;

    @Override
    public String neko$getText() {
        return content.getString();
    }

    @Override
    public int neko$getId() {
        return id;
    }

    @Override
    public void neko$setId(int id) {
        this.id = id;
    }

    @Override
    public GameProfile neko$getSender() {
        return sender;
    }

    @Override
    public void neko$setSender(GameProfile profile) {
        sender = profile;
    }
}
