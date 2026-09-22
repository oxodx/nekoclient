package nl.oxod.nekoclient.mixin;

import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.server.ServerPackManager;
import net.minecraft.server.packs.DownloadQueue;
import nl.oxod.nekoclient.security.ProtectorPackStrip;
import nl.oxod.nekoclient.security.ProtectorServerPackFailureGuard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

@Mixin(ServerPackManager.class)
public abstract class ProtectorServerPackManagerMixin {

    @Unique private static long protector$lastRecoveryReloadMs;
    @Unique private static long protector$lastRecoveryToastMs;

    @Shadow public abstract void popAll();

    @Inject(method = "onDownload", at = @At("HEAD"))
    private void protector$makeFailedServerPackBatchAtomic(Collection<?> data, DownloadQueue.BatchResult result, CallbackInfo ci) {
        if (result == null || result.failed().isEmpty()) return;
        ProtectorServerPackFailureGuard.suppressServerPacksTemporarily();

        try {
            Map<UUID, ?> downloaded = result.downloaded();
            if (downloaded != null) downloaded.clear();
        } catch (Throwable error) {
            MeteorClient.LOG.warn("[NekoClientProtector] Failed to clear partial server-pack batch.", error);
        }
    }

    @Inject(method = "onDownload", at = @At("TAIL"))
    private void protector$recoverFromFailedServerPackDownload(Collection<?> data, DownloadQueue.BatchResult result, CallbackInfo ci) {
        if (result == null || result.failed().isEmpty()) return;

        ProtectorServerPackFailureGuard.suppressServerPacksTemporarily();
        ProtectorPackStrip.clearAll();

        try {
            popAll();
        } catch (Throwable error) {
            MeteorClient.LOG.warn("[NekoClientProtector] Failed to clear server packs after download failure.", error);
        }

        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            client.execute(() -> {
                try {
                    client.getDownloadedPackSource().popAll();
                    long now = System.currentTimeMillis();
                    if (now - protector$lastRecoveryToastMs > 5000L) {
                        protector$lastRecoveryToastMs = now;
                        MeteorClient.LOG.warn("[NekoClientProtector] Server resource pack failed. Restored client resources.");
                    }
                    if (now - protector$lastRecoveryReloadMs > 1000L) {
                        protector$lastRecoveryReloadMs = now;
                        client.reloadResourcePacks();
                    }
                } catch (Throwable error) {
                    MeteorClient.LOG.warn("[NekoClientProtector] Failed to clear downloaded pack source after download failure.", error);
                }
            });
        }
    }
}
