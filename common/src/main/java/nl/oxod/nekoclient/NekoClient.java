/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient;

import nl.oxod.nekoclient.addons.AddonManager;
import nl.oxod.nekoclient.addons.NekoAddon;
import nl.oxod.nekoclient.events.game.OpenScreenEvent;
import nl.oxod.nekoclient.events.neko.KeyInputEvent;
import nl.oxod.nekoclient.events.neko.MouseClickEvent;
import nl.oxod.nekoclient.events.world.TickEvent;
import nl.oxod.nekoclient.gui.GuiThemes;
import nl.oxod.nekoclient.gui.WidgetScreen;
import nl.oxod.nekoclient.gui.tabs.Tabs;
import nl.oxod.nekoclient.systems.Systems;
import nl.oxod.nekoclient.systems.config.Config;
import nl.oxod.nekoclient.systems.hud.screens.AddHudElementScreen;
import nl.oxod.nekoclient.systems.hud.screens.HudEditorScreen;
import nl.oxod.nekoclient.systems.hud.screens.HudElementScreen;
import nl.oxod.nekoclient.systems.modules.Categories;
import nl.oxod.nekoclient.systems.modules.Modules;
import nl.oxod.nekoclient.systems.modules.misc.DiscordPresence;
import nl.oxod.nekoclient.utils.PostInit;
import nl.oxod.nekoclient.utils.PreInit;
import nl.oxod.nekoclient.utils.ReflectInit;
import nl.oxod.nekoclient.utils.Utils;
import nl.oxod.nekoclient.utils.misc.Version;
import nl.oxod.nekoclient.utils.misc.input.KeyAction;
import nl.oxod.nekoclient.utils.misc.input.KeyBinds;
import nl.oxod.nekoclient.utils.network.OnlinePlayers;
import meteordevelopment.orbit.EventBus;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import meteordevelopment.orbit.IEventBus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.resources.Identifier;
import nl.oxod.nekoclient.platform.ModInfo;
import nl.oxod.nekoclient.platform.NekoPlatform;
import nl.oxod.nekoclient.platform.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.MixinEnvironment;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Loader-independent client entry point.
 *
 * <p>The {@code fabric} and {@code neoforge} modules each own a thin entry point class that
 * installs a {@link nl.oxod.nekoclient.platform.NekoPlatform} and then calls {@link #onInitialize()}.
 * Nothing in this class may reference a loader API directly.
 */
public class NekoClient {
  public static final String MOD_ID = "nekoclient";
  public static final String BUILD_NUMBER = "";
  public static final boolean DEBUG = false;

  // These start as placeholders and are replaced by bindPlatform(..) once the loader module has
  // installed one. They cannot be final: mixins run while Minecraft's own classes load, which on
  // NeoForge is before the @Mod constructor, and a class initializer that demanded the platform
  // would throw ExceptionInInitializerError and take down the boot. Crash reporting in particular
  // logs through LOG before any mod code has run.
  private static ModInfo modInfo = new ModInfo(MOD_ID, "NekoClient", "0.0.0", List.of());
  private static String name = "NekoClient";
  private static Version version = new Version("0.0.0");
  private static File folder = new File(MOD_ID);

  public static ModInfo MOD_INFO = modInfo;
  public static String NAME = name;
  public static Version VERSION = version;
  public static File FOLDER = folder;

  public static final Logger LOG = LoggerFactory.getLogger("NekoClient");

  public static NekoAddon ADDON;

  public static Minecraft mc;
  public static final IEventBus EVENT_BUS = new EventBus();

  private static final AtomicBoolean INITIALIZED = new AtomicBoolean();

  /**
   * Subscription target for the client's own event handlers. A dedicated instance keeps the
   * handlers reachable now that the bootstrap is static, and avoids the loader entry point
   * object (which differs per loader) leaking into the event bus.
   */
  private static final NekoClient HANDLERS = new NekoClient();

  /**
   * Subscription target for the client's own handlers; not a loader entry point instance.
   */
  public static final NekoClient INSTANCE = HANDLERS;

  /**
   * Replaces the placeholder metadata with the active loader's.
   *
   * <p>Called by each loader module right after installing its {@link NekoPlatform}. Safe to call
   * more than once; later calls are ignored so the values cannot change under running code.
   */
  public static synchronized void bindPlatform(NekoPlatform platform) {
    if (platformBound) return;
    platformBound = true;

    ModInfo self = platform.self();
    String versionString = self.version().split("[-+]")[0];
    // When building and running through IntelliJ and not Gradle it doesn't replace the version so just use a dummy
    if (versionString.equals("${version}")) versionString = "0.0.0";

    modInfo = self;
    name = self.name();
    version = new Version(versionString);
    folder = platform.gameDirectory().resolve(MOD_ID).toFile();

    MOD_INFO = modInfo;
    NAME = name;
    VERSION = version;
    FOLDER = folder;
  }

  private static boolean platformBound;

  private NekoClient() {
  }

  /**
   * Runs the client bootstrap. Safe to call more than once; only the first call does anything.
   *
   * <p>The only intended caller is {@code MinecraftMixin}, injecting into the end of
   * {@code Minecraft}'s constructor. That is deliberately the *only* trigger: on Fabric the
   * loader entry point runs from inside that same constructor, before the render device exists,
   * and on NeoForge mod constructors run even earlier. Bootstrapping from either of those would
   * fail in {@code Fonts.refresh}, which uploads textures and needs the device.
   */
  public static void onInitialize() {
    if (!INITIALIZED.compareAndSet(false, true)) return;
    init();
  }

  /**
   * Whether {@link #onInitialize()} has completed.
   *
   * <p>The client bootstrap is deferred to the first {@code Minecraft.tick} so that vanilla has
   * finished setting up its texture atlas before we upload GPU resources. A handful of mixin hooks
   * run earlier in the frame than that first tick and must therefore tolerate an uninitialised
   * module registry; they check this before touching {@link Modules}.
   */
  public static boolean isInitialized() {
    return INITIALIZED.get();
  }

  private static void init() {
    // Global minecraft client accessor
    mc = Minecraft.getInstance();

    if (Platform.isInstalled() && Platform.get().isDevelopmentEnvironment()) {
      LOG.info("Force loading mixins");
      MixinEnvironment.getCurrentEnvironment().audit();
    }

    LOG.info("Initializing {}", NAME);

    // Pre-load
    if (!FOLDER.exists()) {
      FOLDER.getParentFile().mkdirs();
      FOLDER.mkdir();
      Systems.addPreLoadTask(() -> Modules.get().get(DiscordPresence.class).enable());
    }

    // Register addons
    AddonManager.init();

    // Register event handlers
    AddonManager.ADDONS.forEach(addon -> {
      try {
        EVENT_BUS.registerLambdaFactory(addon.getPackage(), (lookupInMethod, klass) -> (MethodHandles.Lookup) lookupInMethod.invoke(null, klass, MethodHandles.lookup()));
      } catch (AbstractMethodError e) {
        throw new RuntimeException("Addon \"%s\" is too old and cannot be ran.".formatted(addon.name), e);
      }
    });

    // Register NekoClient lambda factory for orbit event bus
    EVENT_BUS.registerLambdaFactory("nl.oxod.nekoclient", (lookupInMethod, klass) -> (MethodHandles.Lookup) lookupInMethod.invoke(null, klass, MethodHandles.lookup()));

    // Register init classes
    ReflectInit.registerPackages();

    // Pre init
    ReflectInit.init(PreInit.class);

    // Register module categories
    Categories.init();

    // Load systems
    Systems.init();

    // Subscribe after systems are loaded
    EVENT_BUS.subscribe(HANDLERS);

    // Initialise addons
    AddonManager.ADDONS.forEach(NekoAddon::onInitialize);

    // Sort modules after addons have added their own
    Modules.get().sortModules();

    // Load configs
    Systems.load();

    // Post init
    ReflectInit.init(PostInit.class);

    // Protector bootstrap
    nl.oxod.nekoclient.security.ProtectorTracker.bootstrap();
    if (!nl.oxod.nekoclient.security.Protector.isOverlapExternalProtectorPresent()) {
      nl.oxod.nekoclient.security.ProtectorVanillaKeys.primeAsync();
    }

    // Save on shutdown
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      OnlinePlayers.leave();
      Systems.save();
      GuiThemes.save();
    }));
  }

  @EventHandler
  private void onTick(TickEvent.Post event) {
    nl.oxod.nekoclient.security.ProtectorPackResponseScheduler.tick();
    if (mc.gui.screen() == null && mc.gui.overlay() == null && KeyBinds.OPEN_COMMANDS.consumeClick()) {
      mc.gui.setScreen(new ChatScreen(Config.get().prefix.get(), true));
    }
  }

  @EventHandler
  private void onGameLeft(nl.oxod.nekoclient.events.game.GameLeftEvent event) {
    nl.oxod.nekoclient.security.ProtectorPackResponseScheduler.clearAll();
    nl.oxod.nekoclient.security.ProtectorServerPackFailureGuard.clear();
    nl.oxod.nekoclient.security.ProtectorPackStrip.clearAll();
    nl.oxod.nekoclient.security.ResourcePackTruthGuard.clearAll();
  }

  @EventHandler
  private void onKey(KeyInputEvent event) {
    if (event.action == KeyAction.Press && KeyBinds.OPEN_GUI.matches(event.input)) {
      toggleGui();
    }
  }

  @EventHandler
  private void onMouseClick(MouseClickEvent event) {
    if (event.action == KeyAction.Press && KeyBinds.OPEN_GUI.matchesMouse(event.click)) {
      toggleGui();
    }
  }

  private void toggleGui() {
    if (Utils.canCloseGui()) mc.gui.screen().onClose();
    else if (Utils.canOpenGui()) Tabs.get().getFirst().openScreen(GuiThemes.get());
  }

  // Hide HUD
  private boolean wasWidgetScreen, wasHudHiddenRoot;

  @EventHandler(priority = EventPriority.LOWEST)
  private void onOpenScreen(OpenScreenEvent event) {
    if (event.screen instanceof WidgetScreen) {
      if (!wasWidgetScreen) wasHudHiddenRoot = mc.gameRenderer.gameRenderState().guiRenderState.isHudHidden;
      if (GuiThemes.get().hideHUD() || wasHudHiddenRoot) {
        // Always show the MC HUD in the HUD editor screen since people like
        // to align some items with the hotbar or chat
        mc.gameRenderer.gameRenderState().guiRenderState.isHudHidden = !(event.screen instanceof HudEditorScreen)
          && !(event.screen instanceof AddHudElementScreen)
          && !(event.screen instanceof HudElementScreen);
      }
    } else {
      if (wasWidgetScreen) mc.gameRenderer.gameRenderState().guiRenderState.isHudHidden = wasHudHiddenRoot;
      wasHudHiddenRoot = mc.gameRenderer.gameRenderState().guiRenderState.isHudHidden;
    }

    wasWidgetScreen = event.screen instanceof WidgetScreen;
  }

  public static Identifier identifier(String path) {
    return Identifier.fromNamespaceAndPath(NekoClient.MOD_ID, path);
  }
}
