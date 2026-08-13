package nl.oxod.nekoclient.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface KillAuraLivingEntityAccessor {
  @Accessor("attackStrengthTicker")
  int killAura$getAttackStrengthTicker();

  @Accessor("autoSpinAttackDmg")
  float killAura$getAutoSpinAttackDmg();
}
