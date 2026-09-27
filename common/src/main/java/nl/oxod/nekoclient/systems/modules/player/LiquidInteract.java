/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.systems.modules.player;

import nl.oxod.nekoclient.systems.modules.Categories;
import nl.oxod.nekoclient.systems.modules.Module;

public class LiquidInteract extends Module {
  public LiquidInteract() {
    super(Categories.Player, "liquid-interact", "Allows you to interact with liquids.");
  }
}
