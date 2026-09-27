/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient;

import nl.oxod.nekoclient.platform.LoaderDetection;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class MixinPlugin implements IMixinConfigPlugin {
    private static final String mixinPackage = "meteordevelopment.meteorclient.mixin";

    /**
     * Mixins that hook a Fabric-only call site and are therefore skipped on NeoForge.
     *
     * <p>NeoForge rewrites the methods these target, so the injection point does not exist there
     * and applying them would abort the boot with a critical injection failure.
     */
    private static final Set<String> fabricOnlyMixins = Set.of(
        // NeoForge routes tooltip assembly through ClientHooks.gatherTooltipComponents instead of
        // Optional.ifPresent, so neither injection point in this mixin exists on NeoForge.
        mixinPackage + ".GuiGraphicsExtractorMixin",
        // NeoForge refactored EntityFluidInteraction.update(Entity, boolean) into a thin delegate
        // and moved the body into a new update(Entity, Predicate<FluidType>) overload. Naming the
        // method without a descriptor makes Mixin resolve both overloads, fail to find the
        // FluidState.getFlow injection point in the delegate, and throw when EntityFluidInteraction
        // is transformed during Bootstrap.bootStrap() -- killing the boot with no crash report.
        mixinPackage + ".EntityFluidInteractionMixin",
        // NeoForge rewrites Entity.updateSwimming() to route the submerged check through its own
        // fluid system, so the isUnderWater() call this mixin targets no longer exists. Entity is
        // loaded during Bootstrap.bootStrap(), so the unsatisfiable injection aborts the boot with
        // no crash report. See EntityUpdateSwimmingMixin for details.
        mixinPackage + ".EntityUpdateSwimmingMixin",
        // NeoForge adds a 5-arg ModelBlockRenderer.shouldRenderFace and deprecates the 4-arg form,
        // so the unqualified name matches both and only one fits the handler signature, which makes
        // Mixin throw InvalidInjectionException. See ModelBlockRendererXrayMixin.
        mixinPackage + ".ModelBlockRendererXrayMixin"
    );

    /**
     * Mirror image of {@link #fabricOnlyMixins}: hooks a NeoForge-only call site.
     *
     * <p>Each entry has a Fabric counterpart in {@code fabricOnlyMixins} that targets the same
     * injection point through the vanilla method descriptor.
     */
    private static final Set<String> neoforgeOnlyMixins = Set.of(
        mixinPackage + ".EntityFluidInteractionNeoForgeMixin",
        mixinPackage + ".ModelBlockRendererXrayNeoForgeMixin"
    );

    private static boolean loaded;

    private static boolean isOriginsPresent;
    private static boolean isIndigoPresent;
    public static boolean isSodiumPresent;
    private static boolean isLithiumPresent;
    public static boolean isIrisPresent;
    private static boolean isVFPPresent;
    private static boolean isBaritonePresent;

    @Override
    public void onLoad(String mixinPackage) {
        if (loaded) return;

        isIndigoPresent = LoaderDetection.isModLoaded("fabric-renderer-indigo");
        isOriginsPresent = LoaderDetection.isModLoaded("origins");
        isSodiumPresent = LoaderDetection.isModLoaded("sodium");
        isLithiumPresent = LoaderDetection.isModLoaded("lithium");
        isIrisPresent = LoaderDetection.isModLoaded("iris");
        isVFPPresent = LoaderDetection.isModLoaded("viafabricplus");
        // Upstream reads this through FabricLoader; LoaderDetection does the equivalent on
        // whichever loader is active, which is required because MixinPlugin lives in common.
        isBaritonePresent = LoaderDetection.isModLoaded("baritone") || LoaderDetection.isModLoaded("baritone-meteor");

        loaded = true;
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!mixinClassName.startsWith(mixinPackage)) {
            throw new RuntimeException("Mixin " + mixinClassName + " is not in the mixin package");
        } else if (mixinClassName.endsWith("PlayerEntityRendererMixin")) {
            return !isOriginsPresent;
        } else if (mixinClassName.startsWith(mixinPackage + ".sodium")) {
            return isSodiumPresent;
        } else if (mixinClassName.startsWith(mixinPackage + ".indigo")) {
            return isIndigoPresent;
        } else if (mixinClassName.startsWith(mixinPackage + ".lithium")) {
            return isLithiumPresent;
        } else if (mixinClassName.startsWith(mixinPackage + ".viafabricplus")) {
            return isVFPPresent;
        } else if (mixinClassName.startsWith(mixinPackage + ".baritone")) {
            return isBaritonePresent;
        } else if (fabricOnlyMixins.contains(mixinClassName)) {
            // See fabricOnlyMixins for why these cannot apply on NeoForge.
            return LoaderDetection.isFabric();
        } else if (neoforgeOnlyMixins.contains(mixinClassName)) {
            // The NeoForge half of a pair whose other half is in fabricOnlyMixins.
            return !LoaderDetection.isFabric();
        }

        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
