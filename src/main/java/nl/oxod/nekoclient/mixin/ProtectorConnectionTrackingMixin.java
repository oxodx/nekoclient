package nl.oxod.nekoclient.mixin;

import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.Connection;
import nl.oxod.nekoclient.security.ProtectorLocalAddressUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

@Mixin(Connection.class)
public class ProtectorConnectionTrackingMixin {

    @Inject(method = "channelActive", at = @At("HEAD"))
    private void protector$onChannelActive(ChannelHandlerContext context, CallbackInfo ci) {
        try {
            if (context.channel() == null) return;
            SocketAddress addr = context.channel().remoteAddress();
            if (addr instanceof InetSocketAddress inet && inet.getAddress() != null) {
                ProtectorLocalAddressUtil.serverAddress = inet.getAddress().getHostAddress();
            } else {
                ProtectorLocalAddressUtil.serverAddress = null;
            }
        } catch (Throwable ignored) {
            ProtectorLocalAddressUtil.serverAddress = null;
        }
    }

    @Inject(method = "channelInactive", at = @At("HEAD"))
    private void protector$onChannelInactive(ChannelHandlerContext context, CallbackInfo ci) {
        ProtectorLocalAddressUtil.serverAddress = null;
    }
}
