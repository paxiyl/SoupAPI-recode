package me.Padej_.soupapi.melee;

import me.Padej_.soupapi.config.ConfigurableModule;
import me.Padej_.soupapi.render.SmokeFx;
import me.Padej_.soupapi.utils.Palette;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.Random;

/**
 * The rift the Fractured-Eye Katana is summoned through.
 *
 * <p>Its choreography follows the sword's own summoning in the reference pack — a rift that opens in
 * front of the player, energy that turns around it, the blade sliding out of it into the hand — but
 * nothing here is taken from that pack: the geometry, the textures and the timing curves are this
 * mod's. In five stages, on the summon clip's own playhead so the two can never drift apart:
 *
 * <ol>
 *   <li><b>Rift</b> (0-0.5 s): a fracture tears open and widens, flickering.</li>
 *   <li><b>Runes</b> (0.3-1.9 s): three rings of runes fade in one after another and turn at their own
 *       speeds and tilts; glyphs and glass fragments ride round them.</li>
 *   <li><b>Convergence</b> (0.7-1.05 s): a dark core opens; motes rush into it and it flashes.</li>
 *   <li><b>Emergence</b> (1.05-1.65 s): the clip grows the katana out of the centre, hilt first, while
 *       rift smoke flows round the ring.</li>
 *   <li><b>Release</b> (1.9-2.4 s): shards and sparks burst outward as the rings contract and the core
 *       closes, and the blade settles in the hand.</li>
 * </ol>
 *
 * <p>It is drawn in the hand pass, in the same space as the weapon, so it moves with the view and
 * with the player's own weapon placement. It is a pure function of the playhead: there is no state to
 * leak, nothing outlives the clip, and if the weapon is put away, changed or the world goes, there is
 * simply no summon clip playing and nothing is drawn.
 */
public final class MeleePortal extends ConfigurableModule {

    private static final ResourceLocation RING_0 = SmokeFx.RUNE_RING_0;
    private static final ResourceLocation RING_1 = SmokeFx.RUNE_RING_1;
    private static final ResourceLocation RING_2 = SmokeFx.RUNE_RING_2;
    private static final ResourceLocation SMOKE = new ResourceLocation("soupapi", "textures/vfx/smoke_atlas.png");

    /** Where the rift opens, in the hand pass's eye space; the summon clip draws the blade out of here. */
    private static final float PX = -0.05f, PY = 0.12f, PZ = -1.35f;
    /** Hard stop, whatever the clip says. */
    private static final float END = 2.6f;

    private static long seed = 1L;

    private MeleePortal() {
    }

    /** A new summon: a new pattern of fragments and sparks. */
    static void begin() {
        seed = new Random().nextLong();
    }

    static boolean active(MeleeState state) {
        return CONFIG.meleeSummonEnabled && state.summoning() && state.mainTime() < END;
    }

