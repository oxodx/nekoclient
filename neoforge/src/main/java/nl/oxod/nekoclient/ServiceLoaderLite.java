package nl.oxod.nekoclient;

import net.neoforged.fml.ModContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * Service loading scoped to a single mod's classloader.
 *
 * <p>{@link ServiceLoader#load(Class)} only sees providers visible to the caller's classloader.
 * On NeoForge each mod gets its own {@code ModClassLoader}, so addons are only discoverable
 * through the loader of the mod that declared them.
 */
final class ServiceLoaderLite {
    private ServiceLoaderLite() {
    }

    static <T> List<T> loadAll(Class<T> type, ModContainer container) {
        List<T> services = new ArrayList<>();
        ClassLoader classLoader = container.getClass().getClassLoader();
        if (classLoader == null) return services;

        try {
            for (T service : ServiceLoader.load(type, classLoader)) {
                services.add(service);
            }
        } catch (Throwable ignored) {
            // A mod with a broken service file should not stop the rest from loading.
        }
        return services;
    }
}
