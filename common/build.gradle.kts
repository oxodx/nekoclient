plugins {
  id("minecraft-conventions")
}

dependencies {
  minecraft(libs.minecraft)
  compileOnly("org.lwjgl:lwjgl:3.4.1")
  compileOnly("org.lwjgl:lwjgl-glfw:3.4.1")
  compileOnly("org.lwjgl:lwjgl-sdl:3.4.1")
  compileOnly("org.lwjgl:lwjgl-tinyfd:3.4.1")
  compileOnly("com.mojang:authlib:10.0.77")

  // Loader-neutral libraries. Shared code links against these directly, so both
  // loader modules bundle them.
  api(libs.orbit)
  api(libs.starscript)
  api(libs.discord.ipc)
  api(libs.reflections)
  api(libs.netty.handler.proxy) { isTransitive = false }
  api(libs.netty.codec.socks) { isTransitive = false }
  api(libs.waybackauthlib)
  api(libs.minecraft.auth) {
    exclude("com.google.code.gson")
    exclude("com.google.errorprone")
  }

  // Mod APIs that only one loader ships. The compat mixins and integrations that use these
  // stay in :common so the source layout matches upstream, but they are compile-only here
  // and every corresponding mixin config is registered by the fabric module only.
  compileOnly(libs.baritone)
  compileOnly(libs.modmenu)
  compileOnly(libs.sodium) { isTransitive = false }
  compileOnly(libs.lithium) { isTransitive = false }
  compileOnly(libs.iris) { isTransitive = false }
  compileOnly(fabricApi.module("fabric-renderer-indigo", libs.versions.fabric.api.get()) as ModuleDependency) {
    isTransitive = false
  }

  // Mixin and MixinExtras are provided by the loader at runtime on both targets, so they
  // only need to be on the compile classpath.
  compileOnly(libs.fabric.loader)
  compileOnly("io.github.llamalad7:mixinextras-fabric:0.4.1")
}

loom {
  accessWidenerPath = file("src/main/resources/nekoclient.classtweaker")
}

val uiTests = sourceSets.create("uiTest") {
  compileClasspath += sourceSets.main.get().output
  runtimeClasspath += sourceSets.main.get().output
}

val uiEngineTest = tasks.register<JavaExec>("uiEngineTest") {
  group = "verification"
  description = "Checks UI clipping, hit testing, pointer capture, stacking and motion without a game window."
  dependsOn(tasks.named(uiTests.classesTaskName))
  classpath = uiTests.runtimeClasspath
  mainClass.set("nl.oxod.nekoclient.gui.engine.UiEngineTest")
}

tasks.check { dependsOn(uiEngineTest) }
