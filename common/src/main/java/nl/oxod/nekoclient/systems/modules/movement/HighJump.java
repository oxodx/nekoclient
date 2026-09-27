/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.systems.modules.movement;

import nl.oxod.nekoclient.events.entity.player.JumpVelocityMultiplierEvent;
import nl.oxod.nekoclient.settings.DoubleSetting;
import nl.oxod.nekoclient.settings.Setting;
import nl.oxod.nekoclient.settings.SettingGroup;
import nl.oxod.nekoclient.systems.modules.Categories;
import nl.oxod.nekoclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

public class HighJump extends Module {
  private final SettingGroup sgGeneral = settings.getDefaultGroup();

  private final Setting<Double> multiplier = sgGeneral.add(new DoubleSetting.Builder()
    .name("jump-multiplier")
    .description("Jump height multiplier.")
    .defaultValue(1)
    .min(0)
    .build()
  );

  public HighJump() {
    super(Categories.Movement, "high-jump", "Makes you jump higher than normal.");
  }

  @EventHandler
  private void onJumpVelocityMultiplier(JumpVelocityMultiplierEvent event) {
    event.multiplier *= multiplier.get();
  }
}
