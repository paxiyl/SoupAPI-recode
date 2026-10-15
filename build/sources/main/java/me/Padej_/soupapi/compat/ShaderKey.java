package me.Padej_.soupapi.compat;

/**
 * Replacement for {@code net.minecraft.client.gl.ShaderProgramKeys}.
 *
 * <p>1.8.9 has no programmable core pipeline, so a "shader" selection is really a statement about
 * which vertex attributes are in play. {@link RenderSystem#setShader} translates each key into the
 * equivalent fixed-function state — mainly whether texturing is on, which matters because leaving a
 * texture bound while drawing untextured geometry tints it with whatever was last bound.
 */
public enum ShaderKey {
    /** Untextured, colour comes from {@code glColor}. */
    POSITION(false),
    /** Untextured, per-vertex colour. */
    POSITION_COLOR(false),
    /** Textured, colour from {@code glColor}. */
    POSITION_TEX(true),
    /** Textured with per-vertex colour. */
    POSITION_TEX_COLOR(true),
    /** Textured, per-vertex colour, with a lightmap coordinate. */
    POSITION_TEX_COLOR_LIGHT(true),
    /** Line rendering; untextured. */
    RENDERTYPE_LINES(false),
    /** The entity "energy swirl" overlay; textured and additive. */
    ENERGY_SWIRL(true);

    private final boolean textured;

    ShaderKey(boolean textured) {
        this.textured = textured;
    }

    public boolean isTextured() {
        return textured;
    }
}
