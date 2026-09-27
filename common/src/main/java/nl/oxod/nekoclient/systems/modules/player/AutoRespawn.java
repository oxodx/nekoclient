/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.systems.modules.player;

import nl.oxod.nekoclient.events.game.OpenScreenEvent;
import nl.oxod.nekoclient.systems.modules.Categories;
import nl.oxod.nekoclient.systems.modules.Module;
import nl.oxod.nekoclient.systems.modules.Modules;
import nl.oxod.nekoclient.systems.modules.render.WaypointsModule;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.client.gui.screens.DeathScreen;

public class AutoRespawn extends Module {
  public AutoRespawn() {
    super(Categories.Player, "auto-respawn", "Automatically respawns after death.");
  }

  @EventHandler(priority = EventPriority.HIGH)
  private void onOpenScreenEvent(OpenScreenEvent event) {
    if (!(event.screen instanceof DeathScreen)) return;

    Modules.get().get(WaypointsModule.class).addDeath(mc.player.position());
    mc.player.respawn();
    event.cancel();
  }
}
