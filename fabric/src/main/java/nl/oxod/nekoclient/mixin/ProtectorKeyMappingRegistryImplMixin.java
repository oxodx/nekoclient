package nl.oxod.nekoclient.mixin;

import net.minecraft.client.KeyMapping;
import nl.oxod.nekoclient.security.ProtectorModResolver;
import nl.oxod.nekoclient.security.ProtectorTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.LinkedHashSet;

@Mixin(targets = "net.fabricmc.fabric.impl.client.keymapping.KeyMappingRegistryImpl")
public class ProtectorKeyMappingRegistryImplMixin {
    @Inject(method = "registerKeyMapping", at = @At("RETURN"))
    private static void protector$trackModKeyMapping(KeyMapping keyMapping, CallbackInfoReturnable<KeyMapping> cir) {
        LinkedHashSet<String> mods = ProtectorModResolver.modsFromStacktrace();
        if (!mods.isEmpty()) ProtectorTracker.addModKeybind(keyMapping.getName(), mods.getLast());
    }
}
