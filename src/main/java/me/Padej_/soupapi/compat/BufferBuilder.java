package me.Padej_.soupapi.compat;

import java.awt.Color;

/**
 * Replacement for {@code net.minecraft.client.render.BufferBuilder} as the 1.21.4 sources used it.
 *
 * <p>Differences from 1.8.9's class of the same name that this smooths over:
 *
 * <ul>
 *   <li><b>Matrix-aware vertices.</b> 1.21.4 writes {@code vertex(matrix, x, y, z)} and the vertex
 *       is transformed as it is submitted. 1.8.9 has no such overload, so the transform is applied
 *       here on the CPU via {@link Mat4}. See {@link MatrixStack} for why that is preferable to
 *       pushing matrices to GL on Android.</li>
 *   <li><b>Implicit vertex termination.</b> 1.21.4 ends a vertex when the next one starts; 1.8.9
 *       requires an explicit {@code next()}. This class tracks the in-progress vertex and flushes
 *       it at the right moment.</li>
 *   <li><b>Attribute order.</b> 1.8.9 expects attributes written in the format's declared order.
 *       Rather than depend on call order, attributes are accumulated and written in the correct
 *       order when the vertex is flushed, so a {@code .color(...).texture(...)} chain works as well
 *       as {@code .texture(...).color(...)}.</li>
 *   <li><b>Shader colour.</b> 1.8.9 has no shader colour uniform, and {@code glColor} is ignored
 *       once a colour array is bound. {@link RenderSystem#setShaderColor} is therefore multiplied
 *       into each vertex colour here, reproducing 1.21.4's behaviour.</li>
 * </ul>
 */
public final class BufferBuilder implements VertexConsumer {

    private final net.minecraft.client.render.BufferBuilder delegate;
    private VertexFormat format;

    private boolean vertexOpen;
    private double px, py, pz;
    private float u, v;
    private int r = 255, g = 255, b = 255, a = 255;
    private int lightU, lightV;
    private float nx, ny, nz;

    BufferBuilder(net.minecraft.client.render.BufferBuilder delegate) {
        this.delegate = delegate;
    }

    void beginInternal(VertexFormat.DrawMode mode, VertexFormat format) {
        this.format = format;
        this.vertexOpen = false;
        resetAttributes();
        delegate.begin(mode.glMode, format.delegate);
    }

    private void resetAttributes() {
        u = 0f;
        v = 0f;
        r = 255;
        g = 255;
        b = 255;
        a = 255;
        lightU = 240;
        lightV = 240;
        nx = 0f;
        ny = 0f;
        nz = 1f;
    }

    /** Writes the pending vertex to the underlying builder in the format's element order. */
    private void flushVertex() {
        if (!vertexOpen) return;
        delegate.vertex(px, py, pz);
        if (format.hasTexture) {
            delegate.texture(u, v);
        }
        if (format.hasColor) {
            delegate.color(r, g, b, a);
        }
        if (format.hasLight) {
            delegate.texture2(lightU, lightV);
        }
        if (format.hasNormal) {
            delegate.normal(nx, ny, nz);
        }
        delegate.next();
        vertexOpen = false;
        resetAttributes();
    }

    // ---- vertex submission -----------------------------------------------------------------

    public BufferBuilder vertex(Mat4 matrix, float x, float y, float z) {
        flushVertex();
        if (matrix == null) {
            px = x;
            py = y;
            pz = z;
        } else {
            px = matrix.transformX(x, y, z);
            py = matrix.transformY(x, y, z);
            pz = matrix.transformZ(x, y, z);
        }
        vertexOpen = true;
        return this;
    }

    public BufferBuilder vertex(Mat4 matrix, double x, double y, double z) {
        return vertex(matrix, (float) x, (float) y, (float) z);
    }

    public BufferBuilder vertex(MatrixStack matrices, float x, float y, float z) {
        return vertex(matrices.peek().getPositionMatrix(), x, y, z);
    }

    public BufferBuilder vertex(double x, double y, double z) {
        flushVertex();
        px = x;
        py = y;
        pz = z;
        vertexOpen = true;
        return this;
    }

    public BufferBuilder vertex(float x, float y, float z) {
        return vertex((double) x, (double) y, (double) z);
    }

    // ---- attributes ------------------------------------------------------------------------

    public BufferBuilder texture(float u, float v) {
        this.u = u;
        this.v = v;
        return this;
    }

    public BufferBuilder texture(double u, double v) {
        return texture((float) u, (float) v);
    }

    /** Takes a packed ARGB int, matching 1.21.4's {@code VertexConsumer.color(int)}. */
    public BufferBuilder color(int argb) {
        int alpha = (argb >>> 24) & 0xFF;
        int red = (argb >> 16) & 0xFF;
        int green = (argb >> 8) & 0xFF;
        int blue = argb & 0xFF;
        return color(red, green, blue, alpha);
    }

    public BufferBuilder color(int red, int green, int blue, int alpha) {
        // Fold in the emulated shader colour (see RenderSystem#setShaderColor).
        if (RenderSystem.shaderColorIsIdentity()) {
            this.r = clamp255(red);
            this.g = clamp255(green);
            this.b = clamp255(blue);
            this.a = clamp255(alpha);
        } else {
            this.r = clamp255(Math.round(red * RenderSystem.shaderRed()));
            this.g = clamp255(Math.round(green * RenderSystem.shaderGreen()));
            this.b = clamp255(Math.round(blue * RenderSystem.shaderBlue()));
            this.a = clamp255(Math.round(alpha * RenderSystem.shaderAlpha()));
        }
        return this;
    }

    public BufferBuilder color(float red, float green, float blue, float alpha) {
        return color(Math.round(red * 255f), Math.round(green * 255f),
                Math.round(blue * 255f), Math.round(alpha * 255f));
    }

    public BufferBuilder color(Color color) {
        return color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
    }

    /**
     * Lightmap coordinate. No-op unless the active format carries one — see
     * {@link VertexFormats#POSITION_TEXTURE_COLOR_LIGHT}.
     */
    public BufferBuilder light(int packed) {
        this.lightU = packed & 0xFFFF;
        this.lightV = (packed >> 16) & 0xFFFF;
        return this;
    }

    public BufferBuilder light(int u, int v) {
        this.lightU = u;
        this.lightV = v;
        return this;
    }

    public BufferBuilder normal(float x, float y, float z) {
        this.nx = x;
        this.ny = y;
        this.nz = z;
        return this;
    }

    /**
     * Overlay (damage flash) coordinate. 1.8.9 has no overlay texture stage, so this is accepted
     * and dropped to keep ported call sites intact.
     */
    public BufferBuilder overlay(int packed) {
        return this;
    }

    /** Terminates the final vertex. Pair with {@link BufferRenderer#drawWithGlobalProgram}. */
    public BufferBuilder end() {
        flushVertex();
        return this;
    }

    net.minecraft.client.render.BufferBuilder delegate() {
        return delegate;
    }

    private static int clamp255(int value) {
        return value < 0 ? 0 : (value > 255 ? 255 : value);
    }
}
