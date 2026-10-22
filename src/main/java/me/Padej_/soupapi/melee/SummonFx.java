package me.Padej_.soupapi.melee;

import me.Padej_.soupapi.config.ConfigurableModule;
import me.Padej_.soupapi.utils.Palette;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

/**
 * The drawing the summons share.
 *
 * <p>Every summon is billboards in the hand pass's own space: the camera is at the origin looking
 * down -Z there, so a quad in the XY plane already faces the player and already moves with the
 * view and with the player's weapon placement. Only the choreography differs between weapons, so
 * the state setup, the quad and the easing curves live here and the weapons' own files are nothing
 * but their timing.
 */
final class SummonFx extends ConfigurableModule {

    /** Where a summon happens, in that space. */
    static final float PX = -0.05f, PY = 0.12f, PZ = -1.35f;
    /** The playhead every summon is written against; a clip's own progress is scaled onto it. */
    static final float END = 2.6f;

    private SummonFx() {
    }

    static Color accent(float position) {
        if (CONFIG.meleeGlowUseTheme) return Palette.getColor(position);
        return position < 0.4f ? new Color(0x9b4dff) : new Color(0xff4fc3);
    }

    /** A weapon's own effect colour at a position, falling back to the theme. */
    static Color weaponColour(MeleeWeapon weapon, String key, float position) {
        if (CONFIG.meleeGlowUseTheme) return Palette.getColor(position);
        Color own = weapon == null ? null : weapon.colour(key);
        return own != null ? own : accent(position);
    }

    static void begin3d() {
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.enableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.depthMask(false);
        GlStateManager.disableAlpha();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    static void end3d() {
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.depthMask(true);
        GlStateManager.enableAlpha();
        GlStateManager.disableBlend();
        GlStateManager.enableCull();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    static void bind(ResourceLocation texture) {
        mc.getTextureManager().bindTexture(texture);
    }

    static WorldRenderer buffer() {
        WorldRenderer w = Tessellator.getInstance().getWorldRenderer();
        w.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        return w;
    }

    static void draw() {
        Tessellator.getInstance().draw();
    }

    /** One of the 4x4 cells of the shared effects atlas, as UVs. */
    static float[] cell(int index) {
        float u0 = (index % 4) * 0.25f, v0 = (index / 4) * 0.25f;
        return new float[]{u0, v0, u0 + 0.25f, v0 + 0.25f};
    }

    static void sprite(WorldRenderer w, float x, float y, float z, float size, float rotDeg,
                       int atlasCell, Color c, float alpha) {
        float[] uv = cell(atlasCell);
        quad(w, x, y, z, size, rotDeg, uv[0], uv[1], uv[2], uv[3], c, alpha);
    }

    static void quad(WorldRenderer w, float x, float y, float z, float size, float rotDeg,
                     float u0, float v0, float u1, float v1, Color c, float alpha) {
        quad(w, x, y, z, size, size, rotDeg, u0, v0, u1, v1,
                c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, alpha);
    }

    static void quad(WorldRenderer w, float x, float y, float z, float size, float rotDeg,
                     float u0, float v0, float u1, float v1, float r, float g, float b, float alpha) {
        quad(w, x, y, z, size, size, rotDeg, u0, v0, u1, v1, r, g, b, alpha);
    }

    /** A camera-facing rectangle, which in this space is simply one in the XY plane. */
    static void quad(WorldRenderer w, float x, float y, float z, float width, float height, float rotDeg,
                     float u0, float v0, float u1, float v1, float r, float g, float b, float alpha) {
        if (alpha <= 0.003f) return;
        alpha = Math.min(1f, alpha);
        float hw = width / 2f, hh = height / 2f;
        double rad = Math.toRadians(rotDeg);
        float cos = (float) Math.cos(rad), sin = (float) Math.sin(rad);
        float ax = cos * hw, ay = sin * hw;     // across
        float bx = -sin * hh, by = cos * hh;    // up
        w.pos(x - ax - bx, y - ay - by, z).tex(u0, v1).color(r, g, b, alpha).endVertex();
        w.pos(x + ax - bx, y + ay - by, z).tex(u1, v1).color(r, g, b, alpha).endVertex();
        w.pos(x + ax + bx, y + ay + by, z).tex(u1, v0).color(r, g, b, alpha).endVertex();
        w.pos(x - ax + bx, y - ay + by, z).tex(u0, v0).color(r, g, b, alpha).endVertex();
    }

    /** A textured band from one point to another: beams, feather trails, drawn lines of light. */
    static void band(WorldRenderer w, float x0, float y0, float x1, float y1, float z,
                     float width, float u0, float v0, float u1, float v1, Color c, float alpha) {
        if (alpha <= 0.003f) return;
        float dx = x1 - x0, dy = y1 - y0;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        if (length < 1.0E-5f) return;
        float nx = -dy / length * width * 0.5f, ny = dx / length * width * 0.5f;
        float r = c.getRed() / 255f, g = c.getGreen() / 255f, b = c.getBlue() / 255f;
        alpha = Math.min(1f, alpha);
        w.pos(x0 - nx, y0 - ny, z).tex(u0, v1).color(r, g, b, alpha).endVertex();
        w.pos(x1 - nx, y1 - ny, z).tex(u1, v1).color(r, g, b, alpha).endVertex();
        w.pos(x1 + nx, y1 + ny, z).tex(u1, v0).color(r, g, b, alpha).endVertex();
        w.pos(x0 + nx, y0 + ny, z).tex(u0, v0).color(r, g, b, alpha).endVertex();
    }

    static float smooth(float t) {
        t = clamp01(t);
        return t * t * (3f - 2f * t);
    }

    /** A triangular pulse, 1 at the centre and 0 at ±width. */
    static float bump(float t, float centre, float width) {
        float d = Math.abs(t - centre) / width;
        return d >= 1f ? 0f : 1f - d * d;
    }

    /** Rises over [a, b] and falls again over [c, d]: the window an element is on screen for. */
    static float window(float t, float a, float b, float c, float d) {
        return smooth((t - a) / Math.max(1.0E-4f, b - a)) * (1f - smooth((t - c) / Math.max(1.0E-4f, d - c)));
    }

    static float clamp01(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }
}
