package me.Padej_.soupapi.event;

import me.Padej_.soupapi.compat.Camera;
import me.Padej_.soupapi.compat.MatrixStack;
import me.Padej_.soupapi.compat.RenderTickCounter;
import me.Padej_.soupapi.compat.VertexConsumerProvider;

/**
 * Replacement for Fabric API's {@code WorldRenderContext}.
 *
 * <p>Exposes the same four accessors the ported world-render code uses. A single instance is reused
 * each frame and refreshed by the render hook, so world rendering allocates nothing per frame.
 *
 * <p>The matrix stack starts out identity and in <i>camera space</i>: 1.8.9's world rendering has
 * already applied the view transform to the GL matrices by the time these events fire, so
 * mod geometry is positioned relative to {@link #camera()}'s position, exactly as the 1.21.4 code
 * expected.
 */
public final class WorldRenderContext {

    private final MatrixStack matrixStack = new MatrixStack();
    private final RenderTickCounter tickCounter = new RenderTickCounter(0f);
    private final Camera camera = new Camera();

    public void begin(net.minecraft.entity.Entity cameraEntity, float tickDelta) {
        tickCounter.setTickDelta(tickDelta);
        camera.update(cameraEntity, tickDelta);
        // Reset to identity; the previous frame's transforms must not leak into this one.
        while (!matrixStack.isEmpty()) {
            matrixStack.pop();
        }
        matrixStack.loadIdentity();
    }

    public MatrixStack matrixStack() {
        return matrixStack;
    }

    public RenderTickCounter tickCounter() {
        return tickCounter;
    }

    public Camera camera() {
        return camera;
    }

    public VertexConsumerProvider consumers() {
        return VertexConsumerProvider.immediate();
    }

    /** Convenience used by ported code that only wanted the delta. */
    public float tickDelta() {
        return tickCounter.getTickDelta(true);
    }
}
