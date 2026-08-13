package nl.oxod.nekoclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.DownloadQueue;
import nl.oxod.nekoclient.security.Protector;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.nio.file.Path;
import java.util.UUID;

@Mixin(DownloadQueue.class)
public class ProtectorDownloadQueueMixin {
    @Shadow @Final private Path cacheDir;

    @SuppressWarnings("UnresolvedMixinReference")
    @ModifyExpressionValue(
        method = {"lambda$runDownload$0", "method_55485"},
        at = @At(value = "INVOKE", target = "Ljava/nio/file/Path;resolve(Ljava/lang/String;)Ljava/nio/file/Path;"),
        require = 0)
    private Path protector$isolatePackCache(Path original, @Local(argsOnly = true, name = "id") UUID packId) {
        if (!Protector.shouldIsolatePackCache()) return original;
        if (original == null || cacheDir == null || packId == null) return original;
        Path parent = original.getParent();
        if (parent == null || !parent.equals(cacheDir)) return original;

        UUID accountId = Minecraft.getInstance().getUser().getProfileId();
        if (accountId == null) {
            MeteorClient.LOG.warn("[NekoClientProtector] Cannot isolate resource-pack cache: account UUID is null.");
            return original;
        }
        return cacheDir.resolve(accountId.toString()).resolve(packId.toString());
    }
}
