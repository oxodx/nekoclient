package nl.oxod.nekoclient.mixin;

import net.minecraft.locale.DeprecatedTranslationsInfo;
import nl.oxod.nekoclient.security.ProtectorTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;

@Mixin(DeprecatedTranslationsInfo.class)
public abstract class ProtectorDeprecatedTranslationsInfoMixin {
    @Shadow public abstract List<String> removed();
    @Shadow public abstract Map<String, String> renamed();

    @Inject(method = "applyToMap", at = @At("HEAD"))
    private void protector$trackDeprecatedTranslations(Map<String, String> translations, CallbackInfo ci) {
        ProtectorTracker.applyDeprecatedTranslations(removed(), renamed());
    }
}
