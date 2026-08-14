package nl.oxod.nekoclient.systems.modules.misc;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.KeybindSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.world.inventory.AbstractContainerMenu;
import nl.oxod.nekoclient.util.GuiDupeState;

import com.mojang.serialization.JsonOps;

public class GuiDupe extends Module {
  private final SettingGroup sgGeneral = settings.getDefaultGroup();
  private final SettingGroup sgActions = settings.createGroup("Actions");

  private final Setting<Boolean> sendPackets = sgGeneral.add(new BoolSetting.Builder()
    .name("send-packets")
    .description("Sends container packets to the server when interacting with a GUI.")
    .defaultValue(true)
    .build()
  );

  private final Setting<Boolean> delayPackets = sgGeneral.add(new BoolSetting.Builder()
    .name("delay-packets")
    .description("Queues container packets instead of sending them instantly. Use the Flush key to send the queue.")
    .defaultValue(false)
    .visible(sendPackets::get)
    .build()
  );

  private final Setting<Boolean> cancelClose = sgGeneral.add(new BoolSetting.Builder()
    .name("cancel-close")
    .description("Prevents the close GUI packet from being sent when closing a container.")
    .defaultValue(false)
    .build()
  );

  private final Setting<Keybind> flushKey = sgActions.add(new KeybindSetting.Builder()
    .name("flush")
    .description("Sends all queued packets.")
    .build()
  );

  private final Setting<Keybind> flushAndQuitKey = sgActions.add(new KeybindSetting.Builder()
    .name("flush-and-quit")
    .description("Sends all queued packets and closes the current GUI.")
    .build()
  );

  private final Setting<Keybind> desyncKey = sgActions.add(new KeybindSetting.Builder()
    .name("desync")
    .description("Closes the GUI on the server side while keeping it open client side.")
    .build()
  );

  private final Setting<Keybind> closeWithoutPacketKey = sgActions.add(new KeybindSetting.Builder()
    .name("close-without-packet")
    .description("Closes the current GUI without sending a close packet to the server.")
    .build()
  );

  private final Setting<Keybind> copyWindowDataKey = sgActions.add(new KeybindSetting.Builder()
    .name("copy-window-data")
    .description("Copies the container id, state id and title JSON of the current GUI to the clipboard.")
    .build()
  );

  private final Setting<Keybind> fabricateKey = sgActions.add(new KeybindSetting.Builder()
    .name("fabricate")
    .description("Sends a fabricated container button click packet for the current GUI.")
    .build()
  );

  private final Setting<Keybind> saveGuiKey = sgActions.add(new KeybindSetting.Builder()
    .name("save-gui")
    .description("Stores the current GUI to be restored later.")
    .build()
  );

  private final Setting<Keybind> restoreGuiKey = sgActions.add(new KeybindSetting.Builder()
    .name("restore-gui")
    .description("Restores the GUI stored with Save GUI.")
    .build()
  );

  private final Setting<Integer> fabricateButtonId = sgGeneral.add(new IntSetting.Builder()
    .name("fabricate-button-id")
    .description("Button ID used by the Fabricate action.")
    .defaultValue(0)
    .min(0)
    .build()
  );

  public GuiDupe() {
    super(Categories.Misc, "gui-dupe", "Allows you to manipulate container GUI packets, useful for dupe related exploits.");
  }

  public boolean getSendPackets() {
    return sendPackets.get();
  }

  public void setSendPackets(boolean value) {
    sendPackets.set(value);
  }

  public boolean getDelayPackets() {
    return delayPackets.get();
  }

  public void setDelayPackets(boolean value) {
    delayPackets.set(value);
  }

  public int getDelayedCount() {
    return GuiDupeState.delayedPacketCount();
  }

  @Override
  public void onActivate() {
    GuiDupeState.setSendGuiPackets(sendPackets.get());
    GuiDupeState.setDelayGuiPackets(delayPackets.get());
  }

  @Override
  public void onDeactivate() {
    GuiDupeState.setSendGuiPackets(true);
    GuiDupeState.setDelayGuiPackets(false);
    GuiDupeState.clearDelayed();
  }

  @EventHandler
  private void onSendPacket(PacketEvent.Send event) {
    if (event.packet instanceof ServerboundContainerClickPacket
      || event.packet instanceof ServerboundContainerButtonClickPacket) {
      if (delayPackets.get()) {
        GuiDupeState.enqueueDelayed(event.packet);
        event.cancel();
      } else if (!sendPackets.get()) {
        event.cancel();
      }
      return;
    }

    if (event.packet instanceof ServerboundContainerClosePacket) {
      if (GuiDupeState.shouldSuppressNextContainerClosePacket()) {
        event.cancel();
        GuiDupeState.setSuppressNextContainerClosePacket(false);
      } else if (cancelClose.get() || !sendPackets.get()) {
        event.cancel();
      }
    }
  }

