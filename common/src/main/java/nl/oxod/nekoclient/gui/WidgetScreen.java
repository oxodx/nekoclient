/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.gui;

import static com.mojang.blaze3d.platform.InputConstants.*;
import static nl.oxod.nekoclient.NekoClient.mc;
import static nl.oxod.nekoclient.utils.Utils.getWindowHeight;
import static nl.oxod.nekoclient.utils.Utils.getWindowWidth;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import nl.oxod.nekoclient.NekoClient;
import nl.oxod.nekoclient.gui.renderer.GuiDebugRenderer;
import nl.oxod.nekoclient.gui.renderer.GuiRenderer;
import nl.oxod.nekoclient.gui.engine.UiScene;
import nl.oxod.nekoclient.gui.tabs.TabScreen;
import nl.oxod.nekoclient.gui.utils.Cell;
import nl.oxod.nekoclient.gui.widgets.WWidget;
import nl.oxod.nekoclient.gui.widgets.containers.WContainer;
import nl.oxod.nekoclient.gui.widgets.input.WTextBox;
import nl.oxod.nekoclient.utils.Utils;
import nl.oxod.nekoclient.utils.misc.input.Input;
import org.apache.commons.lang3.SystemUtils;
import org.jspecify.annotations.NonNull;
import org.lwjgl.sdl.SDLMouse;

public abstract class WidgetScreen extends Screen {

  private static final GuiRenderer RENDERER = new GuiRenderer();
  private static final GuiDebugRenderer DEBUG_RENDERER = new GuiDebugRenderer();

  public Runnable taskAfterRender;
  protected Runnable enterAction;

  public Screen parent;
  private final UiScene root;

  protected final GuiTheme theme;

  public boolean locked, lockedAllowClose;
  private boolean closed;
  private boolean onClose;
  private boolean debug;

  private boolean closing;


  public double animProgress;

  private List<Runnable> onClosed;

  protected boolean firstInit = true;

  public WidgetScreen(GuiTheme theme, String title) {
    super(Component.literal(title));

    this.parent = mc.gui.screen();
    this.root = new UiScene(theme);
    this.theme = theme;

    root.theme = theme;

    if (parent != null) {
      animProgress = 1;

      if (this instanceof TabScreen && parent instanceof TabScreen) {
        parent = ((TabScreen) parent).parent;
      }
    }
  }

  public <W extends WWidget> Cell<W> add(W widget) {
    return root.add(widget);
  }

  public void clear() {
    root.clear();
  }

  public void invalidate() {
    root.invalidate();
  }

  public boolean isDraggingPanel() { return root.input.draggingPanel(); }

  @Override
  protected void init() {
    root.viewport(getWindowWidth(), getWindowHeight());
    NekoClient.EVENT_BUS.subscribe(this);

    closed = false;

    if (firstInit) {
      firstInit = false;
      initWidgets();
    }

    mc.textInputManager().startTextInput(this);
  }

  public abstract void initWidgets();

  public void reload() {
    clear();
    initWidgets();
  }

  public void onClosed(Runnable action) {
    if (onClosed == null) onClosed = new ArrayList<>(2);
    onClosed.add(action);
  }

  @Override
  public boolean mouseClicked(
    @NonNull MouseButtonEvent click,
    boolean doubled
  ) {
    if (locked) return false;

    double mouseX = click.x();
    double mouseY = click.y();
    double s = mc.getWindow().getGuiScale();

    mouseX *= s;
    mouseY *= s;

    return root.input.click(
      new MouseButtonEvent(mouseX, mouseY, click.buttonInfo()),
      doubled
    );
  }

  @Override
  public boolean mouseReleased(@NonNull MouseButtonEvent click) {
    if (locked) return false;

    double mouseX = click.x();
    double mouseY = click.y();
    double s = mc.getWindow().getGuiScale();

    mouseX *= s;
    mouseY *= s;

    if (
      debug && click.button() == MOUSE_BUTTON_RIGHT
    ) DEBUG_RENDERER.mouseReleased(
      root,
      new MouseButtonEvent(mouseX, mouseY, click.buttonInfo()),
      0
    );

    return root.input.release(
      new MouseButtonEvent(mouseX, mouseY, click.buttonInfo())
    );
  }

  @Override
  public void mouseMoved(double mouseX, double mouseY) {
    if (locked) return;

    double s = mc.getWindow().getGuiScale();
    mouseX *= s;
    mouseY *= s;

    root.input.move(mouseX, mouseY);

  }

  @Override
  public boolean mouseScrolled(
    double mouseX,
    double mouseY,
    double horizontalAmount,
    double verticalAmount
  ) {
    if (locked) return false;

    double scale = mc.getWindow().getGuiScale();
    if (root.input.scroll(mouseX * scale, mouseY * scale, verticalAmount)) return true;

    return super.mouseScrolled(
      mouseX,
      mouseY,
      horizontalAmount,
      verticalAmount
    );
  }

  @Override
  public boolean keyReleased(@NonNull KeyEvent event) {
    if (locked) return false;

    boolean ctrl = SystemUtils.IS_OS_MAC
      ? Input.modsMatch(event.modifiers(), MOD_SUPER)
      : Input.modsMatch(event.modifiers(), MOD_CONTROL);

    if (ctrl && event.key() == KEY_9) {
      debug = !debug;
      return true;
    }

    if (
      (event.key() == KEY_RETURN || event.key() == KEY_NUMPADENTER) &&
      enterAction != null
    ) {
      enterAction.run();
      return true;
    }

    return super.keyReleased(event);
  }

