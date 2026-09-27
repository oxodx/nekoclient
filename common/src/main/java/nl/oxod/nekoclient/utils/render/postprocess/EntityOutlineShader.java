package nl.oxod.nekoclient.utils.render.postprocess;

import nl.oxod.nekoclient.renderer.MeshRenderer;
import nl.oxod.nekoclient.renderer.NekoRenderPipelines;
import nl.oxod.nekoclient.systems.modules.Modules;
import nl.oxod.nekoclient.systems.modules.render.ESP;
import net.minecraft.world.entity.Entity;

public class EntityOutlineShader extends EntityShader {
  private static ESP esp;

  public EntityOutlineShader() {
    super(NekoRenderPipelines.POST_OUTLINE);
  }

  @Override
  protected boolean shouldDraw() {
    if (esp == null) esp = Modules.get().get(ESP.class);
    return esp.isShader();
  }

  @Override
  public boolean shouldDraw(Entity entity) {
    if (!shouldDraw()) return false;
    return !esp.shouldSkip(entity);
  }

  @Override
  protected void setupPass(MeshRenderer renderer) {
    renderer.uniform("OutlineData", OutlineUniforms.write(
      esp.outlineWidth.get(),
      esp.fillOpacity.get().floatValue(),
      esp.shapeMode.get().ordinal(),
      esp.glowMultiplier.get().floatValue()
    ));
  }
}
