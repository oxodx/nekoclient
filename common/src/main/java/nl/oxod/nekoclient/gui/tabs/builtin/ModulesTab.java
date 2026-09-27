/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.gui.tabs.builtin;

import nl.oxod.nekoclient.gui.GuiTheme;
import nl.oxod.nekoclient.gui.GuiThemes;
import nl.oxod.nekoclient.gui.tabs.Tab;
import nl.oxod.nekoclient.gui.tabs.TabScreen;
import net.minecraft.client.gui.screens.Screen;

public class ModulesTab extends Tab {
  public ModulesTab() {
    super("Modules");
  }

  @Override
  public TabScreen createScreen(GuiTheme theme) {
    return theme.modulesScreen();
  }

  @Override
  public boolean isScreen(Screen screen) {
    return GuiThemes.get().isModulesScreen(screen);
  }
}
