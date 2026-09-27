/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.gui.tabs.builtin;

import nl.oxod.nekoclient.gui.GuiTheme;
import nl.oxod.nekoclient.gui.renderer.GuiRenderer;
import nl.oxod.nekoclient.gui.tabs.Tab;
import nl.oxod.nekoclient.gui.tabs.TabScreen;
import nl.oxod.nekoclient.gui.tabs.WindowTabScreen;
import nl.oxod.nekoclient.gui.widgets.containers.WContainer;
import nl.oxod.nekoclient.gui.widgets.containers.WHorizontalList;
import nl.oxod.nekoclient.gui.widgets.pressable.WButton;
import nl.oxod.nekoclient.gui.widgets.pressable.WCheckbox;
import nl.oxod.nekoclient.systems.hud.Hud;
import nl.oxod.nekoclient.systems.hud.screens.HudEditorScreen;
import nl.oxod.nekoclient.utils.misc.NbtUtils;
import net.minecraft.client.gui.screens.Screen;

import static nl.oxod.nekoclient.NekoClient.mc;

public class HudTab extends Tab {
  public HudTab() {
    super("HUD");
  }

  @Override
  public TabScreen createScreen(GuiTheme theme) {
    return new HudScreen(theme, this);
  }

  @Override
  public boolean isScreen(Screen screen) {
    return screen instanceof HudScreen;
  }

  public static class HudScreen extends WindowTabScreen {
    private WContainer settingsContainer;
    private final Hud hud;

    public HudScreen(GuiTheme theme, Tab tab) {
      super(theme, tab);

      hud = Hud.get();
      hud.settings.onActivated();
    }

    @Override
    public void initWidgets() {
      settingsContainer = add(theme.verticalList()).expandX().widget();
      settingsContainer.add(theme.settings(hud.settings)).expandX().widget();

      add(theme.horizontalSeparator()).expandX();

      WButton openEditor = add(theme.button("Edit")).expandX().widget();
      openEditor.action = () -> mc.gui.setScreen(new HudEditorScreen(theme));

      WHorizontalList buttons = add(theme.horizontalList()).expandX().widget();
      buttons.add(theme.confirmedButton("Clear", "Confirm")).expandX().widget().action = hud::clear;
      buttons.add(theme.confirmedButton("Reset to default elements", "Confirm")).expandX().widget().action = hud::resetToDefaultElements;

      add(theme.horizontalSeparator()).expandX();

      WHorizontalList bottom = add(theme.horizontalList()).expandX().widget();

      bottom.add(theme.label("Active: "));
      WCheckbox active = bottom.add(theme.checkbox(hud.active)).expandCellX().widget();
      active.action = () -> hud.active = active.checked;

      WButton resetSettings = bottom.add(theme.button(GuiRenderer.RESET)).widget();
      resetSettings.action = hud.settings::reset;
      resetSettings.tooltip = "Reset";
    }

    @Override
    public void tick() {
      super.tick();

      hud.settings.tick(settingsContainer, theme);
    }

    @Override
    public boolean toClipboard() {
      return NbtUtils.toClipboard(hud);
    }

    @Override
    public boolean fromClipboard() {
      return NbtUtils.fromClipboard(hud);
    }
  }
}
