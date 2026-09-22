package nl.oxod.nekoclient.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.contents.TranslatableContents;
import nl.oxod.nekoclient.security.Protector;
import nl.oxod.nekoclient.security.ProtectorFromPacketAccess;
import nl.oxod.nekoclient.security.ProtectorTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TranslatableContents.class)
public abstract class ProtectorTranslatableContentsMixin implements ProtectorFromPacketAccess {

    @Unique
    private boolean protector$fromPacket;

    @Unique
    private boolean protector$silent;

    @Override
    public void protector$setFromPacket() {
        this.protector$fromPacket = true;
    }

    @Override
    public void protector$setSilent() {
        this.protector$silent = true;
    }

    @Unique
    private static final String PROTECTOR_ALLOW = "\0__protector_allow__";

    @WrapOperation(
        method = "decompose",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/locale/Language;getOrDefault(Ljava/lang/String;)Ljava/lang/String;")
    )
    private String protector$wrapGetOrDefault(Language instance, String id, Operation<String> original) {
        String result = protector$handle(id, id);
        if (result == PROTECTOR_ALLOW) return original.call(instance, id);
        return result;
    }

    @WrapOperation(
        method = "decompose",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/locale/Language;getOrDefault(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;")
    )
    private String protector$wrapGetOrDefaultFallback(Language instance, String idArg, String defaultValue,
                                                      Operation<String> original) {
        String result = protector$handle(idArg, defaultValue);
        if (result == PROTECTOR_ALLOW) return original.call(instance, idArg, defaultValue);
        return result;
    }

    @Unique
    private String protector$handle(String translationKey, String defaultValue) {

        if (protector$silent) return PROTECTOR_ALLOW;
        if (!this.protector$fromPacket) return PROTECTOR_ALLOW;
        if (!Protector.shouldProtectTranslationKeys()) return PROTECTOR_ALLOW;

        Minecraft mc;
        try {
            mc = Minecraft.getInstance();
        } catch (Throwable ignored) {
            return PROTECTOR_ALLOW;
        }
        if (mc == null || mc.hasSingleplayerServer()) return PROTECTOR_ALLOW;

        String replacement = ProtectorTracker.translationReplacement(translationKey, defaultValue);
        return replacement == null ? PROTECTOR_ALLOW : replacement;
    }
}
