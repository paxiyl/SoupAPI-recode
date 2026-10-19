package me.Padej_.soupapi.melee;

import me.Padej_.soupapi.bedrock.GeoBone;
import me.Padej_.soupapi.bedrock.GeoModel;
import me.Padej_.soupapi.bedrock.GeoPose;
import me.Padej_.soupapi.bedrock.Mat4;
import me.Padej_.soupapi.config.ConfigurableModule;
import me.Padej_.soupapi.utils.MathUtility;
import me.Padej_.soupapi.utils.Palette;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.Project;

import java.awt.Color;

/**
 * Draws the carried weapon in place of the first-person hand.
 *
 * <p>Placed the way the pack's own 1.8.9 renderer places it: a dedicated perspective pass with the
 * depth buffer cleared, and the model positioned by one matrix — {@code scale(1/16) ·
 * inverse(global(idle_view) · T(pivot))} — so the pack's {@code idle_view} bone sits at the eye.
 * The player's placement sliders (size, distance, offsets, yaw/pitch/roll) are turns and shifts
 * about the eye on top of that, all zero by default, so out of the box the weapon sits exactly
 * where the pack intends. The camera shake baked into the swings is applied to the real camera in
 * {@link #applyCameraShake}, not to the weapon.
 */
public final class MeleeRenderer extends ConfigurableModule {

    private static final float VIEWMODEL_FOV = 70f;

    private MeleeRenderer() {
    }

