package me.Padej_.soupapi.discord;

import com.sun.jna.Library;

/**
 * The discord-rpc native binding.
 *
 * <p>This interface deliberately holds <b>no</b> {@code INSTANCE} field. A JNA binding written the
 * usual way ({@code DiscordRPC INSTANCE = Native.loadLibrary(...)}) puts the library load in the
 * interface's static initialiser, so the first touch of the type throws {@link UnsatisfiedLinkError}
 * wherever the native is absent — and every touch after that throws {@code NoClassDefFoundError},
 * because a failed initialiser poisons the class for the rest of the JVM's life. On PojavLauncher
 * there is no {@code linux-aarch64/libdiscord-rpc.so} (and no Discord IPC socket to talk to), so
 * that load always fails and the error propagated out of the client-tick handler and crashed the
 * game.
 *
 * <p>{@link DiscordNative} owns the load instead, behind a guard that catches it.
 */
public interface DiscordRPC extends Library {
    void Discord_Initialize(String applicationId, DiscordEventHandlers handlers, boolean autoRegister, String optionalSteamId);
    void Discord_Shutdown();
    void Discord_RunCallbacks();
    void Discord_UpdatePresence(DiscordRichPresence presence);
}
