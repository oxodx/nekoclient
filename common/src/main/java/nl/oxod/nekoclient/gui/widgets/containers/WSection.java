/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.gui.widgets.containers;

import nl.oxod.nekoclient.gui.renderer.GuiRenderer;
import nl.oxod.nekoclient.gui.utils.Cell;
import nl.oxod.nekoclient.gui.widgets.WWidget;
import net.minecraft.client.input.MouseButtonEvent;
import nl.oxod.nekoclient.gui.engine.UiMotion;

import static com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT;

public abstract class WSection extends WVerticalList {
  public Runnable action;

  protected String title;

  protected boolean expanded;
  protected double animProgress;

  private WHeader header;
  protected final WWidget headerWidget;

  private double actualHeight;

  public WSection(String title, boolean expanded, WWidget headerWidget) {
    this.title = title;
    this.expanded = expanded;
    this.headerWidget = headerWidget;

    animProgress = expanded ? 1 : 0;
  }

  @Override
  public void init() {
    header = createHeader();
    header.theme = theme;

    super.add(header).expandX();
  }

  @Override
  public <T extends WWidget> Cell<T> add(T widget) {
    return super.add(widget).padHorizontal(6);
  }

  protected abstract WHeader createHeader();

  public void setExpanded(boolean expanded) {
    this.expanded = expanded;
    invalidate();
  }

  public boolean isExpanded() {
    return expanded;
  }

  @Override
  protected void onCalculateSize() {
    super.onCalculateSize();
    actualHeight = height;
    height = header.height + (actualHeight - header.height) * animProgress;
  }

  @Override
  public boolean render(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
    if (!visible) return true;

    double preProgress = animProgress;

    animProgress = UiMotion.approach(animProgress, expanded ? 1 : 0, 20, delta);

    if (animProgress != preProgress) {
      invalidate();
    }

    renderer.scissorStart(x, y, width, height);
    boolean toReturn = super.render(renderer, mouseX, mouseY, delta);
    renderer.scissorEnd();

    return toReturn;
  }

  @Override
  public boolean clipsChildren() { return true; }

  @Override
  protected void renderWidget(WWidget widget, GuiRenderer renderer, double mouseX, double mouseY, double delta) {
    if (expanded || animProgress > 0 || widget instanceof WHeader) {
      widget.render(renderer, mouseX, mouseY, delta);
    }
  }

  @Override
  protected boolean propagateEvents(WWidget widget) {
    return expanded || widget instanceof WHeader;
  }

  @Override
  public boolean isChildActive(WWidget widget) {
    return widget.visible && (expanded || widget instanceof WHeader);
  }

  protected abstract class WHeader extends WHorizontalList {
    protected String title;

    public WHeader(String title) {
      this.title = title;
    }

    @Override
    public boolean onMouseClicked(MouseButtonEvent click, boolean doubled) {
      if (mouseOver && click.button() == MOUSE_BUTTON_LEFT && !doubled) {
        onClick();
        return true;
      }

      return false;
    }

    protected void onClick() {
      setExpanded(!expanded);

      if (action != null) action.run();
    }
  }
}
