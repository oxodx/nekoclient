package nl.oxod.nekoclient.platform;

import java.util.List;

/**
 * Loader-neutral view of a mod's metadata.
 *
 * <p>Mirrors the fields of {@code fabric.mod.json} and {@code neoforge.mods.toml} that shared
 * code actually reads, so callers never touch a loader metadata class directly.
 */
public record ModInfo(String id, String name, String version, List<String> authors) {
  public ModInfo {
    id = id == null ? "" : id;
    name = name == null || name.isBlank() ? id : name;
    version = version == null ? "" : version;
    authors = authors == null ? List.of() : List.copyOf(authors);
  }
}
