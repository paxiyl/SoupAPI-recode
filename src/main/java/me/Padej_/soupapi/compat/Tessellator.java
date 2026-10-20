package me.Padej_.soupapi.compat;

/**
 * Replacement for 1.21.4's {@code Tessellator} shape.
 *
 * <p>Wraps MCP 1.8.9's singleton tessellator so that {@code Tessellator.getInstance().begin(mode,
 * format)} returns a {@link BufferBuilder} with the newer fluent API. As in 1.8.9 (and 1.21.4),
 * there is exactly one shared buffer, so draws must not be nested: finish one with
 * {@link BufferRenderer#drawWithGlobalProgram} before beginning the next.
 */
public final class Tessellator {

    private static final Tessellator INSTANCE = new Tessellator();

    private final BufferBuilder buffer = new BufferBuilder(
            net.minecraft.client.renderer.Tessellator.getInstance().getWorldRenderer());

    private Tessellator() {
    }

    public static Tessellator getInstance() {
        return INSTANCE;
    }

    public BufferBuilder begin(VertexFormat.DrawMode mode, VertexFormat format) {
        try {
            buffer.beginInternal(mode, format);
        } catch (IllegalStateException alreadyBuilding) {
            // The shared buffer was left mid-build - by the mod after a module threw part-way
            // through a draw, or by another mod. 1.8.9 makes that fatal for whoever begins next
            // (and the next mod to begin is usually not the one that leaked it, which is how this
            // surfaced as a crash inside MbMotionblur). Discard the orphaned vertices and retry.
            discardPending();
            buffer.beginInternal(mode, format);
        }
        return buffer;
    }

    /**
     * Throws away a half-built buffer without drawing it.
     *
     * <p>Discards rather than draws: the orphaned vertices belong to an aborted draw, so submitting
     * them would paint garbage with whatever state happens to be current.
     */
    public static void discardPending() {
        net.minecraft.client.renderer.WorldRenderer wr =
                net.minecraft.client.renderer.Tessellator.getInstance().getWorldRenderer();
        try {
            wr.finishDrawing();
        } catch (Throwable ignored) {
            // Already finished; nothing to discard.
        }
        try {
            wr.reset();
        } catch (Throwable ignored) {
        }
    }

    /** The shared buffer, for code that wants to keep appending to an in-progress draw. */
    public BufferBuilder getBuffer() {
        return buffer;
    }
}
