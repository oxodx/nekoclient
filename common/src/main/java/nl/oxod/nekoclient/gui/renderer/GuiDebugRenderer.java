/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.gui.renderer;

import nl.oxod.nekoclient.NekoClient;
import nl.oxod.nekoclient.gui.utils.Cell;
import nl.oxod.nekoclient.gui.widgets.WWidget;
import nl.oxod.nekoclient.gui.widgets.containers.WContainer;
import nl.oxod.nekoclient.renderer.MeshBuilder;
import nl.oxod.nekoclient.renderer.MeshRenderer;
import nl.oxod.nekoclient.renderer.NekoRenderPipelines;
import nl.oxod.nekoclient.utils.render.color.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;

public class GuiDebugRenderer {
    private static final Color CELL_COLOR = new Color(25, 225, 25);
    private static final Color WIDGET_COLOR = new Color(25, 25, 225);

    private final MeshBuilder mesh = new MeshBuilder(NekoRenderPipelines.WORLD_COLORED_LINES);

    public void render(WWidget widget) {
        if (widget == null) return;

        mesh.begin();
        renderWidget(widget);
        mesh.end();

        MeshRenderer.begin()
            .attachments(Minecraft.getInstance().gameRenderer.mainRenderTarget())
            .pipeline(NekoRenderPipelines.WORLD_COLORED_LINES)
            .mesh(mesh)
            .end();
    }

    public void mouseReleased(WWidget widget, MouseButtonEvent click, int i) {
        if (widget == null) return;

        NekoClient.LOG.info("{} {}", widget.getClass(), i);

        if (widget instanceof WContainer container) {
            for (Cell<?> cell : container.cells) {
                if (cell.widget().isOver(click.x(), click.y())) {
                    mouseReleased(cell.widget(), click, i + 1);
                }
            }
        }
    }

    private void renderWidget(WWidget widget) {
        lineBox(widget.x, widget.y, widget.width, widget.height, WIDGET_COLOR);

        if (widget instanceof WContainer container) {
            for (Cell<?> cell : container.cells) {
                lineBox(cell.x, cell.y, cell.width, cell.height, CELL_COLOR);
                renderWidget(cell.widget());
            }
        }
    }

    private void lineBox(double x, double y, double width, double height, Color color) {
        line(x, y, x + width, y, color);
        line(x + width, y, x + width, y + height, color);
        line(x, y, x, y + height, color);
        line(x, y + height, x + width, y + height, color);
    }

    private void line(double x1, double y1, double x2, double y2, Color color) {
        mesh.ensureLineCapacity();

        mesh.line(
            mesh.vec3(x1, y1, 0).color(color).next(),
            mesh.vec3(x2, y2, 0).color(color).next()
        );
    }
}
