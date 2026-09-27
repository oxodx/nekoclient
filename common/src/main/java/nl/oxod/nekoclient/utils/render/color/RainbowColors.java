/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.utils.render.color;

import nl.oxod.nekoclient.NekoClient;
import nl.oxod.nekoclient.events.world.TickEvent;
import nl.oxod.nekoclient.gui.GuiThemes;
import nl.oxod.nekoclient.gui.WidgetScreen;
import nl.oxod.nekoclient.settings.ColorSetting;
import nl.oxod.nekoclient.settings.Setting;
import nl.oxod.nekoclient.settings.SettingGroup;
import nl.oxod.nekoclient.systems.config.Config;
import nl.oxod.nekoclient.systems.waypoints.Waypoint;
import nl.oxod.nekoclient.systems.waypoints.Waypoints;
import nl.oxod.nekoclient.utils.PostInit;
import nl.oxod.nekoclient.utils.misc.UnorderedArrayList;
import meteordevelopment.orbit.EventHandler;

import java.util.List;

import static nl.oxod.nekoclient.NekoClient.mc;

public class RainbowColors {
  private static final List<Setting<SettingColor>> colorSettings = new UnorderedArrayList<>();
  private static final List<Setting<List<SettingColor>>> colorListSettings = new UnorderedArrayList<>();

  private static final List<SettingColor> colors = new UnorderedArrayList<>();
  private static final List<Runnable> listeners = new UnorderedArrayList<>();

  public static final RainbowColor GLOBAL = new RainbowColor();

  private RainbowColors() {
  }

  @PostInit
  public static void init() {
    NekoClient.EVENT_BUS.subscribe(RainbowColors.class);
  }

  public static void addSetting(Setting<SettingColor> setting) {
    colorSettings.add(setting);
  }

  public static void addSettingList(Setting<List<SettingColor>> setting) {
    colorListSettings.add(setting);
  }

  public static void removeSetting(Setting<SettingColor> setting) {
    colorSettings.remove(setting);
  }

  public static void removeSettingList(Setting<List<SettingColor>> setting) {
    colorListSettings.remove(setting);
  }

  public static void add(SettingColor color) {
    colors.add(color);
  }

  public static void register(Runnable runnable) {
    listeners.add(runnable);
  }

  @EventHandler
  private static void onTick(TickEvent.Post event) {
    GLOBAL.setSpeed(Config.get().rainbowSpeed.get() / 100);
    GLOBAL.getNext();

    for (Setting<SettingColor> setting : colorSettings) {
      if (setting.module == null || setting.module.isActive()) setting.get().update();
    }

    for (Setting<List<SettingColor>> setting : colorListSettings) {
      if (setting.module == null || setting.module.isActive()) {
        for (SettingColor color : setting.get()) color.update();
      }
    }

    for (SettingColor color : colors) {
      color.update();
    }

    for (Waypoint waypoint : Waypoints.get()) {
      waypoint.color.get().update();
    }

    if (mc.gui.screen() instanceof WidgetScreen) {
      for (SettingGroup group : GuiThemes.get().settings) {
        for (Setting<?> setting : group) {
          if (setting instanceof ColorSetting) ((SettingColor) setting.get()).update();
        }
      }
    }

    for (Runnable listener : listeners) listener.run();
  }
}
