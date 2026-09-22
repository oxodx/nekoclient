plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
    maven {
        name = "Fabric"
        url = uri("https://maven.fabricmc.net/")
    }
}

dependencies {
    implementation("net.fabricmc:fabric-loom:${libs.versions.loom.get()}")
    implementation("net.ltgt.errorprone:net.ltgt.errorprone.gradle.plugin:${libs.versions.errorprone.plugin.get()}")
    implementation("net.neoforged.moddev:net.neoforged.moddev.gradle.plugin:${libs.versions.moddev.get()}")
}