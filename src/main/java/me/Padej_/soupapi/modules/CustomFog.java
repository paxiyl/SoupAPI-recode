package me.Padej_.soupapi.modules;

import net.minecraft.client.renderer.GlStateManager;
import me.Padej_.soupapi.SoupModule;
import me.Padej_.soupapi.settings.impl.BooleanSetting;
import me.Padej_.soupapi.settings.impl.EnumSetting;
import me.Padej_.soupapi.settings.impl.SliderSetting;
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
public class CustomFog extends SoupModule {
    public static final BooleanSetting enabled = new BooleanSetting("Enabled", "description", false);
    public static final BooleanSetting thick = new BooleanSetting("Enabled", "description", false);
    public static final EnumSetting<CustomFog.CustomFogShape> shape = new EnumSetting<>("Shape", "description", CustomFogShape.SPHERE, CustomFogShape.class);
    public static final SliderSetting density = new SliderSetting("Density", "desc", 80, 1, 100, 1);
    public static final SliderSetting start = new SliderSetting("Start", "desc", 5, 1, 100, 1);
    public static final SliderSetting end = new SliderSetting("End", "desc", 20, 1, 100, 1);

    public CustomFog() {
        super("Custom Fog", Category.WORLD);
    }

    /** The configured fog colour, from the active palette. */
    public static Color getFogColor() {
        return Palette.getColor(0);
    }

    /** Opacity, 0..1, used as the GL fog density in exponential mode. */
    public static float getDensity() {
        return density.getValue() / 100f;
    }

    /**
     * Applies the fog distance curve. Called at the end of {@code EntityRenderer.renderFog}, so it
     * overrides whatever vanilla just configured.
     */
    public static void applyFogState() {
        if (shape.getValue() == CustomFogShape.SPHERE) {
            GlStateManager.setFog(GL11.GL_EXP2);
            GlStateManager.setFogDensity(getDensity() * 0.1f);
        } else {
            GlStateManager.setFog(GL11.GL_LINEAR);
            GlStateManager.setFogStart(start.getValue());
            GlStateManager.setFogEnd(end.getValue());
        }
        GlStateManager.enableFog();
    }

    public enum CustomFogShape {
        SPHERE, CYLINDER
    }
}
