package nl.oxod.nekoclient.gui;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.GuiThemes;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import nl.oxod.nekoclient.mixin.AbstractContainerScreenAccessor;
import nl.oxod.nekoclient.systems.modules.misc.GuiDupe;
import nl.oxod.nekoclient.util.GuiDupeState;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class GuiDupePanel extends AbstractWidget {
  // ---- Panel geometry ----
  private static final int PANEL_W = 176;
  private static final int PAD = 5;
  private static final int HEADER_H = 16;
  private static final int SECTION_H = 13;
  private static final int BUTTON_H = 15;
  private static final int ROW_GAP = 2;
  private static final int SECTION_GAP = 5;
  private static final int CHAT_H = 16;
  private static final int METRIC_H = 11;

  private static int panelX = 6;
  private static int panelY = 6;
  private static boolean dragging;
  private static int dragOffX;
  private static int dragOffY;

  // ---- Theme colors (from the Meteor GUI theme) ----
  private static final int FALLBACK_WINDOW_FILL = 0xD8141414;
  private static final int FALLBACK_HEADER_FILL = 0xF02E2E2E;
  private static final int FALLBACK_ACCENT = 0xFF913DE2;
  private static final int FALLBACK_ACCENT_SOFT = 0x46913DE2;
  private static final int FALLBACK_BORDER = 0xFF000000;
  private static final int FALLBACK_TEXT = 0xFFFFFFFF;
  private static final int FALLBACK_MUTED = 0xFF969696;
  private static final int FALLBACK_SUCCESS = 0xFF32FF32;
  private static final int FALLBACK_SUCCESS_FILL = 0x3C32FF32;
  private static final int FALLBACK_BTN_FILL_INACTIVE = 0x70121316;

  private static final int BTN_FILL_HOVER = 0x20FFFFFF;

  private final AbstractContainerScreen<?> screen;
  private final EditBox chatField;
  private final List<int[]> packetButtons = new ArrayList<>();
  private final List<int[]> screenButtons = new ArrayList<>();

  public GuiDupePanel(AbstractContainerScreen<?> screen, EditBox chatField) {
    super(panelX, panelY, PANEL_W, panelHeight(), Component.literal("GUI dupe"));
    this.screen = screen;
    this.chatField = chatField;
  }

  private static boolean neko$isActive() {
    return Modules.get().get(GuiDupe.class).isActive();
  }

  private static GuiDupe module() {
    return Modules.get().get(GuiDupe.class);
  }

  private static int panelHeight() {
    return PAD * 2 + HEADER_H + SECTION_H + BUTTON_H + ROW_GAP + BUTTON_H
      + SECTION_GAP + SECTION_H + BUTTON_H + ROW_GAP + BUTTON_H + ROW_GAP + BUTTON_H
      + SECTION_GAP + CHAT_H + SECTION_GAP + METRIC_H;
  }

  private static int pairWidth() {
    return (PANEL_W - PAD * 2 - ROW_GAP) / 2;
  }

  private int chatTop() {
    return panelY + PAD + HEADER_H + SECTION_H + BUTTON_H + ROW_GAP + BUTTON_H
      + SECTION_GAP + SECTION_H + BUTTON_H + ROW_GAP + BUTTON_H + ROW_GAP + BUTTON_H
      + SECTION_GAP;
  }

  private static int themeInt(java.util.function.Function<MeteorGuiTheme, Integer> getter, int fallback) {
    GuiTheme theme = GuiThemes.get();
    if (theme instanceof MeteorGuiTheme meteorTheme) {
      try {
        Integer value = getter.apply(meteorTheme);
        if (value != null) return value;
      } catch (Throwable ignored) {
      }
    }
    return fallback;
  }

  private static int accentColor() {
    return themeInt(theme -> theme.accentColor.get().getPacked(), FALLBACK_ACCENT);
  }

  private static int accentSoftColor() {
    return themeInt(theme -> theme.accentColor.get().copy().a(70).getPacked(), FALLBACK_ACCENT_SOFT);
  }

  private static int windowFillColor() {
    return themeInt(theme -> theme.backgroundColor.get().getPacked(), FALLBACK_WINDOW_FILL);
  }

  private static int headerFillColor() {
    return themeInt(theme -> theme.moduleBackground.get().getPacked(), FALLBACK_HEADER_FILL);
  }

  private static int borderColor() {
    return themeInt(theme -> theme.outlineColor.get().getPacked(), FALLBACK_BORDER);
  }

  private static int textColor() {
    return themeInt(theme -> theme.textColor.get().getPacked(), FALLBACK_TEXT);
  }

  private static int mutedColor() {
    return themeInt(theme -> theme.textSecondaryColor.get().getPacked(), FALLBACK_MUTED);
  }

  private static int successColor() {
    return themeInt(theme -> theme.plusColor.get().getPacked(), FALLBACK_SUCCESS);
  }

  private static int successFillColor() {
    return themeInt(theme -> theme.plusColor.get().copy().a(60).getPacked(), FALLBACK_SUCCESS_FILL);
  }

  private static int buttonFillInactiveColor() {
    return themeInt(theme -> theme.outlineColor.get().copy().a(80).getPacked(), FALLBACK_BTN_FILL_INACTIVE);
  }

  @Override
  protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
    if (!neko$isActive()) {
      if (chatField != null) {
        chatField.visible = false;
        chatField.setX(-1000);
        chatField.setY(-1000);
      }
      return;
    }
    if (chatField != null) chatField.visible = true;

    int px = panelX;
    int py = panelY;
    int pw = PANEL_W;
    int ph = panelHeight();

    neko$rect(g, px, py, px + pw, py + ph, windowFillColor());
    neko$rect(g, px + 1, py + 1, px + pw - 1, py + HEADER_H - 1, headerFillColor());
    neko$outline(g, px, py, px + pw, py + ph, borderColor());
    neko$line(g, px, py + HEADER_H - 1, px + pw, accentColor());

    int titleY = py + (HEADER_H - 8) / 2 - 1;
    graphics_text(g, "GUI DUPE", px + PAD, titleY, textColor());
    int titleW = neko$font().width("GUI DUPE");
    graphics_text(g, " Rev:" + getMenu().getStateId(), px + PAD + titleW + 6, titleY, mutedColor());

    int x = px + PAD;
    int y = py + PAD + HEADER_H;

    graphics_text(g, "PACKET", x, y + 2, mutedColor());
    y += SECTION_H;

    int pw2 = pairWidth();
    GuiDupe module = module();
    boolean send = module.getSendPackets();
    boolean delay = module.getDelayPackets();
    neko$renderToggle(g, x, y, pw2, BUTTON_H, "Send", send, send && !delay, mouseX, mouseY, packetButtons, 0);
    neko$renderToggle(g, x + pw2 + ROW_GAP, y, pw2, BUTTON_H, "Delay", delay, delay, mouseX, mouseY, packetButtons, 1);
    y += BUTTON_H + ROW_GAP;

    neko$renderButton(g, x, y, pw2, BUTTON_H, "Flush", mouseX, mouseY, packetButtons, 2);
    neko$renderButton(g, x + pw2 + ROW_GAP, y, pw2, BUTTON_H, "Clear", mouseX, mouseY, packetButtons, 3);
    y += BUTTON_H;

    y += SECTION_GAP;
    graphics_text(g, "SCREEN", x, y + 2, mutedColor());
    y += SECTION_H;

    neko$renderButton(g, x, y, pw2, BUTTON_H, "Close", mouseX, mouseY, screenButtons, 0);
    neko$renderButton(g, x + pw2 + ROW_GAP, y, pw2, BUTTON_H, "De-sync", mouseX, mouseY, screenButtons, 1);
    y += BUTTON_H + ROW_GAP;

    neko$renderButton(g, x, y, pw2, BUTTON_H, "Save", mouseX, mouseY, screenButtons, 2);
    neko$renderButton(g, x + pw2 + ROW_GAP, y, pw2, BUTTON_H, "Load", mouseX, mouseY, screenButtons, 3);
    y += BUTTON_H + ROW_GAP;

    neko$renderButton(g, x, y, pw2, BUTTON_H, "Copy", mouseX, mouseY, screenButtons, 4);
    neko$renderButton(g, x + pw2 + ROW_GAP, y, pw2, BUTTON_H, "Fabr", mouseX, mouseY, screenButtons, 5);
    y += BUTTON_H;

    y += SECTION_GAP;
    neko$rect(g, panelX + PAD, y, panelX + PANEL_W - PAD, y + CHAT_H, windowFillColor());
    neko$outline(g, panelX + PAD, y, panelX + PANEL_W - PAD, y + CHAT_H, chatField != null && chatField.isFocused() ? accentColor() : mutedColor());
    if (chatField != null) {
      chatField.setX(panelX + PAD + 2);
      chatField.setY(y + 1);
      chatField.setWidth(PANEL_W - PAD * 2 - 4);
    }
    y += CHAT_H;

    y += SECTION_GAP;
    String rev = Integer.toString(getMenu().getStateId());
    String sync = Integer.toString(getMenu().containerId);
    net.minecraft.world.inventory.Slot hovered = ((AbstractContainerScreenAccessor) screen).neko$getHoveredSlot();
    String slot = hovered != null ? Integer.toString(hovered.index) : "--";
    neko$renderMetric(g, x, y, "Rev: ", rev, accentColor());
    neko$renderMetric(g, x + pw2, y, "Sync: ", sync, textColor());
    neko$renderMetric(g, x + pw2 * 2 + ROW_GAP * 2, y, "Slot: ", slot, successColor());

    if (dragging) {
      neko$rect(g, px, py, px + pw, py + HEADER_H - 1, accentSoftColor());
    }
  }

  @Override
  public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
    if (!neko$isActive()) return false;
    int mx = (int) Math.round(event.x());
    int my = (int) Math.round(event.y());

    if (event.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && mx >= panelX && mx < panelX + PANEL_W && my >= panelY && my < panelY + HEADER_H) {
      dragging = true;
      dragOffX = mx - panelX;
      dragOffY = my - panelY;
      return true;
    }

    int ix = 0;
    for (int[] b : packetButtons) {
      if (b == null) {
        ix++;
        continue;
      }
      if (mx >= b[0] && mx < b[2] && my >= b[1] && my < b[3]) {
        handlePacketButton(ix);
        return true;
      }
      ix++;
    }
    int j = 0;
    for (int[] b : screenButtons) {
      if (b == null) {
        j++;
        continue;
      }
      if (mx >= b[0] && mx < b[2] && my >= b[1] && my < b[3]) {
        handleScreenButton(j);
        return true;
      }
      j++;
    }
    return false;
  }

  @Override
  public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
    if (dragging) {
      panelX = Math.max(0, Math.min(screen.width - PANEL_W, (int) Math.round(event.x()) - dragOffX));
      panelY = Math.max(0, Math.min(screen.height - panelHeight(), (int) Math.round(event.y()) - dragOffY));
      return true;
    }
    return false;
  }

  @Override
  public boolean mouseReleased(MouseButtonEvent event) {
    if (dragging) {
      dragging = false;
      return true;
    }
    return false;
  }

  @Override
  public boolean isMouseOver(double x, double y) {
    if (x < panelX || x >= panelX + PANEL_W || y < panelY || y >= panelY + panelHeight()) return false;
    return y < chatTop();
  }

  private AbstractContainerMenu getMenu() {
    return screen.getMenu();
  }

  private Font neko$font() {
    return mc.font;
  }

  @Override
  protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
  }

  private void handlePacketButton(int index) {
    GuiDupe module = module();
    switch (index) {
      case 0 -> {
        boolean next = !module.getSendPackets();
        module.setSendPackets(next);
        if (!next) module.setDelayPackets(false);
      }
      case 1 -> module.setDelayPackets(!module.getDelayPackets());
      case 2 -> module.flushPublic();
      case 3 -> {
        GuiDupeState.clearDelayed();
        module.info("Cleared queued packets.");
      }
    }
  }

  private void handleScreenButton(int index) {
    GuiDupe module = module();
    switch (index) {
      case 0 -> module.closeWithoutPacketPublic();
      case 1 -> module.desyncPublic();
      case 2 -> module.saveGuiPublic();
      case 3 -> module.restoreGuiPublic();
      case 4 -> module.copyWindowDataPublic();
      case 5 -> module.fabricatePublic();
    }
  }

  private void neko$rect(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int color) {
    if (x2 <= x1 || y2 <= y1) return;
    g.fill(x1, y1, x2, y2, color);
  }

  private void neko$line(GuiGraphicsExtractor g, int x1, int y, int x2, int color) {
    g.horizontalLine(x1, x2, y, color);
  }

  private void neko$outline(GuiGraphicsExtractor g, int x, int y, int right, int bottom, int color) {
    g.outline(x, y, right - x, bottom - y, color);
  }

  private void neko$renderButton(GuiGraphicsExtractor g, int x, int y, int w, int h, String label,
                                 int mouseX, int mouseY, List<int[]> out, int slot) {
    boolean hovered = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    neko$rect(g, x, y, x + w, y + h, windowFillColor());
    neko$outline(g, x, y, x + w, y + h, borderColor());
    if (hovered) neko$rect(g, x + 1, y + 1, x + w - 1, y + h - 1, BTN_FILL_HOVER);
    graphics_text(g, label, x + (w - neko$font().width(label)) / 2, y + (h - 8) / 2 - 1, textColor());
    setBounds(out, slot, x, y, x + w, y + h);
  }

  private void neko$renderToggle(GuiGraphicsExtractor g, int x, int y, int w, int h, String label,
                                 boolean on, boolean colored, int mouseX, int mouseY, List<int[]> out, int slot) {
    boolean hovered = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    int fill = on ? (colored ? successFillColor() : windowFillColor()) : buttonFillInactiveColor();
    int border = on ? (colored ? successColor() : textColor()) : mutedColor();
    int textColor = on ? (colored ? successColor() : textColor()) : mutedColor();
    neko$rect(g, x, y, x + w, y + h, fill);
    neko$outline(g, x, y, x + w, y + h, border);
    if (hovered) neko$rect(g, x + 1, y + 1, x + w - 1, y + h - 1, BTN_FILL_HOVER);
    graphics_text(g, label, x + (w - neko$font().width(label)) / 2, y + (h - 8) / 2 - 1, textColor);
    setBounds(out, slot, x, y, x + w, y + h);
  }

  private void setBounds(List<int[]> out, int slot, int x, int y, int right, int bottom) {
    while (out.size() <= slot) out.add(null);
    out.set(slot, new int[]{x, y, right, bottom});
  }

  private void neko$renderMetric(GuiGraphicsExtractor g, int x, int y, String key, String value, int valueColor) {
    int keyW = neko$font().width(key);
    graphics_text(g, key, x, y, mutedColor());
    graphics_text(g, value, x + keyW, y, valueColor);
  }

  private void graphics_text(GuiGraphicsExtractor g, String text, int x, int y, int color) {
    g.text(neko$font(), text, x, y, color, false);
  }
}
