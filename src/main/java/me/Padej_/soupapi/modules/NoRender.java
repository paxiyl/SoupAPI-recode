package me.Padej_.soupapi.modules;

import me.Padej_.soupapi.compat.DrawContext;
import me.Padej_.soupapi.compat.RenderSystem;
import me.Padej_.soupapi.config.ConfigurableModule;
import me.Padej_.soupapi.render.Render2D;

import java.awt.Color;

/**
 * The "no render" options: replaces the burning overlay with something you can see past.
 *
 * <p>1.8.9 does not draw the fire overlay as a HUD element, so there is no overlay event to cancel;
 * it is drawn in {@code ItemRenderer.renderOverlays}. Forge's {@code RenderBlockOverlayEvent}
 * covers exactly that call and is cancellable, which is what {@link #shouldCancelFire()} answers.
 *
 * <p>With a custom colour chosen, a flat band is drawn in its place: {@code noFireOverlayY} is how
 * far up the screen it reaches, as a percentage, and the alpha and colour are the configured ones.
 * All five of these options were read by nothing before.
 */
public class NoRender extends ConfigurableModule {

    public static boolean shouldCancelFire() {
        return CONFIG.noFireOverlayEnabled;
    }

    /** Drawn in the HUD pass, after the rest of the overlay. */
    public static void renderFireReplacement(DrawContext context) {
        if (!CONFIG.noFireOverlayEnabled || !CONFIG.noFireOverlayCustomColorEnabled) return;
        if (mc.thePlayer == null || !mc.thePlayer.isBurning()) return;

        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();
        int band = Math.round(height * (CONFIG.noFireOverlayY / 100f));
        if (band <= 0) return;

        int rgb = CONFIG.noFireOverlayCustomColor;
        int alpha = Math.round(255 * (CONFIG.noFireOverlayAlpha / 100f));
        Color top = new Color((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, 0);
        Color bottom = new Color((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, alpha);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        Render2D.renderRoundedGradientRect(context.getMatrices(), top, bottom, bottom, top,
                0, height - band, width, band, 0f);
        RenderSystem.disableBlend();
    }
}
