/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.systems.modules.movement;

import nl.oxod.nekoclient.events.world.TickEvent;
import nl.oxod.nekoclient.mixin.LivingEntityAccessor;
import nl.oxod.nekoclient.settings.BoolSetting;
import nl.oxod.nekoclient.settings.DoubleSetting;
import nl.oxod.nekoclient.settings.Setting;
import nl.oxod.nekoclient.settings.SettingGroup;
import nl.oxod.nekoclient.systems.modules.Categories;
import nl.oxod.nekoclient.systems.modules.Module;
import nl.oxod.nekoclient.systems.modules.Modules;
import nl.oxod.nekoclient.systems.modules.world.Timer;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PowderSnowBlock;
import net.minecraft.world.phys.Vec3;

public class FastClimb extends Module {
  private final SettingGroup sgGeneral = settings.getDefaultGroup();

  private final Setting<Boolean> timerMode = sgGeneral.add(new BoolSetting.Builder()
    .name("timer-mode")
    .description("Use timer.")
    .defaultValue(false)
    .build()
  );

  private final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
    .name("climb-speed")
    .description("Your climb speed.")
    .defaultValue(0.2872)
    .min(0.0)
    .visible(() -> !timerMode.get())
    .build()
  );

  private final Setting<Double> timer = sgGeneral.add(new DoubleSetting.Builder()
    .name("timer")
    .description("The timer value for Timer.")
    .defaultValue(1.436)
    .min(1)
    .sliderMin(1)
    .visible(timerMode::get)
    .build()
  );

  private boolean resetTimer;

  public FastClimb() {
    super(Categories.Movement, "fast-climb", "Allows you to climb faster.");
  }

  @Override
  public void onActivate() {
    resetTimer = false;
  }

  @EventHandler
  private void onPreTick(TickEvent.Pre event) {
    if (timerMode.get()) {
      if (climbing()) {
        resetTimer = false;
        Modules.get().get(Timer.class).setOverride(timer.get());
      } else if (!resetTimer) {
        Modules.get().get(Timer.class).setOverride(Timer.OFF);
        resetTimer = true;
      }
    }
  }

  @EventHandler
  private void onTick(TickEvent.Post event) {
    if (!timerMode.get() && climbing()) {
      Vec3 velocity = mc.player.getDeltaMovement();
      mc.player.setDeltaMovement(velocity.x, speed.get(), velocity.z);
    }
  }

  private boolean climbing() {
    return (mc.player.horizontalCollision || ((LivingEntityAccessor) mc.player).neko$isJumping()) && (mc.player.onClimbable() || mc.player.getInBlockState().is(Blocks.POWDER_SNOW) && PowderSnowBlock.canEntityWalkOnPowderSnow(mc.player));
  }
}
