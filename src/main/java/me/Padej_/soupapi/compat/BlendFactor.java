package me.Padej_.soupapi.compat;

import org.lwjgl.opengl.GL11;

/**
 * Blend factor constants, replacing {@code GlStateManager.SrcFactor} / {@code GlStateManager.DstFactor}.
 *
 * <p>Those nested enums were added after 1.8.9, whose {@code GlStateManager.blendFunc} takes raw
 * ints. Keeping them as named constants rather than bare GL ints preserves the readability of the
 * ported call sites and lets {@link RenderSystem#blendFunc(SrcFactor, DstFactor)} stay type-safe.
 *
 * <p>The {@code CONSTANT_*} factors are omitted: LWJGL 2 does not expose them and the mod never
 * used them.
 */
public final class BlendFactor {

    private BlendFactor() {
    }

    public enum SrcFactor {
        ZERO(GL11.GL_ZERO),
        ONE(GL11.GL_ONE),
        SRC_COLOR(GL11.GL_SRC_COLOR),
        ONE_MINUS_SRC_COLOR(GL11.GL_ONE_MINUS_SRC_COLOR),
        DST_COLOR(GL11.GL_DST_COLOR),
        ONE_MINUS_DST_COLOR(GL11.GL_ONE_MINUS_DST_COLOR),
        SRC_ALPHA(GL11.GL_SRC_ALPHA),
        ONE_MINUS_SRC_ALPHA(GL11.GL_ONE_MINUS_SRC_ALPHA),
        DST_ALPHA(GL11.GL_DST_ALPHA),
        ONE_MINUS_DST_ALPHA(GL11.GL_ONE_MINUS_DST_ALPHA),
        SRC_ALPHA_SATURATE(GL11.GL_SRC_ALPHA_SATURATE);

        public final int value;

        SrcFactor(int value) {
            this.value = value;
        }
    }

    public enum DstFactor {
        ZERO(GL11.GL_ZERO),
        ONE(GL11.GL_ONE),
        SRC_COLOR(GL11.GL_SRC_COLOR),
        ONE_MINUS_SRC_COLOR(GL11.GL_ONE_MINUS_SRC_COLOR),
        DST_COLOR(GL11.GL_DST_COLOR),
        ONE_MINUS_DST_COLOR(GL11.GL_ONE_MINUS_DST_COLOR),
        SRC_ALPHA(GL11.GL_SRC_ALPHA),
        ONE_MINUS_SRC_ALPHA(GL11.GL_ONE_MINUS_SRC_ALPHA),
        DST_ALPHA(GL11.GL_DST_ALPHA),
        ONE_MINUS_DST_ALPHA(GL11.GL_ONE_MINUS_DST_ALPHA);

        public final int value;

        DstFactor(int value) {
            this.value = value;
        }
    }
}
