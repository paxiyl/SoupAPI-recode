package me.Padej_.soupapi.render;

import me.Padej_.soupapi.compat.BufferBuilder;
import me.Padej_.soupapi.compat.BufferRenderer;
import me.Padej_.soupapi.compat.Mat4;
import me.Padej_.soupapi.compat.MatrixStack;
import me.Padej_.soupapi.compat.RenderSystem;
import me.Padej_.soupapi.compat.ShaderKey;
import me.Padej_.soupapi.compat.Tessellator;
import me.Padej_.soupapi.compat.VertexFormat;
import me.Padej_.soupapi.compat.VertexFormats;

/**
 * Flat 2D shapes for the HUD reticles, all in one draw.
 *
 * <p>A reticle in the targeting-display style is dozens of small pieces - dashes round a ring,
 * ticks along a rail, brackets, chevrons, dotted circles. Drawing each one as its own rounded
 * rectangle, which is what the rest of the HUD does, would be a hundred draw calls a frame for one
 * crosshair, and gl4es on a phone pays for every one of them. So a reticle is built here as plain
 * triangles into a single buffer and drawn once.
 *
 * <p>Strokes are geometry, never {@code GL_LINES}: gl4es clamps line width to one pixel, so a ring
 * drawn with lines is a hairline on PojavLauncher whatever it is asked for.
 *
 * <p>Usage: {@link #begin}, any number of shapes, {@link #end}. Coordinates are in the matrix
 * stack's space at {@code begin}, so a caller can translate to the screen centre and rotate first.
 */
public final class Reticle {

    private static BufferBuilder buffer;
    private static Mat4 matrix;
    private static float r, g, b, a;

    private Reticle() {
    }

    public static void begin(MatrixStack ms) {
        Render2D.setupRender();
        // Shapes here are wound whichever way their maths falls out - an arc runs one way, a
        // stroke the other depending on its direction - so culling would silently drop half of
        // them. Nothing in a flat HUD shape has a back to hide.
        net.minecraft.client.renderer.GlStateManager.disableCull();
        RenderSystem.setShader(ShaderKey.POSITION_COLOR);
        matrix = ms.peek().getPositionMatrix();
        buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
    }

    public static void end() {
        if (buffer == null) return;
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        buffer = null;
        matrix = null;
        net.minecraft.client.renderer.GlStateManager.enableCull();
        Render2D.endRender();
    }

    /** Sets the colour of everything drawn after it, as ARGB. */
    public static void colour(int argb) {
        r = ((argb >> 16) & 255) / 255f;
        g = ((argb >> 8) & 255) / 255f;
        b = (argb & 255) / 255f;
        a = ((argb >>> 24) & 255) / 255f;
    }

    // ---- primitives ------------------------------------------------------------------------

    private static void v(float x, float y) {
        buffer.vertex(matrix, x, y, 0f).color(r, g, b, a);
    }

    public static void tri(float x0, float y0, float x1, float y1, float x2, float y2) {
        if (buffer == null || a <= 0.003f) return;
        v(x0, y0);
        v(x1, y1);
        v(x2, y2);
    }

    public static void quad(float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3) {
        tri(x0, y0, x1, y1, x2, y2);
        tri(x0, y0, x2, y2, x3, y3);
    }

    /** A filled axis-aligned rectangle. */
    public static void rect(float x0, float y0, float x1, float y1) {
        quad(x0, y0, x1, y0, x1, y1, x0, y1);
    }

