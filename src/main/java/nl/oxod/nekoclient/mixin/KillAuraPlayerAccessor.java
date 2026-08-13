package nl.oxod.nekoclient.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Player.class)
public interface KillAuraPlayerAccessor {
  @Invoker("getEnchantedDamage")
  float killAura$getEnchantedDamage(Entity entity, float dmg, DamageSource damageSource);
}
