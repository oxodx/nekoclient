package nl.oxod.nekoclient.mixin;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import nl.oxod.nekoclient.gui.GuiDupeChatField;
import nl.oxod.nekoclient.gui.GuiDupePanel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(AbstractContainerScreen.class)
public abstract class GuiDupeScreenMixin<T extends AbstractContainerMenu> extends Screen {
  @Unique
  private static final int CHAT_H = 16;

  @Unique
  private static String neko$chatText = "";

  @Unique
  private GuiDupePanel guiDupePanel;
  @Unique
  private GuiDupeChatField guiDupeChatField;

  protected GuiDupeScreenMixin(Component title) {
    super(title);
  }

  @Inject(method = "init", at = @At("TAIL"))
  private void neko$guiDupeInit(CallbackInfo ci) {
    if (guiDupePanel == null) {
      guiDupeChatField = new GuiDupeChatField(this.font, 0, 0, 0, CHAT_H, Component.literal("GUI dupe chat"),
        this::submitChat, () -> guiDupeChatField.setFocused(false));
      guiDupeChatField.setMaxLength(256);
      guiDupeChatField.setBordered(false);
      guiDupeChatField.setTextColor(GuiDupePanel.textColor());
      guiDupeChatField.setHint(Component.literal("Type message or /command...").copy().withColor(GuiDupePanel.mutedColor()));
      guiDupeChatField.setResponder(text -> neko$chatText = text);
      guiDupeChatField.setValue(neko$chatText);

      guiDupePanel = new GuiDupePanel((AbstractContainerScreen<?>) (Object) this, guiDupeChatField);
    }
    // init() clears all renderables every time it runs (including when a GUI is
    // restored via setScreen()), so the panel must be re-added on every init.
    addRenderableWidget(guiDupePanel);
    addRenderableWidget(guiDupeChatField);
  }

  @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
  private void neko$guiDupeKeyPressed(KeyEvent input, CallbackInfoReturnable<Boolean> cir) {
    if (!GuiDupePanel.active()) return;
    if (guiDupeChatField != null && guiDupeChatField.isFocused()) {
      guiDupeChatField.keyPressed(input);
      cir.setReturnValue(true);
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
    neko$chatText = "";
    guiDupeChatField.setValue("");
  }
}
