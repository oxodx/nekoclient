package nl.oxod.nekoclient.systems.modules.misc;

import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import nl.oxod.nekoclient.security.Protector;

public class ProtectorModule extends Module {
  private final SettingGroup sgGeneral = settings.getDefaultGroup();

  public final Setting<Boolean> spoofBrand = sgGeneral.add(new BoolSetting.Builder()
    .name("spoof-brand")
    .description("Spoof the client brand reported to servers.")
    .defaultValue(true)
    .onChanged(s -> Protector.refreshRuntimeState())
    .build()
  );

  public final Setting<Boolean> filterChannels = sgGeneral.add(new BoolSetting.Builder()
    .name("filter-channels")
    .description("Filter outgoing custom payload channels to only allow trusted ones.")
    .defaultValue(true)
    .onChanged(s -> Protector.refreshRuntimeState())
    .build()
  );

  public final Setting<Boolean> protectTranslations = sgGeneral.add(new BoolSetting.Builder()
    .name("protect-translations")
    .description("Hide translation keys and keybindings sent by the server that are not vanilla or known to client mods.")
    .defaultValue(true)
    .onChanged(s -> Protector.refreshRuntimeState())
    .build()
  );

  public final Setting<Boolean> disableTelemetry = sgGeneral.add(new BoolSetting.Builder()
    .name("disable-telemetry")
    .description("Disable the Mojang telemetry session.")
    .defaultValue(true)
    .onChanged(s -> Protector.refreshRuntimeState())
    .build()
  );

  public final Setting<Boolean> blockLocalUrls = sgGeneral.add(new BoolSetting.Builder()
    .name("block-local-urls")
    .description("Block resource pack downloads from local or private network addresses.")
    .defaultValue(true)
    .onChanged(s -> Protector.refreshRuntimeState())
    .build()
  );

  public final Setting<Boolean> isolatePackCache = sgGeneral.add(new BoolSetting.Builder()
    .name("isolate-pack-cache")
    .description("Isolate the resource pack download cache per account and pack id.")
    .defaultValue(true)
    .onChanged(s -> Protector.refreshRuntimeState())
    .build()
  );

  public final Setting<Boolean> stripServerPacks = sgGeneral.add(new BoolSetting.Builder()
    .name("strip-server-packs")
    .description("Load server resource packs as language-only so assets are not applied.")
    .defaultValue(false)
    .onChanged(s -> Protector.refreshRuntimeState())
    .build()
  );

  public final Setting<Boolean> skipChatSigning = sgGeneral.add(new BoolSetting.Builder()
    .name("skip-chat-signing")
    .description("Send chat messages without signing them.")
    .defaultValue(false)
    .onChanged(s -> Protector.refreshRuntimeState())
    .build()
  );

  public final Setting<Boolean> spoofClientVanilla = sgGeneral.add(new BoolSetting.Builder()
    .name("spoof-client-vanilla")
    .description("Report the client as vanilla and suppress all non-brand custom payload packets.")
    .defaultValue(true)
    .onChanged(s -> Protector.refreshRuntimeState())
    .build()
  );

  public final Setting<Boolean> forceDenyResourcePack = sgGeneral.add(new BoolSetting.Builder()
    .name("force-deny-resource-pack")
    .description("Always decline server resource packs.")
    .defaultValue(false)
    .build()
  );

  public final Setting<Boolean> bypassResourcePack = sgGeneral.add(new BoolSetting.Builder()
    .name("bypass-resource-pack")
    .description("Pretend to accept the resource pack without applying it.")
    .defaultValue(false)
    .build()
  );

  public final Setting<Integer> packResponseDelayMs = sgGeneral.add(new IntSetting.Builder()
    .name("pack-response-delay-ms")
    .description("Base delay before declining a resource pack, randomized to look human.")
    .defaultValue(20000)
    .min(0)
    .max(120000)
    .sliderRange(0, 60000)
    .build()
  );

  public ProtectorModule() {
    super(Categories.Misc, "protector", "Anti-fingerprinting protections that hide mods, translation keys and channel usage from servers.");

    runInMainMenu = true;
  }

  @Override
  public void onActivate() {
    Protector.refreshRuntimeState();
  }

  @Override
  public void onDeactivate() {
    Protector.refreshRuntimeState();
  }
}
