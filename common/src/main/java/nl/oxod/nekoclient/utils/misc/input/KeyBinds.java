/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.utils.misc.input;

import com.mojang.blaze3d.platform.InputConstants;
import nl.oxod.nekoclient.NekoClient;
import net.minecraft.client.KeyMapping;

public class KeyBinds {
  private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(NekoClient.identifier("nekoclient"));

  public static KeyMapping OPEN_GUI = new KeyMapping("key.nekoclient.open-gui", InputConstants.Type.KEYSYM, InputConstants.KEY_RSHIFT, CATEGORY);
  public static KeyMapping OPEN_COMMANDS = new KeyMapping("key.nekoclient.open-commands", InputConstants.Type.KEYSYM, InputConstants.KEY_PERIOD, CATEGORY);

  private KeyBinds() {
  }

  public static KeyMapping[] apply(KeyMapping[] binds) {
    // Add key binding
    KeyMapping[] newBinds = new KeyMapping[binds.length + 2];

    System.arraycopy(binds, 0, newBinds, 0, binds.length);
    newBinds[binds.length] = OPEN_GUI;
    newBinds[binds.length + 1] = OPEN_COMMANDS;

    return newBinds;
  }
}
