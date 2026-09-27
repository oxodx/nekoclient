/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.systems.modules.player;

import nl.oxod.nekoclient.systems.modules.Categories;
import nl.oxod.nekoclient.systems.modules.Module;

public class Portals extends Module {
  public Portals() {
    super(Categories.Player, "portals", "Allows you to use GUIs normally while in a Nether Portal.");
  }
}
