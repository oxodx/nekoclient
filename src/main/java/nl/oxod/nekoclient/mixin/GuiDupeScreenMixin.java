package nl.oxod.nekoclient.mixin;

import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import nl.oxod.nekoclient.systems.modules.misc.GuiDupe;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(AbstractContainerScreen.class)
public abstract class GuiDupeScreenMixin<T extends AbstractContainerMenu> extends Screen {
  @Shadow
  protected Slot hoveredSlot;

  @Shadow
  public abstract T getMenu();

  // ---- Panel geometry (autism launcher inspired) ----
  @Unique
  private static final int PANEL_W = 176;
  @Unique
  private static final int PAD = 5;
  @Unique
  private static final int HEADER_H = 16;
  @Unique
  private static final int SECTION_H = 13;
  @Unique
  private static final int BUTTON_H = 15;
  @Unique
  private static final int ROW_GAP = 2;
  @Unique
  private static final int SECTION_GAP = 5;
  @Unique
  private static final int CHAT_H = 16;
  @Unique
  private static final int METRIC_H = 11;

  @Unique
  private static int panelX = 6;
  @Unique
  private static int panelY = 6;
  @Unique
  private static boolean dragging;
  @Unique
  private static int dragOffX;
  @Unique
  private static int dragOffY;

  // ---- Autism theme colors ----
  @Unique
  private static final int WINDOW_FILL = 0xD80B0C10;
  @Unique
  private static final int HEADER_FILL = 0xF0181A20;
  @Unique
  private static final int ACCENT = 0xFFFF3B3B;
  @Unique
  private static final int ACCENT_SOFT = 0x44FF3B3B;
  @Unique
  private static final int BORDER_SOFT = 0xCC9F3A3A;
  @Unique
  private static final int TEXT = 0xFFF3ECE7;
  @Unique
  private static final int MUTED = 0xFFB79E9E;
  @Unique
  private static final int SUCCESS = 0xFF3FE87E;
  @Unique
  private static final int BAD = 0xFFFF5555;

  @Unique
  private static final int BTN_FILL = 0xA8120E11;
  @Unique
  private static final int BTN_FILL_INACTIVE = 0x70121316;
  @Unique
  private static final int BTN_FILL_HOVER = 0x20FFFFFF;
  @Unique
  private static final int BTN_TEXT_INACTIVE = 0xFF6B5050;
  @Unique
  private static final int SUCCESS_FILL = 0xD41F5233;

  @Unique
  private EditBox guiDupeChatField;

  @Unique
  private final java.util.List<int[]> neko$packetButtons = new java.util.ArrayList<>();
  @Unique
  private final java.util.List<int[]> neko$screenButtons = new java.util.ArrayList<>();

  protected GuiDupeScreenMixin(Component title) {
    super(title);
  }

