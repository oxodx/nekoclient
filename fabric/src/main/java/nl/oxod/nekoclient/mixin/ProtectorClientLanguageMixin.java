package nl.oxod.nekoclient.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.fabricmc.fabric.impl.resource.pack.ModNioPackResources;
import net.minecraft.client.resources.language.ClientLanguage;
import net.minecraft.locale.Language;
import net.minecraft.server.packs.CompositePackResources;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.VanillaPackResources;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import nl.oxod.nekoclient.security.ProtectorModResolver;
import nl.oxod.nekoclient.security.ProtectorTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.InputStream;
import java.util.List;
import java.util.function.BiConsumer;

@Mixin(ClientLanguage.class)
public class ProtectorClientLanguageMixin {
    @Inject(method = "loadFrom", at = @At("HEAD"))
    private static void protector$clearLanguageTracking(ResourceManager resourceManager, List<String> languageStack,
                                                        boolean defaultRightToLeft,
                                                        CallbackInfoReturnable<ClientLanguage> cir) {
        ProtectorTracker.resetTranslations();
    }

    @WrapOperation(
        method = "appendFrom",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/locale/Language;loadFromJson(Ljava/io/InputStream;Ljava/util/function/BiConsumer;)V"))
    private static void protector$trackTranslations(InputStream stream, BiConsumer<String, String> output,
                                                    Operation<Void> original, @Local Resource resource) {
        PackResources source = resource.source();
        if (source instanceof VanillaPackResources) {
            original.call(stream, trackingOutput(output, (key, value) -> ProtectorTracker.addVanillaTranslation(key)));
            return;
        }
        if (source instanceof FilePackResources || source instanceof CompositePackResources) {
            original.call(stream, trackingOutput(output, ProtectorTracker::addServerTranslation));
            return;
        }
        if (source instanceof PathPackResources) {
            original.call(stream, output);
            return;
        }
        String modId = source instanceof ModNioPackResources modPack
            ? modPack.getFabricModMetadata().getId()
            : ProtectorModResolver.modFromClass(source.getClass());
        if (modId == null) {
            original.call(stream, output);
            return;
        }
        original.call(stream, trackingOutput(output, (key, value) -> ProtectorTracker.addModTranslation(key, modId)));
    }

    private static BiConsumer<String, String> trackingOutput(BiConsumer<String, String> output, BiConsumer<String, String> tracker) {
        return (key, value) -> {
            tracker.accept(key, value);
            output.accept(key, value);
        };
    }
}
