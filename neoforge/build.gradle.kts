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

val commonProject = project(":common")

dependencies {
    // The shared module is compile-only, and its compiled output is added to this module's own
    // output directories below. Both are required for the same reason: NeoForge gives each mod
    // its own ModClassLoader, so the shared classes must be loaded by *this* mod's classloader
    // to resolve Minecraft types from the same place as the mod's own classes. Leaving the
    // common jar on the application classpath instead would load it in a different classloader,
    // and every reference to a Minecraft type from shared code would fail with a VerifyError.
    compileOnly(project(":common"))

    // The shared libraries the mod links against.
    //
    // Each is both a plain runtime dependency and a jarJar entry: jarJar nests it into the
    // published artifact, and the plain dependency puts it on the dev run's classpath. Without
    // the latter, `gradle runClient` would start with the library missing, because nested jars
    // are only unpacked from the *built* mod jar, not from the dev mod folders.
    listOf(
        libs.orbit,
        libs.starscript,
        libs.discord.ipc,
        libs.reflections,
        libs.waybackauthlib,
    ).forEach {
        implementation(it)
        jarJar(it)
    }

    listOf(libs.netty.handler.proxy, libs.netty.codec.socks).forEach {
        implementation(it) { isTransitive = false }
        jarJar(it) { isTransitive = false }
    }

    implementation(libs.minecraft.auth) {
        isTransitive = false
        exclude("com.google.code.gson")
        exclude("com.google.errorprone")
    }
    jarJar(libs.minecraft.auth) {
        isTransitive = false
        exclude("com.google.code.gson")
        exclude("com.google.errorprone")
    }
}

tasks.withType<ProcessResources>().configureEach {
    val propertyMap = mapOf(
        "version" to project.version,
        "commit" to (System.getenv("GITHUB_SHA") ?: "unknown")
    )

    inputs.properties(propertyMap)
    filesMatching("META-INF/neoforge.mods.toml") {
        expand(propertyMap)
    }
}

// The shared module's resources are copied into this module's resources, and its classes are
// merged into the jar. NeoForge resolves `[[mixins]] config = ...` against the mod file's own
// root: in a dev run that root is `build/classes/java/main` + `build/resources/main`, and in the
// published artifact it is the jar root. Putting the mixin configs, class tweaker and assets
// here is what makes them discoverable in both cases; nesting the common jar would not.
val commonOutput = commonProject.sourceSets.main.get().output

// The shared module is copied into this module's own class output directory rather than being
// referenced as a jar. NeoForge gives each mod its own ModClassLoader whose roots are that mod's
// output directories; a project dependency on the application classpath would be loaded by the
// parent loader instead, resolve Minecraft types from a different loader, and make every shared
// reference to a Minecraft type fail to verify.

sourceSets.main {
    // Resources go through processResources so they stay in sync with the source set.
    resources.srcDir(commonProject.file("src/main/resources"))

    compileClasspath += commonOutput
    runtimeClasspath += commonOutput
}

// Copying into compileJava's own output directory makes the two tasks mutually dependent, so
// the merge is instead wired to the stages around it: `preBuild` before compileJava (so the shared
// types resolve) and `classes` after it (so the tree ends up holding both modules).
val mergeBeforeCompile = tasks.register("mergeCommonClassesBeforeCompile", Copy::class) {
    dependsOn(commonProject.tasks.named("classes"))
    from(commonOutput.classesDirs)
    into(layout.buildDirectory.dir("classes/java/main"))
}

val mergeAfterCompile = tasks.register("mergeCommonClassesAfterCompile", Copy::class) {
    dependsOn(commonProject.tasks.named("classes"))
    from(commonOutput.classesDirs)
    into(layout.buildDirectory.dir("classes/java/main"))
}

tasks.named("compileJava") {
    mustRunAfter(mergeBeforeCompile)
    dependsOn(mergeBeforeCompile)
}

tasks.named("classes") {
    dependsOn(mergeAfterCompile)
}

tasks.matching { it.name in setOf("runClient", "runServer", "runData") }.configureEach {
    dependsOn(commonProject.tasks.named("classes"))
}

tasks.named<Jar>("jar") {
    // mergeCommonClasses / mergeCommonResources already copied the shared module into the
    // output directories this task packages, so no extra `from` is needed.

    manifest {
        attributes(
            "Specification-Title" to "NekoClient",
            "Specification-Version" to "1",
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version
        )
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

