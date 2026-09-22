package nl.oxod.nekoclient.security;

import meteordevelopment.meteorclient.systems.modules.Modules;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.protocol.Packet;
import nl.oxod.nekoclient.systems.modules.misc.ProtectorModule;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

public final class Protector {
  private static final String OPSEC_MOD_ID = "opsec";
  private static final String EXPLOIT_PREVENTER_MOD_ID = "exploitpreventer";
  private static final boolean OPSEC_PRESENT;
  private static final boolean EXPLOIT_PREVENTER_PRESENT;

  private static final int ACTIVE = 1;
  private static final int SPOOF_BRAND = 1 << 1;
  private static final int FILTER_CHANNELS = 1 << 2;
  private static final int PROTECT_TRANSLATIONS = 1 << 3;
  private static final int DISABLE_TELEMETRY = 1 << 4;
  private static final int BLOCK_LOCAL_URLS = 1 << 5;
  private static final int ISOLATE_PACK_CACHE = 1 << 6;
  private static final int STRIP_SERVER_PACKS = 1 << 7;
  private static final int SKIP_CHAT_SIGNING = 1 << 8;
  private static final int VANILLA_MODE = 1 << 9;

  private static final RuntimeState UNINITIALIZED_STATE = new RuntimeState(false, null, 0, 0);
  private static volatile RuntimeState runtimeState = UNINITIALIZED_STATE;

  static {
    boolean opsec = false;
    boolean exploitPreventer = false;
    try {
      FabricLoader loader = FabricLoader.getInstance();
      opsec = loader.isModLoaded(OPSEC_MOD_ID);
      exploitPreventer = loader.isModLoaded(EXPLOIT_PREVENTER_MOD_ID);
    } catch (Throwable ignored) {
    }
    OPSEC_PRESENT = opsec;
    EXPLOIT_PREVENTER_PRESENT = exploitPreventer;
  }

  private Protector() {
  }

  public static boolean isExternalProtectorPresent() {
    return isFullExternalProtectorPresent();
  }

  public static boolean isFullExternalProtectorPresent() {
    return OPSEC_PRESENT;
  }

  public static boolean isOverlapExternalProtectorPresent() {
    return OPSEC_PRESENT || EXPLOIT_PREVENTER_PRESENT;
  }

  public static boolean isExploitPreventerPresent() {
    return EXPLOIT_PREVENTER_PRESENT && !OPSEC_PRESENT;
  }

  public static boolean isActive() {
    return has(ACTIVE);
  }

  public static boolean shouldSpoofBrand() {
    return has(SPOOF_BRAND);
  }

  public static String getEffectiveBrand() {
    return "vanilla";
  }

  public static boolean isVanillaMode() {
    return has(VANILLA_MODE);
  }

  public static boolean isFabricMode() {
    int flags = state().effectiveFlags;
    return (flags & ACTIVE) != 0 && (flags & VANILLA_MODE) == 0;
  }

  public static boolean shouldFilterChannels() {
    return has(FILTER_CHANNELS);
  }

  public static boolean shouldProtectTranslationKeys() {
    return has(PROTECT_TRANSLATIONS);
  }

  public static boolean shouldTagPacketComponents() {
    return has(PROTECT_TRANSLATIONS);
  }

  public static boolean shouldDisableTelemetry() {
    return has(DISABLE_TELEMETRY);
  }

  public static boolean shouldBlockLocalUrls() {
    return has(BLOCK_LOCAL_URLS);
  }

  public static boolean shouldIsolatePackCache() {
    return has(ISOLATE_PACK_CACHE);
  }

  public static boolean shouldStripServerPacks() {
    return has(STRIP_SERVER_PACKS);
  }

  public static boolean shouldSkipChatSigning() {
    return has(SKIP_CHAT_SIGNING);
  }

  public static boolean shouldForceDenyResourcePack() {
    ProtectorModule module = currentModule();
    return module != null && module.forceDenyResourcePack.get();
  }

  public static boolean shouldBypassResourcePack() {
    ProtectorModule module = currentModule();
    return module != null && module.bypassResourcePack.get();
  }

