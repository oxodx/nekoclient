/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package nl.oxod.nekoclient;

import nl.oxod.nekoclient.platform.LoaderDetection;
import nl.oxod.nekoclient.platform.LoaderSpecificMixins;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class MixinPlugin implements IMixinConfigPlugin {
  private static final String mixinPackage = "nl.oxod.nekoclient.mixin";

  private static boolean loaded;

  private static boolean isOriginsPresent;
  private static boolean isIndigoPresent;
  public static boolean isSodiumPresent;
  private static boolean isLithiumPresent;
  public static boolean isIrisPresent;
  private static boolean isVFPPresent;
  private static boolean isBaritoneMixinTargetPresent;
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
    // The Baritone mixin hooks baritone.command.defaults.ComeCommand, which is not present in
    // every Baritone build: official Baritone 1.19.0 does not have it, baritone-meteor does.
    // Gating on the mod id alone applied the mixin against a missing class and logged a warning
    // on every launch, so the presence of the target class is what decides.
    isBaritoneMixinTargetPresent = isBaritonePresent
      && LoaderDetection.classExists("baritone.command.defaults.ComeCommand");

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
      return isBaritoneMixinTargetPresent;
    }

    return LoaderSpecificMixins.shouldApply(mixinClassName);
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