    /** The rift, rings, core and smoke: drawn before the weapon so the blade comes out in front. */
    static void renderBack(MeleeState state) {
        if (!active(state)) return;
        float t = state.mainTime();
        float size = CONFIG.meleePortalSize / 100f;
        float glow = CONFIG.meleePortalGlow / 100f;
        float intensity = CONFIG.meleePortalIntensity / 100f;
        float density = CONFIG.meleePortalParticles / 100f;
        Palette.Element previous = Palette.scope(Palette.Element.WEAPON);
        begin3d();
        try {
            Random r = new Random(seed);
            Color a = accent(0f), b = accent(0.5f);

            // Rift smoke flows round the ring, dark, under everything else.
            if (CONFIG.meleePortalSmoke && t > 0.3f && t < 2.4f) {
                float life = clamp01((t - 0.3f) / 0.4f) * (1f - clamp01((t - 1.95f) / 0.45f));
                GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                bind(SMOKE);
                WorldRenderer w = buffer();
                int puffs = Math.round(14 * density);
                for (int i = 0; i < puffs; i++) {
                    float ang = r.nextFloat() * 6.283f + t * (0.6f + r.nextFloat() * 0.5f);
                    float rad = (0.45f + r.nextFloat() * 0.25f) * size;
                    int v = r.nextInt(4);
                    quad(w, PX + (float) Math.cos(ang) * rad, PY + (float) Math.sin(ang) * rad * 0.8f, PZ + 0.02f,
                            0.38f * size, ang * 40f, v * 0.25f, 0f, v * 0.25f + 0.25f, 0.5f,
                            0.05f, 0.02f, 0.09f, 0.7f * life * intensity);
                }
                Tessellator.getInstance().draw();
            }

            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

            // Stage 1: the tear.
            float tear = smooth((t - 0.0f) / 0.5f) * (1f - smooth((t - 2.0f) / 0.4f));
            if (tear > 0f) {
                bind(SmokeFx.FRACTURE);
                float flicker = 0.8f + 0.2f * (float) Math.sin(t * 47f);
                WorldRenderer w = buffer();
                quad(w, PX, PY, PZ, 1.1f * size * tear, t * 6f, 0f, 0f, 1f, 1f, a, tear * flicker * glow * intensity);
                Tessellator.getInstance().draw();
            }

            // Stage 2: three rings, staggered in, turning at their own speeds and tilts.
            ResourceLocation[] rings = {RING_0, RING_1, RING_2};
            float[] radius = {1.15f, 0.88f, 0.62f};
            float[] speed = {38f, -64f, 112f};
            float[] tilt = {16f, -22f, 9f};
            float[] in = {0.3f, 0.45f, 0.6f};
            for (int i = 0; i < 3; i++) {
                float shown = smooth((t - in[i]) / 0.35f);
                float out = smooth((t - 1.9f - i * 0.06f) / 0.4f);
                float k = shown * (1f - out);
                if (k <= 0f) continue;
                float s = radius[i] * size * (0.6f + 0.4f * shown) * (1f - 0.75f * out);
                GlStateManager.pushMatrix();
                GlStateManager.translate(PX, PY, PZ);
                GlStateManager.rotate(tilt[i] + 6f * (float) Math.sin(t * 1.3f + i), 1f, 0f, 0f);
                GlStateManager.rotate(tilt[i] * 0.6f, 0f, 1f, 0f);
                bind(rings[i]);
                WorldRenderer w = buffer();
                quad(w, 0f, 0f, 0f, s, t * speed[i], 0f, 0f, 1f, 1f, i == 1 ? b : a, k * glow * intensity);
                Tessellator.getInstance().draw();
                GlStateManager.popMatrix();
            }

            // Glyphs and fragments riding the rings.
            bind(SmokeFx.FX_ATLAS);
            WorldRenderer w = buffer();
            int riders = Math.round(22 * density);
            for (int i = 0; i < riders; i++) {
                int ring = i % 3;
                float shown = smooth((t - in[ring] - 0.1f) / 0.3f) * (1f - smooth((t - 1.85f) / 0.3f));
                if (shown <= 0f) continue;
                float ang = (float) Math.toRadians(t * speed[ring] * 1.4f) + i * 2.399f;
                float rad = radius[ring] * size * 0.44f;
                boolean shard = i % 4 == 0;
                int cell = shard ? 4 + (i % 3) : 8 + (i & 7);
                float u0 = (cell % 4) * 0.25f, v0 = (cell / 4) * 0.25f;
                quad(w, PX + (float) Math.cos(ang) * rad, PY + (float) Math.sin(ang) * rad, PZ + 0.01f,
                        (shard ? 0.09f : 0.07f) * size, t * 90f + i * 40f, u0, v0, u0 + 0.25f, v0 + 0.25f,
                        shard ? Color.WHITE : (i % 2 == 0 ? a : b), shown * intensity);
            }

            // Stage 3: motes converge on the centre, then the pulse.
            if (t > 0.85f && t < 1.15f) {
                float c = clamp01((t - 0.85f) / 0.2f);
                int motes = Math.round(36 * density);
                for (int i = 0; i < motes; i++) {
                    float ang = r.nextFloat() * 6.283f;
                    float rad = (0.75f + r.nextFloat() * 0.5f) * size * (1f - c * c);
                    quad(w, PX + (float) Math.cos(ang) * rad, PY + (float) Math.sin(ang) * rad, PZ,
                            0.05f * size, 0f, 0f, 0f, 0.25f, 0.25f, i % 2 == 0 ? a : b, (1f - c * 0.5f) * intensity);
                }
            }
            Tessellator.getInstance().draw();

            float pulse = bump(t, 1.05f, 0.18f);
            if (pulse > 0f) {
                bind(SmokeFx.GLOW);
                w = buffer();
                quad(w, PX, PY, PZ + 0.02f, 1.8f * size * (0.5f + pulse), 0f, 0f, 0f, 1f, 1f, Color.WHITE, pulse * glow * intensity);
                Tessellator.getInstance().draw();
            }

            // The dark core: drawn normally, so it darkens.
            float core = smooth((t - 0.7f) / 0.35f) * (1f - smooth((t - 1.95f) / 0.35f));
            if (core > 0f) {
                GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                bind(SmokeFx.PORTAL_CORE);
                w = buffer();
                Color tint = new Color(Math.min(255, a.getRed() / 2 + 40), Math.min(255, a.getGreen() / 3 + 10),
                        Math.min(255, a.getBlue() / 2 + 60));
                quad(w, PX, PY, PZ + 0.015f, 0.95f * size * core, -t * 120f, 0f, 0f, 1f, 1f, tint, core * Math.min(1f, intensity));
                Tessellator.getInstance().draw();
            }
        } finally {
            end3d();
            Palette.scope(previous);
        }
    }

