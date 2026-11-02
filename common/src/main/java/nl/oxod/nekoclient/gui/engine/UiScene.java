package nl.oxod.nekoclient.gui.engine;

import nl.oxod.nekoclient.gui.GuiTheme;
import nl.oxod.nekoclient.gui.renderer.GuiRenderer;
import nl.oxod.nekoclient.gui.widgets.WRoot;
import nl.oxod.nekoclient.gui.widgets.containers.WContainer;

/** Per-screen scene: owns layout lifetime and input state, never shared between screens. */
public final class UiScene extends WContainer implements WRoot {
  public final UiInput input = new UiInput(this);
  private boolean dirty = true;
  private double viewportWidth, viewportHeight;

  public UiScene(GuiTheme theme) {
    this.theme = theme;
  }

  @Override
  public void invalidate() {
    dirty = true;
  }

  public void viewport(double width, double height) {
    if (width != viewportWidth || height != viewportHeight) {
      viewportWidth = width;
      viewportHeight = height;
      invalidate();
    }
  }

  public void layout() {
    // Mutations during measurement may invalidate again; don't lose that signal.
    for (int pass = 0; dirty && pass < 3; pass++) {
      dirty = false;
      cells.sort(java.util.Comparator.comparingInt(cell -> cell.widget().layer()));
      UiLayout.measure(this, viewportWidth);
      UiLayout.arrange(this);
    }
  }

  @Override
  protected void onCalculateSize() {
    width = viewportWidth;
    height = viewportHeight;
  }

  @Override
  protected void onCalculateWidgetPositions() {
    for (var cell : cells) {
      cell.x = cell.padLeft();
      cell.y = cell.padTop();
      cell.width = Math.max(0, width - cell.padLeft() - cell.padRight());
      cell.height = Math.max(0, height - cell.padTop() - cell.padBottom());
      cell.alignWidget();
    }
  }

  @Override
  public boolean render(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
    layout();
    input.move(mouseX, mouseY);
    return super.render(renderer, mouseX, mouseY, delta);
  }
}
