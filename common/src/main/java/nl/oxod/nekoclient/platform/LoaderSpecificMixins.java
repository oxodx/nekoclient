package nl.oxod.nekoclient.platform;

import java.util.Set;

/**
 * Loader-specific mixin policy: which mixins may only apply on one loader.
 *
 * <p>NeoForge patches the methods Meteor's mixins hook -- it reroutes tooltip assembly, turns
 * {@code EntityFluidInteraction.update(Entity, boolean)} into a delegate, rewrites
 * {@code Entity.updateSwimming()} to use its own fluid system, and adds a five-argument
 * {@code ModelBlockRenderer.shouldRenderFace}. Where a call site moves, the injection silently
 * stops existing and the client dies during boot with no crash report, because NeoForge dispatches
 * early loading in parallel and swallows the failure.
 *
 * <p>Each entry here therefore has a counterpart for the other loader in the same package, naming
 * the same injection point through that loader's method descriptor. Exactly one of the pair applies.
 *
 * <p>This lives outside {@code MixinPlugin} deliberately. That class is upstream-owned and is edited
 * on every merge from MeteorDevelopment/meteor-client; keeping our additions to a couple of
 * delegating lines there holds its similarity to upstream above git's 50% rename-detection
 * threshold, so the multi-module move from {@code src/} to {@code common/} is still recognised as a
 * rename and merges cleanly instead of degrading to a modify/delete conflict.
 */
public final class LoaderSpecificMixins {
  private static final String MIXIN_PACKAGE = "nl.oxod.nekoclient.mixin";

  /**
   * Applies only on Fabric; the NeoForge counterpart is listed in {@link #neoForgeOnly}.
   */
  private static final Set<String> FABRIC_ONLY = Set.of(
    // NeoForge routes tooltip assembly through ClientHooks.gatherTooltipComponents instead of
    // Optional.ifPresent, so neither injection point in this mixin exists on NeoForge.
    MIXIN_PACKAGE + ".GuiGraphicsExtractorMixin",
    // NeoForge adds update(Entity, Predicate<FluidType>) and turns update(Entity, boolean)
    // into a delegate, so an unqualified "update" matches both overloads and the
    // FluidState.getFlow point is missing from the delegate. EntityFluidInteraction is loaded
    // during Bootstrap.bootStrap(), so this aborts the boot with no crash report.
    MIXIN_PACKAGE + ".EntityFluidInteractionMixin",
    // NeoForge rewrites updateSwimming() so it no longer calls isUnderWater(), routing the
    // submerged check through its own fluid system. Entity loads during Bootstrap, so again
    // this would abort the boot.
    MIXIN_PACKAGE + ".EntityUpdateSwimmingMixin",
    // NeoForge adds a 5-arg shouldRenderFace (BlockPos before BlockState) and deprecates the
    // 4-arg form, so the unqualified name matches both and only one fits the handler
    // signature -- Mixin throws InvalidInjectionException.
    MIXIN_PACKAGE + ".ModelBlockRendererXrayMixin"
  );

  /**
   * Applies only on NeoForge; the Fabric counterpart is listed in {@link #FABRIC_ONLY}.
   */
  private static final Set<String> NEOFORGE_ONLY = Set.of(
    MIXIN_PACKAGE + ".EntityFluidInteractionNeoForgeMixin",
    MIXIN_PACKAGE + ".ModelBlockRendererXrayNeoForgeMixin"
  );

  private LoaderSpecificMixins() {
  }

  /**
   * Whether a loader-specific mixin should be applied on the active loader.
   *
   * <p>Returns {@code true} for any mixin not listed here, so shared mixins apply everywhere.
   */
  public static boolean shouldApply(String mixinClassName) {
    if (FABRIC_ONLY.contains(mixinClassName)) return LoaderDetection.isFabric();
    if (NEOFORGE_ONLY.contains(mixinClassName)) return !LoaderDetection.isFabric();
    return true;
  }
}
