package me.Padej_.soupapi.modules;

import me.Padej_.soupapi.config.ConfigurableModule;

public class AspectRatio extends ConfigurableModule {


    public static float getRatioByPreset() {
        if (CONFIG.aspectRatioUsePreset) {
            switch (CONFIG.aspectRatioPreset) {
            case _16_9_:
                return 16f / 9f;
            case _5_4_:
                return 5f / 4f;
            case _4_3_:
                return 4f / 3f;
        }
        throw new IllegalStateException("Unexpected value: " + CONFIG.aspectRatioPreset);
        } else {
            return CONFIG.aspectRatioFactor / 100f;
        }
    }

    /** Kept for the FOV event; the aspect itself is applied in {@link #applyToProjection()}. */
    public static void onFovUpdate() {
    }

    /**
     * Stretches the world projection to the configured aspect ratio.
     *
     * <p>No Forge event carries the projection's aspect — {@code FOVUpdateEvent} is only the field
     * of view — so this was written off as impossible for a port with no bytecode patching. It is
     * not: vanilla builds the perspective in {@code setupCameraTransform} and then calls
     * {@code orientCamera}, which fires {@code EntityViewRenderEvent.CameraSetup}. By that point
     * the projection matrix is on the stack, so scaling it along X from there changes exactly what
     * a different aspect ratio would have changed. The matrix mode is restored afterwards, since
     * everything downstream assumes model-view.
     */
    public static void applyToProjection() {
        if (!CONFIG.aspectRatioEnabled) return;

        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
        if (mc.displayHeight <= 0) return;

        float actual = (float) mc.displayWidth / (float) mc.displayHeight;
        float wanted = getRatioByPreset();
        if (wanted <= 0f || Math.abs(wanted - actual) < 1.0E-4f) return;

        // gluPerspective divides x by the aspect, so the correction is the ratio of the two.
        float scaleX = actual / wanted;
        net.minecraft.client.renderer.GlStateManager.matrixMode(org.lwjgl.opengl.GL11.GL_PROJECTION);
        net.minecraft.client.renderer.GlStateManager.scale(scaleX, 1f, 1f);
        net.minecraft.client.renderer.GlStateManager.matrixMode(org.lwjgl.opengl.GL11.GL_MODELVIEW);
    }

    public enum Preset {
        _16_9_, _5_4_, _4_3_
    }
}