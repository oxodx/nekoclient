package nl.oxod.nekoclient;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.screens.ModulesScreen;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforgespi.language.IModInfo;
import nl.oxod.nekoclient.platform.ModInfo;
import nl.oxod.nekoclient.platform.NekoPlatform;
import nl.oxod.nekoclient.platform.Platform;
import nl.oxod.nekoclient.platform.RegistrationPayloadAdapter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * NeoForge implementation of the platform seam and the {@code @Mod} entry point.
 *
 * <p>The constructor runs during mod loading, well before {@code Minecraft} exists, so it only
 * installs the platform. The client bootstrap itself is triggered from {@code Minecraft}'s
 * constructor by {@code MinecraftMixin}, which is the earliest point shared by both loaders.
 */
@Mod(value = NekoClientNeoForge.MOD_ID, dist = Dist.CLIENT)
public class NekoClientNeoForge implements NekoPlatform {
    public static final String MOD_ID = "nekoclient";

    private final ModInfo self;

    public NekoClientNeoForge() {
        this.self = readSelfMetadata();
        // Only the platform is installed here. Mod constructors run long before Minecraft
        // exists, so the client bootstrap is left to MinecraftMixin's constructor injection.
        Platform.install(this);
        MeteorClient.bindPlatform(this);
    }

    // --- NekoPlatform ---

    @Override
    public String loader() {
        return "neoforge";
    }

    @Override
    public Path gameDirectory() {
        return FMLPaths.GAMEDIR.get();
    }

    @Override
    public ModInfo self() {
        return self;
    }

    @Override
    public String commit() {
        return modProperty(MeteorClient.MOD_ID + ":commit", "");
    }

    @Override
    public String color() {
        return modProperty(MeteorClient.MOD_ID + ":color", null);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        var current = FMLLoader.getCurrentOrNull();
        return current == null || !current.isProduction();
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public Set<String> allModIds() {
        Set<String> ids = new LinkedHashSet<>();
        for (IModInfo info : ModList.get().getMods()) {
            ids.add(info.getModId());
        }
        return ids;
    }

    @Override
    public Set<String> declaredDependencies(String modId) {
        Set<String> dependencies = new LinkedHashSet<>();
        for (IModInfo info : ModList.get().getMods()) {
            if (!info.getModId().equals(modId)) continue;

            for (IModInfo.ModVersion dependency : info.getDependencies()) {
                // INCOMPATIBLE means the dep must be absent, so it is not a dependency.
                if (dependency.getType() == IModInfo.DependencyType.INCOMPATIBLE) continue;
                dependencies.add(dependency.getModId());
            }
        }
        dependencies.remove(modId);
        return dependencies;
    }

    @Override
    public String rootModId(String modId) {
        ModContainer container = ModList.get().getModContainerById(modId).orElse(null);
        if (container == null) return modId;

        // Walk up the owning-file chain; the outermost owner is the real mod.
        String root = container.getModId();
        for (IModInfo info : ModList.get().getMods()) {
            if (info.getModId().equals(modId) && info.getOwningFile() != null) {
                String owner = info.getOwningFile().getMods().stream()
                    .map(IModInfo::getModId)
                    .findFirst()
                    .orElse(root);
                if (ModList.get().isLoaded(owner)) root = owner;
            }
        }
        return root;
    }

    @Override
    public <T> List<Entrypoint<? extends T>> entrypoints(String key, Class<T> type) {
        // NeoForge has no generic entrypoint container equivalent to Fabric's. Addons are
        // discovered through the Java service loader scoped to each mod's classloader, so an
        // addon declares itself in META-INF/services/<MeteorAddon subclass>.
        List<Entrypoint<? extends T>> entrypoints = new ArrayList<>();
        for (String modId : allModIds()) {
            ModContainer container = ModList.get().getModContainerById(modId).orElse(null);
            if (container == null) continue;

            for (T value : ServiceLoaderLite.loadAll(type, container)) {
                entrypoints.add(new Entrypoint<>(
                    modId,
                    modInfo(modId),
                    value,
                    modPropertyOf(modId, MeteorClient.MOD_ID + ":color", null)
                ));
            }
        }
        return entrypoints;
    }

    @Override
    public RegistrationPayloadAdapter registrationPayloads() {
        // NeoForge's networking layer has no equivalent of Fabric's `minecraft:register`
        // payload, so the shared filter drops those packets.
        return RegistrationPayloadAdapter.UNSUPPORTED;
    }

    @Override
    public Screen createConfigScreen() {
        return new ModulesScreen(GuiThemes.get());
    }

    // --- helpers ---

    private static ModInfo readSelfMetadata() {
        if (!ModList.get().isLoaded(MOD_ID)) {
            throw new IllegalStateException("NekoClient is not registered with the NeoForge mod list");
        }
        return modInfo(MOD_ID);
    }

    private static ModInfo modInfo(String modId) {
        IModInfo info = ModList.get().getMods().stream()
            .filter(candidate -> candidate.getModId().equals(modId))
            .findFirst()
            .orElse(null);

        if (info == null) return new ModInfo(modId, modId, "0.0.0", List.of());

        return new ModInfo(
            info.getModId(),
            info.getDisplayName(),
            info.getVersion().toString(),
            readAuthors(info)
        );
    }

    /**
     * NeoForge's {@code IModInfo} does not expose the {@code authors} list from the mod metadata,
     * so authors are declared as a mod property instead. Keeping them in a property (rather than
     * parsing the toml) means addons get the same treatment without any file reading at startup.
     */
    private static List<String> readAuthors(IModInfo info) {
        Object authors = info.getModProperties().get(MeteorClient.MOD_ID + ":authors");
        if (authors == null) return List.of();

        if (authors instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return Arrays.stream(String.valueOf(authors).split(","))
            .map(String::trim)
            .filter(author -> !author.isEmpty())
            .toList();
    }

    private static String modProperty(String key, String fallback) {
        return modPropertyOf(MOD_ID, key, fallback);
    }

    private static String modPropertyOf(String modId, String key, String fallback) {
        return ModList.get().getMods().stream()
            .filter(info -> info.getModId().equals(modId))
            .map(info -> info.getModProperties().get(key))
            .filter(java.util.Objects::nonNull)
            .map(String::valueOf)
            .findFirst()
            .orElse(fallback);
    }
}
