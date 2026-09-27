/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.screens.ModulesScreen;
import net.fabricmc.api.ClientModInitializer;
import nl.oxod.nekoclient.platform.ModInfo;
import nl.oxod.nekoclient.platform.NekoPlatform;
import nl.oxod.nekoclient.platform.Platform;
import nl.oxod.nekoclient.platform.RegistrationPayloadAdapter;
import net.fabricmc.fabric.impl.networking.RegistrationPayload;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModDependency;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.api.metadata.Person;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.lang.reflect.Constructor;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Fabric implementation of the platform seam, plus the Fabric mod entry point.
 *
 * <p>{@link #onInitializeClient()} installs the platform before touching any shared code, so
 * {@code MeteorClient}'s static initialiser can rely on {@link Platform#get()}.
 */
public class NekoClientFabric implements ClientModInitializer, NekoPlatform {
    private static final ModInfo SELF = readSelfMetadata();

    @Override
    public void onInitializeClient() {
        // Only the platform is installed here. Fabric invokes this from inside Minecraft's
        // constructor, before the render device exists, so the client bootstrap is left to
        // MinecraftMixin's injection at the end of that constructor.
        Platform.install(this);
        MeteorClient.bindPlatform(this);
    }

    // --- NekoPlatform ---

    @Override
    public String loader() {
        return "fabric";
    }

    @Override
    public Path gameDirectory() {
        return FabricLoader.getInstance().getGameDir();
    }

    @Override
    public ModInfo self() {
        return SELF;
    }

    @Override
    public String commit() {
        return customValue(MeteorClient.MOD_ID + ":commit", "");
    }

    @Override
    public String color() {
        return customValue(MeteorClient.MOD_ID + ":color", null);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public Set<String> allModIds() {
        Set<String> ids = new LinkedHashSet<>();
        for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
            ids.add(mod.getMetadata().getId());
        }
        return ids;
    }

    @Override
    public Set<String> declaredDependencies(String modId) {
        Set<String> dependencies = new LinkedHashSet<>();
        FabricLoader.getInstance().getModContainer(modId).ifPresent(container -> {
            for (ModContainer contained : container.getContainedMods()) {
                dependencies.add(contained.getMetadata().getId());
            }
            for (ModDependency dependency : container.getMetadata().getDependencies()) {
                if (dependency.getKind() != ModDependency.Kind.BREAKS) {
                    dependencies.add(dependency.getModId());
                }
            }
        });
        return dependencies;
    }

    @Override
    public String rootModId(String modId) {
        var current = FabricLoader.getInstance().getModContainer(modId);
        if (current.isEmpty()) return modId;

        ModContainer container = current.get();
        while (true) {
            var parent = container.getContainingMod();
            if (parent.isEmpty()) return container.getMetadata().getId();
            container = parent.get();
        }
    }

    @Override
    public <T> List<Entrypoint<? extends T>> entrypoints(String key, Class<T> type) {
        List<Entrypoint<? extends T>> entrypoints = new ArrayList<>();
        for (var container : FabricLoader.getInstance().getEntrypointContainers(key, type)) {
            ModMetadata metadata = container.getProvider().getMetadata();
            entrypoints.add(new Entrypoint<>(
                metadata.getId(),
                toModInfo(metadata),
                container.getEntrypoint(),
                optionalCustomValue(metadata, MeteorClient.MOD_ID + ":color").orElse(null)
            ));
        }
        return entrypoints;
    }

    @Override
    public RegistrationPayloadAdapter registrationPayloads() {
        return new RegistrationPayloadAdapter() {
            @Override
            public boolean isRegistrationPayload(CustomPacketPayload payload) {
                return payload instanceof RegistrationPayload;
            }

            @Override
            public List<Identifier> channelsOf(CustomPacketPayload payload) {
                return ((RegistrationPayload) payload).channels();
            }

            @Override
            public java.util.Optional<CustomPacketPayload> rebuild(CustomPacketPayload original, List<Identifier> kept) {
                RegistrationPayload rebuilt = newRegistrationPayload((RegistrationPayload) original, kept);
                return java.util.Optional.ofNullable(rebuilt);
            }
        };
    }

    @Override
    public Screen createConfigScreen() {
        return new ModulesScreen(GuiThemes.get());
    }

    // --- helpers ---

    private static ModInfo readSelfMetadata() {
        return FabricLoader.getInstance().getModContainer(MeteorClient.MOD_ID)
            .map(container -> toModInfo(container.getMetadata()))
            .orElseGet(() -> new ModInfo(MeteorClient.MOD_ID, "NekoClient", "0.0.0", List.of()));
    }

    private static ModInfo toModInfo(ModMetadata metadata) {
        List<String> authors = new ArrayList<>(metadata.getAuthors().size());
        for (Person author : metadata.getAuthors()) authors.add(author.getName());
        return new ModInfo(metadata.getId(), metadata.getName(), metadata.getVersion().getFriendlyString(), authors);
    }

    private String customValue(String key, String fallback) {
        return optionalCustomValue(selfMetadata(), key).orElse(fallback);
    }

    private static java.util.Optional<String> optionalCustomValue(ModMetadata metadata, String key) {
        if (!metadata.containsCustomValue(key)) return java.util.Optional.empty();
        try {
            return java.util.Optional.of(metadata.getCustomValue(key).getAsString());
        } catch (RuntimeException e) {
            return java.util.Optional.empty();
        }
    }

    /**
     * Rebuilds a {@link RegistrationPayload} with a filtered channel list.
     *
     * <p>The constructor signature is not stable across Fabric API versions, so both plausible
     * argument orders are attempted reflectively.
     */
    private static RegistrationPayload newRegistrationPayload(RegistrationPayload original, List<Identifier> channels) {
        for (Constructor<?> ctor : RegistrationPayload.class.getDeclaredConstructors()) {
            if (ctor.getParameterCount() != 2) continue;
            try {
                ctor.setAccessible(true);
            } catch (Throwable ignored) {
                continue;
            }
            try {
                return (RegistrationPayload) ctor.newInstance(original.type(), channels);
            } catch (Throwable ignored) {
                try {
                    return (RegistrationPayload) ctor.newInstance(channels, original.type());
                } catch (Throwable ignored2) {
                    // Try the next candidate.
                }
            }
        }
        MeteorClient.LOG.warn("[Protector] No compatible RegistrationPayload constructor; dropping packet.");
        return null;
    }

    private static ModMetadata selfMetadata() {
        return FabricLoader.getInstance().getModContainer(MeteorClient.MOD_ID)
            .map(ModContainer::getMetadata)
            .orElseThrow(() -> new IllegalStateException("NekoClient is not registered with the Fabric loader"));
    }
}
