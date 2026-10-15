package me.Padej_.soupapi.mixin.inject;

import me.Padej_.soupapi.modules.CustomFog;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.player.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.Color;

/**
 * Applies {@link CustomFog} to 1.8.9's fixed-function fog.
 *
 * <p>Replaces the 1.21.4 {@code BackgroundRenderer.applyFog} mixin, which returned a replacement
 * {@code Fog} record. 1.8.9 splits the job across two {@code GameRenderer} methods, so this hooks
 * both: {@code updateFog} chooses the colour (written into the renderer's own fogRed/fogGreen/
 * fogBlue fields, which also drive the sky clear colour), and {@code renderFog} sets the distance
 * curve.
 *
 * <p>Blindness still suppresses custom fog, as before. 1.21.4 also checked for the Darkness
 * effect, which does not exist in 1.8.9 (added in 1.19).
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererFogMixin {

    @Shadow
    private float fogRed;

    @Shadow
    private float fogGreen;

    @Shadow
    private float fogBlue;

    @Inject(method = "updateFog", at = @At("TAIL"))
    private void soupapi$recolourFog(float tickDelta, CallbackInfo ci) {
        if (!shouldApply()) return;
        Color c = CustomFog.getFogColor();
        this.fogRed = c.getRed() / 255f;
        this.fogGreen = c.getGreen() / 255f;
        this.fogBlue = c.getBlue() / 255f;
    }

    @Inject(method = "renderFog", at = @At("TAIL"))
    private void soupapi$applyFogState(int pass, float tickDelta, CallbackInfo ci) {
        if (!shouldApply()) return;
        CustomFog.applyFogState();
    }

    private static boolean shouldApply() {
        if (!CustomFog.enabled.getValue()) return false;
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) return false;
        return !player.hasStatusEffect(StatusEffect.BLINDNESS);
    }
}
