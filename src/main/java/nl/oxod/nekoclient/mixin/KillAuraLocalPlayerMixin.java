package nl.oxod.nekoclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.player.LocalPlayer;
import nl.oxod.nekoclient.systems.modules.combat.killaura.KillAura;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LocalPlayer.class)
public class KillAuraLocalPlayerMixin {

  @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE",
    target = "Lnet/minecraft/client/player/LocalPlayer;getYRot()F"))
  private float killAura$silentMovementYaw(float original) {
    return KillAura.outgoingMovementYaw((LocalPlayer) (Object) this, original);
  }

  @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE",
    target = "Lnet/minecraft/client/player/LocalPlayer;getXRot()F"))
  private float killAura$silentMovementPitch(float original) {
    return KillAura.outgoingMovementPitch((LocalPlayer) (Object) this, original);
  }

  @ModifyExpressionValue(method = "sendPosition", at = @At(value = "INVOKE",
    target = "Lnet/minecraft/client/player/LocalPlayer;getYRot()F"))
  private float killAura$outgoingMovementYaw(float original) {
    return KillAura.outgoingMovementYaw((LocalPlayer) (Object) this, original);
  }

  @ModifyExpressionValue(method = "sendPosition", at = @At(value = "INVOKE",
    target = "Lnet/minecraft/client/player/LocalPlayer;getXRot()F"))
  private float killAura$outgoingMovementPitch(float original) {
    return KillAura.outgoingMovementPitch((LocalPlayer) (Object) this, original);
  }

  @ModifyExpressionValue(method = "aiStep", at = @At(value = "INVOKE",
    target = "Lnet/minecraft/client/player/LocalPlayer;canStartSprinting()Z"))
  private boolean killAura$sprintDecisionMovementTick(boolean original) {
    return original && !KillAura.blocksSprintForCrit();
  }

  @ModifyExpressionValue(method = "aiStep", at = @At(value = "INVOKE",
    target = "Lnet/minecraft/world/entity/player/Input;sprint()Z"))
  private boolean killAura$sprintDecisionInput(boolean original) {
    return original && !KillAura.blocksSprintForCrit();
  }

  @ModifyReturnValue(method = "shouldStopRunSprinting", at = @At("RETURN"))
  private boolean killAura$sprintForceStop(boolean shouldStop) {
    return shouldStop || KillAura.blocksSprintForCrit();
  }
}
