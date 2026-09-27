package nl.oxod.nekoclient.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import nl.oxod.nekoclient.security.Protector;
import nl.oxod.nekoclient.security.ProtectorPacketContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.network.PacketProcessor$ListenerAndPacket")
public class ProtectorPacketProcessorMixin {

    @WrapOperation(
        method = "handle",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/network/protocol/Packet;handle(Lnet/minecraft/network/PacketListener;)V")
    )
    private <T extends PacketListener> void protector$wrapHandle(Packet<?> instance, T listener,
                                                                 Operation<Void> original) {

        if (!(instance instanceof ClientboundCustomPayloadPacket)
            || !Protector.shouldTagPacketComponents()) {
            original.call(instance, listener);
            return;
        }
        ProtectorPacketContext.setProcessingPacket(true);
        try {
            original.call(instance, listener);
        } finally {
            ProtectorPacketContext.setProcessingPacket(false);
        }
    }
}
