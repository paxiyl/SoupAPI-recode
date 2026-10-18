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
 * The player's own arms, reaching from Minecraft's shoulder to the pack's grip.
 *
 * <p>The packs carry no arm artwork. What {@code lefthand_pos} and {@code righthand_pos} hold is a
 * single untextured box each — twelve units long, four across, shoulder at the high-Y end: a vanilla
 * player arm to the unit. The obvious thing is to drop the player's arm into that box, and it is
 * wrong, because the pack's arms are not posed anything like Minecraft's. Measured against 1.8.9's
 * own first-person arm, the pack puts its shoulder within a tenth of a block of vanilla's in X and
 * Z but <em>0.58 of a block higher</em> — up at eye level — with the arm reaching down to a grip
 * near the waist, where vanilla's starts low and reaches up. Pinning the pack's marker onto
 * vanilla's arm drops the whole weapon off the bottom of the screen; using the marker where it is
 * grows an arm out of the middle of your face. Both were rendered. Both are wrong.
 *
 * <p>So the two ends come from different places. The <em>shoulder</em> is Minecraft's — the point
 * {@code ItemRenderer} puts it at, so the arm enters the frame from the lower corner the way every
 * other held item does. The <em>wrist</em> is the pack's, animated, so the hand stays on the grip
 * through a swing. The arm is drawn between them and stretches to span the gap, which is not
 * something anyone can check against their own forearm in first person.
 *
 * <p>Drawn after the weapon and outside its matrix, since the placement no longer applies to it;
 * {@link MeleeRenderer} hands over that matrix so the grip can be found in the view.
 */
public final class MeleeHands extends ConfigurableModule {

    private static final FloatBuffer MATRIX = BufferUtils.createFloatBuffer(16);

    /**
     * Where 1.8.9 puts the first-person right shoulder, and which way that arm's flat side faces.
     *
     * <p>Read out of {@code ItemRenderer.func_178095_a} followed by
     * {@code RenderPlayer.renderRightArm} with swing and equip progress at zero — the same chain of
     * translates and rotates vanilla uses for a bare hand — composed and evaluated at the arm
     * model's shoulder end. The left shoulder is this mirrored in X.
     */
    private static final float[] SHOULDER = {0.4853f, -0.8315f, -0.5254f};
    private static final float[] SIDE = {-0.36795f, -0.73329f, -0.57175f};

    /** A limb is twelve units long, and 1.8.9 draws entity models at a sixteenth of a block. */
    private static final float LIMB_UNITS = 12f;
    private static final float UNIT = 0.0625f;

