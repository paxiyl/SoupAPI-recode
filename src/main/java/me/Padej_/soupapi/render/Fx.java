package me.Padej_.soupapi.render;

import me.Padej_.soupapi.compat.BlendFactor;
import me.Padej_.soupapi.compat.BufferBuilder;
import me.Padej_.soupapi.compat.BufferRenderer;
import me.Padej_.soupapi.compat.Camera;
import me.Padej_.soupapi.compat.Cameras;
import me.Padej_.soupapi.compat.Mat4;
import me.Padej_.soupapi.compat.MatrixStack;
import me.Padej_.soupapi.compat.RenderSystem;
import me.Padej_.soupapi.compat.RotationAxis;
import me.Padej_.soupapi.compat.ShaderKey;
import me.Padej_.soupapi.compat.Tessellator;
import me.Padej_.soupapi.compat.VertexFormat;
import me.Padej_.soupapi.compat.VertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;

import java.awt.Color;

/**
 * Shared drawing primitives for the mod's world-space effects.
 *
 * <p>Rings, arcs, billboarded sprites and discs, in world coordinates, so an effect can be written
 * as a handful of calls rather than its own buffer setup. Everything here is camera-relative
 * internally — callers pass plain world positions.
 */
public final class Fx {

    private Fx() {
    }

    /** One billboarded sprite. Batch with {@link #beginSprites}/{@link #endSprites} when drawing many. */
    public static void sprite(ResourceLocation texture, double x, double y, double z, float size,
                              Color colour, boolean flat) {
        BufferBuilder buffer = beginSprites(texture);
        appendSprite(buffer, x, y, z, size, colour, flat);
        endSprites(buffer);
    }

    public static BufferBuilder beginSprites(ResourceLocation texture) {
        Render2D.setupRender();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.enableDepthTest();
        RenderSystem.blendFunc(BlendFactor.SrcFactor.SRC_ALPHA, BlendFactor.DstFactor.ONE);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShader(ShaderKey.POSITION_TEX_COLOR);
        return Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
    }

