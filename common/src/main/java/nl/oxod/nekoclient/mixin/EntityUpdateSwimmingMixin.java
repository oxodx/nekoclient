/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import nl.oxod.nekoclient.systems.modules.Modules;
import nl.oxod.nekoclient.systems.modules.movement.NoSlow;
import net.minecraft.world.entity.Entity;
import nl.oxod.nekoclient.systems.modules.movement.flight.Flight;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import static nl.oxod.nekoclient.NekoClient.mc;

/**
 * Fabric-only half of the NoSlow "fluid drag" hook on {@link Entity}.
 *
 * <p>NeoForge rewrites {@code Entity.updateSwimming()} so the submerged check no longer calls
 * {@code isUnderWater()}; it routes through its own fluid system instead:
 *
 * <pre>{@code
 * this.setSwimming(this.isSprinting() && (this.isInWater()
 *     || this.fluidInteraction.isInFluidMatching(this, (e, type, _) -> e.canSwimInFluidType(type)))
 *     && !this.isPassenger());
 * }</pre>
 *
 * <p>The vanilla injection point therefore does not exist there. Because {@code Entity} is loaded
 * during {@code Bootstrap.bootStrap()}, an unsatisfiable injection fails while the game is still
 * booting, inside the parallel dispatch that swallows the exception -- the client dies with a
 * 33-line log, exit code 1, and no crash report. Gating this mixin to Fabric keeps NeoForge
 * bootable; the equivalent NeoForge hook is not implemented yet.
 */
@Mixin(Entity.class)
public abstract class EntityUpdateSwimmingMixin {
  @ModifyExpressionValue(method = "updateSwimming", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;isUnderWater()Z"))
  private boolean isSubmergedInWater(boolean submerged) {
    if ((Object) this != mc.player) return submerged;

    if (Modules.get().get(NoSlow.class).fluidDrag()) return false;
    if (Modules.get().get(Flight.class).isActive()) return false;
    return submerged;
  }
}
