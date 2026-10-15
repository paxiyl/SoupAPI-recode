package me.Padej_.soupapi.compat;

/**
 * Replacement for {@code net.minecraft.client.render.BufferRenderer}.
 *
 * <p>In 1.21.4 {@code drawWithGlobalProgram(BuiltBuffer)} submitted a finished buffer through the
 * currently bound core shader. In 1.8.9 the equivalent is {@code Tessellator.draw()}, which uploads
 * the buffer and issues the draw using fixed-function state — the state having been set up by
 * {@link RenderSystem#setShader} and friends.
 */
public final class BufferRenderer {

    private BufferRenderer() {
    }

    public static void drawWithGlobalProgram(BufferBuilder buffer) {
        if (buffer != null) {
            buffer.end();
        }
        net.minecraft.client.render.Tessellator.getInstance().draw();
    }

    /** Convenience for the common "finish the shared buffer and draw it" case. */
    public static void draw() {
        drawWithGlobalProgram(Tessellator.getInstance().getBuffer());
    }
}
