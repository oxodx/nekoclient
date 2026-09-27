package nl.oxod.nekoclient.security;

import nl.oxod.nekoclient.NekoClient;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.BrandPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import nl.oxod.nekoclient.platform.Platform;
import nl.oxod.nekoclient.platform.RegistrationPayloadAdapter;

import java.util.ArrayList;
import java.util.List;

public final class ProtectorChannelFilter {
  private static final boolean DEBUG = Boolean.getBoolean("nekoclient.protector.debug");
  private static final Verdict PASS = new Verdict(Verdict.Kind.PASS, null);
  private static final Verdict DROP = new Verdict(Verdict.Kind.DROP, null);

  private static final String MINECRAFT = "minecraft";
  private static final String REGISTER = "register";
  private static final String UNREGISTER = "unregister";
  private static final String MCO = "mco";

  private ProtectorChannelFilter() {
  }

  public static Verdict pass() {
    return PASS;
  }

  public static Verdict drop() {
    return DROP;
  }

  public static Verdict filter(Packet<?> packet) {
    if (packet == null) return PASS;
    if (!(packet instanceof ServerboundCustomPayloadPacket customPayload)) return PASS;
    if (!Protector.shouldFilterChannels()) return PASS;

    if (Protector.isUserBypass(packet)) return PASS;
    CustomPacketPayload payload = customPayload.payload();
    if (payload == null) return PASS;

    if (payload instanceof BrandPayload) return PASS;

    Identifier id = payloadId(payload);
    if (id == null) return PASS;

    if (Protector.isVanillaMode()) {
      return DROP;
    }

    String namespace = id.getNamespace();
    String path = id.getPath();

    if (MINECRAFT.equals(namespace) && (REGISTER.equals(path) || UNREGISTER.equals(path))) {
      RegistrationPayloadAdapter adapter = Platform.get().registrationPayloads();
      if (adapter.isRegistrationPayload(payload)) {
        return rebuildRegister(adapter, payload);
      }
      return DROP;
    }

    if (MINECRAFT.equals(namespace) && MCO.equals(path)) return DROP;

    if (ProtectorTracker.isWhitelistedChannel(id)) return PASS;

    return DROP;
  }

  private static Identifier payloadId(CustomPacketPayload payload) {
    try {
      CustomPacketPayload.Type<?> type = payload.type();
      if (type != null) return type.id();
    } catch (Throwable ignored) {
    }
    return null;
  }

  private static Verdict rebuildRegister(RegistrationPayloadAdapter adapter, CustomPacketPayload payload) {
    List<Identifier> kept = new ArrayList<>();
    try {
      for (Identifier channel : adapter.channelsOf(payload)) {
        if (ProtectorTracker.isWhitelistedChannel(channel)) {
          kept.add(channel);
        }
      }
    } catch (Throwable t) {
      if (DEBUG) {
        NekoClient.LOG.debug("[Protector] register payload inspection failed: {}", t.getMessage());
      }
      return DROP;
    }

    if (kept.isEmpty()) return DROP;

    CustomPacketPayload rebuilt = adapter.rebuild(payload, kept).orElse(null);
    if (rebuilt == null) return DROP;
    return new Verdict(Verdict.Kind.REPLACE, new ServerboundCustomPayloadPacket(rebuilt));
  }

  public static final class Verdict {
    public enum Kind {PASS, DROP, REPLACE}

    public final Kind kind;
    public final Packet<?> replacement;

    private Verdict(Kind kind, Packet<?> replacement) {
      this.kind = kind;
      this.replacement = replacement;
    }
  }
}
