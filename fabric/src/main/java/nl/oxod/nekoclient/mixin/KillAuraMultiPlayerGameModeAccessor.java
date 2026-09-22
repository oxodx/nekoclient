package nl.oxod.nekoclient.mixin;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MultiPlayerGameMode.class)
public interface KillAuraMultiPlayerGameModeAccessor {
  @Invoker("ensureHasSentCarriedItem")
  void killAura$ensureHasSentCarriedItem();
}
