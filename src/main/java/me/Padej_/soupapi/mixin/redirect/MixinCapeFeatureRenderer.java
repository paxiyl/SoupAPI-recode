package me.Padej_.soupapi.mixin.redirect;

import com.mojang.blaze3d.platform.GlStateManager;
import me.Padej_.soupapi.compat.RenderSystem;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.feature.CapeFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lets capes with translucent pixels render correctly.
 *
 * <p>1.21.4 achieved this by redirecting the cape's render layer from {@code getEntitySolid} to
 * {@code getEntityCutout}. 1.8.9 has no render layers: the cape is drawn immediately with whatever
 * GL state the entity renderer left set, where alpha testing discards fully transparent pixels but
 * partial alpha is not blended.
 *
 * <p>So blending is enabled around the cape draw and restored afterwards. Restoring matters —
 * the fixed-function pipeline is global, and leaving blending on would alter everything drawn
 * after it in the frame.
 */
@Mixin(CapeFeatureRenderer.class)
public class MixinCapeFeatureRenderer {

    @Inject(
            method = "render(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFFFFFF)V",
            at = @At("HEAD")
    )
    private void soupapi$enableCapeBlending(AbstractClientPlayerEntity entity, float limbAngle,
                                            float limbDistance, float tickDelta, float age,
                                            float headYaw, float headPitch, float scale,
                                            CallbackInfo ci) {
        GlStateManager.enableBlend();
        RenderSystem.defaultBlendFunc();
    }

    @Inject(
            method = "render(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFFFFFF)V",
            at = @At("TAIL")
    )
    private void soupapi$restoreCapeBlending(AbstractClientPlayerEntity entity, float limbAngle,
                                             float limbDistance, float tickDelta, float age,
                                             float headYaw, float headPitch, float scale,
                                             CallbackInfo ci) {
        GlStateManager.disableBlend();
    }
}
