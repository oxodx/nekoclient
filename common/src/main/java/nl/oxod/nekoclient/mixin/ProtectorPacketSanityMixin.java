package nl.oxod.nekoclient.mixin;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import nl.oxod.nekoclient.security.ProtectorComponentSanity;
import nl.oxod.nekoclient.security.ProtectorNumericSanity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ProtectorPacketSanityMixin {

    @Inject(method = "handleSetEntityMotion", at = @At("HEAD"), cancellable = true, require = 0)
    private void protector$saneSetEntityMotion(ClientboundSetEntityMotionPacket packet, CallbackInfo ci) {
        if (ProtectorNumericSanity.motionOutOfRange(packet.movement())) ci.cancel();
    }

    @Inject(method = "handleExplosion", at = @At("HEAD"), cancellable = true, require = 0)
    private void protector$saneExplosion(ClientboundExplodePacket packet, CallbackInfo ci) {
        if (ProtectorNumericSanity.outOfRange(packet.center())
            || ProtectorNumericSanity.outOfRange(packet.radius())
            || (packet.playerKnockback().isPresent()
                && ProtectorNumericSanity.motionOutOfRange(packet.playerKnockback().get()))) {
            ci.cancel();
        }
    }

    @Inject(method = "handleMovePlayer", at = @At("HEAD"), cancellable = true, require = 0)
    private void protector$saneMovePlayer(ClientboundPlayerPositionPacket packet, CallbackInfo ci) {
        if (ProtectorNumericSanity.positionMoveOutOfRange(packet.change())) ci.cancel();
    }

    @Inject(method = "handleTeleportEntity", at = @At("HEAD"), cancellable = true, require = 0)
    private void protector$saneTeleportEntity(ClientboundTeleportEntityPacket packet, CallbackInfo ci) {
        if (ProtectorNumericSanity.positionMoveOutOfRange(packet.change())) ci.cancel();
    }

    @Inject(method = "handleEntityPositionSync", at = @At("HEAD"), cancellable = true, require = 0)
    private void protector$saneEntityPositionSync(ClientboundEntityPositionSyncPacket packet, CallbackInfo ci) {
        if (ProtectorNumericSanity.positionMoveOutOfRange(packet.values())) ci.cancel();
    }

    @Inject(method = "handleAddEntity", at = @At("HEAD"), cancellable = true, require = 0)
    private void protector$saneAddEntity(ClientboundAddEntityPacket packet, CallbackInfo ci) {
        if (ProtectorNumericSanity.outOfRange(packet.getX())
            || ProtectorNumericSanity.outOfRange(packet.getY())
            || ProtectorNumericSanity.outOfRange(packet.getZ())
            || ProtectorNumericSanity.motionOutOfRange(packet.getMovement())) {
            ci.cancel();
        }
    }

    @Inject(method = "handleParticleEvent", at = @At("HEAD"), cancellable = true, require = 0)
    private void protector$saneParticles(ClientboundLevelParticlesPacket packet, CallbackInfo ci) {
        if (ProtectorNumericSanity.outOfRange(packet.getX())
            || ProtectorNumericSanity.outOfRange(packet.getY())
            || ProtectorNumericSanity.outOfRange(packet.getZ())
            || ProtectorNumericSanity.outOfRange(packet.getXDist())
            || ProtectorNumericSanity.outOfRange(packet.getYDist())
            || ProtectorNumericSanity.outOfRange(packet.getZDist())
            || ProtectorNumericSanity.outOfRange(packet.getMaxSpeed())
            || packet.getCount() > 100_000) {
            ci.cancel();
        }
    }

    @Inject(method = "handleMoveVehicle", at = @At("HEAD"), cancellable = true, require = 0)
    private void protector$saneMoveVehicle(ClientboundMoveVehiclePacket packet, CallbackInfo ci) {
        if (ProtectorNumericSanity.outOfRange(packet.position())
            || ProtectorNumericSanity.outOfRange(packet.yRot())
            || ProtectorNumericSanity.outOfRange(packet.xRot())) {
            ci.cancel();
        }
    }

    @Inject(method = "handleAddObjective", at = @At("HEAD"), cancellable = true, require = 0)
    private void protector$saneObjective(ClientboundSetObjectivePacket packet, CallbackInfo ci) {
        if (!ProtectorComponentSanity.isSafe(packet.getDisplayName())
            || (packet.getNumberFormat().isPresent()
                && !ProtectorComponentSanity.isSafe(packet.getNumberFormat().get()))) {
            ci.cancel();
        }
    }

    @Inject(method = "handleSetScore", at = @At("HEAD"), cancellable = true, require = 0)
    private void protector$saneScore(ClientboundSetScorePacket packet, CallbackInfo ci) {
        if ((packet.display().isPresent()
                && !ProtectorComponentSanity.isSafe(packet.display().get()))
            || (packet.numberFormat().isPresent()
                && !ProtectorComponentSanity.isSafe(packet.numberFormat().get()))) {
            ci.cancel();
        }
    }

    @Inject(method = "handleSetPlayerTeamPacket", at = @At("HEAD"), cancellable = true, require = 0)
    private void protector$saneTeam(ClientboundSetPlayerTeamPacket packet, CallbackInfo ci) {
        if (packet.getParameters().isEmpty()) return;
        ClientboundSetPlayerTeamPacket.Parameters parameters = packet.getParameters().get();
        if (!ProtectorComponentSanity.isSafe(parameters.displayName())
            || !ProtectorComponentSanity.isSafe(parameters.playerPrefix())
            || !ProtectorComponentSanity.isSafe(parameters.playerSuffix())) {
            ci.cancel();
        }
    }
}
