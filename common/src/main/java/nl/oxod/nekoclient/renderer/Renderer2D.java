/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.renderer;

import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import nl.oxod.nekoclient.gui.renderer.packer.TextureRegion;
import nl.oxod.nekoclient.utils.PreInit;
import nl.oxod.nekoclient.utils.render.color.Color;
import net.minecraft.client.Minecraft;

public class Renderer2D {
  private static final int ROUNDED_CORNER_SEGMENTS = 6;
  public static Renderer2D COLOR;
  public static Renderer2D TEXTURE;

  private final boolean textured;

  public final MeshBuilder triangles;
  public final MeshBuilder lines;

  public Renderer2D(boolean textured) {
    this.textured = textured;

    triangles = new MeshBuilder(textured ? NekoRenderPipelines.UI_TEXTURED : NekoRenderPipelines.UI_COLORED);
    lines = new MeshBuilder(NekoRenderPipelines.UI_COLORED_LINES);
  }

  @PreInit
  public static void init() {
    COLOR = new Renderer2D(false);
    TEXTURE = new Renderer2D(true);
  }

  public void setAlpha(double alpha) {
    triangles.alpha = alpha;
  }

  public void begin() {
    triangles.begin();
    lines.begin();
  }

  public void end() {
    triangles.end();
    lines.end();
  }

  public void render() {
    render(null, null, null);
  }

  public void render(GpuTextureView textureView, GpuSampler sampler) {
    if (!textured)
      throw new IllegalStateException("Tried to render with a texture with a non-textured Renderer2D");

    render("u_Texture", textureView, sampler);
  }

  public void render(String samplerName, GpuTextureView samplerView, GpuSampler sampler) {
    if (lines.isBuilding()) lines.end();
    if (triangles.isBuilding()) triangles.end();

    MeshRenderer.begin()
      .attachments(Minecraft.getInstance().gameRenderer.mainRenderTarget())
      .pipeline(NekoRenderPipelines.UI_COLORED_LINES)
      .mesh(lines)
      .end();

    MeshRenderer.begin()
      .attachments(Minecraft.getInstance().gameRenderer.mainRenderTarget())
      .pipeline(textured ? NekoRenderPipelines.UI_TEXTURED : NekoRenderPipelines.UI_COLORED)
      .mesh(triangles)
      .sampler(samplerName, samplerView, sampler)
      .end();
  }

  // Tris
  public void triangle(double x1, double y1, double x2, double y2, double x3, double y3, Color color) {
    triangles.ensureTriCapacity();

    triangles.triangle(
      triangles.vec2(x1, y1).color(color).next(),
      triangles.vec2(x2, y2).color(color).next(),
      triangles.vec2(x3, y3).color(color).next()
    );
  }

  // Lines

  public void line(double x1, double y1, double x2, double y2, Color color) {
    lines.ensureLineCapacity();

    lines.line(
      lines.vec2(x1, y1).color(color).next(),
      lines.vec2(x2, y2).color(color).next()
    );
  }

  public void boxLines(double x, double y, double width, double height, Color color) {
    lines.ensureCapacity(4, 8);

    int i1 = lines.vec2(x, y).color(color).next();
    int i2 = lines.vec2(x, y + height).color(color).next();
    int i3 = lines.vec2(x + width, y + height).color(color).next();
    int i4 = lines.vec2(x + width, y).color(color).next();

    lines.line(i1, i2);
    lines.line(i2, i3);
    lines.line(i3, i4);
    lines.line(i4, i1);
  }

  // Quads

  public void quad(double x, double y, double width, double height, Color cTopLeft, Color cTopRight, Color cBottomRight, Color cBottomLeft) {
    triangles.ensureQuadCapacity();

    triangles.quad(
      triangles.vec2(x, y).color(cTopLeft).next(),
      triangles.vec2(x, y + height).color(cBottomLeft).next(),
      triangles.vec2(x + width, y + height).color(cBottomRight).next(),
      triangles.vec2(x + width, y).color(cTopRight).next()
    );
  }

  public void quad(double x, double y, double width, double height, Color color) {
    quad(x, y, width, height, color, color, color, color);
  }

