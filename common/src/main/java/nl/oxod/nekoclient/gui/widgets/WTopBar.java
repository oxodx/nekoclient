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
import org.lwjgl.sdl.SDLMouse;

import static nl.oxod.nekoclient.NekoClient.mc;

public abstract class WTopBar extends WHorizontalList {
  protected abstract Color getButtonColor(boolean pressed, boolean hovered);

  protected abstract Color getNameColor();

  protected void renderButton(GuiRenderer renderer, WWidget widget, boolean active, boolean pressed,
                              boolean hovered, double pad, String name) {
    renderer.quad(widget.x, widget.y, widget.width, widget.height, getButtonColor(pressed || active, hovered));
    renderer.text(name, widget.x + pad, widget.y + pad, getNameColor(), false);
  }

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
                SDLMouse.SDL_WarpMouseInWindow(mc.getWindow().handle(), (float) mouseX, (float) mouseY);
      }
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
      double pad = pad();
      boolean active = mc.gui.screen() instanceof TabScreen tabScreen && tabScreen.tab == tab;
      renderButton(renderer, this, active, pressed, mouseOver, pad, tab.name);
    }
  }
}
