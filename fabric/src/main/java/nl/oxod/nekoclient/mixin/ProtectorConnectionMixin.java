package nl.oxod.nekoclient.mixin;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import nl.oxod.nekoclient.security.ProtectorPackStrip;
import nl.oxod.nekoclient.security.ProtectorSpoofPayloadFilter;
import nl.oxod.nekoclient.security.ResourcePackTruthGuard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public abstract class ProtectorConnectionMixin {

    @Unique
    private static final String PROTECTOR_SPOOF_FILTER = "protector_spoof_filter";

    @Shadow private Channel channel;

    @Unique
    private volatile boolean protector$spoofPipelineInstalled;

    @Inject(method = "channelActive", at = @At("HEAD"))
    private void protector$onChannelActive(ChannelHandlerContext context, CallbackInfo ci) {
        protector$spoofPipelineInstalled = false;
        protector$ensureSpoofPipelineFilter();
    }

    @Inject(method = "channelInactive", at = @At("HEAD"))
    private void protector$onChannelInactive(ChannelHandlerContext context, CallbackInfo ci) {
        protector$spoofPipelineInstalled = false;
        ProtectorPackStrip.clearAll();
        ResourcePackTruthGuard.clearAll();
    }

    @Inject(method = "configurePacketHandler", at = @At("TAIL"))
    private void protector$onConfigurePacketHandler(ChannelPipeline pipeline, CallbackInfo ci) {
        protector$ensureSpoofPipelineFilter();
    }

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;)V", at = @At("HEAD"), cancellable = true)
    private void protector$onSendPacket(Packet<?> packet, ChannelFutureListener listener, CallbackInfo ci) {
        protector$ensureSpoofPipelineFilter();

        if (packet instanceof ServerboundResourcePackPacket resourcePackPacket) {
            if (ResourcePackTruthGuard.shouldCancelOutboundStatus(resourcePackPacket)) {
                ci.cancel();
                return;
            }
            ProtectorPackStrip.onPackFinalResponse(resourcePackPacket.id(), resourcePackPacket.action());
        }
        if (packet instanceof ServerboundCustomPayloadPacket) {
            if (ProtectorSpoofPayloadFilter.shouldBlockForVanillaSpoof(packet)) {
                ci.cancel();
                return;
            }
            if (ProtectorSpoofPayloadFilter.shouldDropForProtector(packet)) {
                ci.cancel();
                return;
            }
        }
    }

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V", at = @At("HEAD"), cancellable = true)
    private void protector$onSendPacketWithFlush(Packet<?> packet, ChannelFutureListener listener, boolean flush, CallbackInfo ci) {
        protector$ensureSpoofPipelineFilter();

        if (packet instanceof ServerboundResourcePackPacket resourcePackPacket) {
            if (ResourcePackTruthGuard.shouldCancelOutboundStatus(resourcePackPacket)) {
                ci.cancel();
                return;
            }
            ProtectorPackStrip.onPackFinalResponse(resourcePackPacket.id(), resourcePackPacket.action());
        }
        if (packet instanceof ServerboundCustomPayloadPacket) {
            if (ProtectorSpoofPayloadFilter.shouldBlockForVanillaSpoof(packet)) {
                ci.cancel();
                return;
            }
            if (ProtectorSpoofPayloadFilter.shouldDropForProtector(packet)) {
                ci.cancel();
                return;
            }
        }
    }

    @Unique
    private void protector$ensureSpoofPipelineFilter() {
        if (protector$spoofPipelineInstalled) return;
        Channel pipelineChannel = null;
        try {
            pipelineChannel = channel;
        } catch (Throwable ignored) {
        }
        if (pipelineChannel == null) return;

        try {
            ChannelPipeline pipeline = pipelineChannel.pipeline();
            if (pipeline == null || pipeline.get(PROTECTOR_SPOOF_FILTER) != null) {
                protector$spoofPipelineInstalled = true;
                return;
            }
            if (pipeline.get("encoder") != null) {
                pipeline.addAfter("encoder", PROTECTOR_SPOOF_FILTER, new ProtectorSpoofPayloadFilter());
                protector$spoofPipelineInstalled = true;
            }
        } catch (Throwable t) {
            MeteorClient.LOG.debug("[NekoClientProtector] Failed to install client spoof payload filter", t);
        }
    }
}
