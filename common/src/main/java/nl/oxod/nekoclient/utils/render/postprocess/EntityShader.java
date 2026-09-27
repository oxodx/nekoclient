package nl.oxod.nekoclient.utils.render.postprocess;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import nl.oxod.nekoclient.mixininterface.ILevelRenderer;
import net.minecraft.world.entity.Entity;

import static nl.oxod.nekoclient.NekoClient.mc;

public abstract class EntityShader extends PostProcessShader {
  protected EntityShader(RenderPipeline pipeline) {
    super(pipeline);
  }

  public abstract boolean shouldDraw(Entity entity);

  @Override
  protected void preDraw() {
    ((ILevelRenderer) mc.levelRenderer).neko$pushEntityOutlineFramebuffer(framebuffer);
  }

  @Override
  protected void postDraw() {
    ((ILevelRenderer) mc.levelRenderer).neko$popEntityOutlineFramebuffer();
  }

  public void submitVertices() {
  }
}
