/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.systems.macros;

import nl.oxod.nekoclient.gui.utils.StarscriptTextBoxRenderer;
import nl.oxod.nekoclient.settings.*;
import nl.oxod.nekoclient.utils.misc.ISerializable;
import nl.oxod.nekoclient.utils.misc.Keybind;
import nl.oxod.nekoclient.utils.misc.NekoStarscript;
import nl.oxod.nekoclient.utils.player.ChatUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.meteordev.starscript.Script;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static nl.oxod.nekoclient.NekoClient.mc;

public class Macro implements ISerializable<Macro> {
  public final Settings settings = new Settings();

  private final SettingGroup sgGeneral = settings.getDefaultGroup();

  public Setting<String> name = sgGeneral.add(new StringSetting.Builder()
    .name("name")
    .description("The name of the macro.")
    .build()
  );

  public Setting<List<String>> messages = sgGeneral.add(new StringListSetting.Builder()
    .name("messages")
    .description("The messages for the macro to send.")
    .onChanged(_ -> dirty = true)
    .renderer(StarscriptTextBoxRenderer.class)
    .build()
  );

  public Setting<Keybind> keybind = sgGeneral.add(new KeybindSetting.Builder()
    .name("keybind")
    .description("The bind to run the macro.")
    .build()
  );

  private final List<Script> scripts = new ArrayList<>(1);
  private boolean dirty;

  public Macro() {
  }

  public Macro(Tag tag) {
    fromTag((CompoundTag) tag);
  }

  public boolean onAction(boolean isKey, int value, int modifiers) {
    if (!keybind.get().matches(isKey, value, modifiers) || mc.gui.screen() != null) return false;
    return onAction();
  }

  public boolean onAction() {
    if (dirty) {
      scripts.clear();

      for (String message : messages.get()) {
        Script script = NekoStarscript.compile(message);
        if (script != null) scripts.add(script);
      }

      dirty = false;
    }

    for (Script script : scripts) {
      String message = NekoStarscript.run(script);

      if (message != null) {
        ChatUtils.sendPlayerMsg(message, false);
      }
    }

    return true;
  }

  @Override
  public CompoundTag toTag() {
    CompoundTag tag = new CompoundTag();

    tag.put("settings", settings.toTag());

    return tag;
  }

  @Override
  public Macro fromTag(CompoundTag tag) {
    if (tag.contains("settings")) {
      settings.fromTag(tag.getCompoundOrEmpty("settings"));
    }

    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Macro macro = (Macro) o;
    return Objects.equals(macro.name.get(), this.name.get());
  }
}
