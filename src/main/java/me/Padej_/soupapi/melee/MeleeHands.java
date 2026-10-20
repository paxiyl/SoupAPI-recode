package me.Padej_.soupapi.melee;

import me.Padej_.soupapi.bedrock.GeoModel;
import me.Padej_.soupapi.bedrock.Mat4;
import me.Padej_.soupapi.config.ConfigurableModule;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EnumPlayerModelParts;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.nio.FloatBuffer;

/**
 * The player's own arms, drawn to fill the pack's hand-marker boxes.
 *
 * <p>The markers {@code righthand_pos} / {@code lefthand_pos} are vanilla 4×12×4 arm boxes placed
 * (and animated) by the pack exactly where the player's hand goes. The pack's own renderer draws a
 * vanilla player arm into each box with the standard 64×64 skin layout, and this does the same:
 * one arm box per marker, in the marker's posed space ({@code view · global(marker)}), so the arm
 * tracks the swing and sits in the hand. Ported straight from that renderer.
 */
public final class MeleeHands extends ConfigurableModule {

    private static final FloatBuffer MATRIX = BufferUtils.createFloatBuffer(16);

    private MeleeHands() {
    }

    /** Draws both arms, given the weapon's model-units-to-eye view matrix. */
    public static boolean render(MeleeWeapon weapon, Mat4 view) {
        if (!CONFIG.meleeHands) return false;
        if (view == null) return false;
        if (!(mc.thePlayer instanceof AbstractClientPlayer)) return false;
        AbstractClientPlayer player = (AbstractClientPlayer) mc.thePlayer;
        if (player.isInvisible()) return false;
        GeoModel model = weapon.model();
        if (model == null) return false;
        ResourceLocation skin = player.getLocationSkin();
        if (skin == null) return false;
        boolean slim = "slim".equals(player.getSkinType());

        boolean drew = false;
        try {
            // The weapon pass runs with culling off (packs mix winding); keep it off for the arm
            // box too, so a back-wound face can never make the arm vanish.
            GlStateManager.disableCull();
            GlStateManager.disableBlend();
            GlStateManager.color(1f, 1f, 1f, 1f);
            mc.getTextureManager().bindTexture(skin);
            drew |= arm(model, view, MeleeWeapon.RIGHT_HAND, true, slim, player);
            if (CONFIG.meleeLeftHand) {
                drew |= arm(model, view, MeleeWeapon.LEFT_HAND, false, slim, player);
            }
        } catch (Throwable t) {
            me.Padej_.soupapi.compat.SoupLog.warn(
                    "[SoupAPI] drawing the first-person arms failed; they are now off", t);
            CONFIG.meleeHands = false;
        } finally {
            GlStateManager.color(1f, 1f, 1f, 1f);
        }
        return drew;
    }

    private static boolean arm(GeoModel model, Mat4 view, String bone, boolean right, boolean slim,
                               AbstractClientPlayer player) {
        float[] box = model.cubeBox(bone);
        int idx = model.indexOf(bone);
        if (box == null || idx < 0 || model.ancestorHidden(idx)) return false;

        Mat4 m = view.copy().multiply(model.global(idx));
        GlStateManager.pushMatrix();
        try {
            m.store(MATRIX);
            GlStateManager.multMatrix(MATRIX);
            int u = right ? 40 : 32, v = right ? 16 : 48;
            int dx = slim ? 3 : 4;
            float scale = CONFIG.meleeHandsScale / 100f;
            if (scale != 1f) GlStateManager.scale(scale, scale, scale);
            box(box, 0f, u, v, dx);
            if (player.isWearing(right ? EnumPlayerModelParts.RIGHT_SLEEVE : EnumPlayerModelParts.LEFT_SLEEVE)) {
                box(box, 0.25f, right ? 40 : 48, right ? 32 : 48, dx);
            }
        } finally {
            GlStateManager.popMatrix();
        }
        return true;
    }

    private static void box(float[] b, float inf, int u, int v, int dx) {
        float x0 = b[0] - inf, y0 = b[1] - inf, z0 = b[2] - inf;
        float x1 = b[3] + inf, y1 = b[4] + inf, z1 = b[5] + inf;
        int dy = 12, dz = 4;
        float[] V7 = {x0, y0, z0}, V0 = {x1, y0, z0}, V1 = {x1, y1, z0}, V2 = {x0, y1, z0};
        float[] V3 = {x0, y0, z1}, V4 = {x1, y0, z1}, V5 = {x1, y1, z1}, V6 = {x0, y1, z1};
        GL11.glBegin(GL11.GL_QUADS);
        quad(V4, V0, V1, V5, u + dz + dx, v + dz, u + dz + dx + dz, v + dz + dy);
        quad(V7, V3, V6, V2, u, v + dz, u + dz, v + dz + dy);
        quad(V4, V3, V7, V0, u + dz, v, u + dz + dx, v + dz);
        quad(V1, V2, V6, V5, u + dz + dx, v + dz, u + dz + dx + dx, v);
        quad(V0, V7, V2, V1, u + dz, v + dz, u + dz + dx, v + dz + dy);
        quad(V3, V4, V5, V6, u + dz + dx + dz, v + dz, u + dz + dx + dz + dx, v + dz + dy);
        GL11.glEnd();
    }

    /** Vanilla TexturedQuad UV assignment: v0->(u2,v1), v1->(u1,v1), v2->(u1,v2), v3->(u2,v2). */
    private static void quad(float[] a, float[] b, float[] c, float[] d,
                             float u1, float v1, float u2, float v2) {
        float ex = c[0] - a[0], ey = c[1] - a[1], ez = c[2] - a[2];
        float fx = d[0] - b[0], fy = d[1] - b[1], fz = d[2] - b[2];
        float nx = ey * fz - ez * fy, ny = ez * fx - ex * fz, nz = ex * fy - ey * fx;
        float l = MathHelper.sqrt_float(nx * nx + ny * ny + nz * nz);
        if (l > 0f) {
            nx /= l; ny /= l; nz /= l;
        }
        float s = 1f / 64f;
        GL11.glNormal3f(nx, ny, nz);
        GL11.glTexCoord2f(u2 * s, v1 * s); GL11.glVertex3f(a[0], a[1], a[2]);
        GL11.glTexCoord2f(u1 * s, v1 * s); GL11.glVertex3f(b[0], b[1], b[2]);
        GL11.glTexCoord2f(u1 * s, v2 * s); GL11.glVertex3f(c[0], c[1], c[2]);
        GL11.glTexCoord2f(u2 * s, v2 * s); GL11.glVertex3f(d[0], d[1], d[2]);
    }
}
