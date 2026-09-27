/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.settings;

import nl.oxod.nekoclient.gui.GuiTheme;
import nl.oxod.nekoclient.gui.WidgetScreen;
import nl.oxod.nekoclient.utils.misc.IChangeable;
import nl.oxod.nekoclient.utils.misc.ICopyable;
import nl.oxod.nekoclient.utils.misc.ISerializable;
import net.minecraft.world.level.block.Block;

public interface IBlockData<T extends ICopyable<T> & ISerializable<T> & IChangeable & IBlockData<T>> {
  WidgetScreen createScreen(GuiTheme theme, Block block, BlockDataSetting<T> setting);
}
