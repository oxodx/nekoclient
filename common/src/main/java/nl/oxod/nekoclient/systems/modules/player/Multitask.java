/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.systems.modules.player;

import nl.oxod.nekoclient.settings.BoolSetting;
import nl.oxod.nekoclient.settings.Setting;
import nl.oxod.nekoclient.settings.SettingGroup;
import nl.oxod.nekoclient.systems.modules.Categories;
import nl.oxod.nekoclient.systems.modules.Module;

public class Multitask extends Module {
  private final SettingGroup sgGeneral = settings.getDefaultGroup();

  private final Setting<Boolean> attackingEntities = sgGeneral.add(new BoolSetting.Builder()
    .name("attacking-entities")
    .description("Lets you attack entities while using an item.")
    .defaultValue(true)
    .build()
  );

  public Multitask() {
    super(Categories.Player, "multitask", "Lets you use items and attack at the same time.");
  }

  public boolean attackingEntities() {
    return isActive() && attackingEntities.get();
  }
}