  private static boolean isActive() {
    GuiDupe module = Modules.get().get(GuiDupe.class);
    return module.isActive();
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

  private static int chatFieldY() {
    int y = panelY + PAD + HEADER_H + SECTION_H + BUTTON_H + ROW_GAP + BUTTON_H;
    y += SECTION_GAP + SECTION_H + BUTTON_H + ROW_GAP + BUTTON_H + ROW_GAP + BUTTON_H;
    return y + SECTION_GAP;
  }

  @Inject(method = "init", at = @At("TAIL"))
  private void neko$guiDupeInit(CallbackInfo ci) {
    if (guiDupeChatField == null) {
      guiDupeChatField = new EditBox(this.font, panelX + PAD, chatFieldY(), PANEL_W - PAD * 2, CHAT_H, Component.literal("GUI dupe chat"));
      guiDupeChatField.setMaxLength(256);
      guiDupeChatField.setBordered(false);
      guiDupeChatField.setTextColor(TEXT);
      guiDupeChatField.setHint(Component.literal("Type message or /command...").copy().withColor(MUTED));
      addRenderableWidget(guiDupeChatField);
    }
    guiDupeChatField.setX(panelX + PAD);
    guiDupeChatField.setY(chatFieldY());
    guiDupeChatField.setWidth(PANEL_W - PAD * 2);
  }

  @Inject(method = "extractRenderState", at = @At("TAIL"))
  private void neko$guiDupeRender(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
    if (!isActive()) return;

    int px = panelX;
    int py = panelY;
    int pw = PANEL_W;
    int ph = panelHeight();

    // Panel frame
    neko$rect(graphics, px, py, px + pw, py + ph, WINDOW_FILL);
    neko$rect(graphics, px + 1, py + 1, px + pw - 1, py + HEADER_H - 1, HEADER_FILL);
    neko$outline(graphics, px, py, px + pw, py + ph, BORDER_SOFT);
    neko$line(graphics, px, py + HEADER_H - 1, px + pw, ACCENT);

    // Title
    int titleY = py + (HEADER_H - 8) / 2 - 1;
    graphics.text(font, "GUI DUPE", px + PAD, titleY, TEXT, false);
    int titleW = font.width("GUI DUPE");
    graphics.text(font, " Rev:" + getMenu().getStateId(), px + PAD + titleW + 6, titleY, MUTED, false);

    int x = px + PAD;
    int y = py + PAD + HEADER_H;

    // PACKET section
    graphics.text(font, "PACKET", x, y + 2, MUTED, false);
    y += SECTION_H;

    // Send / Delay connected toggles
    int pw2 = pairWidth();
    GuiDupe module = module();
    boolean send = module.getSendPackets();
    boolean delay = module.getDelayPackets();
    neko$renderToggle(graphics, x, y, pw2, BUTTON_H, "Send", send, send && !delay, mouseX, mouseY, neko$packetButtons, 0);
    neko$renderToggle(graphics, x + pw2 + ROW_GAP, y, pw2, BUTTON_H, "Delay", delay, delay, mouseX, mouseY, neko$packetButtons, 1);
    y += BUTTON_H + ROW_GAP;

    // Flush / Clear
    neko$renderButton(graphics, x, y, pw2, BUTTON_H, "Flush", mouseX, mouseY, neko$packetButtons, 2);
    neko$renderButton(graphics, x + pw2 + ROW_GAP, y, pw2, BUTTON_H, "Clear", mouseX, mouseY, neko$packetButtons, 3);
    y += BUTTON_H;

    // SCREEN section
    y += SECTION_GAP;
    graphics.text(font, "SCREEN", x, y + 2, MUTED, false);
    y += SECTION_H;

    neko$renderButton(graphics, x, y, pw2, BUTTON_H, "Close", mouseX, mouseY, neko$screenButtons, 0);
    neko$renderButton(graphics, x + pw2 + ROW_GAP, y, pw2, BUTTON_H, "De-sync", mouseX, mouseY, neko$screenButtons, 1);
    y += BUTTON_H + ROW_GAP;

    neko$renderButton(graphics, x, y, pw2, BUTTON_H, "Save", mouseX, mouseY, neko$screenButtons, 2);
    neko$renderButton(graphics, x + pw2 + ROW_GAP, y, pw2, BUTTON_H, "Load", mouseX, mouseY, neko$screenButtons, 3);
    y += BUTTON_H + ROW_GAP;

    neko$renderButton(graphics, x, y, pw2, BUTTON_H, "Copy", mouseX, mouseY, neko$screenButtons, 4);
    neko$renderButton(graphics, x + pw2 + ROW_GAP, y, pw2, BUTTON_H, "Fabr", mouseX, mouseY, neko$screenButtons, 5);
    y += BUTTON_H;

    // Chat field frame
    y += SECTION_GAP;
    neko$rect(graphics, panelX + PAD, y, panelX + PANEL_W - PAD, y + CHAT_H, 0x80121316);
    neko$outline(graphics, panelX + PAD, y, panelX + PANEL_W - PAD, y + CHAT_H, guiDupeChatField != null && guiDupeChatField.isFocused() ? ACCENT : BTN_TEXT_INACTIVE);
    if (guiDupeChatField != null) {
      guiDupeChatField.setX(panelX + PAD + 2);
      guiDupeChatField.setY(y + 1);
      guiDupeChatField.setWidth(PANEL_W - PAD * 2 - 4);
    }
    y += CHAT_H;

    // Metrics row
    y += SECTION_GAP;
    String rev = Integer.toString(getMenu().getStateId());
    String sync = Integer.toString(getMenu().containerId);
    String slot = hoveredSlot != null ? Integer.toString(hoveredSlot.index) : "--";
    neko$renderMetric(graphics, x, y, "Rev: ", rev, ACCENT);
    neko$renderMetric(graphics, x + pw2, y, "Sync: ", sync, TEXT);
    neko$renderMetric(graphics, x + pw2 * 2 + ROW_GAP * 2, y, "Slot: ", slot, SUCCESS);

    // Header drag visuals
    if (dragging) {
      neko$rect(graphics, px, py, px + pw, py + HEADER_H - 1, ACCENT_SOFT);
    }
  }

  @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
  private void neko$guiDupeMouseClicked(MouseButtonEvent click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
    if (!isActive()) return;
    int mx = (int) Math.round(click.x());
    int my = (int) Math.round(click.y());

    // Header drag
    if (click.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT && mx >= panelX && mx < panelX + PANEL_W && my >= panelY && my < panelY + HEADER_H) {
      dragging = true;
      dragOffX = mx - panelX;
      dragOffY = my - panelY;
      cir.setReturnValue(true);
      return;
    }

    // PACKET buttons
    int ix = 0;
    for (int[] b : neko$packetButtons) {
      if (b == null) {
        ix++;
        continue;
      }
      if (mx >= b[0] && mx < b[2] && my >= b[1] && my < b[3]) {
        handlePacketButton(ix, click);
        cir.setReturnValue(true);
        return;
      }
      ix++;
    }
    int j = 0;
    for (int[] b : neko$screenButtons) {
      if (b == null) {
        j++;
        continue;
      }
      if (mx >= b[0] && mx < b[2] && my >= b[1] && my < b[3]) {
        handleScreenButton(j, click);
        cir.setReturnValue(true);
        return;
      }
      j++;
    }
  }

  @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
  private void neko$guiDupeMouseDragged(MouseButtonEvent click, double deltaX, double deltaY, CallbackInfoReturnable<Boolean> cir) {
    if (dragging) {
      panelX = Math.max(0, Math.min(width - PANEL_W, (int) Math.round(click.x()) - dragOffX));
      panelY = Math.max(0, Math.min(height - panelHeight(), (int) Math.round(click.y()) - dragOffY));
      cir.setReturnValue(true);
    }
  }

  @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
  private void neko$guiDupeMouseReleased(MouseButtonEvent click, CallbackInfoReturnable<Boolean> cir) {
    if (dragging) {
      dragging = false;
      cir.setReturnValue(true);
    }
  }

  @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
  private void neko$guiDupeKeyPressed(KeyEvent input, CallbackInfoReturnable<Boolean> cir) {
    if (!isActive()) return;
    if (guiDupeChatField != null && guiDupeChatField.isFocused()) {
      if (input.isConfirmation()) {
        submitChat();
        cir.setReturnValue(true);
        return;
      }
      if (input.isEscape()) {
        guiDupeChatField.setFocused(false);
        cir.setReturnValue(true);
        return;
      }
    }
  }

  @Unique
  private void handlePacketButton(int index, MouseButtonEvent click) {
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
        nl.oxod.nekoclient.util.GuiDupeState.clearDelayed();
        module.info("Cleared queued packets.");
      }
    }
  }

  @Unique
  private void handleScreenButton(int index, MouseButtonEvent click) {
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

  @Unique
  private void submitChat() {
    if (guiDupeChatField == null) return;
    String text = guiDupeChatField.getValue();
    if (!text.isEmpty()) {
      if (text.startsWith("/")) {
        if (mc.player != null) mc.player.connection.sendCommand(text.substring(1));
      } else {
        if (mc.player != null) mc.player.connection.sendChat(text);
      }
    }
    guiDupeChatField.setValue("");
  }

  // ---- Render helpers ----

  @Unique
  private void neko$rect(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int color) {
    if (x2 <= x1 || y2 <= y1) return;
    g.fill(x1, y1, x2, y2, color);
  }

  @Unique
  private void neko$line(GuiGraphicsExtractor g, int x1, int y, int x2, int color) {
    g.horizontalLine(x1, x2, y, color);
  }

  @Unique
  private void neko$outline(GuiGraphicsExtractor g, int x, int y, int right, int bottom, int color) {
    g.outline(x, y, right - x, bottom - y, color);
  }

  @Unique
  private void neko$renderButton(GuiGraphicsExtractor g, int x, int y, int w, int h, String label,
                                 int mouseX, int mouseY, java.util.List<int[]> out, int slot) {
    boolean hovered = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    neko$rect(g, x, y, x + w, y + h, BTN_FILL);
    neko$outline(g, x, y, x + w, y + h, BORDER_SOFT);
    if (hovered) neko$rect(g, x + 1, y + 1, x + w - 1, y + h - 1, BTN_FILL_HOVER);
    graphics_text(g, label, x + (w - font.width(label)) / 2, y + (h - 8) / 2 - 1, TEXT);
    int[] bounds = {x, y, x + w, y + h};
    while (out.size() <= slot) out.add(null);
    out.set(slot, bounds);
  }

  @Unique
  private void neko$renderToggle(GuiGraphicsExtractor g, int x, int y, int w, int h, String label,
                                 boolean on, boolean colored, int mouseX, int mouseY, java.util.List<int[]> out, int slot) {
    boolean hovered = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    int fill = on ? (colored ? SUCCESS_FILL : BTN_FILL) : BTN_FILL_INACTIVE;
    int border = on ? (colored ? SUCCESS : BAD) : BTN_TEXT_INACTIVE;
    int textColor = on ? (colored ? SUCCESS : TEXT) : BTN_TEXT_INACTIVE;
    neko$rect(g, x, y, x + w, y + h, fill);
    neko$outline(g, x, y, x + w, y + h, border);
    if (hovered) neko$rect(g, x + 1, y + 1, x + w - 1, y + h - 1, BTN_FILL_HOVER);
    graphics_text(g, label, x + (w - font.width(label)) / 2, y + (h - 8) / 2 - 1, textColor);
    int[] bounds = {x, y, x + w, y + h};
    while (out.size() <= slot) out.add(null);
    out.set(slot, bounds);
  }

  @Unique
  private void neko$renderMetric(GuiGraphicsExtractor g, int x, int y, String key, String value, int valueColor) {
    int keyW = font.width(key);
    graphics_text(g, key, x, y, MUTED);
    graphics_text(g, value, x + keyW, y, valueColor);
  }

  @Unique
  private void graphics_text(GuiGraphicsExtractor g, String text, int x, int y, int color) {
    g.text(font, text, x, y, color, false);
  }
}
