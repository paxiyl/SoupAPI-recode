package me.Padej_.soupapi.compat;

import me.Padej_.soupapi.compat.Mat4;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.nio.FloatBuffer;

/**
 * Thin helpers over the bits of raw GL that {@code GlStateManager} does not wrap in 1.8.9
 * (scissor, line width, matrix upload).
 *
 * <p>Everything here sticks to GL 1.1/1.2 entry points, which is the common subset gl4es implements
 * natively on Android. Nothing in the port reaches for GL 3 core, VAOs, FBO blits or GLSL.
 */
public final class GlCompat {

    private GlCompat() {
    }

    private static final FloatBuffer MATRIX_BUFFER = BufferUtils.createFloatBuffer(16);

    public static void pushMatrix() {
        GlStateManager.pushMatrix();
    }

    public static void popMatrix() {
        GlStateManager.popMatrix();
    }

    /** Multiplies the given matrix onto the current GL matrix. */
    public static void multMatrix(Mat4 matrix) {
        MATRIX_BUFFER.clear();
        matrix.writeTo(MATRIX_BUFFER);
        MATRIX_BUFFER.flip();
        GlStateManager.multMatrix(MATRIX_BUFFER);
    }

    /** Reads back a GL matrix (e.g. {@code GL_MODELVIEW_MATRIX}) into a {@link Mat4}. */
    public static Mat4 readMatrix(int which) {
        MATRIX_BUFFER.clear();
        GlStateManager.getFloat(which, MATRIX_BUFFER);
        MATRIX_BUFFER.rewind();
        return new Mat4().readFrom(MATRIX_BUFFER);
    }

    public static void bindTexture(ResourceLocation texture) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
    }

    public static void lineWidth(float width) {
        // gl4es clamps this to 1.0; callers that need a guaranteed stroke check
        // Caps.thickLinesSupported() and draw geometry instead.
        try {
            GL11.glLineWidth(width);
        } catch (Throwable ignored) {
        }
    }

    /**
     * Enables a scissor box in <i>scaled GUI</i> coordinates, converting to the physical framebuffer
     * pixels GL expects. Mirrors what 1.21.4's {@code DrawContext.enableScissor} does for callers.
     */
    public static void enableScissor(int x1, int y1, int x2, int y2) {
        int scale = Windows.scaleFactor();
        int fbHeight = Windows.height();

        int left = x1 * scale;
        int width = (x2 - x1) * scale;
        int height = (y2 - y1) * scale;
        // GL's origin is bottom-left; the GUI's is top-left.
        int bottom = fbHeight - (y2 * scale);

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(left, bottom, Math.max(0, width), Math.max(0, height));
    }

    public static void disableScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    /** Texture filtering helper; 1.8.9's GlStateManager has no texParameter wrapper. */
    public static void setTextureFilter(boolean linear, boolean mipmap) {
        int min = linear
                ? (mipmap ? GL11.GL_LINEAR_MIPMAP_LINEAR : GL11.GL_LINEAR)
                : (mipmap ? GL11.GL_NEAREST_MIPMAP_LINEAR : GL11.GL_NEAREST);
        int mag = linear ? GL11.GL_LINEAR : GL11.GL_NEAREST;
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, min);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, mag);
    }

    /** Clamp texture wrapping, so rounded-corner atlases do not bleed across edges. */
    public static void setTextureClamp() {
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
    }
}