    /**
     * How far the arm may stretch, in blocks.
     *
     * <p>A forearm is 0.75, and nine of this pack's eleven weapons put their grip 0.70 to 1.21 from
     * the shoulder, which is a reach. The two Annihils hold their blades out to the left at 1.66,
     * and an arm drawn that long reads as rubber, so it stops short of the grip instead: a hand near
     * the weapon looks better than an arm stretched to it.
     */
    private static final float MAX_REACH = 1.35f;

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
     * Draws both arms in the view, given the weapon's own model-units-to-view transform.
     *
     * @return true when something was drawn
     */
    public static boolean render(MeleeWeapon weapon, Mat4 view) {
        if (!CONFIG.meleeHands) return false;
        if (view == null) return false;
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
            GlStateManager.depthMask(true);
            GlStateManager.disableBlend();
            GlStateManager.enableCull();
            GlStateManager.color(1f, 1f, 1f, 1f);
            mc.getTextureManager().bindTexture(skin);

            drew |= arm(model, pose, view, MeleeWeapon.RIGHT_HAND, body.bipedRightArm,
                    body.bipedRightArmwear, true, thin);
            if (CONFIG.meleeLeftHand) {
                drew |= arm(model, pose, view, MeleeWeapon.LEFT_HAND, body.bipedLeftArm,
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

    private static boolean arm(GeoModel model, GeoPose pose, Mat4 view, String bone,
                               ModelRenderer limb, ModelRenderer sleeve, boolean right,
                               boolean thin) {
        if (limb == null) return false;
        float[] wristModel = MeleeWeapon.wristOf(model, pose, bone);
        if (wristModel == null) return false;

        float[] wrist = new float[3];
        view.transform(wristModel[0], wristModel[1], wristModel[2], wrist);

        float mirror = right ? 1f : -1f;
        float[] shoulder = {
                (SHOULDER[0] + CONFIG.meleeHandsOffsetX / 100f) * mirror,
                SHOULDER[1] + CONFIG.meleeHandsOffsetY / 100f,
                SHOULDER[2] + CONFIG.meleeHandsOffsetZ / 100f,
        };

        // +Y runs shoulder to wrist; the other two are rolled to match vanilla's arm, so the skin
        // lands the way it would on a bare hand.
        float[] y = {wrist[0] - shoulder[0], wrist[1] - shoulder[1], wrist[2] - shoulder[2]};
        float length = (float) Math.sqrt(y[0] * y[0] + y[1] * y[1] + y[2] * y[2]);
        if (length < 1.0E-4f) return false;
        y[0] /= length;
        y[1] /= length;
        y[2] /= length;
        if (length > MAX_REACH) length = MAX_REACH;

        float[] side = {SIDE[0] * mirror, SIDE[1], SIDE[2]};
        float along = side[0] * y[0] + side[1] * y[1] + side[2] * y[2];
        float[] x = {side[0] - y[0] * along, side[1] - y[1] * along, side[2] - y[2] * along};
        float xLength = (float) Math.sqrt(x[0] * x[0] + x[1] * x[1] + x[2] * x[2]);
        if (xLength < 1.0E-4f) {
            // The arm points straight down vanilla's flat side; any perpendicular will do.
            x[0] = 1f - y[0] * y[0];
            x[1] = -y[0] * y[1];
            x[2] = -y[0] * y[2];
            xLength = (float) Math.sqrt(x[0] * x[0] + x[1] * x[1] + x[2] * x[2]);
            if (xLength < 1.0E-4f) return false;
        }
        x[0] /= xLength;
        x[1] /= xLength;
        x[2] /= xLength;
        float[] z = {
                x[1] * y[2] - x[2] * y[1],
                x[2] * y[0] - x[0] * y[2],
                x[0] * y[1] - x[1] * y[0],
        };

        Mat4 frame = new Mat4();
        frame.m[0] = x[0];
        frame.m[1] = x[1];
        frame.m[2] = x[2];
        frame.m[4] = y[0];
        frame.m[5] = y[1];
        frame.m[6] = y[2];
        frame.m[8] = z[0];
        frame.m[9] = z[1];
        frame.m[10] = z[2];
        frame.m[12] = shoulder[0];
        frame.m[13] = shoulder[1];
        frame.m[14] = shoulder[2];

        GlStateManager.pushMatrix();
        try {
            MATRIX.clear();
            MATRIX.put(frame.m);
            MATRIX.flip();
            GL11.glMultMatrix(MATRIX);

            // The thickness stays a player arm's; only the length is stretched, so the hand reaches
            // the grip however far away the pack put it.
            float thickness = UNIT * CONFIG.meleeHandsScale / 100f;
            GlStateManager.scale(thickness, length / LIMB_UNITS, thickness);
            // A limb's box starts two units above its rotation point and sits a unit (half a unit on
            // a slim skin) to one side of it; undoing that puts the shoulder end at the origin.
            GlStateManager.translate(right ? (thin ? 0.5f : 1f) : (thin ? -0.5f : -1f), 2f, 0f);

            draw(limb);
            if (sleeve != null) draw(sleeve);
        } finally {
            GlStateManager.popMatrix();
        }
        return true;
    }

    private static void draw(ModelRenderer part) {
        part.rotateAngleX = 0f;
        part.rotateAngleY = 0f;
        part.rotateAngleZ = 0f;
        part.rotationPointX = 0f;
        part.rotationPointY = 0f;
        part.rotationPointZ = 0f;
        part.render(1f);
    }
}
