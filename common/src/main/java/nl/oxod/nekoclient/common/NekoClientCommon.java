package nl.oxod.nekoclient.common;

/**
 * Shared constants for the multi-loader build.
 *
 * Code that is loader-agnostic should eventually live in this module. For now the
 * existing logic lives entirely in the {@code fabric} module.
 */
public final class NekoClientCommon {
    public static final String MOD_ID = "nekoclient";
    public static final String MOD_NAME = "NekoClient";

    private NekoClientCommon() {}
}