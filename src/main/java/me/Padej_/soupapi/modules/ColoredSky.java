package me.Padej_.soupapi.modules;

import me.Padej_.soupapi.compat.BufferBuilder;
import me.Padej_.soupapi.compat.BufferRenderer;
import me.Padej_.soupapi.compat.RenderSystem;
import me.Padej_.soupapi.compat.ShaderKey;
import me.Padej_.soupapi.compat.Tessellator;
import me.Padej_.soupapi.compat.VertexFormat;
import me.Padej_.soupapi.compat.VertexFormats;
import me.Padej_.soupapi.config.ConfigurableModule;
import me.Padej_.soupapi.utils.Palette;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

/**
 * Tints the sky with the palette colour.
 *
 * <p>1.8.9 has no hook for the sky colour — it comes from {@code World.getSkyColor} deep inside
 * {@code RenderGlobal.renderSky} — so this works from the other end instead: the sky is the only
 * thing in the frame that does not write depth, so a full-screen quad drawn with
 * {@code glDepthFunc(GL_GEQUAL)} at the far plane covers exactly the sky and nothing else.
 *
 * <p>That means it tints whatever else sits at the far plane, the sun, moon and clouds included,
 * which is what "coloured sky" is asking for anyway. The option was read by nothing before.
 */
public class ColoredSky extends ConfigurableModule {

    /** Depth the quad sits at, just inside the far plane so float comparison is safe. */
    private static final float FAR = 0.9998f;

    public static void render() {
        // A theme that only recolours the HUD barely registers when a texture pack owns the sky,
        // so Theme World lets the theme tint the sky and the fog as well.
        if (!CONFIG.coloredSkyEnabled && !me.Padej_.soupapi.utils.Themes.world()) return;
        if (mc.theWorld == null) return;

        Color colour = Palette.getColor(0f);

        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        GL11.glOrtho(0.0, 1.0, 1.0, 0.0, -1.0, 1.0);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();

        try {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.depthMask(false);
            RenderSystem.enableDepthTest();
            GlStateManager.depthFunc(GL11.GL_GEQUAL);
            RenderSystem.setShader(ShaderKey.POSITION_COLOR);

            BufferBuilder buffer = Tessellator.getInstance()
                    .begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            int alpha = 150;
            buffer.vertex(0f, 0f, FAR).color(colour.getRed(), colour.getGreen(), colour.getBlue(), alpha);
            buffer.vertex(0f, 1f, FAR).color(colour.getRed(), colour.getGreen(), colour.getBlue(), alpha);
            buffer.vertex(1f, 1f, FAR).color(colour.getRed(), colour.getGreen(), colour.getBlue(), alpha);
            buffer.vertex(1f, 0f, FAR).color(colour.getRed(), colour.getGreen(), colour.getBlue(), alpha);
            BufferRenderer.drawWithGlobalProgram(buffer.end());
        } finally {
            GlStateManager.depthFunc(GL11.GL_LEQUAL);
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
            GlStateManager.matrixMode(GL11.GL_PROJECTION);
            GlStateManager.popMatrix();
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            GlStateManager.popMatrix();
        }
    }
}
