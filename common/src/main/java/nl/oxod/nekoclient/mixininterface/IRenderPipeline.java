/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.mixininterface;

public interface IRenderPipeline {
  void neko$setLineSmooth(boolean lineSmooth);

  boolean neko$getLineSmooth();
}
