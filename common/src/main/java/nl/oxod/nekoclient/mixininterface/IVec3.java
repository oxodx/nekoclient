/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.mixininterface;

import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

@SuppressWarnings("UnusedReturnValue")
public interface IVec3 {
  Vec3 neko$set(double x, double y, double z);

  default Vec3 neko$set(Vec3i vec) {
    return neko$set(vec.getX(), vec.getY(), vec.getZ());
  }

  default Vec3 neko$set(Vector3d vec) {
    return neko$set(vec.x, vec.y, vec.z);
  }

  default Vec3 neko$set(Vec3 pos) {
    return neko$set(pos.x, pos.y, pos.z);
  }

  Vec3 neko$setXZ(double x, double z);

  Vec3 neko$setY(double y);
}
