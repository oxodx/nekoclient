package nl.oxod.nekoclient.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentInitializers;
import nl.oxod.nekoclient.security.ProtectorRegistryComponentCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(DataComponentInitializers.SingleComponentInitializer.class)
public interface ProtectorDataComponentInitializerMixin {

    @SuppressWarnings("UnresolvedMixinReference")
    @WrapOperation(
        method = "lambda$asInitializer$0",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/core/component/DataComponentInitializers$SingleComponentInitializer;create(Lnet/minecraft/core/HolderLookup$Provider;)Ljava/lang/Object;"
        ),
        require = 0
    )
    private static Object protector$skipMissingRemoteComponent(
        DataComponentInitializers.SingleComponentInitializer<?> self,
        HolderLookup.Provider context,
        Operation<Object> original
    ) {
        try {
            return original.call(self, context);
        } catch (RuntimeException error) {
            if (!ProtectorRegistryComponentCompat.shouldSkipMissingComponentData(error)) {
                throw error;
            }
            ProtectorRegistryComponentCompat.reportSkippedMissingComponent(error);

            return null;
        }
    }
}
