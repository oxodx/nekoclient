package nl.oxod.nekoclient.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import nl.oxod.nekoclient.security.ProtectorRegistryComponentCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Item.Properties.class)
public abstract class ProtectorItemPropertiesMixin {

    @SuppressWarnings({"UnresolvedMixinReference", "unchecked"})
    @WrapOperation(
        method = "lambda$delayedHolderComponent$0",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/core/HolderLookup$Provider;getOrThrow(Lnet/minecraft/resources/ResourceKey;)Lnet/minecraft/core/Holder$Reference;"
        ),
        require = 0
    )
    private static <T> Holder.Reference<T> protector$skipMissingTrimMaterialHolder(
        HolderLookup.Provider context,
        ResourceKey<T> valueKey,
        Operation<Holder.Reference<T>> original
    ) {
        try {
            return original.call(context, valueKey);
        } catch (RuntimeException error) {

            if (ProtectorRegistryComponentCompat.shouldSkipMissingDelayedHolder(valueKey, error)) {
                ProtectorRegistryComponentCompat.reportSkippedMissingTrimMaterial(valueKey);
                return null;
            }

            if (ProtectorRegistryComponentCompat.shouldSkipMissingComponentData(error)) {
                ProtectorRegistryComponentCompat.reportSkippedMissingComponent(error);
                return null;
            }
            throw error;
        }
    }
}
