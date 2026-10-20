package me.Padej_.soupapi.compat;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

/**
 * Replacement for {@code com.mojang.blaze3d.systems.RenderSystem}.
 *
 * <p>1.8.9 has {@code GlStateManager} but no {@code RenderSystem}, no shader programs and no shader
 * colour uniform. Most calls map straight through. Two need emulating:
 *
 * <ul>
 *   <li>{@link #setShader} picks fixed-function state (chiefly texture enable/disable) — see
 *       {@link ShaderKey}.</li>
 *   <li>{@link #setShaderColor} has no fixed-function equivalent when per-vertex colours are in
 *       use, because {@code glColor} is ignored once a colour array is bound. The value is stored
 *       here and {@link Buf} multiplies it into every vertex colour it submits, which reproduces
 *       1.21.4's behaviour (where the uniform multiplies the vertex colour) exactly. For geometry
 *       with no per-vertex colour it is also pushed to {@code glColor}, covering both cases.</li>
 * </ul>
 *
 * <p>Note on {@code GlStateManager.color}: 1.8.9 caches colour state, so the port must route all
 * colour changes through here rather than calling {@code GL11.glColor4f} directly, or the cache and
 * the driver drift apart.
 */
public final class RenderSystem {

    private RenderSystem() {
    }

    // ---- shader colour emulation -----------------------------------------------------------

    private static float shaderR = 1f, shaderG = 1f, shaderB = 1f, shaderA = 1f;

    public static void setShaderColor(float r, float g, float b, float a) {
        shaderR = r;
        shaderG = g;
        shaderB = b;
        shaderA = a;
        GlStateManager.color(r, g, b, a);
    }

    public static float shaderRed() {
        return shaderR;
    }

    public static float shaderGreen() {
        return shaderG;
    }

    public static float shaderBlue() {
        return shaderB;
    }

    public static float shaderAlpha() {
        return shaderA;
    }

    /** True when the shader colour is the identity, letting {@link Buf} skip per-vertex maths. */
    static boolean shaderColorIsIdentity() {
        return shaderR == 1f && shaderG == 1f && shaderB == 1f && shaderA == 1f;
    }

    // ---- shader selection ------------------------------------------------------------------

    private static ShaderKey currentShader = ShaderKey.POSITION_TEX_COLOR;

    public static void setShader(ShaderKey key) {
        currentShader = key;
        if (key.isTextured()) {
            GlStateManager.enableTexture2D();
        } else {
            GlStateManager.disableTexture2D();
        }
    }

    public static ShaderKey currentShader() {
        return currentShader;
    }

    /**
     * Undoes the texture-enable that {@link #setShader} turned off for an untextured draw.
     *
     * <p>In 1.21.4 selecting a core shader changes nothing global; in 1.8.9 the equivalent is
     * {@code glDisable(GL_TEXTURE_2D)}, which is sticky. Left disabled, everything drawn afterwards
     * samples no texture and comes out white — which is what happened to held items and block models
     * after the mod drew any colour-only geometry (rounded rects, trails, the HUD backgrounds). Every
     * draw funnels through {@link BufferRenderer}, so restoring it there means texturing can never
     * stay off past one of the mod's draws.
     */
    static void afterDraw() {
        if (!currentShader.isTextured()) {
            GlStateManager.enableTexture2D();
            currentShader = ShaderKey.POSITION_TEX_COLOR;
        }
    }

    /**
     * Puts the global fixed-function state back to what vanilla expects between passes.
     *
     * <p>Called at the end of each of the mod's event handlers, and after any module that threw, so
     * no half-applied state escapes into vanilla's or another mod's rendering. Lighting and the
     * projection are deliberately left alone: those belong to whichever pass is running.
     */
    public static void restoreDefaults() {
        try {
            GlStateManager.setActiveTexture(GL13.GL_TEXTURE0);
            GlStateManager.enableTexture2D();
            currentShader = ShaderKey.POSITION_TEX_COLOR;
            setShaderColor(1f, 1f, 1f, 1f);
            defaultBlendFunc();
            GlStateManager.disableBlend();
            GlStateManager.enableCull();
            GlStateManager.enableDepth();
            GlStateManager.depthMask(true);
            GlStateManager.enableAlpha();
            GlCompat.disableScissor();
            GlCompat.lineWidth(1f);
        } catch (Throwable ignored) {
            // Restoring state must never be the thing that throws.
        }
    }

