package nl.oxod.nekoclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import nl.oxod.nekoclient.systems.modules.movement.Scaffold;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public class ScaffoldLivingEntityMovementMixin {
  @ModifyExpressionValue(method = "jumpFromGround", at = @At(value = "NEW",
    target = "(DDD)Lnet/minecraft/world/phys/Vec3;"))
  private Vec3 scaffold$silentJumpImpulse(Vec3 original) {
    return Scaffold.correctedJumpImpulse((LivingEntity) (Object) this, original);
  }

  @ModifyExpressionValue(method = "updateFallFlyingMovement", at = @At(value = "INVOKE",
    target = "Lnet/minecraft/world/entity/LivingEntity;getXRot()F"))
  private float scaffold$silentGlidePitch(float original) {
    return Scaffold.correctedFallFlyingPitch((LivingEntity) (Object) this, original);
  }

  @ModifyExpressionValue(method = "updateFallFlyingMovement", at = @At(value = "INVOKE",
    target = "Lnet/minecraft/world/entity/LivingEntity;getLookAngle()Lnet/minecraft/world/phys/Vec3;"))
  private Vec3 scaffold$silentGlideLook(Vec3 original) {
    return Scaffold.correctedFallFlyingLook((LivingEntity) (Object) this, original);
  }
}
