/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.mixininterface;

import com.mojang.authlib.GameProfile;

public interface IGuiMessage {
  String neko$getText();

  int neko$getId();

  void neko$setId(int id);

  GameProfile neko$getSender();

  void neko$setSender(GameProfile profile);
}
