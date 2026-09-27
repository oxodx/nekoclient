package nl.oxod.nekoclient.security;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;

public final class ProtectorSpoofPayloadFilter extends ChannelOutboundHandlerAdapter {
  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
    if (msg instanceof ServerboundCustomPayloadPacket packet) {
      if (shouldBlockForVanillaSpoof(packet)) {
        Protector.consumeUserBypass(packet);
        promise.setSuccess();
        return;
      }

      ProtectorChannelFilter.Verdict verdict = ProtectorChannelFilter.filter(packet);
      switch (verdict.kind) {
        case DROP -> {
          Protector.consumeUserBypass(packet);
          promise.setSuccess();
          return;
        }
        case REPLACE -> {
          Protector.consumeUserBypass(packet);
          super.write(ctx, verdict.replacement, promise);
          return;
        }
        case PASS -> Protector.consumeUserBypass(packet);
      }
    }
    super.write(ctx, msg, promise);
  }

  public static boolean shouldBlockForVanillaSpoof(net.minecraft.network.protocol.Packet<?> packet) {
    if (!(packet instanceof ServerboundCustomPayloadPacket customPayload)) return false;
    if (Protector.isUserBypass(packet)) return false;
    if (!Protector.isVanillaMode()) return false;
    String channel = payloadChannel(customPayload.payload());
    return !isBrandChannel(channel);
  }

  public static boolean shouldDropForProtector(net.minecraft.network.protocol.Packet<?> packet) {
    ProtectorChannelFilter.Verdict verdict = ProtectorChannelFilter.filter(packet);
    return verdict.kind == ProtectorChannelFilter.Verdict.Kind.DROP;
  }

  public static String payloadChannel(Object payload) {
    try {
      Object type = payload.getClass().getMethod("type").invoke(payload);
      if (type == null) return null;
      Object id = type.getClass().getMethod("id").invoke(type);
      if (id == null) return null;
      return id.toString();
    } catch (Throwable ignored) {
      return null;
    }
  }

  public static boolean isBrandChannel(String channel) {
    return "minecraft:brand".equals(channel);
  }
}
