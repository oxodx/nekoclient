package nl.oxod.nekoclient.security;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import nl.oxod.nekoclient.platform.Platform;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.jar.JarFile;

public final class ProtectorModResolver {
  private ProtectorModResolver() {
  }

  public static LinkedHashSet<String> modsFromStacktrace() {
    LinkedHashSet<String> mods = new LinkedHashSet<>();
    StackTraceElement[] stack = Thread.currentThread().getStackTrace();
    for (int i = 3; i < stack.length; i++) {
      String className = stack[i].getClassName();
      if ("net.minecraft.client.main.Main".equals(className)) break;
      try {
        String mod = modFromClass(Class.forName(className, false, ProtectorModResolver.class.getClassLoader()));
        if ("fabricloader".equals(mod)) break;
        if (mod != null) mods.add(mod);
      } catch (Throwable ignored) {
      }
    }
    return mods;
  }

  public static Set<String> dependenciesFor(String modId) {
    if (modId == null || modId.isBlank()) return new HashSet<>();
    Set<String> dependencies = new HashSet<>();
    // Fabric API resource and language hooks fire from whichever mod's client entrypoint runs
    // first, which need not be ours, so the platform may not be installed yet. Without it we
    // simply know no declared dependencies, which only makes the tracking less strict.
    if (Platform.isInstalled()) dependencies.addAll(Platform.get().declaredDependencies(modId));
    dependencies.remove(modId);
    dependencies.removeIf(ProtectorModResolver::isCore);
    return dependencies;
  }

  public static String modFromClass(Class<?> type) {
    if (type == null || type.getProtectionDomain() == null || type.getProtectionDomain().getCodeSource() == null) {
      return null;
    }
    try {
      URI uri = type.getProtectionDomain().getCodeSource().getLocation().toURI();
      Path path = Path.of(uri);
      String modId = modFromPath(path);
      return rootModId(modId);
    } catch (Throwable ignored) {
      return null;
    }
  }

  public static String modFromPath(Path path) {
    if (path == null) return null;
    try {
      if (Files.isDirectory(path)) {
        for (Path candidate : new Path[]{
          path.resolve("fabric.mod.json"),
          path.resolve("../resources/main/fabric.mod.json").normalize(),
          path.resolve("../../resources/main/fabric.mod.json").normalize(),
          path.resolve("../../../resources/main/fabric.mod.json").normalize()
        }) {
          String modId = modIdFromJson(candidate);
          if (modId != null) return modId;
        }
        return null;
      }
      try (JarFile jar = new JarFile(path.toFile())) {
        var entry = jar.getJarEntry("fabric.mod.json");
        if (entry == null) return null;
        try (InputStream in = jar.getInputStream(entry);
             InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
          JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
          return root.has("id") ? root.get("id").getAsString() : null;
        }
      }
    } catch (Throwable ignored) {
      return null;
    }
  }

  private static String modIdFromJson(Path path) {
    if (path == null || !Files.isRegularFile(path)) return null;
    try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8)) {
      JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
      return root.has("id") ? root.get("id").getAsString() : null;
    } catch (Throwable ignored) {
      return null;
    }
  }

  /**
   * Resolves the outermost mod id for a possibly jar-in-jar nested mod.
   *
   * <p>Delegates to the loader, which owns the jar-in-jar containment tree. Unknown ids pass through.
   */
  private static String rootModId(String modId) {
    if (modId == null || modId.isBlank()) return null;
    // Reachable before the platform is installed; pass the id through unresolved.
    if (!Platform.isInstalled()) return modId;
    if (!Platform.get().isModLoaded(modId)) return modId;
    String root = Platform.get().rootModId(modId);
    return root == null || root.isBlank() ? modId : root;
  }

  private static boolean isCore(String modId) {
    return switch (modId) {
      case "minecraft", "java", "neoforge", "fabricloader", "fabric-loader", "fmlcore" -> true;
      default -> false;
    };
  }
}
