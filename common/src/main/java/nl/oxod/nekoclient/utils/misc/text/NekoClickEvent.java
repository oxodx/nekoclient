/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.utils.misc.text;

import nl.oxod.nekoclient.mixin.ScreenMixin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * This class does nothing except ensure that {@link ClickEvent}'s containing NekoClient commands can only be executed if they come from the client.
 *
 * @see ScreenMixin#onDefaultHandleClickEvent(ClickEvent, Minecraft, Screen, CallbackInfo)
 */
public class NekoClickEvent implements ClickEvent {
  public final String value;

  public NekoClickEvent(String value) {
    this.value = value;
  }

  @Override
  public @NonNull Action action() {
    return Action.RUN_COMMAND;
  }
}
