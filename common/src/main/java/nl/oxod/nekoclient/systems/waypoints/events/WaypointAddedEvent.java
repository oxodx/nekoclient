/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.systems.waypoints.events;

import nl.oxod.nekoclient.systems.waypoints.Waypoint;

public record WaypointAddedEvent(Waypoint waypoint) {
}
