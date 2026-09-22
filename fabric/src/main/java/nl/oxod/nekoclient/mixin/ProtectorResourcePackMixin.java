package nl.oxod.nekoclient.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import nl.oxod.nekoclient.security.Protector;
import nl.oxod.nekoclient.security.ProtectorPackResponseScheduler;
import nl.oxod.nekoclient.security.ProtectorPackStrip;
import nl.oxod.nekoclient.security.ResourcePackTruthGuard;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientCommonPacketListenerImpl.class)
public abstract class ProtectorResourcePackMixin {
    @Shadow @Final protected Minecraft minecraft;

    @Shadow public abstract void send(Packet<?> packet);

    @Inject(method = "handleResourcePackPush", at = @At("HEAD"), cancellable = true)
    private void protector$onResourcePackSend(ClientboundResourcePackPushPacket packet, CallbackInfo ci) {
        boolean shouldForceDeny = Protector.shouldForceDenyResourcePack();
        boolean shouldBypass = Protector.shouldBypassResourcePack();

        ResourcePackTruthGuard.Verdict verdict =
            ResourcePackTruthGuard.classify(packet, shouldForceDeny, shouldBypass);
        if (!verdict.shouldCancelVanilla()) return;

        ProtectorPackStrip.onPop(packet.id());

        java.util.function.Consumer<ServerboundResourcePackPacket> sender = this::send;
        switch (verdict.kind()) {
            case BYPASS_SUCCESS -> {
                long accepted = ProtectorPackResponseScheduler.acceptDelayMs();
                long downloaded = ProtectorPackResponseScheduler.downloadedDelayMs(accepted);
                long applied = ProtectorPackResponseScheduler.appliedDelayMs(downloaded);
                ProtectorPackResponseScheduler.schedule(packet.id(), ServerboundResourcePackPacket.Action.ACCEPTED, accepted, sender);
                ProtectorPackResponseScheduler.schedule(packet.id(), ServerboundResourcePackPacket.Action.DOWNLOADED, downloaded, sender);
                ProtectorPackResponseScheduler.schedule(packet.id(), ServerboundResourcePackPacket.Action.SUCCESSFULLY_LOADED, applied, sender);
            }
            case DECLINE -> ProtectorPackResponseScheduler.schedule(packet.id(),
                ServerboundResourcePackPacket.Action.DECLINED, ProtectorPackResponseScheduler.declineDelayMs(), sender);

            case INVALID_URL -> send(new ServerboundResourcePackPacket(packet.id(), ServerboundResourcePackPacket.Action.INVALID_URL));
            case FAILED_DOWNLOAD -> {
                long accepted = ProtectorPackResponseScheduler.acceptDelayMs();
                ProtectorPackResponseScheduler.schedule(packet.id(), ServerboundResourcePackPacket.Action.ACCEPTED, accepted, sender);
                ProtectorPackResponseScheduler.schedule(packet.id(), ServerboundResourcePackPacket.Action.FAILED_DOWNLOAD,
                    ProtectorPackResponseScheduler.failedDelayMs(accepted), sender);
            }
            default -> {
            }
        }

        ci.cancel();
    }
}
