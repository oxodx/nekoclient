/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient.mixin;

import nl.oxod.nekoclient.NekoClient;
import nl.oxod.nekoclient.systems.modules.Modules;
import nl.oxod.nekoclient.systems.modules.world.Timer;
import net.minecraft.client.DeltaTracker;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DeltaTracker.Timer.class)
public abstract class RenderTickCounterDynamicMixin {
    @Shadow
    private float deltaTicks;

    @Inject(method = "advanceGameTime(J)I", at = @At(value = "FIELD", target = "Lnet/minecraft/client/DeltaTracker$Timer;lastMs:J", opcode = Opcodes.PUTFIELD))
    private void onBeingRenderTick(long currentMs, CallbackInfoReturnable<Integer> cir) {
        // advanceGameTime runs at Minecraft.runTick:1186, before the first Minecraft.tick at 1208
        // where the client bootstrap happens, so the module registry is not up yet on frame 1.
        if (!NekoClient.isInitialized()) return;

        deltaTicks *= (float) Modules.get().get(Timer.class).getMultiplier();
    }
}
