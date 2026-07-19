/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.renderer;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.meteor.CustomFontChangedEvent;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.renderer.text.CustomTextRenderer;
import meteordevelopment.meteorclient.renderer.text.FontFace;
import meteordevelopment.meteorclient.renderer.text.FontFamily;
import meteordevelopment.meteorclient.renderer.text.FontInfo;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.utils.PreInit;
import meteordevelopment.meteorclient.utils.render.FontUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class Fonts {
    public static final String[] BUILTIN_FONTS = {"JetBrains Mono", "Comfortaa", "Tw Cen MT", "Pixelation"};

    public static String DEFAULT_FONT_FAMILY;
    public static FontFace DEFAULT_FONT;

    public static final List<FontFamily> FONT_FAMILIES = new ArrayList<>();
    public static CustomTextRenderer RENDERER;

    private Fonts() {
    }

    @PreInit
    public static void refresh() {
        FONT_FAMILIES.clear();

        for (String builtinFont : BUILTIN_FONTS) {
            FontUtils.loadBuiltin(FONT_FAMILIES, builtinFont);
        }

        for (String fontPath : FontUtils.getSearchPaths()) {
            FontUtils.loadSystem(FONT_FAMILIES, new File(fontPath));
        }

        FONT_FAMILIES.sort(Comparator.comparing(FontFamily::getName));

        MeteorClient.LOG.info("Found {} font families.", FONT_FAMILIES.size());

        FontInfo defaultInfo = FontUtils.getBuiltinFontInfo(BUILTIN_FONTS[1]);
        if (defaultInfo == null) {
            for (String builtinFont : BUILTIN_FONTS) {
                defaultInfo = FontUtils.getBuiltinFontInfo(builtinFont);
                if (defaultInfo != null) break;
            }
        }

        if (defaultInfo != null) {
            DEFAULT_FONT_FAMILY = defaultInfo.family();
            FontFamily family = getFamily(DEFAULT_FONT_FAMILY);
            if (family != null) DEFAULT_FONT = family.get(defaultInfo.type());
        }

        if (DEFAULT_FONT == null) {
            MeteorClient.LOG.error("No built-in fonts could be loaded. Falling back to first available font family.");
            if (!FONT_FAMILIES.isEmpty()) {
                DEFAULT_FONT_FAMILY = FONT_FAMILIES.get(0).getName();
                DEFAULT_FONT = FONT_FAMILIES.get(0).get(FontInfo.Type.Regular);
            }
        }

        Config config = Config.get();
        load(config != null ? config.font.get() : DEFAULT_FONT);
    }

    public static void load(FontFace fontFace) {
        if (RENDERER != null) {
            if (RENDERER.fontFace.equals(fontFace)) return;
            else RENDERER.destroy();
        }

        try {
            RENDERER = new CustomTextRenderer(fontFace);
            MeteorClient.EVENT_BUS.post(CustomFontChangedEvent.get());
        } catch (Exception e) {
            if (fontFace.equals(DEFAULT_FONT)) {
                throw new RuntimeException("Failed to load default font: " + fontFace, e);
            }

            MeteorClient.LOG.error("Failed to load font: {}", fontFace, e);
            load(Fonts.DEFAULT_FONT);
        }

        if (mc.screen instanceof WidgetScreen widgetScreen && Config.get().customFont.get()) {
            widgetScreen.invalidate();
        }
    }

    public static FontFamily getFamily(String name) {
        for (FontFamily fontFamily : Fonts.FONT_FAMILIES) {
            if (fontFamily.getName().equalsIgnoreCase(name)) {
                return fontFamily;
            }
        }

        return null;
    }
}
