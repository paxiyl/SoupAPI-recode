package me.Padej_.soupapi.compat;

import net.minecraft.util.Identifier;

/**
 * Replacement for {@code net.minecraft.client.render.RenderLayer}.
 *
 * <p>In 1.21.4 a render layer is a named bundle of pipeline state plus a vertex format, and
 * geometry for one layer is batched and flushed together. 1.8.9 has no such concept, so a layer
 * here is reduced to what it actually means on the fixed-function pipeline: a draw mode, a vertex
 * format, an optional texture, and a pair of state-setup/teardown steps.
 *
 * <p>{@link VertexConsumerProvider} applies {@link #startDrawing()} before handing out a buffer and
 * {@link #endDrawing()} after flushing it, which reproduces the batching semantics closely enough
 * for the mod's use: each layer is drawn in one pass per frame.
 */
public final class RenderLayer {

    /** How a layer blends. */
    public enum Transparency {
        NONE,
        TRANSLUCENT,
        ADDITIVE
    }

    private final String name;
    private final VertexFormat.DrawMode drawMode;
    private final VertexFormat format;
    private final Identifier texture;
    private final Transparency transparency;
    private final boolean culling;
    private final boolean depthTest;
    private final boolean lightmap;

    private RenderLayer(Builder b) {
        this.name = b.name;
        this.drawMode = b.drawMode;
        this.format = b.format;
        this.texture = b.texture;
        this.transparency = b.transparency;
        this.culling = b.culling;
        this.depthTest = b.depthTest;
        this.lightmap = b.lightmap;
    }

    public String getName() {
        return name;
    }

    public VertexFormat.DrawMode getDrawMode() {
        return drawMode;
    }

    public VertexFormat getFormat() {
        return format;
    }

    /** Applies this layer's state and selects the matching fixed-function "shader". */
    public void startDrawing() {
        if (texture != null) {
            RenderSystem.setShader(format.hasColor ? ShaderKey.POSITION_TEX_COLOR : ShaderKey.POSITION_TEX);
            RenderSystem.setShaderTexture(0, texture);
        } else {
            RenderSystem.setShader(format.hasColor ? ShaderKey.POSITION_COLOR : ShaderKey.POSITION);
        }

        switch (transparency) {
            case TRANSLUCENT:
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                break;
            case ADDITIVE:
                RenderSystem.enableBlend();
                RenderSystem.blendFunc(BlendFactor.SrcFactor.SRC_ALPHA, BlendFactor.DstFactor.ONE);
                break;
            case NONE:
            default:
                RenderSystem.disableBlend();
                break;
        }

        if (culling) {
            RenderSystem.enableCull();
        } else {
            RenderSystem.disableCull();
        }

        if (depthTest) {
            RenderSystem.enableDepthTest();
        } else {
            RenderSystem.disableDepthTest();
        }

        if (!lightmap) {
            // The port renders these layers full-bright rather than sampling the lightmap; see
            // VertexFormats.POSITION_TEXTURE_COLOR_LIGHT for why.
            com.mojang.blaze3d.platform.GlStateManager.disableLighting();
        }
    }

    public void endDrawing() {
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    public static Builder builder(String name, VertexFormat.DrawMode mode, VertexFormat format) {
        return new Builder(name, mode, format);
    }

    /** Mirrors the shape of 1.21.4's {@code MultiPhaseParameters.builder()} chain. */
    public static final class Builder {
        private final String name;
        private final VertexFormat.DrawMode drawMode;
        private final VertexFormat format;
        private Identifier texture;
        private Transparency transparency = Transparency.NONE;
        private boolean culling = true;
        private boolean depthTest = true;
        private boolean lightmap = false;

        private Builder(String name, VertexFormat.DrawMode drawMode, VertexFormat format) {
            this.name = name;
            this.drawMode = drawMode;
            this.format = format;
        }

        public Builder texture(Identifier texture) {
            this.texture = texture;
            return this;
        }

        public Builder transparency(Transparency transparency) {
            this.transparency = transparency;
            return this;
        }

        public Builder cull(boolean culling) {
            this.culling = culling;
            return this;
        }

        public Builder depthTest(boolean depthTest) {
            this.depthTest = depthTest;
            return this;
        }

        public Builder lightmap(boolean lightmap) {
            this.lightmap = lightmap;
            return this;
        }

        public RenderLayer build() {
            return new RenderLayer(this);
        }
    }
}
