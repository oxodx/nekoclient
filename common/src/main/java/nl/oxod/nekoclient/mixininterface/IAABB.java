/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.mixininterface;

import net.minecraft.core.BlockPos;

public interface IAABB {
    void neko$expand(double v);

    void neko$set(double x1, double y1, double z1, double x2, double y2, double z2);

    default void neko$set(BlockPos pos) {
        neko$set(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
    }
}
