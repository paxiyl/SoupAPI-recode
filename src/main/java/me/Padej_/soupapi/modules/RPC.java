package me.Padej_.soupapi.modules;

import me.Padej_.soupapi.compat.SoupLog;
import me.Padej_.soupapi.config.ConfigurableModule;
import me.Padej_.soupapi.discord.DiscordEventHandlers;
import me.Padej_.soupapi.discord.DiscordNative;
import me.Padej_.soupapi.discord.DiscordRPC;
import me.Padej_.soupapi.discord.DiscordRichPresence;
import me.Padej_.soupapi.utils.MC_Tiers;

/**
 * Discord rich presence.
 *
 * <p>Every call into the native goes through {@link DiscordNative}, and every one of them is
 * guarded: rich presence is the one module whose failure mode is a {@link Throwable} rather than a
 * visual glitch, and on PojavLauncher it always fails because Android has no discord-rpc native.
 * A failure disables the module for the session rather than propagating out of the tick handler,
 * which is what crashed the client before.
 */
public class RPC extends ConfigurableModule {
    private static boolean initialized = false;
    /** Set once the native has thrown; stops the module retrying every tick. */
    private static boolean failed = false;
    private static final String DEFAULT_ICON = "icon";
    private static long lastUpdate = 0;
    private static String lastState = "";
    private static final long UPDATE_INTERVAL = 5000L; // 5 секунд
    private static DiscordRichPresence presence;

    public static void init() {
        DiscordRPC rpc = DiscordNative.get();
        if (rpc == null) {
            failed = true;
            return;
        }

        long startTimestamp = System.currentTimeMillis() / 1000;

        DiscordEventHandlers handlers = new DiscordEventHandlers();
        rpc.Discord_Initialize("1363751101794619463", handlers, true, "");

        presence = new DiscordRichPresence();
        presence.state = getState();
        lastState = presence.state;

        presence.startTimestamp = startTimestamp;
        presence.largeImageKey = CONFIG.mctiersEnabled ? MC_Tiers.getMcTiersGameModeIcon() : DEFAULT_ICON;
        presence.largeImageText = mc.getVersion() + " " + mc.getVersion();
        presence.instance = 1;

        presence.button_label_1 = "View On Modrinth";
        presence.button_url_1 = "https://modrinth.com/mod/soup-api";

        rpc.Discord_UpdatePresence(presence);

        initialized = true;
    }

    public static void onTick() {
        if (failed) return;
        try {
            tick();
        } catch (Throwable t) {
            // A native that loaded but then misbehaved, or Discord going away mid-session.
            failed = true;
            initialized = false;
            presence = null;
            SoupLog.error("[RPC] Rich presence failed and has been disabled for this session", t);
        }
    }

    private static void tick() {
        boolean enabled = CONFIG.rpcEnabled;

        if (enabled && !initialized) {
            init();
            if (failed) return;
        }

        if (!enabled && initialized) {
            shutdown();
        }

        if (initialized) {
            DiscordRPC rpc = DiscordNative.get();
            if (rpc == null) {
                failed = true;
                initialized = false;
                return;
            }
            rpc.Discord_RunCallbacks();

            long currentTime = System.currentTimeMillis();
            if (currentTime - lastUpdate >= UPDATE_INTERVAL) {
                String newState = getState();
                String newLargeImageKey = CONFIG.mctiersEnabled ? MC_Tiers.getMcTiersGameModeIcon() : DEFAULT_ICON;
                String newLargeImageText = CONFIG.mctiersEnabled ? CONFIG.mctiersGameMode.name() : mc.getVersion();

                boolean shouldUpdate = false;

                if (!newState.equals(lastState)) {
                    lastState = newState;
                    presence.state = newState;
                    shouldUpdate = true;
                }

                if (!newLargeImageKey.equals(presence.largeImageKey)) {
                    presence.largeImageKey = newLargeImageKey;
                    shouldUpdate = true;
                }

                if (!newLargeImageText.equals(presence.largeImageText)) {
                    presence.largeImageText = newLargeImageText;
                    shouldUpdate = true;
                }

                if (shouldUpdate) {
                    rpc.Discord_UpdatePresence(presence);
                }

                lastUpdate = currentTime;
            }
        }
    }

    public static void shutdown() {
        DiscordRPC rpc = DiscordNative.get();
        if (rpc != null) {
            try {
                rpc.Discord_Shutdown();
            } catch (Throwable t) {
                SoupLog.warn("[RPC] Discord_Shutdown failed", t);
            }
        }
        initialized = false;
        presence = null;
    }

    private static String getState() {
        switch (CONFIG.rpcState) {
            case NAME:
                if (mc.getSession().getProfile() != null && mc.getSession().getProfile().getName() != null) {
                return mc.getSession().getProfile().getName();
                } else {
                return "Bruh -_-. Name is Null";
                }

            case IP:
                if (mc.getNetHandler() != null && mc.getCurrentServerData() != null
                && mc.getCurrentServerData().serverIP != null) {
                return mc.getCurrentServerData().serverIP.toLowerCase();
                } else {
                return "Offline";
                }

            case CUSTOM:
                return CONFIG.rpcCustomStateText != null ? CONFIG.rpcCustomStateText : "No state";
        }
        throw new IllegalStateException("Unexpected value: " + CONFIG.rpcState);
    }

    public enum State {
        NAME, IP, CUSTOM
    }
}


