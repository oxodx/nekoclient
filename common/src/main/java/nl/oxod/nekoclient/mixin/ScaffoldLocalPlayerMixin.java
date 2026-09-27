package nl.oxod.nekoclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.player.LocalPlayer;
import nl.oxod.nekoclient.systems.modules.movement.Scaffold;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LocalPlayer.class)
public class ScaffoldLocalPlayerMixin {
  @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE",
    target = "Lnet/minecraft/client/player/LocalPlayer;getYRot()F"))
  private float scaffold$silentMovementYaw(float original) {
    return Scaffold.outgoingMovementYaw((LocalPlayer) (Object) this, original);
  }

  @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE",
    target = "Lnet/minecraft/client/player/LocalPlayer;getXRot()F"))
  private float scaffold$silentMovementPitch(float original) {
    return Scaffold.outgoingMovementPitch((LocalPlayer) (Object) this, original);
  }

  @ModifyExpressionValue(method = "sendPosition", at = @At(value = "INVOKE",
    target = "Lnet/minecraft/client/player/LocalPlayer;getYRot()F"))
  private float scaffold$outgoingMovementYaw(float original) {
    return Scaffold.outgoingMovementYaw((LocalPlayer) (Object) this, original);
  }

  @ModifyExpressionValue(method = "sendPosition", at = @At(value = "INVOKE",
    target = "Lnet/minecraft/client/player/LocalPlayer;getXRot()F"))
  private float scaffold$outgoingMovementPitch(float original) {
    return Scaffold.outgoingMovementPitch((LocalPlayer) (Object) this, original);
  }
}
