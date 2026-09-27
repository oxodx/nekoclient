/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.mixin;

import nl.oxod.nekoclient.mixininterface.IEntityRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EntityRenderState.class)
public abstract class EntityRenderStateMixin implements IEntityRenderState {
  @Unique
  private Entity entity;

  @Override
  @Nullable // "EntityCulling mod can prevent the code that sets the entity from running"
  public Entity neko$getEntity() {
    return entity;
  }

  @Override
  public void neko$setEntity(Entity entity) {
    this.entity = entity;
  }
}
