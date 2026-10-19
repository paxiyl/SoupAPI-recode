package me.Padej_.soupapi.modules;

import me.Padej_.soupapi.config.ConfigurableModule;
import me.Padej_.soupapi.utils.Palette;
import me.Padej_.soupapi.utils.SkyStyle;
import me.Padej_.soupapi.utils.Themes;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

/**
 * A sky that actually replaces the sky.
 *
 * <p>Not a tint over the vanilla sky — an opaque, moving, themed sky drawn over it. Each style owns
 * a seamless texture ({@code assets/soupapi/textures/sky/}); two layers of it are scrolled across
 * the view, one slow and one faster, so the sky drifts like cloud and parallaxes as you turn. It is
 * drawn at the far plane with {@code GL_LEQUAL}, so it paints the sky, sun, moon and clouds and
 * leaves the terrain alone, and the base layer is opaque so the old sky is gone, not shining
 * through. A theme recolours the same texture by multiplying it, so one texture serves a family of
 * themes in different colours.
 */
public class AnimatedSky extends ConfigurableModule {

    /** Far plane in the ortho below (window depth ≈ 1), so the sky sits behind the world. */
    private static final float FAR = -0.9998f;

    private static final String BASE = "textures/sky/";

    /** The style to draw: the player's explicit pick, or the theme's. */
    public static SkyStyle style() {
        if (CONFIG.skyStyle != SkyStyle.PLAIN) return CONFIG.skyStyle;
        if (!CONFIG.coloredSkyEnabled) return SkyStyle.PLAIN;
        return Themes.skyStyle();
    }

    /** True when the sky pass has something to draw. */
    public static boolean active() {
        return style() != SkyStyle.PLAIN;
    }

    /** The seamless texture a style scrolls. Several styles share one. */
    private static ResourceLocation texture(SkyStyle style) {
        String name;
        switch (style) {
            case VOID_RIFT:   name = "void"; break;
            case CODE_FALL:   name = "matrix"; break;
            case CIRCUIT_SKY:
            case GRID:
            case HEX_GRID:
            case SCANLINES:
            case SPEED_LINES: name = "cyber"; break;
            case EMBER_SKY:
            case LAVA_FLOW:   name = "hellfire"; break;
            case AURORA:
            case SILK:        name = "aurora"; break;
            case OCEAN_DEPTH:
            case RIPPLE:      name = "ocean"; break;
            case SUNSET:      name = "sunset"; break;
            case STORM_FRONT: name = "storm"; break;
            case ECLIPSE:     name = "void"; break;
            default:          name = "nebula"; break;   // NEBULA, PLASMA, BANDS, VORTEX, LATTICE
        }
        return new ResourceLocation("soupapi", BASE + name + ".png");
    }

    /** True for styles whose motion reads best as a vertical fall rather than a drift. */
    private static boolean vertical(SkyStyle style) {
        switch (style) {
            case CODE_FALL:
            case EMBER_SKY:
            case LAVA_FLOW:
            case OCEAN_DEPTH:
            case SCANLINES:
                return true;
            default:
                return false;
        }
    }

    public static void render() {
        SkyStyle style = style();
        if (style == SkyStyle.PLAIN || mc.theWorld == null) return;
        Entity view = mc.getRenderViewEntity();
        if (view == null) return;

        float time = (System.currentTimeMillis() % 3600000L) / 1000f
                * (CONFIG.skySpeed / 100f) * style.speed();
        // Parallax from where the player is looking: turning scrolls the sky sideways, looking up or
        // down scrolls it vertically, so it reads as a sky wrapped around the world.
        float yaw = view.rotationYaw;
        float pitch = view.rotationPitch;
        float lookU = yaw / 90f;
        float lookV = pitch / 180f;

        Color tint = CONFIG.coloredSkyUseTheme ? Palette.getColor(0f) : new Color(CONFIG.coloredSkyColor);
        // Keep most of the texture's own colour; the theme only leans it toward its hue.
        float tr = (tint.getRed() / 255f) * 0.5f + 0.5f;
        float tg = (tint.getGreen() / 255f) * 0.5f + 0.5f;
        float tb = (tint.getBlue() / 255f) * 0.5f + 0.5f;

        float scale = Math.max(0.2f, CONFIG.skyScale / 100f);
        boolean vert = vertical(style);
        float drift = 0.02f * time;
        float driftFast = 0.05f * time;

        ResourceLocation tex = texture(style);

        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        GL11.glOrtho(0.0, 1.0, 1.0, 0.0, -1.0, 1.0);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();

        try {
            GlStateManager.disableLighting();
            GlStateManager.disableAlpha();
            GlStateManager.depthMask(false);
            GlStateManager.enableDepth();
            GlStateManager.depthFunc(GL11.GL_LEQUAL);
            GlStateManager.enableTexture2D();
            mc.getTextureManager().bindTexture(tex);
            repeatWrap();

            // Base layer: opaque, so the vanilla sky is gone.
            GlStateManager.disableBlend();
            float spanA = 1.7f * scale, spanAV = 1.0f * scale;
            layer(spanA, spanAV,
                    lookU + (vert ? 0f : drift), lookV + (vert ? drift : 0f),
                    tr, tg, tb, 1f);

            // A second, faster, additive layer of the same sky for depth and motion.
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            float spanB = 2.6f * scale, spanBV = 1.5f * scale;
            layer(spanB, spanBV,
                    -lookU * 0.6f + (vert ? 0f : driftFast), -lookV * 0.6f + (vert ? driftFast : 0f),
                    tr, tg, tb, 0.35f);
        } finally {
            GlStateManager.depthFunc(GL11.GL_LEQUAL);
            GlStateManager.depthMask(true);
            GlStateManager.disableBlend();
            GlStateManager.enableAlpha();
            GlStateManager.enableLighting();
            GlStateManager.color(1f, 1f, 1f, 1f);
            GlStateManager.matrixMode(GL11.GL_PROJECTION);
            GlStateManager.popMatrix();
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            GlStateManager.popMatrix();
        }
    }

    /** One full-screen textured layer; (spanU, spanV) is how many tiles span the view. */
    private static void layer(float spanU, float spanV, float offU, float offV,
                              float r, float g, float b, float a) {
        GlStateManager.color(r, g, b, a);
        GL11.glBegin(GL11.GL_QUADS);
        corner(0f, 0f, offU, offV);
        corner(1f, 0f, offU + spanU, offV);
        corner(1f, 1f, offU + spanU, offV + spanV);
        corner(0f, 1f, offU, offV + spanV);
        GL11.glEnd();
    }

    private static void corner(float x, float y, float u, float v) {
        GL11.glTexCoord2f(u, v);
        GL11.glVertex3f(x, y, FAR);
    }

    private static void repeatWrap() {
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
    }
}
