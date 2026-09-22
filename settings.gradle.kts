pluginManagement {
  repositories {
    maven {
      name = "Fabric"
      url = uri("https://maven.fabricmc.net/")
    }
    maven {
      name = "NeoForged"
      url = uri("https://maven.neoforged.net/releases")
    }
    mavenCentral()
    gradlePluginPortal()
  }
}

rootProject.name = "nekoclient"
include("common")
include("fabric")
include("neoforge")
