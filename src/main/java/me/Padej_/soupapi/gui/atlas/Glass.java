package me.Padej_.soupapi.gui.atlas;

import me.Padej_.soupapi.compat.BufferBuilder;
import me.Padej_.soupapi.compat.BufferRenderer;
import me.Padej_.soupapi.compat.Mat4;
import me.Padej_.soupapi.compat.MatrixStack;
import me.Padej_.soupapi.compat.RenderSystem;
import me.Padej_.soupapi.compat.ShaderKey;
import me.Padej_.soupapi.compat.Tessellator;
import me.Padej_.soupapi.compat.VertexFormat;
import me.Padej_.soupapi.compat.VertexFormats;
import me.Padej_.soupapi.render.Render2D;

import java.awt.Color;

/**
 * The Atlas look without shaders.
 *
 * <p>Atlas draws its window as refracting "liquid glass" over a blurred copy of the world, which
 * takes GLSL and a framebuffer copy - neither of which this mod uses, because gl4es on
 * PojavLauncher runs neither reliably. What makes the look is not the refraction but its layers: a
 * translucent pane, a soft shadow, a bright rim catching light along the top, and selection pills
 * that are themselves little panes. Those are all fixed-function here: rounded gradient quads,
 * the mod's blurred-shadow texture, and thin rims.
 */
public final class Glass {

    private Glass() {
    }

    public static int alpha(int colour, float factor) {
        int a = Math.round(((colour >>> 24) & 255) * Math.max(0f, Math.min(1f, factor)));
        return (a << 24) | (colour & 0xFFFFFF);
    }

    public static int blend(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int aa = (a >>> 24) & 255, ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        int ba = (b >>> 24) & 255, br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        return (Math.round(aa + (ba - aa) * t) << 24) | (Math.round(ar + (br - ar) * t) << 16)
                | (Math.round(ag + (bg - ag) * t) << 8) | Math.round(ab + (bb - ab) * t);
    }

    public static Color c(int argb) {
        return new Color((argb >> 16) & 255, (argb >> 8) & 255, argb & 255, (argb >>> 24) & 255);
    }

    /** A rounded rectangle, top colour to bottom colour. */
    public static void rect(MatrixStack ms, float x, float y, float x2, float y2, float radius, int top, int bottom) {
        float w = x2 - x, h = y2 - y;
        if (w <= 0.5f || h <= 0.5f) return;
        radius = Math.min(radius, Math.min(w, h) / 2f);
        if ((((top | bottom) >>> 24) & 255) == 0) return;
        // One fan with the colour per vertex by height: translucent gradients blend correctly, which
        // Render2D's destination-alpha gradient cannot do.
        Render2D.setupRender();
        RenderSystem.setShader(ShaderKey.POSITION_COLOR);
        Mat4 m = ms.peek().getPositionMatrix();
        BufferBuilder b = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        vertex(b, m, (x + x2) / 2f, (y + y2) / 2f, y, h, top, bottom);
        float[][] corners = {{x2 - radius, y2 - radius}, {x2 - radius, y + radius}, {x + radius, y + radius}, {x + radius, y2 - radius}};
        int steps = radius < 0.5f ? 0 : Math.max(2, Math.min(8, (int) radius));
        for (int i = 0; i < 4; i++) {
            for (int k = 0; k <= steps; k++) {
                double a = Math.toRadians(i * 90 + (steps == 0 ? 0 : 90.0 * k / steps));
                vertex(b, m, corners[i][0] + (float) Math.sin(a) * radius, corners[i][1] + (float) Math.cos(a) * radius, y, h, top, bottom);
            }
        }
        vertex(b, m, corners[0][0], corners[0][1] + radius, y, h, top, bottom);
        BufferRenderer.drawWithGlobalProgram(b.end());
        Render2D.endRender();
    }

    private static void vertex(BufferBuilder b, Mat4 m, float px, float py, float y, float h, int top, int bottom) {
        int c = blend(top, bottom, Math.max(0f, Math.min(1f, (py - y) / h)));
        b.vertex(m, px, py, 0f).color(((c >> 16) & 255) / 255f, ((c >> 8) & 255) / 255f, (c & 255) / 255f, ((c >>> 24) & 255) / 255f);
    }

    public static void rect(MatrixStack ms, float x, float y, float x2, float y2, float radius, int colour) {
        rect(ms, x, y, x2, y2, radius, colour, colour);
    }

    /** A soft shadow under a pane. */
    public static void shadow(MatrixStack ms, float x, float y, float x2, float y2, int blur, int colour) {
        if (((colour >>> 24) & 255) == 0) return;
        Render2D.drawBlurredShadow(ms, x, y, x2 - x, y2 - y, blur, c(colour));
    }

    /** A thin rim: the light caught along the top edge of the glass, fading down the sides. */
    public static void rim(MatrixStack ms, float x, float y, float x2, float y2, float radius, float width, int top, int bottom) {
        float inset = radius * 0.7f;
        rect(ms, x + inset, y, x2 - inset, y + width, Math.min(radius, width), top);
        rect(ms, x + inset, y2 - width, x2 - inset, y2, Math.min(radius, width), bottom);
        rect(ms, x, y + radius, x + width, y2 - radius, 0f, top, bottom);
        rect(ms, x2 - width, y + radius, x2, y2 - radius, 0f, top, bottom);
    }

    /** The window: a translucent pane with a sheen along its top and a rim. */
    public static void pane(MatrixStack ms, float x, float y, float x2, float y2, float radius, int tint, int ink) {
        rect(ms, x, y, x2, y2, radius, alpha(tint, 0.86f), alpha(tint, 0.94f));
        rect(ms, x, y, x2, y + 70, radius, alpha(ink, 0.07f), alpha(ink, 0f));
        rim(ms, x, y, x2, y2, radius, 1f, alpha(ink, 0.22f), alpha(ink, 0.06f));
    }

    /** A selection pill: a small pane of tinted glass with its own rim. */
    public static void pill(MatrixStack ms, float x, float y, float x2, float y2, float radius, int accent, int ink, float shown) {
        rect(ms, x, y, x2, y2, radius, alpha(accent, 0.30f * shown), alpha(accent, 0.18f * shown));
        rect(ms, x, y, x2, y + (y2 - y) * 0.45f, radius, alpha(ink, 0.10f * shown), alpha(ink, 0f));
        rim(ms, x, y, x2, y2, radius, 1f, alpha(ink, 0.35f * shown), alpha(accent, 0.25f * shown));
    }

    public static void dot(MatrixStack ms, float cx, float cy, float r, int colour) {
        rect(ms, cx - r, cy - r, cx + r, cy + r, r, colour);
    }
}
