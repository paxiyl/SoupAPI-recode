package me.Padej_.soupapi.modules;

import me.Padej_.soupapi.compat.SoupScreen;

import me.Padej_.soupapi.compat.Texts;

import me.Padej_.soupapi.config.ConfigurableModule;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.IChatComponent;

public class SwingHand extends ConfigurableModule {
    public static float xPos = 0.7f;
    public static float yPos = -0.4f;
    public static float zPos = -0.85f;
    public static float scale = 0.75f;
    public static int rotX = 0;
    public static int rotY = -13;
    public static int rotZ = 8;
    public static int xSwingRot = -55;
    public static int ySwingRot = 0;
    public static int zSwingRot = 90;
    public static int speed = 100;

    private static boolean isConfigScreen() {
        if (mc.currentScreen == null) {
            return false;
        } else {
            return (mc.currentScreen instanceof SoupScreen ? ((SoupScreen) mc.currentScreen).getTitle() : null).equals(Texts.literal("Swing Hand GuiScreen"));
        }
    }

    public static float getxPos() {
        return isConfigScreen() ? xPos : CONFIG.swingHand_xPos;
    }

    public static float getyPos() {
        return isConfigScreen() ? yPos : CONFIG.swingHand_yPos;
    }

    public static float getzPos() {
        return isConfigScreen() ? zPos : CONFIG.swingHand_zPos;
    }

    public static float getScale() {
        return isConfigScreen() ? scale : CONFIG.swingHand_scale;
    }

    public static int getxSwingRot() {
        return isConfigScreen() ? xSwingRot : CONFIG.swingHand_xSwingRot;
    }

    public static int getySwingRot() {
        return isConfigScreen() ? ySwingRot : CONFIG.swingHand_ySwingRot;
    }

    public static int getzSwingRot() {
        return isConfigScreen() ? zSwingRot : CONFIG.swingHand_zSwingRot;
    }

    public static int getRotX() {
        return isConfigScreen() ? rotX : CONFIG.swingHand_rotX;
    }

    public static int getRotY() {
        return isConfigScreen() ? rotY : CONFIG.swingHand_rotY;
    }

    public static int getRotZ() {
        return isConfigScreen() ? rotZ : CONFIG.swingHand_rotZ;
    }

    public static int getSpeed() {
        return isConfigScreen() ? speed : CONFIG.swingHand_speed;
    }

    // ---- the first-person transform ---------------------------------------------------------

    /** The values the mod ships with; the transform is expressed as a delta from these. */
    private static final float DEF_X = 0.7f;
    private static final float DEF_Y = -0.4f;
    private static final float DEF_Z = -0.85f;
    private static final float DEF_SCALE = 0.75f;
    private static final int DEF_ROT_X = 0;
    private static final int DEF_ROT_Y = -13;
    private static final int DEF_ROT_Z = 8;
    private static final int DEF_SWING_X = -55;
    private static final int DEF_SWING_Y = 0;
    private static final int DEF_SWING_Z = 90;

    /**
     * Applies the configured hand placement to the current GL matrix.
     *
     * <p>The 1.21.4 version replaced {@code ItemRenderer.transformFirstPersonItem} wholesale from a
     * mixin. This port has no mixins and 1.8.9 keeps that transform inside a private method, so the
     * values are applied around vanilla's own placement instead, as an offset from the defaults
     * above: at default settings the hand sits exactly where vanilla puts it, and every slider moves
     * it from there. The swing rotations are applied in proportion to the swing animation, so they
     * only bend the arm while it is actually swinging.
     *
     * <p>{@code speed} scales how far through that swing the extra rotation is taken, which is as
     * much of it as can be done without owning the animation itself.
     */
    public static void applyTransform(float tickDelta) {
        GlStateManager.translate(getxPos() - DEF_X, getyPos() - DEF_Y, getzPos() - DEF_Z);

        float scale = getScale() / DEF_SCALE;
        if (Math.abs(scale - 1f) > 1.0E-4f) {
            GlStateManager.scale(scale, scale, scale);
        }

        GlStateManager.rotate(getRotX() - DEF_ROT_X, 1f, 0f, 0f);
        GlStateManager.rotate(getRotY() - DEF_ROT_Y, 0f, 1f, 0f);
        GlStateManager.rotate(getRotZ() - DEF_ROT_Z, 0f, 0f, 1f);

        float swing = swingAmount(tickDelta);
        if (swing > 0f) {
            GlStateManager.rotate((getxSwingRot() - DEF_SWING_X) * swing, 1f, 0f, 0f);
            GlStateManager.rotate((getySwingRot() - DEF_SWING_Y) * swing, 0f, 1f, 0f);
            GlStateManager.rotate((getzSwingRot() - DEF_SWING_Z) * swing, 0f, 0f, 1f);
        }
    }

    /** 0 at rest, peaking at 1 in the middle of a swing. */
    private static float swingAmount(float tickDelta) {
        if (mc.thePlayer == null) return 0f;
        float progress = mc.thePlayer.getSwingProgress(tickDelta) * (getSpeed() / 100f);
        progress = Math.max(0f, Math.min(1f, progress));
        return (float) Math.sin(progress * Math.PI);
    }

    public enum Config {
        CONFIG_HANDS
    }
}
