package nl.oxod.nekoclient.utils.render.postprocess;

import static nl.oxod.nekoclient.NekoClient.mc;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.world.entity.Entity;
import nl.oxod.nekoclient.mixininterface.ILevelRenderer;

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

  public void submitVertices() {}
}
