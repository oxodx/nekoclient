/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.mixin;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import nl.oxod.nekoclient.NekoClient;
import nl.oxod.nekoclient.commands.Commands;
import nl.oxod.nekoclient.systems.config.Config;
import nl.oxod.nekoclient.systems.modules.Modules;
import nl.oxod.nekoclient.systems.modules.movement.GUIMove;
import nl.oxod.nekoclient.systems.modules.render.NoRender;
import nl.oxod.nekoclient.utils.Utils;
import nl.oxod.nekoclient.utils.misc.text.NekoClickEvent;
import nl.oxod.nekoclient.utils.misc.text.RunnableClickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.ClickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.mojang.blaze3d.platform.InputConstants.*;

@Mixin(value = Screen.class, priority = 500) // needs to be before baritone
public abstract class ScreenMixin {

    @Unique
    private static boolean neko$isArray(int key) {
        return key == KEY_RIGHT || key == KEY_LEFT || key == KEY_DOWN || key == KEY_UP;
    }

    @Inject(method = "extractTransparentBackground", at = @At("HEAD"), cancellable = true)
    private void onExtractTransparentBackground(CallbackInfo ci) {
        if (Utils.canUpdate() && Modules.get().get(NoRender.class).noGuiBackground())
            ci.cancel();
    }

    @Inject(method = "defaultHandleClickEvent", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;error(Ljava/lang/String;Ljava/lang/Object;)V", remap = false), cancellable = true)
    private static void onDefaultHandleClickEvent(ClickEvent event, Minecraft minecraft, Screen activeScreen, CallbackInfo ci) {
        if (event instanceof RunnableClickEvent runnableClickEvent) {
            runnableClickEvent.runnable.run();
            ci.cancel();
        } else if (event instanceof NekoClickEvent nekoClickEvent && nekoClickEvent.value.startsWith(Config.get().prefix.get())) {
            try {
                Commands.dispatch(nekoClickEvent.value.substring(Config.get().prefix.get().length()));
            } catch (CommandSyntaxException e) {
                NekoClient.LOG.error("Failed to run command", e);
            } finally {
                ci.cancel();
            }
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onKeyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) (this) instanceof ChatScreen) return;
        GUIMove guiMove = Modules.get().get(GUIMove.class);
        if ((guiMove.disableArrows() && neko$isArray(event.key())) || (guiMove.disableSpace() && event.key() == KEY_SPACE)) {
            cir.setReturnValue(true);
        }
    }
}
