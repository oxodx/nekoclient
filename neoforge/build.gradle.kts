plugins {
    id("neoforge-conventions")
}

val modId = "nekoclient"

neoForge {
    version = libs.versions.neoforge.get()

    runs {
        create("client") {
            client()
        }
        create("server") {
            server()
        }
        create("data") {
            clientData()
        }
    }

    mods {
        create(modId) {
            sourceSet(sourceSets.main.get())
        }
    }
}

dependencies {
    implementation(project(":common"))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}