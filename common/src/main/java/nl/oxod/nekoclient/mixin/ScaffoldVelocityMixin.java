package nl.oxod.nekoclient.mixin;

import net.minecraft.world.entity.Entity;
import nl.oxod.nekoclient.systems.modules.movement.Scaffold;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Entity.class)
public abstract class ScaffoldVelocityMixin {
  @ModifyArg(
    method = "moveRelative",
    at = @At(
      value = "INVOKE",
      target = "Lnet/minecraft/world/entity/Entity;getInputVector(Lnet/minecraft/world/phys/Vec3;FF)Lnet/minecraft/world/phys/Vec3;"
    ),
    index = 2
  )
  private float scaffold$silentMovementYaw(float vanillaYaw) {
    return Scaffold.correctedMovementYaw((Entity) (Object) this, vanillaYaw);
  }
}
