package me.Padej_.soupapi.compat;

/**
 * Replacement for {@code net.minecraft.client.render.VertexFormats}.
 *
 * <p>Only the formats the mod actually begins a buffer with are defined. Each wraps the equivalent
 * 1.8.9 format and records which attributes exist, so {@link BufferBuilder} knows which of the
 * chained {@code texture}/{@code color}/{@code light}/{@code normal} calls are meaningful and which
 * to drop.
 */
public final class VertexFormats {

    private VertexFormats() {
    }

    public static final VertexFormat POSITION = new VertexFormat(
            net.minecraft.client.render.VertexFormats.POSITION, false, false, false, false);

    public static final VertexFormat POSITION_COLOR = new VertexFormat(
            net.minecraft.client.render.VertexFormats.POSITION_COLOR, false, true, false, false);

    public static final VertexFormat POSITION_TEXTURE = new VertexFormat(
            net.minecraft.client.render.VertexFormats.POSITION_TEXTURE, true, false, false, false);

    public static final VertexFormat POSITION_TEXTURE_COLOR = new VertexFormat(
            net.minecraft.client.render.VertexFormats.POSITION_TEXTURE_COLOR, true, true, false, false);

    /**
     * 1.21.4's {@code LINES} is {@code POSITION_COLOR_NORMAL}: the normal drives the line shader's
     * screen-space width expansion. 1.8.9 draws real GL lines, so the normal has no consumer and
     * this maps to plain {@code POSITION_COLOR}; {@link BufferBuilder#normal} becomes a no-op for
     * it, which keeps the eight ported {@code .normal(...)} calls harmless.
     */
    public static final VertexFormat LINES = new VertexFormat(
            net.minecraft.client.render.VertexFormats.POSITION_COLOR, false, true, false, false);

    /**
     * World-space quads that sampled the lightmap in 1.21.4.
     *
     * <p>1.8.9's nearest equivalent, {@code POSITION_TEXTURE2_COLOR}, orders its elements
     * differently, and the mod only ever used this format for geometry it wanted rendered
     * full-bright anyway. So it maps to {@code POSITION_TEXTURE_COLOR} and the single call site
     * disables lighting instead of writing a lightmap coordinate; {@link BufferBuilder#light}
     * becomes a no-op. Visually identical, and it avoids depending on an element order that differs
     * between versions.
     */
    public static final VertexFormat POSITION_TEXTURE_COLOR_LIGHT = new VertexFormat(
            net.minecraft.client.render.VertexFormats.POSITION_TEXTURE_COLOR, true, true, false, false);
}