  /**
   * Draws a rounded rectangle as one non-overlapping triangle fan. Keeping it in the colored mesh
   * avoids stretched corner textures and the dark seams caused by stacking translucent circles on
   * top of a rectangle.
   */
  public void roundedQuad(double x, double y, double width, double height, double radius, Color color) {
    if (width <= 0 || height <= 0) return;

    radius = Math.max(0, Math.min(radius, Math.min(width, height) / 2));
    if (radius == 0) {
      quad(x, y, width, height, color);
      return;
    }

    int perimeterVertices = 4 * (ROUNDED_CORNER_SEGMENTS + 1);
    triangles.ensureCapacity(perimeterVertices + 1, perimeterVertices * 3);

    int center = triangles.vec2(x + width / 2, y + height / 2).color(color).next();
    int first = -1;
    int previous = -1;

    double[][] corners = {
      {x + width - radius, y + radius, -90},
      {x + width - radius, y + height - radius, 0},
      {x + radius, y + height - radius, 90},
      {x + radius, y + radius, 180}
    };

    for (double[] corner : corners) {
      for (int i = 0; i <= ROUNDED_CORNER_SEGMENTS; i++) {
        double angle = Math.toRadians(corner[2] + 90.0 * i / ROUNDED_CORNER_SEGMENTS);
        int vertex = triangles.vec2(
          corner[0] + Math.cos(angle) * radius,
          corner[1] + Math.sin(angle) * radius
        ).color(color).next();

        if (first == -1) first = vertex;
        if (previous != -1) triangles.triangle(center, previous, vertex);
        previous = vertex;
      }
    }

    triangles.triangle(center, previous, first);
  }

  // Textured quads

  public void texQuad(double x, double y, double width, double height, Color color) {
    triangles.ensureQuadCapacity();

    triangles.quad(
      triangles.vec2(x, y).vec2(0, 0).color(color).next(),
      triangles.vec2(x, y + height).vec2(0, 1).color(color).next(),
      triangles.vec2(x + width, y + height).vec2(1, 1).color(color).next(),
      triangles.vec2(x + width, y).vec2(1, 0).color(color).next()
    );
  }

  public void texQuad(double x, double y, double width, double height, TextureRegion texture, Color color) {
    triangles.ensureQuadCapacity();

    triangles.quad(
      triangles.vec2(x, y).vec2(texture.x1, texture.y1).color(color).next(),
      triangles.vec2(x, y + height).vec2(texture.x1, texture.y2).color(color).next(),
      triangles.vec2(x + width, y + height).vec2(texture.x2, texture.y2).color(color).next(),
      triangles.vec2(x + width, y).vec2(texture.x2, texture.y1).color(color).next()
    );
  }

  public void texQuad(double x, double y, double width, double height, double rotation, double texX1, double texY1, double texX2, double texY2, Color color) {
    triangles.ensureQuadCapacity();

    double rad = Math.toRadians(rotation);
    double cos = Math.cos(rad);
    double sin = Math.sin(rad);

    double oX = x + width / 2;
    double oY = y + height / 2;

    double _x1 = ((x - oX) * cos) - ((y - oY) * sin) + oX;
    double _y1 = ((y - oY) * cos) + ((x - oX) * sin) + oY;
    int i1 = triangles.vec2(_x1, _y1).vec2(texX1, texY1).color(color).next();

    double _x2 = ((x - oX) * cos) - ((y + height - oY) * sin) + oX;
    double _y2 = ((y + height - oY) * cos) + ((x - oX) * sin) + oY;
    int i2 = triangles.vec2(_x2, _y2).vec2(texX1, texY2).color(color).next();

    double _x3 = ((x + width - oX) * cos) - ((y + height - oY) * sin) + oX;
    double _y3 = ((y + height - oY) * cos) + ((x + width - oX) * sin) + oY;
    int i3 = triangles.vec2(_x3, _y3).vec2(texX2, texY2).color(color).next();

    double _x4 = ((x + width - oX) * cos) - ((y - oY) * sin) + oX;
    double _y4 = ((y - oY) * cos) + ((x + width - oX) * sin) + oY;
    int i4 = triangles.vec2(_x4, _y4).vec2(texX2, texY1).color(color).next();

    triangles.quad(i1, i2, i3, i4);
  }

  public void texQuad(double x, double y, double width, double height, double rotation, TextureRegion region, Color color) {
    texQuad(x, y, width, height, rotation, region.x1, region.y1, region.x2, region.y2, color);
  }
}
