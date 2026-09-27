/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.modintegration;

import nl.oxod.nekoclient.systems.modules.Modules;
import nl.oxod.nekoclient.systems.modules.render.Fullbright;
import nl.oxod.nekoclient.systems.modules.render.Xray;

import java.util.function.BooleanSupplier;

/*
 * Hook for BadOptimizations mod compatibility.
 * Signals when the lightmap needs to be updated due to Fullbright or Xray state changes.
 */
@SuppressWarnings("unused") // Used in fabric.mod.json
public class BadOptimizationsHook implements BooleanSupplier {
  private int lastState;

  @Override
  public boolean getAsBoolean() {
    Modules m = Modules.get();
    if (m == null) return false;

    int state = (m.get(Fullbright.class).getGamma() ? 1 : 0) | (m.isActive(Xray.class) ? 2 : 0);
    boolean changed = state != lastState;
    lastState = state;
    return changed;
  }
}
