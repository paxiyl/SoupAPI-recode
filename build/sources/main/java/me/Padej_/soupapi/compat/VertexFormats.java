package me.Padej_.soupapi.compat;

import net.minecraft.client.renderer.vertex.DefaultVertexFormats;

/**
 * Vertex formats, mapping the 1.21.4 names onto MCP's {@link DefaultVertexFormats}.
 *
 * <p>Only the formats the mod actually begins a buffer with are defined. Each records which
 * attributes exist so {@link BufferBuilder} knows which of the chained
 * {@code texture}/{@code color}/{@code light} calls are meaningful and which to drop.
 */
public final class VertexFormats {

    private VertexFormats() {
    }

    public static final VertexFormat POSITION = new VertexFormat(
            DefaultVertexFormats.POSITION, false, false, false, false);

    public static final VertexFormat POSITION_COLOR = new VertexFormat(
            DefaultVertexFormats.POSITION_COLOR, false, true, false, false);

    public static final VertexFormat POSITION_TEXTURE = new VertexFormat(
            DefaultVertexFormats.POSITION_TEX, true, false, false, false);

    public static final VertexFormat POSITION_TEXTURE_COLOR = new VertexFormat(
            DefaultVertexFormats.POSITION_TEX_COLOR, true, true, false, false);

    /**
     * 1.21.4's {@code LINES} is {@code POSITION_COLOR_NORMAL}: the normal drives the line shader's
     * screen-space width expansion. 1.8.9 draws real GL lines, so the normal has no consumer and
     * this maps to plain {@code POSITION_COLOR}; {@link BufferBuilder#normal} becomes a no-op for
     * it, which keeps the ported {@code .normal(...)} calls harmless.
     */
    public static final VertexFormat LINES = new VertexFormat(
            DefaultVertexFormats.POSITION_COLOR, false, true, false, false);

    /**
     * World-space quads that sampled the lightmap in 1.21.4.
     *
     * <p>The mod only used this format for geometry it wanted rendered full-bright anyway, so it
     * maps to {@code POSITION_TEX_COLOR} and the single call site disables lighting instead of
     * writing a lightmap coordinate. Visually identical, and it avoids depending on an element
     * order that differs between versions.
     */
    public static final VertexFormat POSITION_TEXTURE_COLOR_LIGHT = new VertexFormat(
            DefaultVertexFormats.POSITION_TEX_COLOR, true, true, false, false);
}
