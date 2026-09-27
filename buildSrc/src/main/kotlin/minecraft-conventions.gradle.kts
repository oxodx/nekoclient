plugins {
    id("base-conventions")
}

apply(plugin = "net.fabricmc.fabric-loom")

// Loom generates typed accessors (minecraft(..), fabricApi.module(..), loom { .. }) only in
// precompiled script plugins that are applied via the plugins { } block, so consumers that need
// them must use `minecraft-conventions` rather than applying Loom from their own body.
