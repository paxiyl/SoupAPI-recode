package me.Padej_.soupapi.compat;

import org.lwjgl.opengl.GL11;

/**
 * Replacement for {@code net.minecraft.client.render.VertexFormat}, carrying the nested
 * {@link DrawMode} enum the 1.21.4 sources use at every {@code begin(...)} call site.
 *
 * <p>1.8.9 <i>has</i> a class by this name, but its {@code begin} takes a raw GL primitive int and
 * it has no {@code DrawMode}. Rather than rewrite 17 call sites, this compat class keeps the newer
 * shape and holds the 1.8.9 format it maps onto, so ported files change only their imports.
 */
public final class VertexFormat {

    /** Replacement for {@code VertexFormat.DrawMode}. */
    public enum DrawMode {
        QUADS(GL11.GL_QUADS),
        TRIANGLES(GL11.GL_TRIANGLES),
        TRIANGLE_STRIP(GL11.GL_TRIANGLE_STRIP),
        TRIANGLE_FAN(GL11.GL_TRIANGLE_FAN),
        /**
         * In 1.21.4 this meant "lines expanded to screen-space quads by the core line shader".
         * 1.8.9 has no such shader, so it becomes a real {@code GL_LINES} draw. Callers that need a
         * guaranteed stroke thickness must build geometry themselves — gl4es clamps
         * {@code glLineWidth} to 1.0 (see {@link Caps#thickLinesSupported()}).
         */
        LINES(GL11.GL_LINES),
        DEBUG_LINES(GL11.GL_LINES),
        DEBUG_LINE_STRIP(GL11.GL_LINE_STRIP);

        public final int glMode;

        DrawMode(int glMode) {
            this.glMode = glMode;
        }
    }

    final net.minecraft.client.render.VertexFormat delegate;
    final boolean hasColor;
    final boolean hasTexture;
    final boolean hasLight;
    final boolean hasNormal;

    VertexFormat(net.minecraft.client.render.VertexFormat delegate,
                 boolean hasTexture, boolean hasColor, boolean hasLight, boolean hasNormal) {
        this.delegate = delegate;
        this.hasTexture = hasTexture;
        this.hasColor = hasColor;
        this.hasLight = hasLight;
        this.hasNormal = hasNormal;
    }
}
