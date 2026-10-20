package me.Padej_.soupapi.compat;

/**
 * Replacement for {@code net.minecraft.client.render.VertexConsumerProvider}.
 *
 * <p>1.8.9 has a single shared tessellator and draws immediately, so this does not batch across
 * layers: asking for a buffer for a different layer flushes the previous one first. Call
 * {@link #draw()} when a render pass finishes to flush whatever is still open.
 */
public final class VertexConsumerProvider {

    private static final VertexConsumerProvider IMMEDIATE = new VertexConsumerProvider();

    private RenderLayer active;

    private VertexConsumerProvider() {
    }

    public static VertexConsumerProvider immediate() {
        return IMMEDIATE;
    }

    public VertexConsumer getBuffer(RenderLayer layer) {
        if (active != null && active != layer) {
            draw();
        }
        if (active != layer) {
            active = layer;
            layer.startDrawing();
            return Tessellator.getInstance().begin(layer.getDrawMode(), layer.getFormat());
        }
        return Tessellator.getInstance().getBuffer();
    }

    /** Flushes the open layer, if any. */
    public void draw() {
        if (active == null) return;
        RenderLayer layer = active;
        active = null;
        BufferRenderer.drawWithGlobalProgram(Tessellator.getInstance().getBuffer());
        layer.endDrawing();
    }
}
