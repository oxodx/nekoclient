package nl.oxod.nekoclient.mixin;

import net.minecraft.client.ClientBrandRetriever;
import nl.oxod.nekoclient.security.Protector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientBrandRetriever.class)
public class ProtectorClientBrandRetrieverMixin {
    @Inject(method = "getClientModName", at = @At("HEAD"), cancellable = true, remap = false)
    private static void protector$spoofClientBrand(CallbackInfoReturnable<String> cir) {

        if (Protector.isFullExternalProtectorPresent()) return;

        if (Protector.isVanillaMode()) {
            cir.setReturnValue("vanilla");
            return;
        }

        if (Protector.shouldSpoofBrand()) {
            cir.setReturnValue(Protector.getEffectiveBrand());
        }
    }
}
