package nl.oxod.nekoclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
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

  @ModifyExpressionValue(method = "pick(Lnet/minecraft/world/entity/Entity;DDF)Lnet/minecraft/world/phys/HitResult;",
    at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getViewVector(F)Lnet/minecraft/world/phys/Vec3;"))
  private static Vec3 killAura$silentViewVector(Vec3 original, Entity camera, double blockInteractionRange,
                                                double entityInteractionRange, float tickDelta) {
    if (camera != Minecraft.getInstance().player) {
      return original;
    }
    return KillAura.silentViewVector((LocalPlayer) camera, original);
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
