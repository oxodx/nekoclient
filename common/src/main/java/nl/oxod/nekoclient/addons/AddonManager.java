/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.addons;

import nl.oxod.nekoclient.NekoClient;
import nl.oxod.nekoclient.platform.ModInfo;
import nl.oxod.nekoclient.platform.NekoPlatform;
import nl.oxod.nekoclient.platform.Platform;

import java.util.ArrayList;
import java.util.List;

public class AddonManager {
    public static final List<NekoAddon> ADDONS = new ArrayList<>();

    public static void init() {
        // NekoClient pseudo addon
        {
            NekoClient.ADDON = new NekoAddon() {
                @Override
                public void onInitialize() {}

                @Override
                public String getPackage() {
                    return "nl.oxod.nekoclient";
                }

                @Override
                public String getWebsite() {
                    return "https://meteorclient.com";
                }

                @Override
                public GithubRepo getRepo() {
                    return new GithubRepo("oxodx", "nekoclient");
                }

                @Override
                public String getCommit() {
                    String commit = Platform.get().commit();
                    return commit == null || commit.isEmpty() || "unknown".equals(commit) ? null : commit;
                }
            };

            ModInfo self = Platform.get().self();

            NekoClient.ADDON.name = self.name();
            NekoClient.ADDON.authors = self.authors().toArray(String[]::new);

            // An empty author list is not fatal -- TitleScreenCredits still renders, it just
            // produces "NekoClient by " with nobody after it. Say so, because the usual cause is a
            // loader-specific metadata declaration drifting out of sync with fabric.mod.json.
            if (self.authors().isEmpty()) {
                NekoClient.LOG.warn(
                    "No authors found in mod metadata, so the title screen credit will list none. "
                        + "NeoForge reads these from the [modproperties.{}] table in neoforge.mods.toml, "
                        + "while Fabric reads the authors array in fabric.mod.json.",
                    NekoClient.MOD_ID);
            }

            // NekoClient is the only mod declaring this key; third-party addons may omit it.
            applyColor(NekoClient.ADDON, Platform.get().color());

            ADDONS.add(NekoClient.ADDON);
        }

        // Addons
        for (NekoPlatform.Entrypoint<? extends NekoAddon> entrypoint : Platform.get().entrypoints("meteor", NekoAddon.class)) {
            ModInfo metadata = entrypoint.mod();
            NekoAddon addon = entrypoint.value();

            addon.name = metadata.name();

            if (metadata.authors().isEmpty()) throw new RuntimeException("Addon \"%s\" requires at least 1 author to be defined in its mod metadata.".formatted(addon.name));
            addon.authors = metadata.authors().toArray(String[]::new);

            applyColor(addon, entrypoint.color());
            ADDONS.add(addon);
        }
    }

    private static void applyColor(NekoAddon addon, String color) {
        if (color != null && !color.isBlank()) addon.color.parse(color);
    }
}
