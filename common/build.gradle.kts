plugins {
    id("minecraft-conventions")
}

dependencies {
    minecraft(libs.minecraft)

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
    compileOnly(libs.viafabricplus) { isTransitive = false }
    compileOnly(libs.viafabricplus.api) { isTransitive = false }
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

// Headless checks for Scaffold's pure movement/rotation calculations. A separate
// source set keeps these executable checks independent of a test framework.
val scaffoldRegression = sourceSets.create("scaffoldRegression") {
    compileClasspath += sourceSets.main.get().output + sourceSets.main.get().compileClasspath
    runtimeClasspath += sourceSets.main.get().output + sourceSets.main.get().runtimeClasspath
}
val scaffoldRegressionTest = tasks.register<JavaExec>("scaffoldRegressionTest") {
    dependsOn(tasks.named(scaffoldRegression.classesTaskName))
    classpath = scaffoldRegression.runtimeClasspath
    mainClass.set("nl.oxod.nekoclient.systems.modules.movement.ScaffoldRotationTest")
    javaLauncher.set(javaToolchains.launcherFor(java.toolchain))
}
tasks.named("check") {
    dependsOn(scaffoldRegressionTest)
}
