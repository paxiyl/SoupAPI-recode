package me.Padej_.soupapi.melee;

import me.Padej_.soupapi.config.ConfigurableModule;
import me.Padej_.soupapi.utils.MathUtility;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.util.ResourceLocation;

/**
 * The player's own arms, holding the weapon.
 *
 * <p>The packs ship arms of their own, but they are posed around the view anchor of the mod they
 * were built for — an anchor this port cannot use, because the packs disagree about where it is —
 * so against a blade-centred fit they land in the wrong place and read as enormous flat sheets.
 * With them off the weapon floated with nothing holding it, which looks worse than either.
 *
 * <p>These are the arms Minecraft already has: the player's own model, their own skin, their own
 * slim-or-wide arm. Drawn at a fixed place in the view with the weapon's own placement left alone,
 * so adding them cannot move the viewmodel — they are posed to reach the weapon, not the other way
 * round.
 */
public final class MeleeHands extends ConfigurableModule {

    private static ModelPlayer wide;
    private static ModelPlayer slim;

    private MeleeHands() {
    }

    private static ModelPlayer model(boolean thin) {
        if (thin) {
            if (slim == null) slim = new ModelPlayer(0f, true);
            return slim;
        }
        if (wide == null) wide = new ModelPlayer(0f, false);
        return wide;
    }

    /**
     * Draws the arms, inside the hand pass and outside the weapon's own matrix.
     *
     * @param swing 0 at rest, rising to 1 across a swing, so the arms move with the attack
     */
    public static void render(float swing) {
        if (!CONFIG.meleeHands) return;
        if (!(mc.thePlayer instanceof AbstractClientPlayer)) return;
        AbstractClientPlayer player = (AbstractClientPlayer) mc.thePlayer;

        ResourceLocation skin = player.getLocationSkin();
        if (skin == null) return;
        boolean thin = "slim".equals(player.getSkinType());
        ModelPlayer model = model(thin);

        GlStateManager.pushMatrix();
        try {
            RenderHelper.enableStandardItemLighting();
            GlStateManager.enableDepth();
            GlStateManager.disableBlend();
            GlStateManager.color(1f, 1f, 1f, 1f);
            mc.getTextureManager().bindTexture(skin);

            // A hand's worth of lift as the swing peaks, so the arms are not static under a weapon
            // that is moving.
            float lift = MathUtility.sin(swing * (float) Math.PI) * 0.12f
                    * (CONFIG.meleeHandsSwing / 100f);

            arm(model, model.bipedRightArm, model.bipedRightArmwear, true, lift, thin);
            if (CONFIG.meleeLeftHand) {
                arm(model, model.bipedLeftArm, model.bipedLeftArmwear, false, lift, thin);
            }
        } catch (Throwable t) {
            me.Padej_.soupapi.compat.SoupLog.warn(
                    "[SoupAPI] drawing the first-person arms failed; they are now off", t);
            CONFIG.meleeHands = false;
        } finally {
            RenderHelper.disableStandardItemLighting();
            GlStateManager.color(1f, 1f, 1f, 1f);
            GlStateManager.popMatrix();
        }
    }

    /**
     * One arm, reaching in from the bottom corner.
     *
     * <p>The numbers are the same shape as vanilla's own first-person arm: out to the side, down,
     * forward, then rotated so the forearm runs away from the camera and the hand ends up near
     * where the weapon is drawn.
     */
    private static void arm(ModelPlayer model, ModelRenderer limb, ModelRenderer sleeve,
                            boolean right, float lift, boolean thin) {
        float side = right ? 1f : -1f;
        float reach = CONFIG.meleeHandsReach / 100f;
        float spread = CONFIG.meleeHandsSpread / 100f;

        GlStateManager.pushMatrix();
        GlStateManager.translate(
                side * (0.52f * spread) + CONFIG.meleeHandsOffsetX / 100f * side,
                -0.62f + lift + CONFIG.meleeHandsOffsetY / 100f,
                -0.60f * reach + CONFIG.meleeHandsOffsetZ / 100f);

        GlStateManager.rotate(side * 12f, 0f, 1f, 0f);
        GlStateManager.rotate(-72f + CONFIG.meleeHandsPitch, 1f, 0f, 0f);
        GlStateManager.rotate(side * -8f, 0f, 0f, 1f);

        // Into model space: a limb is drawn in sixteenths, +Y down, around its own pivot.
        GlStateManager.scale(-1f, -1f, 1f);
        GlStateManager.translate(side * -0.0625f, -0.38f, 0f);

        limb.rotateAngleX = 0f;
        limb.rotateAngleY = 0f;
        limb.rotateAngleZ = 0f;
        limb.rotationPointX = 0f;
        limb.rotationPointY = 0f;
        limb.rotationPointZ = 0f;
        limb.render(0.0625f);
        if (sleeve != null) {
            sleeve.rotateAngleX = 0f;
            sleeve.rotateAngleY = 0f;
            sleeve.rotateAngleZ = 0f;
            sleeve.rotationPointX = 0f;
            sleeve.rotationPointY = 0f;
            sleeve.rotationPointZ = 0f;
            sleeve.render(0.0625f);
        }
        GlStateManager.popMatrix();
    }
}
