package me.Padej_.soupapi.mixin.event;

import me.Padej_.soupapi.compat.Camera;
import me.Padej_.soupapi.compat.Cameras;
import me.Padej_.soupapi.compat.MatrixStack;
import me.Padej_.soupapi.compat.RenderSystem;
import me.Padej_.soupapi.compat.RotationAxis;
import me.Padej_.soupapi.event.SoupEvents;
import me.Padej_.soupapi.modules.TargetHud;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Drives {@link SoupEvents#WORLD_RENDER_LAST}, replacing Fabric API's
 * {@code WorldRenderEvents.LAST}.
 *
 * <p>Injected at the tail of {@code GameRenderer.renderWorld(int, float, long)} — the private
 * overload that does the actual work — so the camera transform is still on the GL matrix stack and
 * mod geometry is positioned in camera space, which is what the ported render code assumes.
 *
 * <p>The camera singleton is refreshed here, before listeners run, so every call site that reads
 * {@link Cameras#get()} during this frame sees a consistent position and rotation.
 */
@Mixin(GameRenderer.class)
public class WorldRenderHookMixin {

    @Inject(method = "renderWorld(IFJ)V", at = @At("TAIL"))
    private void soupapi$worldRenderLast(int pass, float tickDelta, long limitTime, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;

        net.minecraft.entity.Entity cameraEntity =
                mc.getCameraEntity() != null ? mc.getCameraEntity() : mc.player;
        Cameras.update(cameraEntity, tickDelta);
        captureWorldSpaceMatrices();
        SoupEvents.fireWorldRenderLast(cameraEntity, tickDelta);
    }

    /**
     * Records the projection, model-view and camera-space matrices the target HUD needs to project
     * world positions onto the screen.
     *
     * <p>In 1.21.4 this lived in its own {@code GameRenderer} hook keyed off a field access inside
     * {@code renderWorld}. Doing it here instead means it is captured at a point already known to
     * be correct, and the GL matrices read back are the ones actually in effect for this frame.
     */
    private static void captureWorldSpaceMatrices() {
        Camera camera = Cameras.get();
        MatrixStack matrixStack = new MatrixStack();
        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0f));

        TargetHud.lastProjMat.set(RenderSystem.getProjectionMatrix());
        TargetHud.lastModMat.set(RenderSystem.getModelViewMatrix());
        TargetHud.lastWorldSpaceMatrix.set(matrixStack.peek().getPositionMatrix());
    }
}
