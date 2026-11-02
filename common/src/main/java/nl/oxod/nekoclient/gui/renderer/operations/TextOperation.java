/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.gui.renderer.operations;

import nl.oxod.nekoclient.gui.renderer.GuiRenderOperation;
import nl.oxod.nekoclient.renderer.text.TextRenderer;

public class TextOperation extends GuiRenderOperation<TextOperation> {
  private String text;
  private TextRenderer renderer;
  private double alpha;

  public boolean title;

  public TextOperation set(String text, TextRenderer renderer, boolean title) {
    return set(text, renderer, title, 1);
  }

  public TextOperation set(String text, TextRenderer renderer, boolean title, double alpha) {
    this.text = text;
    this.renderer = renderer;
    this.title = title;
    this.alpha = alpha;

    return this;
  }

  @Override
  protected void onRun() {
    renderer.setAlpha(alpha);
    renderer.render(text, x, y, color);
  }
}