  public static int packResponseDelayMs() {
    ProtectorModule module = currentModule();
    return module == null ? 0 : Math.max(0, module.packResponseDelayMs.get());
  }

  public static void refreshRuntimeState() {
    publishRuntimeState(currentModule());
  }

  public static void publishRuntimeState(ProtectorModule module) {
    if (module == null) {
      runtimeState = UNINITIALIZED_STATE;
      return;
    }

    int sourceFlags = sourceFlags(module);
    RuntimeState current = runtimeState;
    if (current.initialized && current.module == module && current.sourceFlags == sourceFlags) return;

    runtimeState = new RuntimeState(true, module, sourceFlags,
      effectiveFlags(sourceFlags, OPSEC_PRESENT, isOverlapExternalProtectorPresent()));
  }

  private static boolean has(int flag) {
    return (state().effectiveFlags & flag) != 0;
  }

  private static RuntimeState state() {
    RuntimeState state = runtimeState;
    if (state.initialized) return state;
    publishRuntimeState(currentModule());
    return runtimeState;
  }

  private static ProtectorModule currentModule() {
    try {
      Modules modules = Modules.get();
      if (modules == null) return null;
      return modules.get(ProtectorModule.class);
    } catch (Throwable ignored) {
      return null;
    }
  }

  static int sourceFlags(ProtectorModule module) {
    int flags = 0;
    if (module.isActive()) flags |= ACTIVE;
    if (module.spoofBrand.get()) flags |= SPOOF_BRAND;
    if (module.filterChannels.get()) flags |= FILTER_CHANNELS;
    if (module.protectTranslations.get()) flags |= PROTECT_TRANSLATIONS;
    if (module.disableTelemetry.get()) flags |= DISABLE_TELEMETRY;
    if (module.blockLocalUrls.get()) flags |= BLOCK_LOCAL_URLS;
    if (module.isolatePackCache.get()) flags |= ISOLATE_PACK_CACHE;
    if (module.stripServerPacks.get()) flags |= STRIP_SERVER_PACKS;
    if (module.skipChatSigning.get()) flags |= SKIP_CHAT_SIGNING;
    if (module.spoofClientVanilla.get()) flags |= VANILLA_MODE;
    return flags;
  }

  static int effectiveFlags(int sourceFlags, boolean fullExternalProtector, boolean overlapExternalProtector) {
    if (fullExternalProtector || (sourceFlags & ACTIVE) == 0) return 0;

    int flags = sourceFlags;
    if (overlapExternalProtector) {
      flags &= ~(PROTECT_TRANSLATIONS | BLOCK_LOCAL_URLS | ISOLATE_PACK_CACHE);
    }
    return flags;
  }

  static Object runtimeStateIdentityForTests() {
    return runtimeState;
  }

  private record RuntimeState(boolean initialized, ProtectorModule module, int sourceFlags, int effectiveFlags) {
  }

  private static final int MAX_BYPASS_ENTRIES = 1024;

  private static final Map<Object, Boolean> USER_BYPASS_PACKETS =
    Collections.synchronizedMap(new IdentityHashMap<>());

  public static void markUserBypass(Packet<?> packet) {
    if (packet == null) return;
    synchronized (USER_BYPASS_PACKETS) {
      if (USER_BYPASS_PACKETS.size() >= MAX_BYPASS_ENTRIES) {
        Iterator<Map.Entry<Object, Boolean>> it = USER_BYPASS_PACKETS.entrySet().iterator();
        if (it.hasNext()) {
          it.next();
          it.remove();
        }
      }
      USER_BYPASS_PACKETS.put(packet, Boolean.TRUE);
    }
  }

  public static boolean isUserBypass(Packet<?> packet) {
    if (packet == null) return false;
    return USER_BYPASS_PACKETS.containsKey(packet);
  }

  public static void consumeUserBypass(Packet<?> packet) {
    if (packet == null) return;
    USER_BYPASS_PACKETS.remove(packet);
  }
}
