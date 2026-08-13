package nl.oxod.nekoclient.security;

public final class ProtectorServerPackFailureGuard {
  private static volatile long suppressServerPacksUntilMs;

  private ProtectorServerPackFailureGuard() {
  }

  public static void suppressServerPacksTemporarily() {
    suppressServerPacksUntilMs = Math.max(suppressServerPacksUntilMs, System.currentTimeMillis() + 15_000L);
  }

  public static boolean shouldSuppressServerPacks() {
    return System.currentTimeMillis() < suppressServerPacksUntilMs;
  }

  public static void clear() {
    suppressServerPacksUntilMs = 0L;
  }
}
