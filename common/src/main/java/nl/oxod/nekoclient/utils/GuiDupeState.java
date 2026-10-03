/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.inventory.AbstractContainerMenu;

import java.util.ArrayDeque;
import java.util.Deque;

public final class GuiDupeState {
  private static final Deque<Packet<?>> delayedPackets = new ArrayDeque<>();

  private static volatile boolean sendGuiPackets = true;
  private static volatile boolean delayGuiPackets;
  private static volatile boolean suppressNextContainerClosePacket;

  private static Screen storedScreen;
  private static AbstractContainerMenu storedMenu;

  private GuiDupeState() {
  }

  public static boolean shouldSendGuiPackets() {
    return sendGuiPackets;
  }

  public static void setSendGuiPackets(boolean value) {
    sendGuiPackets = value;
  }

  public static boolean shouldDelayGuiPackets() {
    return delayGuiPackets;
  }

  public static void setDelayGuiPackets(boolean value) {
    delayGuiPackets = value;
  }

  public static boolean shouldSuppressNextContainerClosePacket() {
    return suppressNextContainerClosePacket;
  }

  public static void setSuppressNextContainerClosePacket(boolean value) {
    suppressNextContainerClosePacket = value;
  }

  public static synchronized int delayedPacketCount() {
    return delayedPackets.size();
  }

  public static synchronized void enqueueDelayed(Packet<?> packet) {
    delayedPackets.addLast(packet);
  }

  public static synchronized void flushDelayed(Minecraft mc) {
    if (mc.getConnection() == null) return;
    net.minecraft.network.Connection connection = mc.getConnection().getConnection();
    if (connection == null) return;
    Packet<?> packet;
    while ((packet = delayedPackets.pollFirst()) != null) {
      connection.send(packet, null, true);
    }
  }

  public static synchronized void clearDelayed() {
    delayedPackets.clear();
  }

  public static void storeScreen(Screen screen, AbstractContainerMenu menu) {
    storedScreen = screen;
    storedMenu = menu;
  }

  public static Screen getStoredScreen() {
    return storedScreen;
  }

  public static AbstractContainerMenu getStoredMenu() {
    return storedMenu;
  }

  public static void clearStored() {
    storedScreen = null;
    storedMenu = null;
  }
}
