package nl.oxod.nekoclient.util;

import net.minecraft.client.KeyMapping;

public interface KeyMappingBridge {
  static KeyMappingBridge of(KeyMapping mapping) {
    return (KeyMappingBridge) mapping;
  }

  boolean nekoclient$isActuallyDown();

  void nekoclient$resetPressedState();

  void nekoclient$simulatePress(boolean pressed);
}
