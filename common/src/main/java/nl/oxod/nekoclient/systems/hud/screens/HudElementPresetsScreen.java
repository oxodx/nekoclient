/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.systems.hud.screens;

import nl.oxod.nekoclient.gui.GuiTheme;
import nl.oxod.nekoclient.gui.WindowScreen;
import nl.oxod.nekoclient.gui.widgets.containers.WHorizontalList;
import nl.oxod.nekoclient.gui.widgets.input.WTextBox;
import nl.oxod.nekoclient.gui.widgets.pressable.WPlus;
import nl.oxod.nekoclient.systems.hud.Hud;
import nl.oxod.nekoclient.systems.hud.HudElementInfo;
import nl.oxod.nekoclient.utils.Utils;
import org.jspecify.annotations.Nullable;

public class HudElementPresetsScreen extends WindowScreen {
  private final HudElementInfo<?> info;
  private final int x, y;

  private final WTextBox searchBar;
  private HudElementInfo<?>.@Nullable Preset firstPreset;

  public HudElementPresetsScreen(GuiTheme theme, HudElementInfo<?> info, int x, int y) {
    super(theme, "Select preset for " + info.title);

    this.info = info;
    this.x = x + 9;
    this.y = y;

    searchBar = theme.textBox("");
    searchBar.action = () -> {
      clear();
      initWidgets();
    };

    enterAction = () -> {
      if (firstPreset == null) return;
      Hud.get().add(firstPreset, x, y);
      onClose();
    };
  }

  @Override
  public void initWidgets() {
    firstPreset = null;

    // Search bar
    add(searchBar).expandX();
    searchBar.setFocused(true);

    // Presets
    for (HudElementInfo<?>.Preset preset : info.presets) {
      if (!Utils.searchTextDefault(preset.title, searchBar.get(), false)) continue;

      WHorizontalList l = add(theme.horizontalList()).expandX().widget();

      l.add(theme.label(preset.title));

      WPlus add = l.add(theme.plus()).expandCellX().right().widget();
      add.action = () -> {
        Hud.get().add(preset, x, y);
        onClose();
      };

      if (firstPreset == null) firstPreset = preset;
    }
  }
}
