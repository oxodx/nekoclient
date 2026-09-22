package nl.oxod.nekoclient.util;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import meteordevelopment.meteorclient.renderer.Renderer3D;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.core.BlockPos;

public final class ScaffoldPlaceRenderer {
  private static final int CAP = 32;
  private static final long DURATION_NS = 320_000_000L;
  private static final double FADE_START = 0.62;
  private static final double INFLATE = 0.0035;
  private static final float BASE_ALPHA = 0.55f;

  private static final int[] PX = new int[CAP];
  private static final int[] PY = new int[CAP];
  private static final int[] PZ = new int[CAP];
  private static final long[] AT = new long[CAP];
  private static int idx;

  private static volatile boolean on;
  private static volatile boolean customColor;
  private static volatile int color = 0xFFFF3B3B;

  private ScaffoldPlaceRenderer() {
  }

  public static void push(boolean enabled, boolean custom, int customArgb) {
    on = enabled;
    customColor = custom;
    color = customArgb;
  }

  public static void disable() {
    on = false;
  }

  public static void recordPlacement(BlockPos pos) {
    if (!on || pos == null) return;
    PX[idx] = pos.getX();
    PY[idx] = pos.getY();
    PZ[idx] = pos.getZ();
    AT[idx] = System.nanoTime();
    idx = (idx + 1) % CAP;
  }

  public static boolean isActive() {
    return on;
  }

  public static void render(Render3DEvent event) {
    if (!isActive()) return;
    long now = System.nanoTime();
    int argb = baseColor();
    for (int i = 0; i < CAP; i++) {
      long t = AT[i];
      if (t == 0L) continue;
      long age = now - t;
      if (age < 0L || age >= DURATION_NS) continue;
      ripple(event, PX[i], PY[i], PZ[i], age / (double) DURATION_NS, argb);
    }
  }

  private static int baseColor() {
    if (customColor) return color | 0xFF000000;
    Color accent;
    if (GuiThemes.get() instanceof MeteorGuiTheme theme) accent = theme.accentColor.get();
    else accent = new Color(145, 61, 226);
    return (0xFF << 24) | ((accent.r & 0xFF) << 16) | ((accent.g & 0xFF) << 8) | (accent.b & 0xFF);
  }

  private static void ripple(Render3DEvent event, int bx, int by, int bz, double t, int argb) {
    double inv = 1.0 - t;
    double scale = 1.0 - inv * inv * inv;
    double fade = t < FADE_START ? 1.0 : 1.0 - (t - FADE_START) / (1.0 - FADE_START);
    int alpha = (int) (BASE_ALPHA * fade * 255.0);
    if (alpha <= 0) return;
    Color c = new Color(argb & 0xFFFFFF).a(alpha);
    double half = 0.5 * scale + INFLATE;
    event.renderer.boxSides(
      bx + 0.5 - half, by + 0.5 - half, bz + 0.5 - half,
      bx + 0.5 + half, by + 0.5 + half, bz + 0.5 + half,
      c, 0
    );
  }
}
