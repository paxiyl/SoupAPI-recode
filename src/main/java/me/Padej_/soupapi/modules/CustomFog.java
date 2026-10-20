package me.Padej_.soupapi.modules;

import me.Padej_.soupapi.config.ConfigurableModule;

import net.minecraft.client.renderer.GlStateManager;
import me.Padej_.soupapi.utils.Palette;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

/**
 * Replaces the world fog with a palette-coloured one.
 *
 * <p Restructured for 1.8.9. 1.21.4 modelled fog as an immutable {@code Fog} record returned from
 * {@code BackgroundRenderer.applyFog}, which the mod simply replaced. 1.8.9 has no such object:
 * fog is fixed-function GL state, set up across two {@code EntityRenderer} methods — {@code
 * updateFog} picks the colour and {@code renderFog} sets the distance curve. So instead of
 * building a Fog value, this module exposes the colour and applies the GL state, and
 * {@code GameRendererFogMixin} calls into it at the end of each of those methods.
 *
 * <p>The sphere/cylinder distinction is kept as a choice between GL's {@code GL_EXP2} and
 * {@code GL_LINEAR} fog modes, which is the closest fixed-function equivalent: 1.21.4's
 * {@code FogShape.SPHERE} falls off with radial distance while {@code CYLINDER} is effectively
 * linear in view distance. Both are implemented natively by gl4es.
 */
public class CustomFog extends ConfigurableModule {


    /**
     * Whether custom fog should replace vanilla's this frame.
     *
     * <p>Blindness suppresses it, as before. 1.21.4 also checked the Darkness effect, which does
     * not exist in 1.8.9 (added in 1.19).
     */
    public static boolean shouldApply() {
        if (!CONFIG.customFogEnabled) return false;
        net.minecraft.client.entity.EntityPlayerSP player =
                net.minecraft.client.Minecraft.getMinecraft().thePlayer;
        return player != null && !player.isPotionActive(net.minecraft.potion.Potion.blindness);
    }

    /** The configured fog colour, from the active palette. */
    public static Color getFogColor() {
        return Palette.getColor(0);
    }

    /**
     * Opacity, 0..1, used as the GL fog density in exponential mode.
     *
     * <p>{@code customFogThick} doubles it, which is what "thick" meant on 1.21.4 where the fog
     * record carried a second, denser layer. The option was read by nothing before this.
     */
    public static float getDensity() {
        float density = CONFIG.customFogDensity / 100f;
        return CONFIG.customFogThick ? Math.min(1f, density * 2f) : density;
    }

    /**
     * Applies the fog distance curve.
     *
     * <p>Called from Forge's {@code EntityViewRenderEvent.RenderFogEvent}, which fires at the end
     * of {@code EntityRenderer.setupFog} — so this overrides whatever vanilla just configured,
     * which is what the 1.21.4 mixin on {@code renderFog} did. Without it the sphere/cylinder
     * choice did nothing: the fog density event alone only covers the exponential curve.
     */
    public static void applyFogState() {
        if (CONFIG.customFogShape == CustomFogShape.SPHERE) {
            GlStateManager.setFog(GL11.GL_EXP2);
            GlStateManager.setFogDensity(getDensity() * 0.1f);
        } else {
            GlStateManager.setFog(GL11.GL_LINEAR);
            GlStateManager.setFogStart(CONFIG.customFogStart);
            GlStateManager.setFogEnd(CONFIG.customFogEnd);
        }
        GlStateManager.enableFog();
    }

    public enum CustomFogShape {
        SPHERE, CYLINDER
    }
}
