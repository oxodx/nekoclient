/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.gui.widgets;

import nl.oxod.nekoclient.gui.renderer.GuiRenderer;
import nl.oxod.nekoclient.gui.tabs.Tab;
import nl.oxod.nekoclient.gui.tabs.TabScreen;
import nl.oxod.nekoclient.gui.tabs.Tabs;
import nl.oxod.nekoclient.gui.widgets.containers.WHorizontalList;
import nl.oxod.nekoclient.gui.widgets.pressable.WPressable;
import nl.oxod.nekoclient.utils.render.color.Color;
import net.minecraft.client.gui.screens.Screen;

import static nl.oxod.nekoclient.NekoClient.mc;
import static com.mojang.blaze3d.platform.InputConstants.*;

public abstract class WTopBar extends WHorizontalList {
  protected abstract Color getButtonColor(boolean pressed, boolean hovered);

  protected abstract Color getNameColor();

  public WTopBar() {
    spacing = 0;
  }

  @Override
  public void init() {
    for (Tab tab : Tabs.get()) {
      add(new WTopBarButton(tab));
    }
  }

  protected class WTopBarButton extends WPressable {
    private final Tab tab;

    public WTopBarButton(Tab tab) {
      this.tab = tab;
    }

    @Override
    protected void onCalculateSize() {
      double pad = pad();

      width = pad + theme.textWidth(tab.name) + pad;
      height = pad + theme.textHeight() + pad;
    }

    @Override
    protected void onPressed(int button) {
      Screen screen = mc.gui.screen();

      if (!(screen instanceof TabScreen tabScreen) || tabScreen.tab != tab) {
        double mouseX = mc.mouseHandler.xpos();
        double mouseY = mc.mouseHandler.ypos();

        tab.openScreen(theme);
        grabOrReleaseMouse(mc.getWindow(), CURSOR_NORMAL, mouseX, mouseY);
      }
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
      double pad = pad();
      Color color = getButtonColor(pressed || (mc.gui.screen() instanceof TabScreen tabScreen && tabScreen.tab == tab), mouseOver);

      renderer.quad(x, y, width, height, color);
      renderer.text(tab.name, x + pad, y + pad, getNameColor(), false);
    }
  }
}
