package me.Padej_.soupapi.compat;

/**
 * Replacement for 1.21.4's {@code BufferRenderer}.
 *
 * <p>In 1.21.4 {@code drawWithGlobalProgram(BuiltBuffer)} submitted a finished buffer through the
 * currently bound core shader. In 1.8.9 the equivalent is {@code Tessellator.draw()}, which
 * uploads the buffer and issues the draw using fixed-function state — the state having been set up
 * by {@link RenderSystem#setShader} and friends.
 */
public final class BufferRenderer {

    private BufferRenderer() {
    }

    public static void drawWithGlobalProgram(BufferBuilder buffer) {
        if (buffer != null) {
            buffer.end();
        }
        try {
            net.minecraft.client.renderer.Tessellator.getInstance().draw();
        } catch (IllegalStateException notBuilding) {
            // "Not building!" - a draw with no matching begin, which 1.8.9 treats as fatal. It is
            // not worth taking the client down for, and swallowing it here keeps the shared buffer
            // in a sane state for whatever draws next.
            if (REPORTED_NOT_BUILDING.compareAndSet(false, true)) {
                SoupLog.warn("[SoupAPI] draw() without a matching begin(); skipped", notBuilding);
            }
        } finally {
            RenderSystem.afterDraw();
        }
    }

    private static final java.util.concurrent.atomic.AtomicBoolean REPORTED_NOT_BUILDING =
            new java.util.concurrent.atomic.AtomicBoolean();

    /** Convenience for the common "finish the shared buffer and draw it" case. */
    public static void draw() {
        drawWithGlobalProgram(Tessellator.getInstance().getBuffer());
    }
}
