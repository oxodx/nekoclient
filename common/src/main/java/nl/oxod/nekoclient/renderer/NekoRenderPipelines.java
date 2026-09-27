/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.renderer;

import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import nl.oxod.nekoclient.NekoClient;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManager;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public abstract class NekoRenderPipelines {
  private static final List<RenderPipeline> PIPELINES = new ArrayList<>();

  // Snippets

  private static final BindGroupLayout MESH_BIND_GROUP = BindGroupLayout.builder()
    .withUniform("MeshData", UniformType.UNIFORM_BUFFER)
    .build();

  // Deliberately does NOT include BindGroupLayouts.GLOBALS. MeshRenderer draws on its own
  // RenderPass and only binds MeshData plus the pipeline's own uniforms -- projection and
  // modelview are folded into MeshData by MeshUniforms.write(..) -- so it never calls
  // RenderSystem.bindDefaultUniforms(). Adding the Globals layout here would make
  // GlCommandEncoder.trySetup() demand a uniform that nothing binds, and every mesh draw would
  // fail with "Missing uniform Globals (should be UNIFORM_BUFFER)".
  private static final RenderPipeline.Snippet MESH_UNIFORMS = RenderPipeline.builder()
    .withBindGroupLayout(MESH_BIND_GROUP)
    .buildSnippet();

  // World

  public static final RenderPipeline WORLD_COLORED = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
    .withLocation(NekoClient.identifier("pipeline/world_colored"))
    .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR).withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
    .withVertexShader(NekoClient.identifier("shaders/pos_color.vert"))
    .withFragmentShader(NekoClient.identifier("shaders/pos_color.frag"))
    .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
    .withCull(false)
    .build()
  );

  public static final RenderPipeline WORLD_COLORED_LINES = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
    .withLineSmooth()
    .withLocation(NekoClient.identifier("pipeline/world_colored_lines"))
    .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR).withPrimitiveTopology(PrimitiveTopology.DEBUG_LINES)
    .withVertexShader(NekoClient.identifier("shaders/pos_color.vert"))
    .withFragmentShader(NekoClient.identifier("shaders/pos_color.frag"))
    .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
    .withCull(false)
    .build()
  );

  public static final RenderPipeline WORLD_COLORED_DEPTH = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
    .withLocation(NekoClient.identifier("pipeline/world_colored_depth"))
    .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR).withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
    .withVertexShader(NekoClient.identifier("shaders/pos_color.vert"))
    .withFragmentShader(NekoClient.identifier("shaders/pos_color.frag"))
    .withDepthStencilState(new DepthStencilState(DepthStencilState.DEFAULT.depthTest(), false))
    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
    .withCull(false)
    .build()
  );

  public static final RenderPipeline WORLD_COLORED_LINES_DEPTH = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
    .withLineSmooth()
    .withLocation(NekoClient.identifier("pipeline/world_colored_lines_depth"))
    .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR).withPrimitiveTopology(PrimitiveTopology.DEBUG_LINES)
    .withVertexShader(NekoClient.identifier("shaders/pos_color.vert"))
    .withFragmentShader(NekoClient.identifier("shaders/pos_color.frag"))
    .withDepthStencilState(new DepthStencilState(DepthStencilState.DEFAULT.depthTest(), false))
    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
    .withCull(false)
    .build()
  );

  // UI

  public static final RenderPipeline UI_COLORED = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
    .withLocation(NekoClient.identifier("pipeline/ui_colored"))
    .withVertexBinding(0, NekoVertexFormats.POS2_COLOR).withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
    .withVertexShader(NekoClient.identifier("shaders/pos_color.vert"))
    .withFragmentShader(NekoClient.identifier("shaders/pos_color.frag"))
    .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
    .withCull(true)
    .build()
  );

  public static final RenderPipeline UI_COLORED_LINES = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
    .withLocation(NekoClient.identifier("pipeline/ui_colored_lines"))
    .withVertexBinding(0, NekoVertexFormats.POS2_COLOR).withPrimitiveTopology(PrimitiveTopology.DEBUG_LINES)
    .withVertexShader(NekoClient.identifier("shaders/pos_color.vert"))
    .withFragmentShader(NekoClient.identifier("shaders/pos_color.frag"))
    .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
    .withCull(true)
    .build()
  );

  public static final RenderPipeline UI_TEXTURED = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
    .withLocation(NekoClient.identifier("pipeline/ui_textured"))
    .withVertexBinding(0, NekoVertexFormats.POS2_TEXTURE_COLOR).withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
    .withVertexShader(NekoClient.identifier("shaders/pos_tex_color.vert"))
    .withFragmentShader(NekoClient.identifier("shaders/pos_tex_color.frag"))
    .withBindGroupLayout(BindGroupLayout.builder().withSampler("u_Texture").build())
    .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
    .withCull(true)
    .build()
  );

  public static final RenderPipeline UI_TEXT = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
    .withLocation(NekoClient.identifier("pipeline/ui_text"))
    .withVertexBinding(0, NekoVertexFormats.POS2_TEXTURE_COLOR).withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
    .withVertexShader(NekoClient.identifier("shaders/text.vert"))
    .withFragmentShader(NekoClient.identifier("shaders/text.frag"))
    .withBindGroupLayout(BindGroupLayout.builder().withSampler("u_Texture").build())
    .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
    .withCull(true)
    .build()
  );

  // Post Process

  public static final RenderPipeline POST_OUTLINE = add(new ExtendedRenderPipelineBuilder()
    .withLocation(NekoClient.identifier("pipeline/post/outline"))
    .withVertexBinding(0, NekoVertexFormats.POS2).withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
    .withVertexShader(NekoClient.identifier("shaders/post-process/base.vert"))
    .withFragmentShader(NekoClient.identifier("shaders/post-process/outline.frag"))
    .withBindGroupLayout(BindGroupLayout.builder()
      .withSampler("u_Texture")
      .withUniform("PostData", UniformType.UNIFORM_BUFFER)
      .withUniform("OutlineData", UniformType.UNIFORM_BUFFER)
      .build())
    .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
    .withCull(false)
    .build()
  );

  public static final RenderPipeline POST_IMAGE = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
    .withLocation(NekoClient.identifier("pipeline/post/image"))
    .withVertexBinding(0, NekoVertexFormats.POS2).withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
    .withVertexShader(NekoClient.identifier("shaders/post-process/base.vert"))
    .withFragmentShader(NekoClient.identifier("shaders/post-process/image.frag"))
    .withBindGroupLayout(BindGroupLayout.builder()
      .withSampler("u_Texture")
      .withSampler("u_TextureI")
      .withUniform("PostData", UniformType.UNIFORM_BUFFER)
      .withUniform("ImageData", UniformType.UNIFORM_BUFFER)
      .build())
    .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
    .withCull(false)
    .build()
  );

  // Blur

  public static final RenderPipeline BLUR_DOWN = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
    .withLocation(NekoClient.identifier("pipeline/blur/down"))
    .withVertexBinding(0, NekoVertexFormats.POS2).withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
    .withVertexShader(NekoClient.identifier("shaders/blur.vert"))
    .withFragmentShader(NekoClient.identifier("shaders/blur_down.frag"))
    .withBindGroupLayout(BindGroupLayout.builder()
      .withSampler("u_Texture")
      .withUniform("BlurData", UniformType.UNIFORM_BUFFER)
      .build())
    .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
    .withCull(false)
    .build()
  );

  public static final RenderPipeline BLUR_UP = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
    .withLocation(NekoClient.identifier("pipeline/blur/up"))
    .withVertexBinding(0, NekoVertexFormats.POS2).withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
    .withVertexShader(NekoClient.identifier("shaders/blur.vert"))
    .withFragmentShader(NekoClient.identifier("shaders/blur_up.frag"))
    .withBindGroupLayout(BindGroupLayout.builder()
      .withSampler("u_Texture")
      .withUniform("BlurData", UniformType.UNIFORM_BUFFER)
      .build())
    .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
    .withCull(false)
    .build()
  );

  public static final RenderPipeline BLUR_PASSTHROUGH = add(new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
    .withLocation(NekoClient.identifier("pipeline/blur/up"))
    .withVertexBinding(0, NekoVertexFormats.POS2).withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
    .withVertexShader(NekoClient.identifier("shaders/passthrough.vert"))
    .withFragmentShader(NekoClient.identifier("shaders/passthrough.frag"))
    .withBindGroupLayout(BindGroupLayout.builder().withSampler("u_Texture").build())
    .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
    .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
    .withCull(false)
    .build()
  );

  private static RenderPipeline add(RenderPipeline pipeline) {
    PIPELINES.add(pipeline);
    return pipeline;
  }

  public static void precompile() {
    GpuDevice device = RenderSystem.getDevice();
    ResourceManager resources = Minecraft.getInstance().getResourceManager();

    for (RenderPipeline pipeline : PIPELINES) {
      device.precompilePipeline(pipeline, (identifier, _) -> {
        var resource = resources.getResource(identifier).get();

        try (var in = resource.open()) {
          return IOUtils.toString(in, StandardCharsets.UTF_8);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      });
    }
  }

  private NekoRenderPipelines() {
  }
}
