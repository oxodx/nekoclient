/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.systems.modules.misc;

import nl.oxod.nekoclient.systems.modules.Categories;
import nl.oxod.nekoclient.systems.modules.Module;

public class BetterBeacons extends Module {
  public BetterBeacons() {
    super(Categories.Misc, "better-beacons", "Select effects unaffected by beacon level.");
  }
}
