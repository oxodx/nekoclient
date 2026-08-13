package nl.oxod.nekoclient.mixin;

import net.fabricmc.fabric.impl.networking.PayloadTypeRegistryImpl;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import nl.oxod.nekoclient.security.ProtectorModResolver;
import nl.oxod.nekoclient.security.ProtectorTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SuppressWarnings("UnstableApiUsage")
@Mixin(PayloadTypeRegistryImpl.class)
public class ProtectorPayloadTypeRegistryImplMixin {
    @Inject(method = "register", at = @At("RETURN"))
    private void protector$trackPayloadDefaultMod(CustomPacketPayload.Type<?> type, StreamCodec<?, ?> codec,
                                                  CallbackInfoReturnable<CustomPacketPayload.TypeAndCodec<?, ?>> cir) {
        for (String mod : ProtectorModResolver.modsFromStacktrace()) {
            ProtectorTracker.addDefaultAllowedMod(mod);
            ProtectorTracker.addDefaultAllowedMods(ProtectorModResolver.dependenciesFor(mod));
        }
    }
}
