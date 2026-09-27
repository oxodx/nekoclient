/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.systems.modules.player;

import nl.oxod.nekoclient.gui.GuiTheme;
import nl.oxod.nekoclient.gui.widgets.WWidget;
import nl.oxod.nekoclient.settings.DoubleSetting;
import nl.oxod.nekoclient.settings.Setting;
import nl.oxod.nekoclient.settings.SettingGroup;
import nl.oxod.nekoclient.systems.modules.Categories;
import nl.oxod.nekoclient.systems.modules.Module;
import nl.oxod.nekoclient.utils.Utils;

public class Reach extends Module {
  private final SettingGroup sgGeneral = settings.getDefaultGroup();

  private final Setting<Double> blockReach = sgGeneral.add(new DoubleSetting.Builder()
    .name("extra-block-reach")
    .description("The distance to add to your block reach.")
    .sliderMax(1)
    .build()
  );

  private final Setting<Double> entityReach = sgGeneral.add(new DoubleSetting.Builder()
    .name("extra-entity-reach")
    .description("The distance to add to your entity reach.")
    .sliderMax(1)
    .build()
  );

  public Reach() {
    super(Categories.Player, "reach", "Gives you super long arms.");
  }

  @Override
  public WWidget getWidget(GuiTheme theme) {
    return theme.label("Note: on vanilla servers you may give yourself up to 4 blocks of additional reach for specific actions - " +
      "interacting with block entities (chests, furnaces, etc.) or with vehicles. This does not work on paper servers.", Utils.getWindowWidth() / 3.0);
  }

  public double blockReach() {
    return isActive() ? blockReach.get() : 0;
  }

  public double entityReach() {
    return isActive() ? entityReach.get() : 0;
  }
}
