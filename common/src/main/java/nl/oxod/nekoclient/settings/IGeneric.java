/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.settings;

import nl.oxod.nekoclient.gui.GuiTheme;
import nl.oxod.nekoclient.gui.WidgetScreen;
import nl.oxod.nekoclient.utils.misc.ICopyable;
import nl.oxod.nekoclient.utils.misc.ISerializable;

public interface IGeneric<T extends IGeneric<T>> extends ICopyable<T>, ISerializable<T> {
  WidgetScreen createScreen(GuiTheme theme, GenericSetting<T> setting);
}
