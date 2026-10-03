package nl.oxod.nekoclient.systems.modules.misc;

import nl.oxod.nekoclient.events.packets.PacketEvent;
import nl.oxod.nekoclient.events.world.TickEvent;
import nl.oxod.nekoclient.settings.BoolSetting;
import nl.oxod.nekoclient.settings.IntSetting;
import nl.oxod.nekoclient.settings.KeybindSetting;
import nl.oxod.nekoclient.settings.Setting;
import nl.oxod.nekoclient.settings.SettingGroup;
import nl.oxod.nekoclient.systems.modules.Categories;
import nl.oxod.nekoclient.systems.modules.Module;
import nl.oxod.nekoclient.utils.misc.Keybind;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.world.inventory.AbstractContainerMenu;
import nl.oxod.nekoclient.utils.GuiDupeState;

import com.mojang.serialization.JsonOps;

public class GuiDupe extends Module {
  private final boolean[] pressedActions = new boolean[8];

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
    resetSession();
  }

  @EventHandler
  private void onSendPacket(PacketEvent.Send event) {
    if (event.packet instanceof ServerboundContainerClickPacket
      || event.packet instanceof ServerboundContainerButtonClickPacket) {
      if (!sendPackets.get()) {
        event.cancel();
      } else if (delayPackets.get()) {
        GuiDupeState.enqueueDelayed(event.packet);
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
    Setting<?>[] keys = {flushKey, flushAndQuitKey, desyncKey, closeWithoutPacketKey,
      copyWindowDataKey, fabricateKey, saveGuiKey, restoreGuiKey};
    Runnable[] actions = {this::flush, () -> { flush(); closeScreen(true); }, this::desync,
      this::closeWithoutPacket, this::copyWindowData, this::fabricate, this::saveGui, this::restoreGui};
    for (int i = 0; i < keys.length; i++) {
      boolean pressed = ((Keybind) keys[i].get()).isPressed();
      if (pressed && !pressedActions[i] && !(mc.gui.screen() instanceof AbstractContainerScreen<?> screen
        && nl.oxod.nekoclient.gui.GuiDupePanel.isTyping(screen))) actions[i].run();
      pressedActions[i] = pressed;
    }
  }

  @EventHandler
  private void onGameLeft(nl.oxod.nekoclient.events.game.GameLeftEvent event) {
    resetSession();
  }

  private void resetSession() {
    GuiDupeState.clearDelayed();
    GuiDupeState.clearStored();
    GuiDupeState.setSuppressNextContainerClosePacket(false);
    java.util.Arrays.fill(pressedActions, false);
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
    if (!hasContainer()) return;
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
    if (!hasContainer()) return;
    GuiDupeState.setSuppressNextContainerClosePacket(!sendPacket);
    try {
      mc.player.closeContainer();
    } finally {
      GuiDupeState.setSuppressNextContainerClosePacket(false);
    }
    if (!sendPacket) info("GUI closed without packet.");
  }

  private boolean hasContainer() {
    return mc.player != null && mc.getConnection() != null
      && mc.gui.screen() instanceof AbstractContainerScreen<?> screen
      && screen.getMenu() == mc.player.containerMenu;
  }

  private void closeWithoutPacket() {
    closeScreen(false);
  }

  public void closeWithoutPacketPublic() {
    closeScreen(false);
  }

  private void fabricate() {
    if (!hasContainer()) return;
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
    if (!hasContainer()) {
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
    if (!hasContainer()) {
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
    if (mc.player == null || mc.getConnection() == null) return;
    if (GuiDupeState.getStoredScreen() == null || GuiDupeState.getStoredMenu() == null) {
      error("No stored GUI.");
      return;
    }
    if (mc.gui.screen() instanceof AbstractContainerScreen<?> && mc.player.containerMenu != GuiDupeState.getStoredMenu()) {
      closeScreen(true);
    }
    mc.player.containerMenu = GuiDupeState.getStoredMenu();
    mc.gui.setScreen(GuiDupeState.getStoredScreen());
    info("GUI restored.");
  }

  public void restoreGuiPublic() {
    restoreGui();
  }
}
