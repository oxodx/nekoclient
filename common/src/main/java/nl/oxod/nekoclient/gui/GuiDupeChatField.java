package nl.oxod.nekoclient.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

public class GuiDupeChatField extends EditBox {
  private final Runnable onSubmit;
  private final Runnable onUnfocus;

  public GuiDupeChatField(Font font, int x, int y, int width, int height, Component label, Runnable onSubmit, Runnable onUnfocus) {
    super(font, x, y, width, height, label);
    this.onSubmit = onSubmit;
    this.onUnfocus = onUnfocus;
  }

  @Override
  public boolean keyPressed(KeyEvent input) {
    if (input.isConfirmation()) {
      onSubmit.run();
      return true;
    }
    if (input.isEscape()) {
      onUnfocus.run();
      return true;
    }
    return super.keyPressed(input);
  }
}
