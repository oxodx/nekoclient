package nl.oxod.nekoclient.platform;

/**
 * Holder for the active {@link NekoPlatform}.
 *
 * <p>The platform module installs its implementation during mod construction, before any shared
 * client code runs. Everything else resolves the service through {@link #get()}.
 */
public final class Platform {
    private static volatile NekoPlatform instance;

    private Platform() {
    }

    public static void install(NekoPlatform platform) {
        if (platform == null) throw new NullPointerException("platform");
        if (instance != null) throw new IllegalStateException("Platform already installed: " + instance.loader());
        instance = platform;
    }

    public static boolean isInstalled() {
        return instance != null;
    }

    public static NekoPlatform get() {
        NekoPlatform platform = instance;
        if (platform == null) {
            throw new IllegalStateException("No NekoPlatform installed. The active loader module must call "
                + "Platform.install(..) before touching MeteorClient.");
        }
        return platform;
    }
}
