package nl.oxod.nekoclient.platform;

import java.lang.reflect.Method;

/**
 * Loader detection that works before {@link Platform#install} has run.
 *
 * <p>Mixin config plugins are evaluated while the mixin configs are parsed, which is earlier than
 * any mod entry point, so they cannot use the {@link NekoPlatform} service. Instead this probes
 * the classpath reflectively and keeps every loader call behind a {@code try} block, so a missing
 * loader class degrades to "not loaded" rather than failing the mixin phase.
 */
public final class LoaderDetection {
  private static final String FABRIC_LOADER = "net.fabricmc.loader.api.FabricLoader";
  private static final String NEOFORGE_LIST = "net.neoforged.fml.ModList";

  private LoaderDetection() {
  }

  public static boolean classExists(String name) {
    try {
      Class.forName(name, false, LoaderDetection.class.getClassLoader());
      return true;
    } catch (Throwable ignored) {
      return false;
    }
  }

  /**
   * Whether a class is on the classpath <em>without loading it</em>.
   *
   * <p>Use this, not {@link #classExists}, when the class being probed is also the target of a
   * mixin. A mixin config plugin runs during config preparation; loading its target there makes
   * Mixin fail the boot with {@code MixinTargetAlreadyLoadedException}, because the class is now
   * loaded before the mixin can be applied to it. A resource lookup answers the same question
   * without triggering class loading.
   *
   * @param binaryName dotted class name, e.g. {@code baritone.command.defaults.ComeCommand}
   */
  public static boolean classPresent(String binaryName) {
    if (binaryName == null || binaryName.isBlank()) return false;
    try {
      return LoaderDetection.class.getClassLoader()
        .getResource(binaryName.replace('.', '/') + ".class") != null;
    } catch (Throwable ignored) {
      return false;
    }
  }

  public static boolean isFabric() {
    return classExists(FABRIC_LOADER);
  }

  public static boolean isNeoForge() {
    return classExists(NEOFORGE_LIST);
  }

  /**
   * Best-effort "is this mod loaded" probe for the active loader. Returns {@code false} when the
   * loader is unknown, the mod id is unknown to it, or reflection fails.
   */
  public static boolean isModLoaded(String modId) {
    if (modId == null || modId.isBlank()) return false;
    try {
      if (isFabric()) return fabricIsModLoaded(modId);
      if (isNeoForge()) return neoforgeIsModLoaded(modId);
    } catch (Throwable ignored) {
      // A loader that cannot answer is treated as "not loaded", so the mixin is skipped.
    }
    return false;
  }

  private static boolean fabricIsModLoaded(String modId) throws Exception {
    Class<?> loader = Class.forName(FABRIC_LOADER);
    Object instance = loader.getMethod("getInstance").invoke(null);
    return (Boolean) loader.getMethod("isModLoaded", String.class).invoke(instance, modId);
  }

  private static boolean neoforgeIsModLoaded(String modId) throws Exception {
    Class<?> listClass = Class.forName(NEOFORGE_LIST);
    Object modList = listClass.getMethod("get").invoke(null);

    Method byId = findMethod(listClass, "getModContainerById", String.class);
    Object container = byId == null ? null : byId.invoke(modList, modId);
    if (container == null) return false;

    Method isLoaded = findMethod(container.getClass(), "isLoaded", Object.class);
    return isLoaded != null && Boolean.TRUE.equals(isLoaded.invoke(container, modList));
  }

  private static Method findMethod(Class<?> owner, String name, Class<?> parameter) {
    for (Class<?> type = owner; type != null; type = type.getSuperclass()) {
      try {
        return type.getDeclaredMethod(name, parameter);
      } catch (NoSuchMethodException ignored) {
        // Try the superclass.
      }
    }
    return null;
  }
}
