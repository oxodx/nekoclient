/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.gui.screens.settings;

import nl.oxod.nekoclient.gui.GuiTheme;
import nl.oxod.nekoclient.gui.screens.settings.base.CollectionListSettingScreen;
import nl.oxod.nekoclient.gui.widgets.WWidget;
import nl.oxod.nekoclient.settings.Setting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;

import java.util.List;

public class SoundEventListSettingScreen extends CollectionListSettingScreen<SoundEvent> {
    public SoundEventListSettingScreen(GuiTheme theme, Setting<List<SoundEvent>> setting) {
        super(theme, "Select Sounds", setting, setting.get(), BuiltInRegistries.SOUND_EVENT);
    }

    @Override
    protected WWidget getValueWidget(SoundEvent value) {
        return theme.label(value.location().getPath());
    }

    @Override
    protected String[] getValueNames(SoundEvent value) {
        return new String[]{
            value.location().toString(),
            I18n.get("subtitles." + value.location().getPath())
        };
    }
}
