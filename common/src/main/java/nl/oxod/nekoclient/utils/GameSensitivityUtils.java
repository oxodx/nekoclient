/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.utils;

import static nl.oxod.nekoclient.NekoClient.mc;

public class GameSensitivityUtils {
  public static float getSensitivity(float rot) {
    return getDeltaMouse(rot) * getGCDValue();
  }

  public static float getGCDValue() {
    return (float) (getGCD() * 0.15);
  }

  public static float getGCD() {
    float f1;
    return (f1 = (float) (mc.options.sensitivity().get() * 0.6 + 0.2)) * f1 * f1 * 8;
  }

  public static float getDeltaMouse(float delta) {
    return Math.round(delta / getGCDValue());
  }
}
