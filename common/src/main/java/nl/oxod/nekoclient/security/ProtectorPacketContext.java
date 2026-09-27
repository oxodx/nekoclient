package nl.oxod.nekoclient.security;

public final class ProtectorPacketContext {
  private static final ThreadLocal<int[]> PROCESSING_DEPTH = ThreadLocal.withInitial(() -> new int[1]);

  private ProtectorPacketContext() {
  }

  public static boolean isProcessingPacket() {
    return PROCESSING_DEPTH.get()[0] != 0;
  }

  public static void setProcessingPacket(boolean value) {
    int[] depth = PROCESSING_DEPTH.get();
    if (value) {
      depth[0]++;
    } else if (depth[0] > 0) {
      depth[0]--;
    }
  }
}
