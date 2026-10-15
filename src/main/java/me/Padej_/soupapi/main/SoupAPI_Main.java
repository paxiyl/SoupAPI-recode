package me.Padej_.soupapi.main;

import me.Padej_.soupapi.SoupAPI;
import me.Padej_.soupapi.config.ConfigHolder;
import me.Padej_.soupapi.config.SoupAPI_Config;

/**
 * Compatibility shim for the former Fabric entrypoint.
 *
 * <p>The Forge entry point is {@link SoupAPI}, but a lot of the mod reads
 * {@code SoupAPI_Main.configHolder} and {@code SoupAPI_Main.initTime}. Keeping this as a thin
 * forwarder avoids touching all of those call sites for no behavioural gain.
 */
public final class SoupAPI_Main {

    private SoupAPI_Main() {
    }

    public static ConfigHolder<SoupAPI_Config> configHolder() {
        return SoupAPI.configHolder;
    }

    /**
     * Field-style access, matching how the sources already read it. Kept as a method-backed
     * constant would not see the config registered in pre-init.
     */
    public static ConfigHolder<SoupAPI_Config> getConfigHolder() {
        return SoupAPI.configHolder;
    }
}
