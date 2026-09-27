package nl.oxod.nekoclient.mixin;

import net.minecraft.client.Minecraft;
import nl.oxod.nekoclient.systems.modules.movement.Scaffold;
import nl.oxod.nekoclient.util.InputClicker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class ScaffoldMinecraftMixin {
  @Inject(method = "tick", at = @At("HEAD"))
  private void scaffold$onClientTickStart(CallbackInfo ci) {
    InputClicker.onClientTickStart();
  }

  @Inject(method = "handleKeybinds", at = @At("HEAD"))
  private void scaffold$beforeHandleKeybinds(CallbackInfo ci) {
    Scaffold.beforeHandleKeybinds();
    InputClicker.beforeHandleKeybinds();
  }

  @Inject(method = "handleKeybinds", at = @At("TAIL"))
  private void scaffold$afterHandleKeybinds(CallbackInfo ci) {
    InputClicker.afterHandleKeybinds();
  }
}
