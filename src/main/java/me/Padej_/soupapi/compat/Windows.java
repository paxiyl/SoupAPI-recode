package me.Padej_.soupapi.compat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;

/**
 * Window metrics, replacing {@code MinecraftClient.getWindow()}.
 *
 * <p>1.8.9 has a {@link Window} class but no accessor for it: it is constructed on demand (it was
 * {@code ScaledResolution} before the yarn rename) and recomputes the GUI scale from the current
 * options each time. These helpers construct one per call, which is what vanilla 1.8.9 code does
 * too, and matches the behaviour of re-reading the scale every frame.
 */
public final class Windows {

    private Windows() {
    }

    private static Window window() {
        return new Window(MinecraftClient.getInstance());
    }

    /** GUI-space width, i.e. the coordinate space HUD rendering uses. */
    public static int scaledWidth() {
        return (int) window().getScaledWidth();
    }

    public static int scaledHeight() {
        return (int) window().getScaledHeight();
    }

    public static double scaledWidthD() {
        return window().getScaledWidth();
    }

    public static double scaledHeightD() {
        return window().getScaledHeight();
    }

    public static int scaleFactor() {
        return window().getScaleFactor();
    }

    /** Physical framebuffer width in pixels. */
    public static int width() {
        return window().getWidth();
    }

    public static int height() {
        return window().getHeight();
    }
}
