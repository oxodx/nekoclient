/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.systems.modules.movement;

import nl.oxod.nekoclient.settings.EnumSetting;
import nl.oxod.nekoclient.settings.Setting;
import nl.oxod.nekoclient.settings.SettingGroup;
import nl.oxod.nekoclient.systems.modules.Categories;
import nl.oxod.nekoclient.systems.modules.Module;

public class Sneak extends Module {
  private final SettingGroup sgGeneral = settings.getDefaultGroup();

  private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
    .name("mode")
    .description("Which method to sneak.")
    .defaultValue(Mode.Vanilla)
    .build()
  );

  public Sneak() {
    super(Categories.Movement, "sneak", "Sneaks for you");
  }

  public boolean doPacket() {
    return isActive() && !mc.player.getAbilities().flying && mode.get() == Mode.Packet;
  }

  public boolean doVanilla() {
    return isActive() && !mc.player.getAbilities().flying && mode.get() == Mode.Vanilla;
  }

  public enum Mode {
    Packet,
    Vanilla
  }
}
