/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.commands.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import nl.oxod.nekoclient.commands.Command;
import nl.oxod.nekoclient.renderer.Fonts;
import nl.oxod.nekoclient.systems.Systems;
import nl.oxod.nekoclient.systems.friends.Friend;
import nl.oxod.nekoclient.systems.friends.Friends;
import nl.oxod.nekoclient.utils.network.Capes;
import nl.oxod.nekoclient.utils.network.NekoExecutor;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;

public class ReloadCommand extends Command {
    public ReloadCommand() {
        super("reload", "Reloads many systems.");
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
        builder.executes(_ -> {
            warning("Reloading systems, this may take a while.");

            Systems.load();
            Capes.init();
            Fonts.refresh();
            NekoExecutor.execute(() -> Friends.get().forEach(Friend::updateInfo));

            return SINGLE_SUCCESS;
        });
    }
}
