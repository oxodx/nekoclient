/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.gui.themes.neko.widgets;

import nl.oxod.nekoclient.gui.renderer.GuiRenderer;
import nl.oxod.nekoclient.gui.themes.neko.NekoGuiTheme;
import nl.oxod.nekoclient.gui.themes.neko.NekoWidget;
import nl.oxod.nekoclient.gui.utils.AlignmentX;
import nl.oxod.nekoclient.gui.widgets.pressable.WPressable;
import nl.oxod.nekoclient.systems.modules.Module;
import net.minecraft.util.Mth;

import static com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT;
import static com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT;
import static nl.oxod.nekoclient.NekoClient.mc;

public class WNekoModule extends WPressable implements NekoWidget {
  private final Module module;
  private final String title;

  private double titleWidth;

  private double animationProgress1;

  private double animationProgress2;

  public WNekoModule(Module module, String title) {
    this.module = module;
    this.title = title;
    this.tooltip = module.description;

    if (module.isActive()) {
      animationProgress1 = 1;
      animationProgress2 = 1;
    } else {
      animationProgress1 = 0;
      animationProgress2 = 0;
    }
  }

  @Override
  public double pad() {
    return theme.scale(4);
  }

  @Override
  protected void onCalculateSize() {
    double pad = pad();
    double toggleWidth = theme.scale(18);

    if (titleWidth == 0) titleWidth = theme.textWidth(title);

    width = pad + titleWidth + pad + toggleWidth + pad;
    height = pad + theme.textHeight() + pad;
  }

  @Override
  protected void onPressed(int button) {
    if (button == MOUSE_BUTTON_LEFT) module.toggle();
    else if (button == MOUSE_BUTTON_RIGHT) mc.gui.setScreen(theme.moduleScreen(module));
  }

  @Override
  protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
    NekoGuiTheme theme = theme();
    double pad = pad();

    animationProgress1 += delta * 4 * ((module.isActive() || mouseOver) ? 1 : -1);
    animationProgress1 = Mth.clamp(animationProgress1, 0, 1);

    animationProgress2 += delta * 6 * (module.isActive() ? 1 : -1);
    animationProgress2 = Mth.clamp(animationProgress2, 0, 1);

    double radius = theme.scale(theme.cornerRadius.get());
    renderer.roundedQuad(this, radius, theme.surfaceColor.get());
    if (animationProgress1 > 0) {
      renderer.setAlpha(animationProgress1);
      renderer.roundedQuad(this, radius, theme.surfaceHighColor.get());
      renderer.setAlpha(1);
    }
    if (animationProgress2 > 0) {
      renderer.setAlpha(animationProgress2 * 0.55);
      renderer.roundedQuad(this, radius, theme.activeSurfaceColor.get());
      renderer.setAlpha(1);
      renderer.roundedQuad(x, y + pad, theme.scale(2), height - pad * 2,
        theme.scale(1), theme.accentColor.get());
    }

    double x = this.x + pad;
    double w = width - pad * 2;

    if (theme.moduleAlignment.get() == AlignmentX.Center) {
      x += w / 2 - titleWidth / 2;
    } else if (theme.moduleAlignment.get() == AlignmentX.Right) {
      x += w - titleWidth;
    }

    renderer.text(title, x, y + pad, animationProgress2 > 0.5 ? theme.titleTextColor.get() : theme.textColor.get(), false);

    double trackW = theme.scale(17);
    double trackH = theme.scale(9);
    double trackX = this.x + width - pad - trackW;
    double trackY = y + (height - trackH) / 2;
    renderer.roundedQuad(trackX, trackY, trackW, trackH, trackH / 2,
      animationProgress2 > 0.5 ? theme.accentColor.get() : theme.outlineColor.get());
    double knob = theme.scale(7);
    double knobX = trackX + theme.scale(1) + (trackW - knob - theme.scale(2)) * animationProgress2;
    renderer.roundedQuad(knobX, trackY + (trackH - knob) / 2, knob, knob, knob / 2,
      theme.textColor.get());
  }
}
