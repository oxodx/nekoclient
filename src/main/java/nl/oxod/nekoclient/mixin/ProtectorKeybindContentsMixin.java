package nl.oxod.nekoclient.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.KeybindContents;
import nl.oxod.nekoclient.security.Protector;
import nl.oxod.nekoclient.security.ProtectorFromPacketAccess;
import nl.oxod.nekoclient.security.ProtectorTracker;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Supplier;

@Mixin(KeybindContents.class)
public abstract class ProtectorKeybindContentsMixin implements ProtectorFromPacketAccess {

    @Shadow @Final private String name;

    @Unique
    private boolean protector$fromPacket;

    @Unique
    private Object protector$cachedBlocked;

    @Override
    public void protector$setFromPacket() {
        this.protector$fromPacket = true;
    }

    @WrapOperation(
        method = "getNestedComponent",
        at = @At(value = "INVOKE", target = "Ljava/util/function/Supplier;get()Ljava/lang/Object;")
    )
    private Object protector$interceptKeybind(Supplier<?> supplier, Operation<Object> original) {
        if (!this.protector$fromPacket) return original.call(supplier);
        if (!Protector.shouldProtectTranslationKeys()) return original.call(supplier);

        Minecraft mc;
        try {
            mc = Minecraft.getInstance();
        } catch (Throwable ignored) {
            return original.call(supplier);
        }
        if (mc == null || mc.hasSingleplayerServer()) return original.call(supplier);

        if (!ProtectorTracker.shouldBlockKeybind(name)) return original.call(supplier);

        if (protector$cachedBlocked != null) return protector$cachedBlocked;
        Component replacement = Component.literal(name);
        protector$cachedBlocked = replacement;
        return replacement;
    }
}
