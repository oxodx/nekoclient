/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.mixin;

import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.LastSeenMessagesTracker;
import net.minecraft.network.chat.SignedMessageChain;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.flag.FeatureFlagSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientPacketListener.class)
public interface ClientPacketListenerAccessor {
  @Accessor("serverChunkRadius")
  int neko$getServerChunkRadius();

  @Accessor("signedMessageEncoder")
  SignedMessageChain.Encoder neko$getSignedMessageEncoder();

  @Accessor("lastSeenMessages")
  LastSeenMessagesTracker neko$getLastSeenMessages();

  @Accessor("registryAccess")
  RegistryAccess.Frozen neko$getRegistryAccess();

  @Accessor("enabledFeatures")
  FeatureFlagSet neko$getEnabledFeatures();

  @Accessor("COMMAND_NODE_BUILDER")
  static ClientboundCommandsPacket.NodeBuilder<ClientSuggestionProvider> neko$getCommandNodeFactory() {
    return null;
  }
}
