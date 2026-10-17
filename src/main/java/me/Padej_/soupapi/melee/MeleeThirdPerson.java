package me.Padej_.soupapi.melee;

import me.Padej_.soupapi.bedrock.GeoModel;
import me.Padej_.soupapi.bedrock.GeoPose;
import me.Padej_.soupapi.config.ConfigurableModule;
import me.Padej_.soupapi.utils.MathUtility;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

/**
 * The carried weapon on the player's own body, for third person and the inventory view.
 *
 * <p>Only the weapon is drawn, never the pack's arms: the body already has arms, and the pack's are
 * posed for a first-person camera. The weapon hangs off the right hand, following the player
 * model's own arm angle, so it swings with the vanilla animation rather than fighting it.
 */
public final class MeleeThirdPerson extends ConfigurableModule {

    private MeleeThirdPerson() {
    }

    /** Draws the weapon in the player's right hand. Called from the player render hook. */
    public static void render(EntityPlayer player, RenderPlayer renderer, float partialTicks) {
        if (!CONFIG.meleeThirdPerson) return;
        if (player != mc.thePlayer) return;              // other players carry their own, not yours
        MeleeWeapon weapon = Melee.active();
        if (weapon == null) return;
        GeoModel model = weapon.model();
        GeoPose pose = weapon.pose();
        ResourceLocation texture = weapon.texture();
        if (model == null || pose == null || texture == null) return;
        if (!(renderer.getMainModel() instanceof ModelPlayer)) return;
        ModelPlayer body = (ModelPlayer) renderer.getMainModel();

        GlStateManager.pushMatrix();
        try {
            GlStateManager.disableCull();
            // Into the model's own space, then out to the right shoulder and down the arm.
            GlStateManager.scale(-1f, -1f, 1f);
            body.bipedRightArm.postRender(0.0625f);

            MeleeWeapon.Fit fit = weapon.fit();
            float size = CONFIG.meleeSize / 100f * 0.55f;
            GlStateManager.translate(-0.0625f, 0.45f, 0.0625f);
            GlStateManager.rotate(-90f, 1f, 0f, 0f);
            GlStateManager.rotate(180f, 0f, 1f, 0f);

            float scale = fit.scale * size / 16f;
            GlStateManager.translate(fit.offsetX * size, fit.offsetY * size, fit.offsetZ * size);
            GlStateManager.scale(-scale, scale, scale);

            mc.getTextureManager().bindTexture(texture);
            GlStateManager.color(1f, 1f, 1f, 1f);
            model.render(pose);
        } catch (Throwable t) {
            me.Padej_.soupapi.compat.SoupLog.warn(
                    "[SoupAPI] drawing the melee weapon in third person failed; it is now off", t);
            CONFIG.meleeThirdPerson = false;
        } finally {
            GlStateManager.enableCull();
            GlStateManager.color(1f, 1f, 1f, 1f);
            GlStateManager.popMatrix();
        }
    }

    /** Kept so the renderer and this agree on what "a swing" looks like. */
    static float swingProgress(EntityPlayer player, float partialTicks) {
        return MathUtility.clamp(player.getSwingProgress(partialTicks), 0f, 1f);
    }
}
