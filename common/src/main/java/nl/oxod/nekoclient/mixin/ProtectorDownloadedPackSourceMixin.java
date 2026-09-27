package nl.oxod.nekoclient.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.resources.server.DownloadedPackSource;
import net.minecraft.client.resources.server.PackReloadConfig;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.RepositorySource;
import nl.oxod.nekoclient.security.Protector;
import nl.oxod.nekoclient.security.ProtectorLangOnlyPackResources;
import nl.oxod.nekoclient.security.ProtectorPackStrip;
import nl.oxod.nekoclient.security.ProtectorServerPackFailureGuard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.file.Path;
import java.util.UUID;

@Mixin(DownloadedPackSource.class)
public abstract class ProtectorDownloadedPackSourceMixin {

    @Inject(method = "createRepositorySource", at = @At("RETURN"), cancellable = true)
    private void protector$suppressDownloadedPackSourceAfterFailure(CallbackInfoReturnable<RepositorySource> cir) {
        RepositorySource original = cir.getReturnValue();
        cir.setReturnValue(output -> {
            if (ProtectorServerPackFailureGuard.shouldSuppressServerPacks()) return;
            original.loadPacks(output);
        });
    }

    @WrapOperation(
        method = "loadRequestedPacks",
        at = @At(value = "NEW", target = "(Ljava/nio/file/Path;)Lnet/minecraft/server/packs/FilePackResources$FileResourcesSupplier;"))
    private FilePackResources.FileResourcesSupplier protector$wrapFilePackSupplier(
            Path file,
            Operation<FilePackResources.FileResourcesSupplier> original,
            @Local PackReloadConfig.IdAndPath idAndPath) {

        FilePackResources.FileResourcesSupplier real = original.call(file);

        if (!Protector.shouldStripServerPacks()) return real;
        UUID packId = idAndPath.id();
        if (!ProtectorPackStrip.isWrapped(packId)) return real;

        return new FilePackResources.FileResourcesSupplier(file) {
            @Override
            public PackResources openPrimary(PackLocationInfo loc) {
                return new ProtectorLangOnlyPackResources(real.openPrimary(loc));
            }

            @Override
            public PackResources openFull(PackLocationInfo loc, Pack.Metadata md) {
                return new ProtectorLangOnlyPackResources(real.openFull(loc, md));
            }
        };
    }
}
