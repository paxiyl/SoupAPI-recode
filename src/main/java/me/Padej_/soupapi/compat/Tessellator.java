package me.Padej_.soupapi.compat;

/**
 * Replacement for {@code net.minecraft.client.render.Tessellator}'s 1.21.4 shape.
 *
 * <p>Wraps 1.8.9's singleton tessellator so that {@code Tessellator.getInstance().begin(mode,
 * format)} returns a {@link BufferBuilder} with the newer fluent API. As in 1.8.9 (and 1.21.4),
 * there is exactly one shared buffer, so draws must not be nested: finish one with
 * {@link BufferRenderer#drawWithGlobalProgram} before beginning the next.
 */
public final class Tessellator {

    private static final Tessellator INSTANCE = new Tessellator();

    private final BufferBuilder buffer =
            new BufferBuilder(net.minecraft.client.render.Tessellator.getInstance().getBuffer());

    private Tessellator() {
    }

    public static Tessellator getInstance() {
        return INSTANCE;
    }

    public BufferBuilder begin(VertexFormat.DrawMode mode, VertexFormat format) {
        buffer.beginInternal(mode, format);
        return buffer;
    }

    /** The shared buffer, for code that wants to keep appending to an in-progress draw. */
    public BufferBuilder getBuffer() {
        return buffer;
    }
}
