/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.renderer;

import nl.oxod.nekoclient.NekoClient;
import nl.oxod.nekoclient.events.neko.CustomFontChangedEvent;
import nl.oxod.nekoclient.gui.WidgetScreen;
import nl.oxod.nekoclient.renderer.text.CustomTextRenderer;
import nl.oxod.nekoclient.renderer.text.FontFace;
import nl.oxod.nekoclient.renderer.text.FontFamily;
import nl.oxod.nekoclient.renderer.text.FontInfo;
import nl.oxod.nekoclient.systems.config.Config;
import nl.oxod.nekoclient.utils.PreInit;
import nl.oxod.nekoclient.utils.render.FontUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static nl.oxod.nekoclient.NekoClient.mc;

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

    NekoClient.LOG.info("Found {} font families.", FONT_FAMILIES.size());

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
      NekoClient.LOG.error("No built-in fonts could be loaded. Falling back to first available font family.");
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
      NekoClient.EVENT_BUS.post(CustomFontChangedEvent.get());
    } catch (Exception e) {
      if (fontFace.equals(DEFAULT_FONT)) {
        throw new RuntimeException("Failed to load default font: " + fontFace, e);
      }

      NekoClient.LOG.error("Failed to load font: {}", fontFace, e);
      load(Fonts.DEFAULT_FONT);
    }

    if (mc.gui.screen() instanceof WidgetScreen widgetScreen && Config.get().customFont.get()) {
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
