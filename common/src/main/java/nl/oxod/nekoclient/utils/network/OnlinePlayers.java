/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.utils.network;

public class OnlinePlayers {
  private static long lastPingTime;

  private OnlinePlayers() {
  }

  public static void update() {
    long time = System.currentTimeMillis();

    if (time - lastPingTime > 5 * 60 * 1000) {
      NekoExecutor.execute(() -> Http.post("https://meteorclient.com/api/online/ping").ignoreExceptions().send());

      lastPingTime = time;
    }
  }

  public static void leave() {
    NekoExecutor.execute(() -> Http.post("https://meteorclient.com/api/online/leave").ignoreExceptions().send());
  }
}