  @EventHandler
  private void onTick(TickEvent.Post event) {
    if (flushKey.get().isPressed()) flush();
    if (flushAndQuitKey.get().isPressed()) {
      flush();
      closeScreen(true);
    }
    if (desyncKey.get().isPressed()) desync();
    if (closeWithoutPacketKey.get().isPressed()) closeWithoutPacket();
    if (copyWindowDataKey.get().isPressed()) copyWindowData();
    if (fabricateKey.get().isPressed()) fabricate();
    if (saveGuiKey.get().isPressed()) saveGui();
    if (restoreGuiKey.get().isPressed()) restoreGui();
  }

  private void flush() {
    if (mc.player == null || mc.getConnection() == null) return;
    int count = GuiDupeState.delayedPacketCount();
    GuiDupeState.flushDelayed(mc);
    info("Flushed %d queued packet(s).", count);
  }

  public void flushPublic() {
    flush();
  }

  private void desync() {
    if (mc.player == null || mc.player.containerMenu == null) return;
    if (mc.player.containerMenu == mc.player.inventoryMenu) {
      error("Inventory GUI can't be desynced.");
      return;
    }
    net.minecraft.network.Connection connection = mc.getConnection().getConnection();
    connection.send(new ServerboundContainerClosePacket(mc.player.containerMenu.containerId), null, true);
    info("Desynced GUI, close packet sent while screen stays open.");
  }

  public void desyncPublic() {
    desync();
  }

  private void closeScreen(boolean sendPacket) {
    if (mc.gui.screen() == null) return;

    if (!sendPacket) {
      GuiDupeState.setSuppressNextContainerClosePacket(true);
      mc.gui.setScreen(null);
      GuiDupeState.setSuppressNextContainerClosePacket(false);
      info("GUI closed without packet.");
    } else {
      mc.gui.setScreen(null);
    }
  }

  private void closeWithoutPacket() {
    closeScreen(false);
  }

  public void closeWithoutPacketPublic() {
    closeScreen(false);
  }

  private void fabricate() {
    if (mc.player == null || mc.player.containerMenu == null) return;
    if (mc.player.containerMenu == mc.player.inventoryMenu) {
      error("Inventory GUI can't be fabricated.");
      return;
    }
    net.minecraft.network.Connection connection = mc.getConnection().getConnection();
    connection.send(new ServerboundContainerButtonClickPacket(mc.player.containerMenu.containerId, fabricateButtonId.get()), null, true);
    info("Fabricated button click %d on container %d.", fabricateButtonId.get(), mc.player.containerMenu.containerId);
  }

  public void fabricatePublic() {
    fabricate();
  }

  private void copyWindowData() {
    if (mc.gui.screen() == null || mc.player == null || mc.player.containerMenu == null) {
      error("No GUI open.");
      return;
    }

    AbstractContainerMenu menu = mc.player.containerMenu;
    Component title = mc.gui.screen() instanceof AbstractContainerScreen<?> screen ? screen.getTitle() : Component.literal("");
    String titleJson = ComponentSerialization.CODEC.encodeStart(JsonOps.INSTANCE, title)
      .result()
      .map(Object::toString)
      .orElse("");
    String data = "sync=" + menu.containerId + " revision=" + menu.getStateId() + " title=" + titleJson;
    mc.keyboardHandler.setClipboard(data);
    info("Copied window data to clipboard.");
  }

  public void copyWindowDataPublic() {
    copyWindowData();
  }

  private void saveGui() {
    if (mc.gui.screen() == null || mc.player == null || mc.player.containerMenu == null) {
      error("No GUI open.");
      return;
    }
    GuiDupeState.storeScreen(mc.gui.screen(), mc.player.containerMenu);
    info("GUI stored.");
  }

  public void saveGuiPublic() {
    saveGui();
  }

  private void restoreGui() {
    if (GuiDupeState.getStoredScreen() == null || GuiDupeState.getStoredMenu() == null) {
      error("No stored GUI.");
      return;
    }
    mc.gui.setScreen(GuiDupeState.getStoredScreen());
    if (mc.player != null) {
      mc.player.containerMenu = GuiDupeState.getStoredMenu();
    }
    info("GUI restored.");
  }

  public void restoreGuiPublic() {
    restoreGui();
  }
}
