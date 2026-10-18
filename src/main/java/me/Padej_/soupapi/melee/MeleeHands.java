package me.Padej_.soupapi.melee;

import me.Padej_.soupapi.bedrock.GeoModel;
import me.Padej_.soupapi.bedrock.GeoPose;
import me.Padej_.soupapi.bedrock.Mat4;
import me.Padej_.soupapi.config.ConfigurableModule;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.nio.FloatBuffer;

/**
 * The player's own arms, dropped into the pack's hand markers.
 *
 * <p>The packs carry no arm artwork. What {@code lefthand_pos} and {@code righthand_pos} hold is a
 * single untextured box each, twelve units long and four across, shoulder at the high-Y end — a
 * vanilla player arm, to the unit. The mod they were built for reads those as "put the player's arm
 * here", and so does this: the marker stays hidden and the player's own limb is drawn into it, with
 * their skin and their arm width, animated by whatever clip is playing because the pack animates
 * those bones in every one.
 *
 * <p>Drawn inside the weapon's own matrix, so nothing here can move the viewmodel.
 */
public final class MeleeHands extends ConfigurableModule {

    private static final FloatBuffer MATRIX = BufferUtils.createFloatBuffer(16);
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
     * Draws both arms. Call inside the weapon's matrix, where one unit is one model pixel.
     *
     * @return true when something was drawn
     */
    public static boolean render(MeleeWeapon weapon) {
        if (!CONFIG.meleeHands) return false;
        if (!(mc.thePlayer instanceof AbstractClientPlayer)) return false;
        AbstractClientPlayer player = (AbstractClientPlayer) mc.thePlayer;
        GeoModel model = weapon.model();
        GeoPose pose = weapon.pose();
        if (model == null || pose == null) return false;

        ResourceLocation skin = player.getLocationSkin();
        if (skin == null) return false;
        boolean thin = "slim".equals(player.getSkinType());
        ModelPlayer body = model(thin);

        boolean drew = false;
        try {
            RenderHelper.enableStandardItemLighting();
            GlStateManager.enableDepth();
            GlStateManager.disableBlend();
            GlStateManager.color(1f, 1f, 1f, 1f);
            mc.getTextureManager().bindTexture(skin);

            drew |= arm(model, pose, MeleeWeapon.RIGHT_HAND, body.bipedRightArm,
                    body.bipedRightArmwear, true, thin);
            if (CONFIG.meleeLeftHand) {
                drew |= arm(model, pose, MeleeWeapon.LEFT_HAND, body.bipedLeftArm,
                        body.bipedLeftArmwear, false, thin);
            }
        } catch (Throwable t) {
            me.Padej_.soupapi.compat.SoupLog.warn(
                    "[SoupAPI] drawing the first-person arms failed; they are now off", t);
            CONFIG.meleeHands = false;
        } finally {
            RenderHelper.disableStandardItemLighting();
            GlStateManager.color(1f, 1f, 1f, 1f);
        }
        return drew;
    }

    private static boolean arm(GeoModel model, GeoPose pose, String bone, ModelRenderer limb,
                               ModelRenderer sleeve, boolean right, boolean thin) {
        Mat4 matrix = model.boneMatrix(bone, pose);
        float[] box = model.cubeBox(bone);
        if (matrix == null || box == null || limb == null) return false;

        GlStateManager.pushMatrix();
        try {
            // Onto the bone, then into the marker box it carries: an arm-shaped box, twelve units
            // long, shoulder at the high-Y end.
            MATRIX.clear();
            MATRIX.put(matrix.m);
            MATRIX.flip();
            GL11.glMultMatrix(MATRIX);
            GlStateManager.translate((box[0] + box[3]) / 2f, box[4], (box[2] + box[5]) / 2f);

            float size = CONFIG.meleeHandsScale / 100f;
            GlStateManager.scale(size, size, size);
            GlStateManager.rotate(CONFIG.meleeHandsPitch, 1f, 0f, 0f);
            GlStateManager.rotate((right ? 1f : -1f) * CONFIG.meleeHandsYaw, 0f, 1f, 0f);
            GlStateManager.translate(
                    (right ? 1f : -1f) * CONFIG.meleeHandsOffsetX / 10f,
                    CONFIG.meleeHandsOffsetY / 10f,
                    CONFIG.meleeHandsOffsetZ / 10f);

            // A limb is built with +Y downward from the shoulder, so it runs the other way from the
            // marker; flipping Y points it down the box, from the shoulder toward the wrist.
            GlStateManager.scale(1f, -1f, 1f);
            // A limb's own box starts two units above its rotation point and is centred to one
            // side of it - outward, so the two arms mirror: a unit on a normal skin, half on a slim
            // one. Undoing that puts the box on the marker rather than beside it.
            float half = thin ? 0.5f : 1f;
            GlStateManager.translate(right ? half : -half, 2f, 0f);

            limb.rotateAngleX = 0f;
            limb.rotateAngleY = 0f;
            limb.rotateAngleZ = 0f;
            limb.rotationPointX = 0f;
            limb.rotationPointY = 0f;
            limb.rotationPointZ = 0f;
            limb.render(1f);
            if (sleeve != null) {
                sleeve.rotateAngleX = 0f;
                sleeve.rotateAngleY = 0f;
                sleeve.rotateAngleZ = 0f;
                sleeve.rotationPointX = 0f;
                sleeve.rotationPointY = 0f;
                sleeve.rotationPointZ = 0f;
                sleeve.render(1f);
            }
        } finally {
            GlStateManager.popMatrix();
        }
        return true;
    }
}