    /** Stage 5's burst, drawn after the weapon so the shards fly in front of it. */
    static void renderFront(MeleeState state) {
        if (!active(state)) return;
        float t = state.mainTime();
        float burst = (t - 1.95f) / 0.55f;
        if (burst <= 0f || burst >= 1f) return;
        float size = CONFIG.meleePortalSize / 100f;
        float intensity = CONFIG.meleePortalIntensity / 100f;
        Palette.Element previous = Palette.scope(Palette.Element.WEAPON);
        begin3d();
        try {
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            bind(SmokeFx.FX_ATLAS);
            WorldRenderer w = buffer();
            Random r = new Random(seed ^ 0x5DEECE66DL);
            int n = Math.round(46 * CONFIG.meleePortalParticles / 100f);
            float ease = 1f - (1f - burst) * (1f - burst);
            for (int i = 0; i < n; i++) {
                float ang = r.nextFloat() * 6.283f;
                float speed = (0.5f + r.nextFloat() * 1.1f) * size;
                float dz = 0.2f + r.nextFloat() * 0.6f;
                boolean shard = i % 3 == 0;
                int cell = shard ? 4 + (i % 3) : (i % 3 == 1 ? 2 : 0);
                float u0 = (cell % 4) * 0.25f, v0 = (cell / 4) * 0.25f;
                quad(w, PX + (float) Math.cos(ang) * speed * ease, PY + (float) Math.sin(ang) * speed * ease, PZ + dz * ease,
                        (shard ? 0.11f : 0.07f) * size, i * 37f + burst * 400f, u0, v0, u0 + 0.25f, v0 + 0.25f,
                        shard ? Color.WHITE : accent(r.nextFloat()), (1f - burst) * intensity);
            }
            Tessellator.getInstance().draw();
        } finally {
            end3d();
            Palette.scope(previous);
        }
    }

    // ---- helpers ---------------------------------------------------------------------------------

    private static Color accent(float position) {
        if (CONFIG.meleeGlowUseTheme) return Palette.getColor(position);
        return position < 0.4f ? new Color(0x9b4dff) : new Color(0xff4fc3);
    }

    private static void begin3d() {
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.enableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.depthMask(false);
        GlStateManager.disableAlpha();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    private static void end3d() {
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.depthMask(true);
        GlStateManager.enableAlpha();
        GlStateManager.disableBlend();
        GlStateManager.enableCull();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    private static void bind(ResourceLocation texture) {
        mc.getTextureManager().bindTexture(texture);
    }

    private static WorldRenderer buffer() {
        WorldRenderer w = Tessellator.getInstance().getWorldRenderer();
        w.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        return w;
    }

    private static void quad(WorldRenderer w, float x, float y, float z, float size, float rotDeg,
                             float u0, float v0, float u1, float v1, Color c, float alpha) {
        quad(w, x, y, z, size, rotDeg, u0, v0, u1, v1, c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, alpha);
    }

    /** A camera-facing square in the hand pass's eye space (which already faces the camera). */
    private static void quad(WorldRenderer w, float x, float y, float z, float size, float rotDeg,
                             float u0, float v0, float u1, float v1, float r, float g, float b, float alpha) {
        if (alpha <= 0.003f) return;
        alpha = Math.min(1f, alpha);
        float h = size / 2f;
        double rad = Math.toRadians(rotDeg);
        float c = (float) Math.cos(rad) * h, s = (float) Math.sin(rad) * h;
        w.pos(x - c + s, y - s - c, z).tex(u0, v1).color(r, g, b, alpha).endVertex();
        w.pos(x + c + s, y + s - c, z).tex(u1, v1).color(r, g, b, alpha).endVertex();
        w.pos(x + c - s, y + s + c, z).tex(u1, v0).color(r, g, b, alpha).endVertex();
        w.pos(x - c - s, y - s + c, z).tex(u0, v0).color(r, g, b, alpha).endVertex();
    }

    private static float smooth(float t) {
        t = clamp01(t);
        return t * t * (3f - 2f * t);
    }

    private static float bump(float t, float centre, float width) {
        float d = Math.abs(t - centre) / width;
        return d >= 1f ? 0f : 1f - d * d;
    }

    private static float clamp01(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }
}
