package nl.oxod.nekoclient.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Minecraft.class)
public interface KillAuraMinecraftAccessor {
  @Accessor("missTime")
  int killAura$getMissTime();
}