    public static void appendSprite(BufferBuilder buffer, double x, double y, double z, float size,
                                    Color colour, boolean flat) {
        Camera camera = Cameras.get();
        Vec3 cameraPos = camera.getPos();

        MatrixStack matrices = new MatrixStack();
        matrices.translate(x - cameraPos.xCoord, y - cameraPos.yCoord, z - cameraPos.zCoord);
        if (flat) {
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90f));
        } else {
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        }
        Mat4 matrix = matrices.peek().getPositionMatrix();

        float half = size / 2f;
        int r = colour.getRed();
        int g = colour.getGreen();
        int b = colour.getBlue();
        int a = colour.getAlpha();

        buffer.vertex(matrix, -half, -half, 0f).texture(0f, 1f).color(r, g, b, a);
        buffer.vertex(matrix, half, -half, 0f).texture(1f, 1f).color(r, g, b, a);
        buffer.vertex(matrix, half, half, 0f).texture(1f, 0f).color(r, g, b, a);
        buffer.vertex(matrix, -half, half, 0f).texture(0f, 0f).color(r, g, b, a);
    }

    public static void endSprites(BufferBuilder buffer) {
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        Render2D.endRender();
    }

    /** A horizontal circle, queued with the mod's other 3D shapes. */
    public static void ring(double x, double y, double z, double radius, int segments, Color colour) {
        int steps = Math.max(8, segments);
        for (int i = 0; i < steps; i++) {
            double a0 = i * (Math.PI * 2 / steps);
            double a1 = (i + 1) * (Math.PI * 2 / steps);
            line(x + Math.cos(a0) * radius, y, z + Math.sin(a0) * radius,
                    x + Math.cos(a1) * radius, y, z + Math.sin(a1) * radius, colour);
        }
    }

    /** A circle standing on its edge, rotated about Y by {@code tilt} radians. */
    public static void verticalRing(double x, double y, double z, double radius, int segments,
                                    double tilt, Color colour) {
        int steps = Math.max(8, segments);
        for (int i = 0; i < steps; i++) {
            double a0 = i * (Math.PI * 2 / steps);
            double a1 = (i + 1) * (Math.PI * 2 / steps);
            double x0 = Math.cos(a0) * radius * Math.cos(tilt);
            double z0 = Math.cos(a0) * radius * Math.sin(tilt);
            double x1 = Math.cos(a1) * radius * Math.cos(tilt);
            double z1 = Math.cos(a1) * radius * Math.sin(tilt);
            line(x + x0, y + Math.sin(a0) * radius, z + z0,
                    x + x1, y + Math.sin(a1) * radius, z + z1, colour);
        }
    }

    /** A ring tilted away from horizontal, for Saturn-style halos. */
    public static void tiltedRing(double x, double y, double z, double radius, int segments,
                                  double tiltRadians, double spin, Color colour) {
        int steps = Math.max(8, segments);
        double cos = Math.cos(tiltRadians);
        double sin = Math.sin(tiltRadians);
        for (int i = 0; i < steps; i++) {
            double a0 = i * (Math.PI * 2 / steps) + spin;
            double a1 = (i + 1) * (Math.PI * 2 / steps) + spin;
            double x0 = Math.cos(a0) * radius;
            double z0 = Math.sin(a0) * radius;
            double x1 = Math.cos(a1) * radius;
            double z1 = Math.sin(a1) * radius;
            line(x + x0, y + z0 * sin, z + z0 * cos,
                    x + x1, y + z1 * sin, z + z1 * cos, colour);
        }
    }

    /** An arc of a horizontal circle, in radians. */
    public static void arc(double x, double y, double z, double radius, int segments,
                           double from, double to, Color colour) {
        int steps = Math.max(2, segments);
        for (int i = 0; i < steps; i++) {
            double a0 = from + (to - from) * (i / (double) steps);
            double a1 = from + (to - from) * ((i + 1) / (double) steps);
            line(x + Math.cos(a0) * radius, y, z + Math.sin(a0) * radius,
                    x + Math.cos(a1) * radius, y, z + Math.sin(a1) * radius, colour);
        }
    }

    public static void line(double x1, double y1, double z1, double x2, double y2, double z2,
                            Color colour) {
        Render3D_Shapes.drawLine(new Vec3(x1, y1, z1), new Vec3(x2, y2, z2), colour);
    }

    // ---- glowing geometry ------------------------------------------------------------------
    //
    // GL lines are one pixel wide on gl4es, which is what PojavLauncher runs: glLineWidth is
    // ignored there (see Caps.thickLinesSupported). Anything built from lines therefore arrives on
    // a phone as a hairline, which is why the ring-shaped effects looked so weak. Everything below
    // builds real geometry instead - soft-edged bands made of quads - so a ring is a glowing band
    // at any width, on any driver.

    /**
     * A glowing band in the shape of a ring.
     *
     * @param radius centre of the band
     * @param width  how thick the band is; it fades to nothing at both edges
     * @param tilt   radians away from horizontal, for Saturn-style rings
     * @param spin   radians of rotation about the ring's own axis
     */
    public static void glowRing(double x, double y, double z, double radius, double width,
                                int segments, double tilt, double spin, Color colour) {
        glowArc(x, y, z, radius, width, segments, tilt, spin, 0.0, Math.PI * 2, colour);
    }

    /** As {@link #glowRing}, but only {@code from}..{@code to} radians of it. */
    public static void glowArc(double x, double y, double z, double radius, double width,
                               int segments, double tilt, double spin, double from, double to,
                               Color colour) {
        int steps = Math.max(8, segments);
        Vec3 cameraPos = Cameras.get().getPos();
        double sin = Math.sin(tilt);
        double cos = Math.cos(tilt);

        Render2D.setupRender();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.enableDepthTest();
        RenderSystem.blendFunc(BlendFactor.SrcFactor.SRC_ALPHA, BlendFactor.DstFactor.ONE);
        RenderSystem.setShader(ShaderKey.POSITION_COLOR);
        BufferBuilder buffer = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        Mat4 matrix = new MatrixStack().peek().getPositionMatrix();
        int r = colour.getRed();
        int g = colour.getGreen();
        int b = colour.getBlue();
        int a = colour.getAlpha();
        double half = width / 2.0;

        for (int i = 0; i < steps; i++) {
            double a0 = from + (to - from) * (i / (double) steps) + spin;
            double a1 = from + (to - from) * ((i + 1) / (double) steps) + spin;

            for (int edge = 0; edge < 2; edge++) {
                double rIn = edge == 0 ? radius - half : radius;
                double rOut = edge == 0 ? radius : radius + half;
                int aIn = edge == 0 ? 0 : a;
                int aOut = edge == 0 ? a : 0;

                double[] p0in = point(x, y, z, a0, rIn, sin, cos, cameraPos);
                double[] p1in = point(x, y, z, a1, rIn, sin, cos, cameraPos);
                double[] p1out = point(x, y, z, a1, rOut, sin, cos, cameraPos);
                double[] p0out = point(x, y, z, a0, rOut, sin, cos, cameraPos);

                buffer.vertex(matrix, (float) p0in[0], (float) p0in[1], (float) p0in[2]).color(r, g, b, aIn);
                buffer.vertex(matrix, (float) p1in[0], (float) p1in[1], (float) p1in[2]).color(r, g, b, aIn);
                buffer.vertex(matrix, (float) p1out[0], (float) p1out[1], (float) p1out[2]).color(r, g, b, aOut);
                buffer.vertex(matrix, (float) p0out[0], (float) p0out[1], (float) p0out[2]).color(r, g, b, aOut);
            }
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        Render2D.endRender();
    }

    private static double[] point(double x, double y, double z, double angle, double radius,
                                  double tiltSin, double tiltCos, Vec3 cameraPos) {
        double px = Math.cos(angle) * radius;
        double pz = Math.sin(angle) * radius;
        return new double[]{
                x + px - cameraPos.xCoord,
                y + pz * tiltSin - cameraPos.yCoord,
                z + pz * tiltCos - cameraPos.zCoord};
    }

    /**
     * A thick line: a quad turned to face the camera, so it keeps its width from any angle.
     *
     * <p>Use this anywhere a stroke needs to be visible rather than hinted at.
     */
    public static void glowLine(double x1, double y1, double z1, double x2, double y2, double z2,
                                double width, Color colour) {
        Vec3 cameraPos = Cameras.get().getPos();

        double dx = x2 - x1;
        double dy = y2 - y1;
        double dz = z2 - z1;
        // Perpendicular to both the line and the direction of view.
        double mx = (x1 + x2) / 2 - cameraPos.xCoord;
        double my = (y1 + y2) / 2 - cameraPos.yCoord;
        double mz = (z1 + z2) / 2 - cameraPos.zCoord;
        double nx = dy * mz - dz * my;
        double ny = dz * mx - dx * mz;
        double nz = dx * my - dy * mx;
        double length = Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (length < 1.0E-6) return;
        double half = width / 2.0;
        nx = nx / length * half;
        ny = ny / length * half;
        nz = nz / length * half;

        Render2D.setupRender();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.enableDepthTest();
        RenderSystem.blendFunc(BlendFactor.SrcFactor.SRC_ALPHA, BlendFactor.DstFactor.ONE);
        RenderSystem.setShader(ShaderKey.POSITION_COLOR);
        BufferBuilder buffer = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        Mat4 matrix = new MatrixStack().peek().getPositionMatrix();

        int r = colour.getRed();
        int g = colour.getGreen();
        int b = colour.getBlue();
        int a = colour.getAlpha();
        double ox = -cameraPos.xCoord;
        double oy = -cameraPos.yCoord;
        double oz = -cameraPos.zCoord;

        // Two quads so the stroke fades at both edges rather than ending in a hard line.
        for (int edge = 0; edge < 2; edge++) {
            double s = edge == 0 ? -1 : 1;
            buffer.vertex(matrix, (float) (x1 + ox), (float) (y1 + oy), (float) (z1 + oz)).color(r, g, b, a);
            buffer.vertex(matrix, (float) (x2 + ox), (float) (y2 + oy), (float) (z2 + oz)).color(r, g, b, a);
            buffer.vertex(matrix, (float) (x2 + ox + nx * s), (float) (y2 + oy + ny * s),
                    (float) (z2 + oz + nz * s)).color(r, g, b, 0);
            buffer.vertex(matrix, (float) (x1 + ox + nx * s), (float) (y1 + oy + ny * s),
                    (float) (z1 + oz + nz * s)).color(r, g, b, 0);
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        Render2D.endRender();
    }

    /** Clamps an alpha fraction onto a colour. */
    public static Color withAlpha(Color colour, float alpha) {
        int a = Math.round(255 * Math.max(0f, Math.min(1f, alpha)));
        return Render2D.injectAlpha(colour, a);
    }
}
