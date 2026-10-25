package nl.oxod.nekoclient.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonInfo;
import nl.oxod.nekoclient.mixin.accessor.KeyboardHandlerAccessor;
import nl.oxod.nekoclient.mixin.accessor.MouseHandlerAccessor;
import nl.oxod.nekoclient.utils.KeyMappingBridge;
import org.lwjgl.sdl.SDLMouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(KeyMapping.class)
public abstract class KeyMappingMixin implements KeyMappingBridge {
  @Shadow
  protected InputConstants.Key key;

  @Shadow
  public abstract void setDown(boolean down);

  @Override
  @Unique
  public boolean nekoclient$isActuallyDown() {
    Minecraft mc = Minecraft.getInstance();
    if (mc == null || mc.getWindow() == null) return false;
    Window window = mc.getWindow();
    int code = key.getValue();
    if (key.getType() == InputConstants.Type.MOUSE) {
      return (SDLMouse.SDL_GetMouseState(null, null) & (1 << (code - 1))) != 0;
    }
    return InputConstants.isKeyDown(code);
  }

  @Override
  @Unique
  public void nekoclient$resetPressedState() {
    setDown(nekoclient$isActuallyDown());
  }

  @Override
  @Unique
  public void nekoclient$simulatePress(boolean pressed) {
    Minecraft mc = Minecraft.getInstance();
    if (mc == null || mc.getWindow() == null || mc.keyboardHandler == null || mc.mouseHandler == null) return;
    Window window = mc.getWindow();
    int action = pressed ? InputConstants.PRESS : InputConstants.RELEASE;
    switch (key.getType()) {
      case KEYBOARD -> ((KeyboardHandlerAccessor) mc.keyboardHandler).nekoclient$invokeKeyPress(
        window.handle(), action, new KeyEvent(key.getValue(), 0, 0)
      );
      case MOUSE -> ((MouseHandlerAccessor) mc.mouseHandler).nekoclient$invokeOnButton(
        window.handle(), new MouseButtonInfo(key.getValue(), 0), action
      );
      default -> setDown(pressed);
    }
  }
}
