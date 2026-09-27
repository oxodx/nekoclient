package nl.oxod.nekoclient.utils.render.postprocess;

import nl.oxod.nekoclient.renderer.MeshRenderer;
import nl.oxod.nekoclient.renderer.NekoRenderPipelines;
import nl.oxod.nekoclient.systems.modules.Modules;
import nl.oxod.nekoclient.systems.modules.render.StorageESP;

public class StorageOutlineShader extends PostProcessShader {
  private static StorageESP storageESP;

  public StorageOutlineShader() {
    super(NekoRenderPipelines.POST_OUTLINE);
  }

  @Override
  protected boolean shouldDraw() {
    if (storageESP == null) storageESP = Modules.get().get(StorageESP.class);
    return storageESP.isShader();
  }

  @Override
  protected void setupPass(MeshRenderer renderer) {
    renderer.uniform("OutlineData", OutlineUniforms.write(
      storageESP.outlineWidth.get(),
      storageESP.fillOpacity.get() / 255.0f,
      storageESP.shapeMode.get().ordinal(),
      storageESP.glowMultiplier.get().floatValue()
    ));
  }
}
