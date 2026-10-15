package me.Padej_.soupapi.compat;

import net.minecraft.client.MinecraftClient;

/**
 * The frame's camera, as a process-wide singleton.
 *
 * <p>1.21.4 reached the camera through {@code mc.gameRenderer.getCamera()}. 1.8.9's
 * {@code GameRenderer} has no such accessor — its camera is a static utility holding only the
 * interpolated eye position — so the port keeps one {@link Camera} that the world-render hook
 * refreshes at the start of every frame and that the ~58 call sites read from here.
 *
 * <p>If nothing has refreshed it yet (for example a HUD element drawn before the first world
 * frame), it falls back to the current camera entity with a zero tick delta rather than returning
 * null.
 */
public final class Cameras {

    private Cameras() {
    }

    private static final Camera CAMERA = new Camera();
    private static boolean updatedThisSession;

    /** Called once per frame by the world-render hook, before any mod rendering. */
    public static void update(net.minecraft.entity.Entity focused, float tickDelta) {
        CAMERA.update(focused, tickDelta);
        updatedThisSession = true;
    }

    public static Camera get() {
        if (!updatedThisSession) {
            MinecraftClient mc = MinecraftClient.getInstance();
            CAMERA.update(mc.getCameraEntity() != null ? mc.getCameraEntity() : mc.player, 0f);
        }
        return CAMERA;
    }
}
