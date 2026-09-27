group = providers.gradleProperty("maven_group").get()

// CI stamps a dated version on daily builds with -PreleaseVersion=2026.09.27, which
// flows into the jar name and into fabric.mod.json / neoforge.mods.toml. Falls back
// to the Fabric API version, which encodes the Minecraft and loader it targets.
version = providers.gradleProperty("releaseVersion").getOrElse(libs.versions.fabric.api.get())
