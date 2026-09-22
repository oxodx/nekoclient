plugins {
    id("common-conventions")
}

apply(plugin = "net.fabricmc.fabric-loom")
apply(plugin = "net.ltgt.errorprone")

base {
    archivesName = rootProject.name
}