/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.utils;

import net.minecraft.client.KeyMapping;

public interface KeyMappingBridge {
  static KeyMappingBridge of(KeyMapping mapping) {
    return (KeyMappingBridge) mapping;
  }

  boolean nekoclient$isActuallyDown();

  void nekoclient$resetPressedState();

  void nekoclient$simulatePress(boolean pressed);
}
