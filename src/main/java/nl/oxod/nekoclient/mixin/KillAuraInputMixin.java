package nl.oxod.nekoclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import nl.oxod.nekoclient.systems.modules.combat.killaura.KillAura;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(KeyboardInput.class)
public abstract class KillAuraInputMixin {
  @ModifyExpressionValue(
    method = "tick",
    at = @At(value = "NEW", target = "(ZZZZZZZ)Lnet/minecraft/world/entity/player/Input;")
  )
  private Input killAura$modifyMovementInput(Input original) {
    return KillAura.modifyMovementInput((ClientInput) (Object) this, original);
  }
}