    /** Called from the hand render hook. Returns true when it drew, so vanilla stays cancelled. */
    public static boolean renderFirstPerson(float partialTicks) {
        MeleeWeapon weapon = Melee.active();
        if (weapon == null) return false;
        Melee.advance();

        GeoModel model = weapon.model();
        GeoPose pose = weapon.pose();
        if (model == null || pose == null) return false;
        ResourceLocation texture = weapon.texture();
        if (texture == null) return false;

        Minecraft mc = client();
        EntityPlayerSP player = mc.thePlayer;
        if (player == null || mc.theWorld == null) return false;

        GlStateManager.clear(GL11.GL_DEPTH_BUFFER_BIT);
        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        Project.gluPerspective(VIEWMODEL_FOV,
                (float) mc.displayWidth / (float) mc.displayHeight, 0.02F, 64F);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();

        try {
            GlStateManager.disableCull();           // packs mix winding; let both sides draw
            GlStateManager.enableDepth();
            GlStateManager.depthMask(true);
            GlStateManager.enableAlpha();
            GlStateManager.enableRescaleNormal();
            RenderHelper.enableStandardItemLighting();
            lightmapForEye(mc, player);

            if (CONFIG.meleeBob) viewBobbing(mc, player, partialTicks);
            if (CONFIG.meleeSway) sway(player, partialTicks);
            userPlacement(weapon);

            model.computeGlobals(pose);
            Mat4 view = model.viewFromBone("idle_view");
            hide(model, true);

            GlStateManager.color(1f, 1f, 1f, 1f);
            mc.getTextureManager().bindTexture(texture);
            model.drawPosed(view);

            MeleeHands.render(weapon, view);

            if (CONFIG.meleeGlow) glow(weapon, model, view);
        } catch (Throwable t) {
            me.Padej_.soupapi.compat.SoupLog.error(
                    "[SoupAPI] drawing the melee weapon failed; it is now off", t);
            CONFIG.meleeEnabled = false;
        } finally {
            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableRescaleNormal();
            GlStateManager.enableCull();
            GlStateManager.color(1f, 1f, 1f, 1f);
            me.Padej_.soupapi.compat.RenderSystem.restoreDefaults();
            GlStateManager.matrixMode(GL11.GL_PROJECTION);
            GlStateManager.popMatrix();
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);
            GlStateManager.popMatrix();
        }
        return true;
    }

    /** Hides the hand-marker placeholders, and one of Dark Star's two forms, before drawing. */
    private static void hide(GeoModel model, boolean hidePlaceholders) {
        model.setHidden(MeleeWeapon.ARM_MESH, hidePlaceholders);
        // The two Dark Star forms share a model; show one. (Harmless on weapons without them.)
        model.setHidden("devil", true);
    }

    /** The player's own fine-tuning: all zero by default, so the base pose is the pack's. */
    private static void userPlacement(MeleeWeapon weapon) {
        MeleePlacement.Values p = MeleePlacement.of(weapon);
        GlStateManager.translate(p.offsetX / 100f, p.offsetY / 100f, (p.offsetZ - p.distance) / 100f);
        if (p.yaw != 0) GlStateManager.rotate(p.yaw, 0f, 1f, 0f);
        if (p.pitch != 0) GlStateManager.rotate(p.pitch, 1f, 0f, 0f);
        if (p.roll != 0) GlStateManager.rotate(p.roll, 0f, 0f, 1f);
        if (p.size != 100) {
            float s = p.size / 100f;
            GlStateManager.scale(s, s, s);
        }
    }

    private static void lightmapForEye(Minecraft mc, EntityPlayerSP player) {
        int light = mc.theWorld.getCombinedLight(
                new BlockPos(player.posX, player.posY + player.getEyeHeight(), player.posZ), 0);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit,
                (float) (light & 65535), (float) (light >> 16));
        mc.entityRenderer.enableLightmap();
    }

    // ---- camera shake ----------------------------------------------------------------------

    /**
     * The pack bakes the swing's recoil into a bone called {@code camera}; applied to the real
     * camera it reads as the kick it was meant to be. Called from the camera-setup hook.
     */
    public static void applyCameraShake(EntityViewRenderEvent.CameraSetup event) {
        if (CONFIG.meleeCameraShake <= 0) return;
        MeleeWeapon weapon = Melee.active();
        if (weapon == null) return;
        GeoModel model = weapon.model();
        GeoPose pose = weapon.pose();
        if (model == null || pose == null) return;
        GeoBone camera = model.bone("camera");
        if (camera == null) return;
        int i = camera.index();
        float s = CONFIG.meleeCameraShake / 100f;
        event.pitch -= pose.rotX(i) * s;
        event.yaw -= pose.rotY(i) * s;
        event.roll -= pose.rotZ(i) * s;
    }

    // ---- vanilla camera helpers (copied, so the viewmodel moves exactly like a hand) --------

    private static void viewBobbing(Minecraft mc, EntityPlayerSP player, float pt) {
        if (!mc.gameSettings.viewBobbing) return;
        float distance = player.distanceWalkedModified - player.prevDistanceWalkedModified;
        float walked = -(player.distanceWalkedModified + distance * pt);
        float amount = player.prevCameraYaw + (player.cameraYaw - player.prevCameraYaw) * pt;
        float pitch = player.prevCameraPitch + (player.cameraPitch - player.prevCameraPitch) * pt;
        float strength = CONFIG.meleeBobStrength / 100f;
        GlStateManager.translate(
                MathUtility.sin(walked * (float) Math.PI) * amount * 0.5f * strength,
                -Math.abs(MathUtility.cos(walked * (float) Math.PI) * amount) * strength, 0f);
        GlStateManager.rotate(MathUtility.sin(walked * (float) Math.PI) * amount * 3f * strength, 0f, 0f, 1f);
        GlStateManager.rotate(Math.abs(MathUtility.cos(walked * (float) Math.PI - 0.2f) * amount) * 5f * strength, 1f, 0f, 0f);
        GlStateManager.rotate(pitch, 1f, 0f, 0f);
    }

    private static void sway(EntityPlayerSP player, float pt) {
        float armPitch = player.prevRenderArmPitch + (player.renderArmPitch - player.prevRenderArmPitch) * pt;
        float armYaw = player.prevRenderArmYaw + (player.renderArmYaw - player.prevRenderArmYaw) * pt;
        float strength = CONFIG.meleeSwayStrength / 100f;
        GlStateManager.rotate((player.rotationPitch - armPitch) * 0.1f * strength, 1f, 0f, 0f);
        GlStateManager.rotate((player.rotationYaw - armYaw) * 0.1f * strength, 0f, 1f, 0f);
    }

    // ---- glow ------------------------------------------------------------------------------

    /**
     * An additive pass masked by the pack's specular map: the parts the artist marked shiny light
     * up, the shape following the artwork. Skipped when a weapon ships no {@code _s} map, so a
     * weapon without one is not lit flat all over.
     */
    private static void glow(MeleeWeapon weapon, GeoModel model, Mat4 view) {
        ResourceLocation mask = weapon.glowMask();
        if (mask == null) return;
        Color colour = glowColour();
        float pulse = 1f;
        if (CONFIG.meleeGlowPulse > 0) {
            float speed = CONFIG.meleeGlowPulse / 100f;
            pulse = 0.65f + 0.35f * MathUtility.sin((System.currentTimeMillis() % 100000L) / 1000f * speed * 3.2f);
        }
        float alpha = CONFIG.meleeGlowStrength / 100f * pulse;

        GlStateManager.depthMask(false);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GlStateManager.disableLighting();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);
        GlStateManager.color(colour.getRed() / 255f, colour.getGreen() / 255f, colour.getBlue() / 255f, alpha);
        client().getTextureManager().bindTexture(mask);
        model.drawPosed(view);
        GlStateManager.enableLighting();
        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    /** The colour the glow uses: the theme's palette, or the weapon's own fixed colour. */
    static Color glowColour() {
        if (CONFIG.meleeGlowUseTheme) return Palette.getColor(0);
        return new Color(CONFIG.meleeGlowColor);
    }

    static Minecraft client() {
        return mc;
    }
}
