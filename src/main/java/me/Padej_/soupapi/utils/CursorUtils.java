package me.Padej_.soupapi.utils;

/**
 * Mouse-cursor shape control.
 *
 * <p>Every method is a no-op on this version, deliberately.
 *
 * <p>1.21.4 changed the cursor through GLFW's standard cursor handles
 * ({@code glfwCreateStandardCursor} / {@code glfwSetCursor}). 1.8.9 runs on LWJGL 2, which has no
 * standard-cursor API at all — {@code org.lwjgl.input.Mouse.setNativeCursor} only accepts a cursor
 * built from raw pixel data, and there is no way to ask the platform for its arrow, hand or resize
 * cursor. Shipping hand-drawn bitmaps for three cursors would look wrong on every desktop and is
 * meaningless on PojavLauncher, which is touch-driven and has no pointer to reshape.
 *
 * <p>The class is kept rather than deleted so the HUD-position and settings screens still compile
 * and read naturally; the only loss is that the pointer does not change shape over draggable
 * elements.
 */
public class CursorUtils {

    private CursorUtils() {
    }

    public static void setDefaultCursor() {
    }

    public static void setHandCursor() {
    }

    public static void setResizeCursor() {
    }

    public static void destroy() {
    }
}
