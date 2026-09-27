/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import nl.oxod.nekoclient.NekoClient;
import nl.oxod.nekoclient.commands.Command;
import nl.oxod.nekoclient.commands.arguments.PlayerArgumentType;
import nl.oxod.nekoclient.events.neko.KeyInputEvent;
import nl.oxod.nekoclient.events.neko.MouseClickEvent;
import nl.oxod.nekoclient.utils.misc.input.Input;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.network.chat.Component;

public class SpectateCommand extends Command {

    private final StaticListener shiftListener = new StaticListener();

    public SpectateCommand() {
        super("spectate", "Allows you to spectate nearby players");
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
        builder.then(literal("reset").executes(_ -> {
            mc.setCameraEntity(mc.player);
            return SINGLE_SUCCESS;
        }));

        builder.then(argument("player", PlayerArgumentType.create()).executes(context -> {
            mc.setCameraEntity(PlayerArgumentType.get(context));
            mc.player.sendSystemMessage(Component.literal("Sneak to un-spectate."));
            NekoClient.EVENT_BUS.subscribe(shiftListener);
            return SINGLE_SUCCESS;
        }));
    }

    private static class StaticListener {
        @EventHandler
        private void onKey(KeyInputEvent event) {
            if (Input.isPressed(mc.options.keyShift)) {
                mc.setCameraEntity(mc.player);
                event.cancel();
                NekoClient.EVENT_BUS.unsubscribe(this);
            }
        }

        @EventHandler
        private void onMouse(MouseClickEvent event) {
            if (Input.isPressed(mc.options.keyShift)) {
                mc.setCameraEntity(mc.player);
                event.cancel();
                NekoClient.EVENT_BUS.unsubscribe(this);
            }
        }
    }
}
