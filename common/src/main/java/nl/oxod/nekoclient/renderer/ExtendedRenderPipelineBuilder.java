/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.renderer;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import nl.oxod.nekoclient.mixininterface.IRenderPipeline;
import org.jspecify.annotations.NonNull;

public class ExtendedRenderPipelineBuilder extends RenderPipeline.Builder {
  private boolean lineSmooth;

  public ExtendedRenderPipelineBuilder(RenderPipeline.Snippet... snippets) {
    for (RenderPipeline.Snippet snippet : snippets) {
      withSnippet(snippet);
    }
  }

  public ExtendedRenderPipelineBuilder withLineSmooth() {
    lineSmooth = true;
    return this;
  }

  @Override
  public @NonNull RenderPipeline build() {
    RenderPipeline pipeline = super.build();
    ((IRenderPipeline) pipeline).neko$setLineSmooth(lineSmooth);

    return pipeline;
  }
}
