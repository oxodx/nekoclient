/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.utils.render;

import java.util.List;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.gui.GuiRenderState;

public class NekoMcGuiRenderer extends GuiRenderer {
  public NekoMcGuiRenderer(GuiRenderState renderState, FeatureRenderDispatcher featureRenderDispatcher,
      List<PictureInPictureRenderer<?>> pictureInPictureRenderers) {
    super(renderState, featureRenderDispatcher, pictureInPictureRenderers);
  }
}