    public static void setShaderTexture(int unit, ResourceLocation texture) {
        if (unit != 0) {
            GlStateManager.setActiveTexture(GL13.GL_TEXTURE0 + unit);
        }
        GlCompat.bindTexture(texture);
        if (unit != 0) {
            GlStateManager.setActiveTexture(GL13.GL_TEXTURE0);
        }
    }

    public static void setShaderTexture(int unit, int glId) {
        if (unit != 0) {
            GlStateManager.setActiveTexture(GL13.GL_TEXTURE0 + unit);
        }
        GlStateManager.bindTexture(glId);
        if (unit != 0) {
            GlStateManager.setActiveTexture(GL13.GL_TEXTURE0);
        }
    }

    // ---- blending --------------------------------------------------------------------------

    public static void enableBlend() {
        GlStateManager.enableBlend();
    }

    public static void disableBlend() {
        GlStateManager.disableBlend();
    }

    public static void defaultBlendFunc() {
        GlStateManager.tryBlendFuncSeparate(
                GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
    }

    public static void blendFunc(int src, int dst) {
        GlStateManager.blendFunc(src, dst);
    }

    public static void blendFunc(BlendFactor.SrcFactor src, BlendFactor.DstFactor dst) {
        GlStateManager.blendFunc(src.value, dst.value);
    }

    public static void blendFuncSeparate(int srcRgb, int dstRgb, int srcAlpha, int dstAlpha) {
        GlStateManager.tryBlendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
    }

    public static void blendFuncSeparate(BlendFactor.SrcFactor srcRgb, BlendFactor.DstFactor dstRgb,
                                         BlendFactor.SrcFactor srcAlpha, BlendFactor.DstFactor dstAlpha) {
        GlStateManager.tryBlendFuncSeparate(srcRgb.value, dstRgb.value, srcAlpha.value, dstAlpha.value);
    }

    // ---- depth / culling / masks -----------------------------------------------------------

    public static void enableDepthTest() {
        GlStateManager.enableDepth();
    }

    public static void disableDepthTest() {
        GlStateManager.disableDepth();
    }

    public static void depthMask(boolean enable) {
        GlStateManager.depthMask(enable);
    }

    public static void depthFunc(int func) {
        GlStateManager.depthFunc(func);
    }

    public static void enableCull() {
        GlStateManager.enableCull();
    }

    public static void disableCull() {
        GlStateManager.disableCull();
    }

    public static void colorMask(boolean r, boolean g, boolean b, boolean a) {
        GlStateManager.colorMask(r, g, b, a);
    }

    public static void clearColor(float r, float g, float b, float a) {
        GlStateManager.clearColor(r, g, b, a);
    }

    public static void clear(int mask) {
        GlStateManager.clear(mask);
    }

    /** 1.21.4 took a {@code boolean getError} second argument; kept for call-site parity. */
    public static void clear(int mask, boolean getError) {
        GlStateManager.clear(mask);
    }

    public static void lineWidth(float width) {
        GlCompat.lineWidth(width);
    }

    public static void enableScissor(int x1, int y1, int x2, int y2) {
        GlCompat.enableScissor(x1, y1, x2, y2);
    }

    public static void disableScissor() {
        GlCompat.disableScissor();
    }

    public static void enableAlphaTest() {
        GlStateManager.enableAlpha();
    }

    public static void disableAlphaTest() {
        GlStateManager.disableAlpha();
    }

    // ---- matrices --------------------------------------------------------------------------

    /**
     * 1.21.4 exposed a global model-view {@code MatrixStack}. 1.8.9 has only the GL stack, so this
     * returns a stack seeded from the live GL model-view matrix. Mutating it affects only the
     * returned copy; call {@link MatrixStack#applyToGl()} to push it back.
     */
    public static MatrixStack getModelViewStack() {
        MatrixStack stack = new MatrixStack();
        stack.set(getModelViewMatrix());
        return stack;
    }

    public static Mat4 getModelViewMatrix() {
        return GlCompat.readMatrix(GL11.GL_MODELVIEW_MATRIX);
    }

    public static Mat4 getProjectionMatrix() {
        return GlCompat.readMatrix(GL11.GL_PROJECTION_MATRIX);
    }

    // ---- render-thread helpers -------------------------------------------------------------

    /**
     * 1.8.9 is single-threaded for rendering: the client thread <i>is</i> the render thread, so this
     * is always true and {@link #recordRenderCall} can just run the task inline.
     */
    public static boolean isOnRenderThread() {
        return true;
    }

    public static void recordRenderCall(Runnable call) {
        call.run();
    }

    /** No initialisation phases to assert against in 1.8.9. */
    public static void assertInInitPhase() {
    }
}
