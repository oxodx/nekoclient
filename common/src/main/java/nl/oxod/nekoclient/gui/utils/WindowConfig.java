/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.gui.utils;

import nl.oxod.nekoclient.utils.misc.ISerializable;
import net.minecraft.nbt.CompoundTag;

public class WindowConfig implements ISerializable<WindowConfig> {
  public boolean expanded = true;
  public double x = -1;
  public double y = -1;
  public double scroll;
  public long zIndex;

  // Saving

  @Override
  public CompoundTag toTag() {
    CompoundTag tag = new CompoundTag();

    tag.putBoolean("expanded", expanded);
    tag.putDouble("x", x);
    tag.putDouble("y", y);
    tag.putDouble("scroll", scroll);
    tag.putLong("zIndex", zIndex);

    return tag;
  }

  @Override
  public WindowConfig fromTag(CompoundTag tag) {
    tag.getBoolean("expanded").ifPresent(bool -> expanded = bool);
    tag.getDouble("x").ifPresent(x1 -> x = x1);
    tag.getDouble("y").ifPresent(y1 -> y = y1);
    tag.getDouble("scroll").ifPresent(value -> scroll = Math.max(0, value));
    tag.getLong("zIndex").ifPresent(value -> zIndex = value);

    return this;
  }
}
