package nl.oxod.nekoclient.util;

import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.Renderer3D;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class KillAuraRenderer {
  private static final long HIT_MARKER_DURATION_MS = 500L;
  private static final int ACCENT_RGB = 0xFF3B3B;
  private static final int HIT_MARKER_FILL = 0x59FF3B3B;

  private static volatile AABB frozenBox;
  private static volatile long hitAtMs;

  private KillAuraRenderer() {
  }

  public static void render(Render3DEvent event, LivingEntity target) {
    Minecraft mc = Minecraft.getInstance();
    if (mc == null || mc.level == null || mc.player == null) return;

    long now = System.currentTimeMillis();
    float wave = 0.5F + 0.5F * (float) Math.sin(now * 0.001D * Math.PI * 2.0D * 1.2D);

    if (target != null && target.isAlive()) {
      Vec3 interpolated = target.getPosition(event.tickDelta);
      AABB box = target.getBoundingBox().inflate(0.06D)
        .move(interpolated.subtract(target.position()));

      int fillAlpha = (int) (0x10 + 0x10 * wave);
      Color fill = new Color(ACCENT_RGB).a(fillAlpha);
      event.renderer.box(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, fill, null, ShapeMode.Sides, 0);

      int lineAlpha = (int) (0xD0 + 0x2F * wave);
      Color line = new Color(ACCENT_RGB).a(lineAlpha);
      drawCornerBrackets(event.renderer, box, line);
    }

    AABB marker = frozenBox;
    if (marker != null && now - hitAtMs < HIT_MARKER_DURATION_MS) {
      event.renderer.box(marker.minX, marker.minY, marker.minZ, marker.maxX, marker.maxY, marker.maxZ,
        new Color(HIT_MARKER_FILL), null, ShapeMode.Sides, 0);
    }
  }

  public static void show(AABB box) {
    if (box == null) return;
    frozenBox = box;
    hitAtMs = System.currentTimeMillis();
  }

  public static void clear() {
    frozenBox = null;
  }

  private static void drawCornerBrackets(Renderer3D renderer, AABB box, Color color) {
    double bracket = Math.min(0.35D, Math.min(box.getXsize(), Math.min(box.getYsize(), box.getZsize())) * 0.3D);
    if (bracket <= 0.0D) return;
    for (int corner = 0; corner < 8; corner++) {
      double x = (corner & 1) == 0 ? box.minX : box.maxX;
      double y = (corner & 2) == 0 ? box.minY : box.maxY;
      double z = (corner & 4) == 0 ? box.minZ : box.maxZ;
      double dx = (corner & 1) == 0 ? bracket : -bracket;
      double dy = (corner & 2) == 0 ? bracket : -bracket;
      double dz = (corner & 4) == 0 ? bracket : -bracket;
      renderer.line(x, y, z, x + dx, y, z, color);
      renderer.line(x, y, z, x, y + dy, z, color);
      renderer.line(x, y, z, x, y, z + dz, color);
    }
  }
}
