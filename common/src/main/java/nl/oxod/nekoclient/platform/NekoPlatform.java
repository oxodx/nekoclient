package nl.oxod.nekoclient.platform;

import net.minecraft.client.gui.screens.Screen;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * The seam between shared client code and the mod loader that hosts it.
 *
 * <p>Implementations live in the {@code fabric} and {@code neoforge} modules and must be
 * installed through {@link Platform#install} <em>before</em> {@code NekoClient} is loaded,
 * because {@code NekoClient}'s static state is derived from this service.
 */
public interface NekoPlatform {
  /**
   * Lowercase loader id, e.g. {@code fabric} or {@code neoforge}.
   */
  String loader();

  /**
   * Directory the game was launched from; the mod folder is a child of it.
   */
  Path gameDirectory();

  /**
   * Metadata of NekoClient itself, as declared by the active loader.
   */
  ModInfo self();

  /**
   * Value of the {@code nekoClient:commit} custom metadata key, or an empty string.
   */
  String commit();

  /**
   * Value of the {@code nekoClient:color} custom metadata key, or {@code null} if unset.
   */
  String color();

  /**
   * Whether the game runs from a development environment.
   */
  boolean isDevelopmentEnvironment();

  boolean isModLoaded(String modId);

  /**
   * Ids of every mod the loader has loaded, excluding core loader/mod infrastructure.
   */
  Set<String> allModIds();

  /**
   * Ids a mod declares a dependency on, plus the ids of any mods nested inside its jar.
   * Returns an empty set for unknown mods.
   */
  Set<String> declaredDependencies(String modId);

  /**
   * Outermost mod id for a mod that may be nested inside another mod's jar (jar-in-jar).
   * Returns {@code modId} unchanged when it is not nested or unknown to the loader.
   */
  String rootModId(String modId);

  /**
   * Entry points declared under {@code key} whose value is assignable to {@code type}.
   */
  <T> List<Entrypoint<? extends T>> entrypoints(String key, Class<T> type);

  /**
   * Register-payload support for the loader, or {@link RegistrationPayloadAdapter#UNSUPPORTED}.
   */
  RegistrationPayloadAdapter registrationPayloads();

  /**
   * Screen shown by the loader's own mod-list UI, or {@code null} when the loader has no such
   * screen (or the corresponding mod is not installed).
   */
  Screen createConfigScreen();

  /**
   * A declared entry point together with the metadata of the mod that declared it.
   */
  record Entrypoint<T>(String modId, ModInfo mod, T value, String color) {
  }
}
