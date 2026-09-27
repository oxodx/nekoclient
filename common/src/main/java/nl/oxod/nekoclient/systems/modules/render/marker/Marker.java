/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.systems.modules.render.marker;

import nl.oxod.nekoclient.events.render.Render3DEvent;
import nl.oxod.nekoclient.events.world.TickEvent;
import nl.oxod.nekoclient.gui.GuiTheme;
import nl.oxod.nekoclient.gui.renderer.GuiRenderer;
import nl.oxod.nekoclient.gui.widgets.WLabel;
import nl.oxod.nekoclient.gui.widgets.WWidget;
import nl.oxod.nekoclient.gui.widgets.containers.WHorizontalList;
import nl.oxod.nekoclient.gui.widgets.containers.WVerticalList;
import nl.oxod.nekoclient.gui.widgets.input.WDropdown;
import nl.oxod.nekoclient.gui.widgets.pressable.WButton;
import nl.oxod.nekoclient.gui.widgets.pressable.WCheckbox;
import nl.oxod.nekoclient.gui.widgets.pressable.WMinus;
import nl.oxod.nekoclient.systems.modules.Categories;
import nl.oxod.nekoclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;

public class Marker extends Module {
  private final MarkerFactory factory = new MarkerFactory();
  private final ArrayList<BaseMarker> markers = new ArrayList<>();

  public Marker() {
    super(Categories.Render, "marker", "Renders shapes. Useful for large scale projects");
  }

  @EventHandler
  private void onTick(TickEvent.Post event) {
    for (BaseMarker marker : markers) {
      if (marker.isVisible()) marker.tick();
    }
  }

  @EventHandler
  private void onRender(Render3DEvent event) {
    for (BaseMarker marker : markers) {
      if (marker.isVisible()) marker.render(event);
    }
  }

  @Override
  public CompoundTag toTag() {
    CompoundTag tag = super.toTag();

    ListTag list = new ListTag();
    for (BaseMarker marker : markers) {
      CompoundTag mTag = new CompoundTag();
      mTag.putString("type", marker.getTypeName());
      mTag.put("marker", marker.toTag());

      list.add(mTag);
    }

    tag.put("markers", list);
    return tag;
  }

  @Override
  public Module fromTag(CompoundTag tag) {
    super.fromTag(tag);

    markers.clear();
    ListTag list = tag.getListOrEmpty("markers");

    for (Tag tagII : list) {
      CompoundTag tagI = (CompoundTag) tagII;

      String type = tagI.getStringOr("type", "");
      BaseMarker marker = factory.createMarker(type);

      if (marker != null) {
        CompoundTag markerTag = (CompoundTag) tagI.get("marker");
        if (markerTag != null) marker.fromTag(markerTag);

        markers.add(marker);
      }
    }

    return this;
  }

  @Override
  public WWidget getWidget(GuiTheme theme) {
    WVerticalList list = theme.verticalList();
    fillList(theme, list);
    return list;
  }

  protected void fillList(GuiTheme theme, WVerticalList list) {
    // Marker List
    for (BaseMarker marker : markers) {
      WHorizontalList hList = list.add(theme.horizontalList()).expandX().widget();

      // Name
      WLabel label = hList.add(theme.label(marker.name.get())).widget();
      label.tooltip = marker.description.get();

      // Dimension
      hList.add(theme.label(" - " + marker.getDimension().toString())).expandX().widget().color = theme.textSecondaryColor();

      // Toggle
      WCheckbox checkbox = hList.add(theme.checkbox(marker.isActive())).widget();
      checkbox.action = () -> {
        if (marker.isActive() != checkbox.checked) marker.toggle();
      };

      // Edit
      WButton edit = hList.add(theme.button(GuiRenderer.EDIT)).widget();
      edit.action = () -> mc.gui.setScreen(marker.getScreen(theme));

      // Remove
      WMinus remove = hList.add(theme.minus()).widget();
      remove.action = () -> {
        markers.remove(marker);
        marker.settings.unregisterColorSettings();

        list.clear();
        fillList(theme, list);
      };
    }

    // Bottom
    WHorizontalList bottom = list.add(theme.horizontalList()).expandX().widget();

    WDropdown<String> newMarker = bottom.add(theme.dropdown(factory.getNames(), factory.getNames()[0])).widget();
    WButton add = bottom.add(theme.button("Add")).expandX().widget();
    add.action = () -> {
      String name = newMarker.get();
      markers.add(factory.createMarker(name));

      list.clear();
      fillList(theme, list);
    };

  }
}
