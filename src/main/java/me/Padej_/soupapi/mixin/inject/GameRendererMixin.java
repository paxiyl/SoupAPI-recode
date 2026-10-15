package me.Padej_.soupapi.mixin.inject;

import me.Padej_.soupapi.modules.AspectRatio;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Applies the custom aspect ratio.
 *
 * <p>Reworked for 1.8.9. 1.21.4 built the projection in
 * {@code GameRenderer.getBasicProjectionMatrix(float)} and the mod replaced the returned matrix.
 * 1.8.9 has no such method: {@code setupCamera} calls GLU's {@code gluPerspective(fov, aspect,
 * near, far)} directly, so the aspect argument is redirected instead. That is both a smaller
 * change and more faithful — vanilla's own near/far and zoom handling are left untouched.
 *
 * <p>The 1.21.4 version also hooked {@code renderFloatingItem} three times for the totem-pop
 * shader colour and scale. That method, and the Totem of Undying itself, arrived in 1.11; the
 * hooks and {@code GameRendererAccessor} are therefore removed rather than retargeted. See
 * PORTING.md.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Redirect(
            method = "setupCamera",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/lwjgl/util/glu/Project;gluPerspective(FFFF)V"
            )
    )
    private void soupapi$overrideAspectRatio(float fov, float aspect, float near, float far) {
        if (AspectRatio.enabled.getValue()) {
            aspect = AspectRatio.getRatioByPreset();
        }
        org.lwjgl.util.glu.Project.gluPerspective(fov, aspect, near, far);
    }
}
