package me.Padej_.soupapi.discord;

import com.sun.jna.Native;
import me.Padej_.soupapi.compat.Caps;
import me.Padej_.soupapi.compat.SoupLog;

/**
 * Loads the discord-rpc native once, tolerating its absence.
 *
 * <p>Rich presence needs two things the mod cannot assume: the {@code discord-rpc} shared library
 * for this platform, and a running Discord desktop client to open its IPC socket. Neither exists
 * under PojavLauncher — Android has no build of the native and the Discord Android app exposes no
 * IPC endpoint — so on Android the load is not even attempted. Anywhere else it is attempted once
 * and, if it fails, rich presence stays off for the session instead of taking the game down.
 *
 * <p>The attempt is made at most once: {@code Native.loadLibrary} searches the whole resource path
 * before it gives up, which is far too slow to repeat every tick.
 */
public final class DiscordNative {

    private static boolean attempted;
    private static DiscordRPC instance;
    private static String unavailableReason;

    private DiscordNative() {
    }

    /** The binding, or {@code null} if the native is unavailable here. Never throws. */
    public static synchronized DiscordRPC get() {
        if (attempted) return instance;
        attempted = true;

        if (Caps.isAndroid()) {
            unavailableReason = "Android has no discord-rpc native and no Discord IPC socket";
            SoupLog.info("[RPC] Rich presence disabled: " + unavailableReason);
            return null;
        }

        try {
            instance = (DiscordRPC) Native.loadLibrary("discord-rpc", DiscordRPC.class);
        } catch (Throwable t) {
            // UnsatisfiedLinkError when the library is missing for this OS/architecture,
            // NoClassDefFoundError if JNA itself failed to initialise.
            instance = null;
            unavailableReason = String.valueOf(t.getMessage());
            SoupLog.warn("[RPC] discord-rpc native unavailable; rich presence disabled for this "
                    + "session (" + t.getClass().getSimpleName() + ")");
        }
        return instance;
    }

    public static boolean isAvailable() {
        return get() != null;
    }

    /** Why rich presence is off, or {@code null} if it is not. */
    public static String unavailableReason() {
        get();
        return unavailableReason;
    }
}
