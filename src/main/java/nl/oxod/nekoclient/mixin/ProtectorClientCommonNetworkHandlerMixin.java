package nl.oxod.nekoclient.mixin;

import nl.oxod.nekoclient.security.ProtectorPackResponseScheduler;
import nl.oxod.nekoclient.security.ProtectorPackStrip;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.protocol.common.ClientboundResourcePackPopPacket;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import nl.oxod.nekoclient.security.ResourcePackTruthGuard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;
import java.util.UUID;

@Mixin(ClientCommonPacketListenerImpl.class)
public abstract class ProtectorClientCommonNetworkHandlerMixin {

    @Inject(method = "handleResourcePackPush", at = @At("HEAD"))
    private void protector$onPackPush(ClientboundResourcePackPushPacket packet, CallbackInfo ci) {
        ProtectorPackStrip.onPackPush(packet.id());
    }

    @Inject(method = "handleResourcePackPop", at = @At("HEAD"))
    private void protector$onPackPop(ClientboundResourcePackPopPacket packet, CallbackInfo ci) {
        Optional<UUID> id = packet.id();
        ProtectorPackStrip.onPop(id.orElse(null));
        ResourcePackTruthGuard.onPop(id.orElse(null));

        ProtectorPackResponseScheduler.cancel(id.orElse(null));
    }
}