    /** A stroke from one point to another, {@code w} wide. */
    public static void line(float x0, float y0, float x1, float y1, float w) {
        float dx = x1 - x0, dy = y1 - y0;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 1.0E-4f) return;
        float nx = -dy / len * w * 0.5f, ny = dx / len * w * 0.5f;
        quad(x0 + nx, y0 + ny, x1 + nx, y1 + ny, x1 - nx, y1 - ny, x0 - nx, y0 - ny);
    }

    /** A thick arc of a circle, from angle {@code a0} to {@code a1} in degrees (0 is up, clockwise). */
    public static void arc(float cx, float cy, float radius, float w, float a0, float a1, int segments) {
        float inner = radius - w * 0.5f, outer = radius + w * 0.5f;
        segments = Math.max(1, segments);
        float px0 = 0, py0 = 0, px1 = 0, py1 = 0;
        for (int i = 0; i <= segments; i++) {
            double ang = Math.toRadians(a0 + (a1 - a0) * i / segments);
            float s = (float) Math.sin(ang), c = (float) -Math.cos(ang);
            float ix = cx + s * inner, iy = cy + c * inner, ox = cx + s * outer, oy = cy + c * outer;
            if (i > 0) quad(px0, py0, px1, py1, ox, oy, ix, iy);
            px0 = ix;
            py0 = iy;
            px1 = ox;
            py1 = oy;
        }
    }

    /** A whole ring. */
    public static void ring(float cx, float cy, float radius, float w) {
        arc(cx, cy, radius, w, 0f, 360f, Math.max(24, (int) (radius * 1.6f)));
    }

    /**
     * A ring broken into {@code count} dashes, each covering {@code duty} of its slot, rotated by
     * {@code phase} degrees - turning the phase is what makes a dashed ring march.
     */
    public static void dashes(float cx, float cy, float radius, float w, int count, float duty, float phase) {
        float slot = 360f / count;
        int seg = Math.max(1, (int) (radius * slot * duty / 40f));
        for (int i = 0; i < count; i++) {
            float start = phase + i * slot;
            arc(cx, cy, radius, w, start, start + slot * duty, seg);
        }
    }

    /** Small square dots round a ring. */
    public static void dots(float cx, float cy, float radius, int count, float size, float phase) {
        float h = size * 0.5f;
        for (int i = 0; i < count; i++) {
            double ang = Math.toRadians(phase + i * 360f / count);
            float x = cx + (float) Math.sin(ang) * radius, y = cy - (float) Math.cos(ang) * radius;
            rect(x - h, y - h, x + h, y + h);
        }
    }

    /** Radial ticks round a ring, every {@code major}th one longer. */
    public static void ticks(float cx, float cy, float radius, int count, float len, float w, float phase, int major) {
        for (int i = 0; i < count; i++) {
            double ang = Math.toRadians(phase + i * 360f / count);
            float s = (float) Math.sin(ang), c = (float) -Math.cos(ang);
            float l = major > 0 && i % major == 0 ? len * 1.8f : len;
            line(cx + s * radius, cy + c * radius, cx + s * (radius + l), cy + c * (radius + l), w);
        }
    }

    /** A regular polygon's outline; {@code rot} turns it, 0 puts a vertex straight up. */
    public static void polygon(float cx, float cy, int sides, float radius, float w, float rot) {
        for (int i = 0; i < sides; i++) {
            double a0 = Math.toRadians(rot + i * 360f / sides), a1 = Math.toRadians(rot + (i + 1) * 360f / sides);
            float x0 = cx + (float) Math.sin(a0) * radius, y0 = cy - (float) Math.cos(a0) * radius;
            float x1 = cx + (float) Math.sin(a1) * radius, y1 = cy - (float) Math.cos(a1) * radius;
            // Overlap the joins by half a stroke so corners close.
            float dx = x1 - x0, dy = y1 - y0;
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            float ex = dx / len * w * 0.5f, ey = dy / len * w * 0.5f;
            line(x0 - ex, y0 - ey, x1 + ex, y1 + ey, w);
        }
    }

    /** A filled triangle pointing in direction {@code rot} (0 is up), centred on (x, y). */
    public static void arrow(float x, float y, float size, float rot) {
        double ang = Math.toRadians(rot);
        float s = (float) Math.sin(ang), c = (float) -Math.cos(ang);
        float px = -c, py = s;                     // perpendicular
        float tipX = x + s * size, tipY = y + c * size;
        float bx = x - s * size * 0.6f, by = y - c * size * 0.6f;
        tri(tipX, tipY, bx + px * size * 0.8f, by + py * size * 0.8f, bx - px * size * 0.8f, by - py * size * 0.8f);
    }

    /** An open chevron (two strokes) pointing in direction {@code rot}. */
    public static void chevron(float x, float y, float size, float w, float rot) {
        double ang = Math.toRadians(rot);
        float s = (float) Math.sin(ang), c = (float) -Math.cos(ang);
        float px = -c, py = s;
        float tipX = x + s * size * 0.5f, tipY = y + c * size * 0.5f;
        float bx = x - s * size * 0.5f, by = y - c * size * 0.5f;
        line(bx + px * size * 0.7f, by + py * size * 0.7f, tipX, tipY, w);
        line(bx - px * size * 0.7f, by - py * size * 0.7f, tipX, tipY, w);
    }

    /**
     * A corner bracket at (x, y): two arms of length {@code len}, opening toward ({@code sx},
     * {@code sy}) - pass ±1 for each to pick the corner.
     */
    public static void bracket(float x, float y, float len, float w, float sx, float sy) {
        float h = w * 0.5f;
        rect(Math.min(x, x + sx * len), y - h, Math.max(x, x + sx * len), y + h);
        rect(x - h, Math.min(y, y + sy * len), x + h, Math.max(y, y + sy * len));
    }

    /** A dotted straight line: {@code count} small squares from one point to another. */
    public static void dotted(float x0, float y0, float x1, float y1, int count, float size) {
        float h = size * 0.5f;
        for (int i = 0; i < count; i++) {
            float t = count == 1 ? 0.5f : i / (float) (count - 1);
            float x = x0 + (x1 - x0) * t, y = y0 + (y1 - y0) * t;
            rect(x - h, y - h, x + h, y + h);
        }
    }

    /** A filled disc. */
    public static void disc(float cx, float cy, float radius) {
        int seg = Math.max(12, (int) (radius * 3f));
        for (int i = 0; i < seg; i++) {
            double a0 = Math.PI * 2 * i / seg, a1 = Math.PI * 2 * (i + 1) / seg;
            tri(cx, cy, cx + (float) Math.sin(a0) * radius, cy - (float) Math.cos(a0) * radius,
                    cx + (float) Math.sin(a1) * radius, cy - (float) Math.cos(a1) * radius);
        }
    }
}
