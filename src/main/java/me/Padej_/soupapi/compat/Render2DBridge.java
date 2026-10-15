package me.Padej_.soupapi.compat;

/**
 * The few primitives {@link DrawContext} needs, expressed with the compat buffer layer.
 *
 * <p>This lives in the compat package rather than in {@code render.Render2D} to avoid a cycle: the
 * render package already depends on the compat package.
 */
final class Render2DBridge {

    private Render2DBridge() {
    }

    /** Matrix-aware equivalent of {@code Gui.fill}, taking an ARGB colour. */
    static void fill(MatrixStack matrices, float x1, float y1, float x2, float y2, int argb) {
        if (x1 > x2) {
            float t = x1;
            x1 = x2;
            x2 = t;
        }
        if (y1 > y2) {
            float t = y1;
            y1 = y2;
            y2 = t;
        }

        Mat4 m = matrices.peek().getPositionMatrix();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(ShaderKey.POSITION_COLOR);

        BufferBuilder buffer = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        buffer.vertex(m, x1, y2, 0f).color(argb);
        buffer.vertex(m, x2, y2, 0f).color(argb);
        buffer.vertex(m, x2, y1, 0f).color(argb);
        buffer.vertex(m, x1, y1, 0f).color(argb);
        BufferRenderer.drawWithGlobalProgram(buffer);

        RenderSystem.disableBlend();
    }

    /** Matrix-aware textured quad with an explicit source region. */
    static void texturedQuad(MatrixStack matrices, float x, float y, float width, float height,
                             float u, float v, float regionWidth, float regionHeight,
                             float textureWidth, float textureHeight) {
        Mat4 m = matrices.peek().getPositionMatrix();
        float u0 = u / textureWidth;
        float u1 = (u + regionWidth) / textureWidth;
        float v0 = v / textureHeight;
        float v1 = (v + regionHeight) / textureHeight;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        BufferBuilder buffer = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        buffer.vertex(m, x, y + height, 0f).texture(u0, v1).color(255, 255, 255, 255);
        buffer.vertex(m, x + width, y + height, 0f).texture(u1, v1).color(255, 255, 255, 255);
        buffer.vertex(m, x + width, y, 0f).texture(u1, v0).color(255, 255, 255, 255);
        buffer.vertex(m, x, y, 0f).texture(u0, v0).color(255, 255, 255, 255);
        BufferRenderer.drawWithGlobalProgram(buffer);

        RenderSystem.disableBlend();
    }
}
