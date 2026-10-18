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
 * The player's own arms, hung off the pack's hand bones.
 *
 * <p>The packs ship arms of their own, but they are posed around the view anchor of the mod they
 * were built for and land in the wrong place against a blade-centred fit. What they do have is the
 * <em>bones</em> — {@code lefthand_pos} and {@code righthand_pos} — and those are animated by every
 * clip whether their mesh is drawn or not. So the mesh stays off and the player's own arm is placed
 * on the bone's matrix instead: their model, their skin, their arm width, moving exactly with the
 * animation rather than bobbing along beside it.
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

            drew |= arm(model, pose, "righthand_pos", body.bipedRightArm, body.bipedRightArmwear, true);
            if (CONFIG.meleeLeftHand) {
                drew |= arm(model, pose, "lefthand_pos", body.bipedLeftArm, body.bipedLeftArmwear, false);
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
                               ModelRenderer sleeve, boolean right) {
        Mat4 matrix = model.boneMatrix(bone, pose);
        float[] pivot = model.bonePivot(bone);
        if (matrix == null || pivot == null || limb == null) return false;

        GlStateManager.pushMatrix();
        try {
            // Onto the bone, then to its pivot - the point the pack puts the wrist at.
            MATRIX.clear();
            MATRIX.put(matrix.m);
            MATRIX.flip();
            GL11.glMultMatrix(MATRIX);
            GlStateManager.translate(pivot[0], pivot[1], pivot[2]);

            float size = CONFIG.meleeHandsScale / 100f;
            GlStateManager.scale(size, size, size);
            GlStateManager.rotate(CONFIG.meleeHandsPitch, 1f, 0f, 0f);
            GlStateManager.rotate((right ? 1f : -1f) * CONFIG.meleeHandsYaw, 0f, 1f, 0f);
            GlStateManager.translate(
                    (right ? 1f : -1f) * CONFIG.meleeHandsOffsetX / 10f,
                    CONFIG.meleeHandsOffsetY / 10f,
                    CONFIG.meleeHandsOffsetZ / 10f);

            // A limb is built in sixteenths with +Y downward, so it is drawn upside down relative
            // to the model's own space; the arm hangs back from the wrist rather than forward.
            GlStateManager.scale(1f, -1f, 1f);
            GlStateManager.translate(0f, -2f, 0f);

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
