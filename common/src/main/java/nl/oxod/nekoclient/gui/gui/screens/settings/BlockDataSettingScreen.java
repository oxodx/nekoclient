/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.gui.screens.settings;

import nl.oxod.nekoclient.gui.GuiTheme;
import nl.oxod.nekoclient.gui.renderer.GuiRenderer;
import nl.oxod.nekoclient.gui.screens.settings.base.CollectionMapSettingScreen;
import nl.oxod.nekoclient.gui.widgets.WWidget;
import nl.oxod.nekoclient.gui.widgets.pressable.WButton;
import nl.oxod.nekoclient.settings.BlockDataSetting;
import nl.oxod.nekoclient.settings.IBlockData;
import nl.oxod.nekoclient.utils.misc.IChangeable;
import nl.oxod.nekoclient.utils.render.DisplayItemUtils;
import nl.oxod.nekoclient.utils.misc.ICopyable;
import nl.oxod.nekoclient.utils.misc.ISerializable;
import nl.oxod.nekoclient.utils.misc.Names;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jspecify.annotations.Nullable;

import static nl.oxod.nekoclient.NekoClient.mc;

public class BlockDataSettingScreen<T extends ICopyable<T> & ISerializable<T> & IChangeable & IBlockData<T>> extends CollectionMapSettingScreen<Block, T> {
    private final BlockDataSetting<T> setting;
    private boolean invalidate;

    public BlockDataSettingScreen(GuiTheme theme, BlockDataSetting<T> setting) {
        super(theme, "Configure Blocks", setting, setting.get(), BuiltInRegistries.BLOCK);

        this.setting = setting;
    }

    @Override
    protected boolean includeValue(Block value) {
        return value != Blocks.AIR;
    }

    @Override
    protected WWidget getValueWidget(Block block) {
        return theme.itemWithLabel(DisplayItemUtils.toStack(block), Names.get(block));
    }

    @Override
    protected WWidget getDataWidget(Block block, @Nullable T blockData) {
        WButton edit = theme.button(GuiRenderer.EDIT);
        edit.action = () -> {
            T data = blockData;
            if (data == null) data = setting.defaultData.get().copy();

            mc.gui.setScreen(data.createScreen(theme, block, setting));
            invalidate = true;
        };
        return edit;
    }

    @Override
    protected void onRenderBefore(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        if (invalidate) {
            this.invalidateTable();
            invalidate = false;
        }
    }

    @Override
    protected String[] getValueNames(Block block) {
        return new String[]{
            Names.get(block),
            BuiltInRegistries.BLOCK.getKey(block).toString()
        };
    }
}
