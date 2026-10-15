package me.Padej_.soupapi.mixin.event;

import me.Padej_.soupapi.event.SoupEvents;
import me.Padej_.soupapi.font.FontRenderers;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Drives {@link SoupEvents#HUD_RENDER}, replacing Fabric API's {@code HudRenderCallback}.
 *
 * <p>{@code InGameHud.render(float)} runs in scaled GUI space with the ortho projection already
 * set up, so listeners can draw in GUI coordinates directly, as they did on 1.21.4.
 *
 * <p>This is also where the fonts are brought up: it is the earliest point that is guaranteed to
 * run with game options loaded and a GL context current, which font atlas creation needs.
 */
@Mixin(InGameHud.class)
public class HudRenderHookMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void soupapi$hudRender(float tickDelta, CallbackInfo ci) {
        FontRenderers.ensureLoaded();
        SoupEvents.fireHudRender(tickDelta);
    }
}
