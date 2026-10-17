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
import me.Padej_.soupapi.utils.SkyStyle;
import me.Padej_.soupapi.utils.Themes;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

/**
 * A sky that moves.
 *
 * <p>Drawn as a grid of quads over the whole screen at the far plane, each corner coloured by a
 * function of where it is and what time it is. That is the one way to get a moving pattern across
 * the sky on this version: there is no shader to write a noise function into, and gl4es would not
 * run one if there were. A 40 by 24 grid is under a thousand quads, which costs nothing next to a
 * single chunk, and because the colour is evaluated per corner and interpolated across each quad
 * the result is smooth rather than blocky.
 *
 * <p>It sits at the far plane with {@code GL_GEQUAL}, so it paints whatever the world did not —
 * the sky, the sun, the clouds — and leaves the terrain alone.
 */
public class AnimatedSky extends ConfigurableModule {

    private static final int COLUMNS = 40;
    private static final int ROWS = 24;
    private static final float FAR = 0.9998f;

    /** The style to draw: the player's choice, or the theme's. */
    public static SkyStyle style() {
        if (!CONFIG.coloredSkyEnabled) return SkyStyle.PLAIN;
        if (CONFIG.skyStyle != SkyStyle.PLAIN) return CONFIG.skyStyle;
        return Themes.skyStyle();
    }

