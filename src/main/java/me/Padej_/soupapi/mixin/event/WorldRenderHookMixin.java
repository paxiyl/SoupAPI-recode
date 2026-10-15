package me.Padej_.soupapi.mixin.event;

import me.Padej_.soupapi.compat.Cameras;
import me.Padej_.soupapi.event.SoupEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
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

    @Shadow
    private MinecraftClient client;

    @Inject(method = "renderWorld(IFJ)V", at = @At("TAIL"))
    private void soupapi$worldRenderLast(int pass, float tickDelta, long limitTime, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;

        net.minecraft.entity.Entity cameraEntity =
                mc.getCameraEntity() != null ? mc.getCameraEntity() : mc.player;
        Cameras.update(cameraEntity, tickDelta);
        SoupEvents.fireWorldRenderLast(cameraEntity, tickDelta);
    }
}
