/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.systems.modules.misc;

import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;
import nl.oxod.nekoclient.NekoClient;
import nl.oxod.nekoclient.events.world.ServerConnectBeginEvent;
import nl.oxod.nekoclient.settings.BoolSetting;
import nl.oxod.nekoclient.settings.DoubleSetting;
import nl.oxod.nekoclient.settings.Setting;
import nl.oxod.nekoclient.settings.SettingGroup;
import nl.oxod.nekoclient.systems.modules.Categories;
import nl.oxod.nekoclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.multiplayer.ServerData;

public class AutoReconnect extends Module {
  private final SettingGroup sgGeneral = settings.getDefaultGroup();

  public final Setting<Double> time = sgGeneral.add(new DoubleSetting.Builder()
    .name("delay")
    .description("The amount of seconds to wait before reconnecting to the server.")
    .defaultValue(3.5)
    .min(0)
    .decimalPlaces(1)
    .build()
  );

  public final Setting<Boolean> button = sgGeneral.add(new BoolSetting.Builder()
    .name("hide-buttons")
    .description("Will hide the buttons related to Auto Reconnect.")
    .defaultValue(false)
    .build()
  );

  public Pair<ServerAddress, ServerData> lastServerConnection;

  public AutoReconnect() {
    super(Categories.Misc, "auto-reconnect", "Automatically reconnects when disconnected from a server.");
    NekoClient.EVENT_BUS.subscribe(new StaticListener());
  }

  private class StaticListener {
    @EventHandler
    private void onGameJoined(ServerConnectBeginEvent event) {
      lastServerConnection = new ObjectObjectImmutablePair<>(event.address, event.info);
    }
  }
}