  @Override
  public boolean keyPressed(@NonNull KeyEvent input) {
    if (locked) return false;

    boolean shouldReturn = root.input.key(input, false) || super.keyPressed(input);
    if (shouldReturn) return true;

    // Select next text box if TAB was pressed
    if (input.key() == KEY_TAB) {
      return root.input.focusNext(Input.modsMatch(input.modifiers(), MOD_SHIFT));
    }

    boolean ctrl = SystemUtils.IS_OS_MAC
      ? Input.modsMatch(input.modifiers(), MOD_SUPER)
      : Input.modsMatch(input.modifiers(), MOD_CONTROL);

    return (
      (ctrl && input.key() == KEY_C && toClipboard()) ||
      (ctrl && input.key() == KEY_V && fromClipboard())
    );
  }

  public void keyRepeated(KeyEvent input) {
    if (locked) return;

    root.input.key(input, true);
  }

  @Override
  public boolean charTyped(@NonNull CharacterEvent input) {
    if (locked) return false;

    return root.input.character(input);
  }

  @Override
  public void extractBackground(
    @NonNull GuiGraphicsExtractor graphics,
    int mouseX,
    int mouseY,
    float deltaTicks
  ) {
    if (this.minecraft.level == null) {
      this.extractPanorama(graphics, deltaTicks);
    }
  }

  public void renderCustom(
    GuiGraphicsExtractor graphics,
    int mouseX,
    int mouseY,
    float delta
  ) {
    int s = mc.getWindow().getGuiScale();
    mouseX *= s;
    mouseY *= s;

    animProgress += (delta / 20) * 14 * (closing ? -1 : 1);
    animProgress = Mth.clamp(animProgress, 0, 1);

    if (closing && (animProgress == 0 || parent != null)) {
      closeInternal();
    }

    GuiKeyEvents.canUseKeys = true;

    // Apply projection without scaling
    Utils.unscaledProjection();

    onRenderBefore(graphics, mouseX, mouseY, delta);

    RENDERER.theme = theme;
    theme.beforeRender();

    RENDERER.begin(graphics);
    RENDERER.setFrameAlpha(animProgress);
    theme.renderScreenBackground(RENDERER, getWindowWidth(), getWindowHeight());
    root.render(RENDERER, mouseX, mouseY, delta / 20);
    RENDERER.setFrameAlpha(1);
    RENDERER.end();

    boolean tooltip = RENDERER.renderTooltip(
      graphics,
      mouseX,
      mouseY,
      delta / 20
    );

    if (debug) {
      DEBUG_RENDERER.render(root);
      if (tooltip) DEBUG_RENDERER.render(RENDERER.tooltipWidget);
    }

    Utils.scaledProjection();

    runAfterRenderTasks();
  }

  protected void runAfterRenderTasks() {
    if (taskAfterRender != null) {
      taskAfterRender.run();
      taskAfterRender = null;
    }
  }

  protected void onRenderBefore(
    GuiGraphicsExtractor graphics,
    int mouseX,
    int mouseY,
    float delta
  ) {}

  @Override
  public void resize(int width, int height) {
    super.resize(width, height);
    root.viewport(getWindowWidth(), getWindowHeight());
    root.invalidate();
  }

  @Override
  public void onClose() {
    if (!locked || lockedAllowClose) {
      closing = true;
    }
  }

  @Override
  public void removed() {
    if (!closed || lockedAllowClose) {
      closed = true;
      root.input.reset();
      onClosed();

      Input.setCursorStyle(CursorTypes.ARROW);

      loopWidgets(root, widget -> {
        if (
          widget instanceof WTextBox textBox && textBox.isFocused()
        ) textBox.setFocused(false);
      });

      NekoClient.EVENT_BUS.unsubscribe(this);
      GuiKeyEvents.canUseKeys = true;

      if (onClosed != null) {
        for (Runnable action : onClosed) action.run();
      }

      if (onClose) {
        double restoreX = mc.mouseHandler.xpos();
        double restoreY = mc.mouseHandler.ypos();

        taskAfterRender = () -> {
          locked = true;
          mc.gui.setScreen(parent);

          // Restore mouse position to where it was when the screen was closed
          if (parent != null) {
            SDLMouse.SDL_WarpMouseInWindow(
              mc.getWindow().handle(),
              (float) restoreX,
              (float) restoreY
            );
          }

          mc.textInputManager().stopTextInput(this);
        };
      }
    }
  }

  private void closeInternal() {
    boolean preOnClose = onClose;
    onClose = true;

    super.onClose();
    removed();

    onClose = preOnClose;
  }

  private void loopWidgets(WWidget widget, Consumer<WWidget> action) {
    action.accept(widget);

    if (widget instanceof WContainer wContainer) {
      for (Cell<?> cell : wContainer.cells) loopWidgets(cell.widget(), action);
    }
  }

  protected void onClosed() {}

  public boolean toClipboard() {
    return false;
  }

  public boolean fromClipboard() {
    return false;
  }

  @Override
  public boolean shouldCloseOnEsc() {
    return !locked || lockedAllowClose;
  }

  @Override
  public boolean isPauseScreen() {
    return false;
  }

}