    public static void render() {
        SkyStyle style = style();
        if (style == SkyStyle.PLAIN) return;
        if (mc.theWorld == null) return;

        float time = (System.currentTimeMillis() % 3600000L) / 1000f
                * (CONFIG.skySpeed / 100f) * style.speed();
        float scale = Math.max(0.2f, CONFIG.skyScale / 100f);
        float alpha = CONFIG.coloredSkyAlpha / 100f;

        Color a = CONFIG.coloredSkyUseTheme ? Palette.getColor(0f) : new Color(CONFIG.coloredSkyColor);
        Color b = CONFIG.coloredSkyUseTheme ? Palette.getColor(0.5f) : new Color(CONFIG.skyColor2);
        Color dark = new Color(CONFIG.skyDarkColor);

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
            for (int row = 0; row < ROWS; row++) {
                float y0 = row / (float) ROWS;
                float y1 = (row + 1) / (float) ROWS;
                for (int column = 0; column < COLUMNS; column++) {
                    float x0 = column / (float) COLUMNS;
                    float x1 = (column + 1) / (float) COLUMNS;
                    corner(buffer, style, x0, y0, time, scale, a, b, dark, alpha);
                    corner(buffer, style, x0, y1, time, scale, a, b, dark, alpha);
                    corner(buffer, style, x1, y1, time, scale, a, b, dark, alpha);
                    corner(buffer, style, x1, y0, time, scale, a, b, dark, alpha);
                }
            }
            BufferRenderer.drawWithGlobalProgram(buffer.end());

            if (style.wantsStars() && CONFIG.skyStars > 0) {
                stars(time, alpha);
            }
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

    private static void corner(BufferBuilder buffer, SkyStyle style, float x, float y, float time,
                               float scale, Color a, Color b, Color dark, float alpha) {
        float mix = pattern(style, x, y, time, scale);
        mix = Math.max(0f, Math.min(1f, mix));
        // The pattern mixes the theme's two colours, and the darkest part of it falls back to the
        // night colour rather than to black, so a sky never goes completely flat.
        float toDark = Math.max(0f, 0.35f - mix) / 0.35f;
        int red = (int) (lerp(a.getRed(), b.getRed(), mix) * (1f - toDark) + dark.getRed() * toDark);
        int green = (int) (lerp(a.getGreen(), b.getGreen(), mix) * (1f - toDark) + dark.getGreen() * toDark);
        int blue = (int) (lerp(a.getBlue(), b.getBlue(), mix) * (1f - toDark) + dark.getBlue() * toDark);
        int out = (int) (255 * alpha * (0.45f + 0.55f * mix));
        buffer.vertex(x, y, FAR).color(clamp(red), clamp(green), clamp(blue), clamp(out));
    }

    private static float lerp(float from, float to, float t) {
        return from + (to - from) * t;
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    /**
     * The pattern, as a number from 0 to 1 at one point of the sky.
     *
     * <p>{@code y} is 0 overhead and 1 at the bottom of the screen, so a style that wants a horizon
     * reads from the lower half.
     */
    private static float pattern(SkyStyle style, float x, float y, float t, float scale) {
        float sx = (x - 0.5f) * scale;
        float sy = (y - 0.5f) * scale;
        switch (style) {
            case AURORA: {
                float fold = wave(x * 2.3f + t * 0.25f) * 0.18f + wave(x * 5.1f - t * 0.4f) * 0.08f;
                float band = 1f - Math.abs((y + fold) - 0.32f) * 5.5f;
                float second = 1f - Math.abs((y + fold * 1.6f) - 0.52f) * 7f;
                return Math.max(0f, Math.max(band, second * 0.7f));
            }
            case NEBULA:
                return 0.5f + 0.5f * (wave(sx * 1.7f + t * 0.1f) * wave(sy * 2.1f - t * 0.07f)
                        + wave(sx * 3.3f - sy * 2.6f + t * 0.13f) * 0.5f);
            case BANDS:
                return 0.5f + 0.5f * wave(y * 9f + t * 0.5f);
            case PLASMA:
                return 0.5f + 0.25f * (wave(sx * 6f + t) + wave(sy * 5f - t * 0.8f)
                        + wave((sx + sy) * 4f + t * 0.6f));
            case VOID_RIFT: {
                float drift = wave(y * 3f + t * 0.3f) * 0.12f;
                float rift = 1f - Math.abs(x - 0.5f + drift) * 9f;
                return Math.max(0f, rift) * (0.6f + 0.4f * wave(t * 1.3f));
            }
            case GRID: {
                float depth = 1f / Math.max(0.08f, y + 0.08f);
                float lines = ridge((x - 0.5f) * depth * 4f) + ridge(depth * 1.2f + t * 0.8f);
                return Math.min(1f, lines * 0.6f);
            }
            case HEX_GRID: {
                float hx = sx * 7f;
                float hy = sy * 7f + (Math.round(hx) % 2 == 0 ? 0.5f : 0f);
                return Math.min(1f, (ridge(hx) + ridge(hy)) * 0.5f
                        + 0.25f * wave(t - (float) Math.sqrt(sx * sx + sy * sy) * 4f));
            }
            case SCANLINES:
                return 0.25f + 0.75f * step(fract(y * 26f + t * 0.9f), 0.5f);
            case VORTEX: {
                float angle = (float) Math.atan2(sy, sx);
                float radius = (float) Math.sqrt(sx * sx + sy * sy);
                return 0.5f + 0.5f * wave(angle * 3f + radius * 7f - t * 0.9f);
            }
            case ECLIPSE: {
                float radius = (float) Math.sqrt(sx * sx + sy * sy) * 3.2f;
                float ring = 1f - Math.abs(radius - 0.75f) * 7f;
                return Math.max(radius > 0.75f ? 0.2f : 0f, ring);
            }
            case CODE_FALL: {
                float column = (float) Math.floor(x * 48f);
                float head = fract(t * 0.6f + column * 0.137f);
                float distance = fract(y - head + 1f);
                return Math.max(0f, 1f - distance * 6f);
            }
            case EMBER_SKY: {
                float heat = Math.max(0f, (y - 0.45f) * 2.2f);
                return Math.min(1f, heat * (0.7f + 0.3f * wave(x * 8f + t * 1.4f)));
            }
            case OCEAN_DEPTH: {
                float shaft = ridge((x - 0.5f) * 6f + wave(t * 0.3f) * 0.6f);
                return Math.min(1f, shaft * Math.max(0f, 1f - y * 1.2f) * 1.6f);
            }
            case LAVA_FLOW: {
                float cell = wave(sx * 4f + wave(sy * 3f + t * 0.2f) * 1.5f)
                        + wave(sy * 5f - t * 0.15f);
                return 0.5f + 0.3f * cell;
            }
            case SUNSET:
                return Math.max(0f, 1f - (float) Math.sqrt((x - 0.78f) * (x - 0.78f)
                        + (y - 0.72f) * (y - 0.72f)) * 1.7f);
            case RIPPLE: {
                float radius = (float) Math.sqrt(sx * sx + sy * sy);
                return 0.5f + 0.5f * wave(radius * 14f - t * 1.6f);
            }
            case SPEED_LINES: {
                float angle = (float) Math.atan2(sy, sx);
                return step(fract(angle * 4.5f + t * 1.2f), 0.55f) * Math.min(1f,
                        (float) Math.sqrt(sx * sx + sy * sy) * 2.4f);
            }
            case LATTICE:
                return Math.min(1f, ridge((sx + sy) * 8f + t * 0.4f)
                        + ridge((sx - sy) * 8f - t * 0.4f));
            case SILK:
                return 0.5f + 0.5f * wave(sx * 2.5f + wave(sy * 2f + t * 0.3f) * 2f);
            case STORM_FRONT: {
                float front = 1f - Math.abs(x - fract(t * 0.12f)) * 3.4f;
                float flicker = wave(t * 9f) > 0.93f ? 0.4f : 0f;
                return Math.max(0.1f, Math.max(front, 0f) + flicker);
            }
            case CIRCUIT_SKY: {
                float trace = step(fract(x * 18f), 0.12f) + step(fract(y * 12f), 0.12f);
                float pulse = wave(t * 1.1f + (float) Math.floor(x * 18f) * 0.3f);
                return Math.min(1f, trace * (0.35f + 0.65f * Math.max(0f, pulse)));
            }
            default:
                return 0.5f;
        }
    }

    /** A sine in 0..1 of its own period, so the callers read as "waves per screen". */
    private static float wave(float v) {
        return (float) Math.sin(v * Math.PI * 2.0);
    }

    /** A thin bright line wherever {@code v} crosses a whole number. */
    private static float ridge(float v) {
        float f = Math.abs(fract(v) - 0.5f) * 2f;
        return Math.max(0f, f * f * f);
    }

    private static float fract(float v) {
        return v - (float) Math.floor(v);
    }

    private static float step(float v, float edge) {
        return v < edge ? 1f : 0f;
    }

    /** Points of light, for the styles that ask for them. */
    private static void stars(float time, float alpha) {
        RenderSystem.setShader(ShaderKey.POSITION_COLOR);
        BufferBuilder buffer = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        int count = CONFIG.skyStars;
        for (int i = 0; i < count; i++) {
            // A fixed scatter, so the stars do not swim about between frames.
            float x = fract(i * 0.7548777f);
            float y = fract(i * 0.5698403f);
            float twinkle = 0.45f + 0.55f * Math.abs(wave(time * 0.6f + i * 0.31f));
            int bright = clamp((int) (255 * alpha * twinkle));
            float size = 0.0016f + 0.0014f * fract(i * 0.311f);
            buffer.vertex(x - size, y - size * 2f, FAR).color(255, 255, 255, bright);
            buffer.vertex(x - size, y + size * 2f, FAR).color(255, 255, 255, bright);
            buffer.vertex(x + size, y + size * 2f, FAR).color(255, 255, 255, bright);
            buffer.vertex(x + size, y - size * 2f, FAR).color(255, 255, 255, bright);
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }
}
