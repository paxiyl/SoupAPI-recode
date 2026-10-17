package me.Padej_.soupapi.melee;

import me.Padej_.soupapi.bedrock.GeoModel;
import me.Padej_.soupapi.bedrock.GeoPose;
import me.Padej_.soupapi.config.ConfigurableModule;
import me.Padej_.soupapi.utils.MathUtility;
import me.Padej_.soupapi.utils.Palette;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.awt.Color;

/**
 * Draws the carried weapon in place of the first-person hand.
 *
 * <p>The model is placed by the fit worked out when it loaded — its idle pose normalised to a
 * block, then sized and pushed out in front of the eye — rather than by the view anchors in the
 * pack, which disagree between weapons. Everything after that is offsets the player can turn.
 *
 * <p>The extra passes all reuse the same compiled bones, so a glow, an outline and four phantom
 * copies cost six {@code glCallList} runs per bone rather than six rebuilds of the geometry. On
 * gl4es that difference is the difference between the effects being usable and not.
 */
public final class MeleeRenderer extends ConfigurableModule {

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

        MeleeEffects.track(weapon, partialTicks);

        // The player's own arms first, in the view's own frame: they hold the weapon, and nothing
        // about them touches the weapon's placement.
        MeleeHands.render(Melee.state().attacking() ? Melee.state().progress() : 0f);

        GlStateManager.pushMatrix();
        try {
            GlStateManager.disableCull();           // the X mirror flips the winding
            GlStateManager.enableDepth();
            GlStateManager.enableAlpha();
            RenderHelper.enableStandardItemLighting();

            place(weapon, partialTicks);

            // Anything additive that should sit *behind* the weapon is drawn first, without
            // writing depth; the textured pass then covers the middle of it and only what sticks
            // out past the silhouette survives. Drawing these afterwards instead - which is what
            // the first version did - paints a flat sheet of accent colour over the artwork and
            // buries the weapon in its own glow.
            if (CONFIG.meleeOutline) outline(weapon, model, pose);
            if (CONFIG.meleePhantom) phantoms(weapon, model, texture);

            model.setHidden(MeleeWeapon.ARM_MESH, !CONFIG.meleeShowArms);

            // The weapon as the pack drew it.
            GlStateManager.color(1f, 1f, 1f, 1f);
            GlStateManager.depthMask(true);
            mc.getTextureManager().bindTexture(texture);
            model.render(pose);

            if (CONFIG.meleeChroma) chroma(weapon, model, pose, texture);
            if (CONFIG.meleeGlow) glow(weapon, model, pose);
            MeleeEffects.render(weapon, partialTicks);
        } catch (Throwable t) {
            me.Padej_.soupapi.compat.SoupLog.error(
                    "[SoupAPI] drawing the melee weapon failed; it is now off", t);
            CONFIG.meleeEnabled = false;
        } finally {
            RenderHelper.disableStandardItemLighting();
            GlStateManager.enableCull();
            GlStateManager.color(1f, 1f, 1f, 1f);
            me.Padej_.soupapi.compat.RenderSystem.restoreDefaults();
            GlStateManager.popMatrix();
        }
        return true;
    }

    /** Puts the matrix where the model should be drawn, including bob, sway and the kick. */
    private static void place(MeleeWeapon weapon, float partialTicks) {
        MeleeWeapon.Fit fit = weapon.fit();
        MeleePlacement.Values placement = MeleePlacement.of(weapon);
        float size = placement.size / 100f;
        boolean arms = CONFIG.meleeShowArms;
        float fitScale = arms ? fit.armScale : fit.scale;
        float fitX = arms ? fit.armOffsetX : fit.offsetX;
        float fitY = arms ? fit.armOffsetY : fit.offsetY;
        float fitZ = arms ? fit.armOffsetZ : fit.offsetZ;

        if (CONFIG.meleeBob) bob(partialTicks);
        if (CONFIG.meleeSway) sway();

        // Where the weapon sits in the view.
        GlStateManager.translate(
                placement.offsetX / 100f,
                placement.offsetY / 100f,
                -placement.distance / 100f);

        // Then the turns, about the weapon's own centre: first the one that points every blade the
        // same way, then whatever the player has set on top of it.
        if (CONFIG.meleeAutoAlign && !arms && fit.alignDegrees != 0f) {
            GlStateManager.rotate(fit.alignDegrees, fit.alignX, fit.alignY, fit.alignZ);
        }
        if (placement.yaw != 0) GlStateManager.rotate(placement.yaw, 0f, 1f, 0f);
        if (placement.pitch != 0) GlStateManager.rotate(placement.pitch, 1f, 0f, 0f);
        if (placement.roll != 0) GlStateManager.rotate(placement.roll, 0f, 0f, 1f);

        shake(weapon);

        // Finally bring the weapon's centre to the origin, so every turn above pivots around it.
        GlStateManager.translate(fitX * size, fitY * size, fitZ * size);

        float scale = fitScale * size / 16f;
        GlStateManager.scale(-scale, scale, scale);
    }

    /**
     * The pack's own camera track, applied to the weapon rather than the camera.
     *
     * <p>Every attack animation moves a bone called {@code camera}: on the mod it was built for
     * that drives a screen shake. A first-person hand hook runs after the view matrix is set, so
     * the shake cannot be put back into the camera from here — but rotating the weapon by the same
     * amount reads as the same kick, and it cannot make the player motion-sick on a phone.
     */
    private static void shake(MeleeWeapon weapon) {
        if (CONFIG.meleeCameraShake <= 0) return;
        GeoModel model = weapon.model();
        GeoPose pose = weapon.pose();
        me.Padej_.soupapi.bedrock.GeoBone camera = model.bone("camera");
        if (camera == null) return;
        float strength = CONFIG.meleeCameraShake / 100f;
        int i = camera.index();
        GlStateManager.rotate(pose.rotZ(i) * strength, 0f, 0f, 1f);
        GlStateManager.rotate(pose.rotY(i) * strength, 0f, 1f, 0f);
        GlStateManager.rotate(pose.rotX(i) * strength, 1f, 0f, 0f);
        GlStateManager.translate(
                pose.posX(i) * strength / 64f,
                pose.posY(i) * strength / 64f,
                pose.posZ(i) * strength / 64f);
    }

    private static void bob(float partialTicks) {
        if (mc.thePlayer == null) return;
        float distance = mc.thePlayer.distanceWalkedModified
                - mc.thePlayer.prevDistanceWalkedModified;
        float walked = -(mc.thePlayer.distanceWalkedModified + distance * partialTicks);
        float amount = (mc.thePlayer.prevCameraYaw
                + (mc.thePlayer.cameraYaw - mc.thePlayer.prevCameraYaw) * partialTicks);
        float strength = CONFIG.meleeBobStrength / 100f;
        GlStateManager.translate(
                MathUtility.sin(walked * (float) Math.PI) * amount * 0.5f * strength,
                -Math.abs(MathUtility.cos(walked * (float) Math.PI) * amount) * strength,
                0f);
        GlStateManager.rotate(MathUtility.sin(walked * (float) Math.PI) * amount * 3f * strength,
                0f, 0f, 1f);
    }

    private static float swayYaw;
    private static float swayPitch;

    /** Lags the weapon behind the view, the way a heavy thing in a hand would. */
    private static void sway() {
        if (mc.thePlayer == null) return;
        float yawDelta = MathUtility.wrapDegrees(mc.thePlayer.rotationYaw - mc.thePlayer.prevRotationYaw);
        float pitchDelta = mc.thePlayer.rotationPitch - mc.thePlayer.prevRotationPitch;
        float strength = CONFIG.meleeSwayStrength / 100f;
        swayYaw += (Math.max(-12f, Math.min(12f, yawDelta)) - swayYaw) * 0.25f;
        swayPitch += (Math.max(-12f, Math.min(12f, pitchDelta)) - swayPitch) * 0.25f;
        GlStateManager.translate(swayYaw * 0.004f * strength, swayPitch * 0.004f * strength, 0f);
        GlStateManager.rotate(-swayYaw * 0.35f * strength, 0f, 1f, 0f);
        GlStateManager.rotate(-swayPitch * 0.35f * strength, 1f, 0f, 0f);
    }

    // ---- the extra passes ------------------------------------------------------------------

    /**
     * An additive pass masked by the pack's specular map.
     *
     * <p>A gun pack marks its metal and its emissive detail in the {@code _s} texture, which makes
     * it exactly the mask this wants: the parts the artist called shiny are the parts that light
     * up, and the shape of the glow follows the artwork instead of flooding the whole model.
     */
    private static void glow(MeleeWeapon weapon, GeoModel model, GeoPose pose) {
        // Without the pack's specular map there is no mask, and glowing through the base texture
        // lights the whole weapon evenly - which is what "Free Dark Star" looked like, since it is
        // the one weapon here that ships no _s file.
        ResourceLocation mask = weapon.glowMask();
        if (mask == null) return;
        Color colour = glowColour();
        float pulse = 1f;
        if (CONFIG.meleeGlowPulse > 0) {
            float speed = CONFIG.meleeGlowPulse / 100f;
            pulse = 0.65f + 0.35f * MathUtility.sin(
                    (System.currentTimeMillis() % 100000L) / 1000f * speed * 3.2f);
        }
        float alpha = CONFIG.meleeGlowStrength / 100f * pulse;

        GlStateManager.depthMask(false);
        GlStateManager.enableDepth();
        GlStateManager.depthFunc(GL11.GL_LEQUAL);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GlStateManager.disableLighting();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240f, 240f);
        GlStateManager.color(colour.getRed() / 255f, colour.getGreen() / 255f,
                colour.getBlue() / 255f, alpha);
        mc.getTextureManager().bindTexture(mask);
        model.render(pose);
        GlStateManager.enableLighting();
        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
    }

    /**
     * A rim of colour around the silhouette.
     *
     * <p>The model again, slightly larger and additive, drawn before the textured pass and writing
     * no depth: the real model then covers the middle and only the sliver that pokes out past the
     * silhouette is left. A shader would do this with a fresnel term; 1.8.9 has no shader to put one
     * in, and gl4es would not run it. The first version tried the inverted-hull trick instead and
     * culled front faces — which, with the model mirrored in X, culls the wrong half and paints the
     * whole weapon flat.
     */
    private static void outline(MeleeWeapon weapon, GeoModel model, GeoPose pose) {
        Color colour = glowColour();
        float width = 1f + CONFIG.meleeOutlineWidth / 1000f;

        GlStateManager.pushMatrix();
        GlStateManager.scale(width, width, width);
        GlStateManager.depthMask(false);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GlStateManager.disableLighting();
        GlStateManager.disableTexture2D();
        GlStateManager.color(colour.getRed() / 255f, colour.getGreen() / 255f,
                colour.getBlue() / 255f, CONFIG.meleeOutlineStrength / 100f);
        model.render(pose);
        GlStateManager.enableTexture2D();
        GlStateManager.enableLighting();
        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    /**
     * Copies of the weapon a few frames behind the swing.
     *
     * <p>Each one is the pose the weapon actually held, kept by {@link MeleeEffects}, so the smear
     * follows the animation exactly rather than approximating it with a rotation.
     */
    private static void phantoms(MeleeWeapon weapon, GeoModel model, ResourceLocation texture) {
        int count = Math.max(1, CONFIG.meleePhantomCount);
        Color colour = glowColour();

        GlStateManager.depthMask(false);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GlStateManager.disableLighting();
        mc.getTextureManager().bindTexture(texture);
        for (int i = 0; i < count; i++) {
            GeoPose ghost = MeleeEffects.pastPose(i);
            if (ghost == null) continue;
            float fade = (1f - (i + 1f) / (count + 1f)) * CONFIG.meleePhantomStrength / 100f;
            if (fade <= 0.01f) continue;
            GlStateManager.color(colour.getRed() / 255f, colour.getGreen() / 255f,
                    colour.getBlue() / 255f, fade);
            model.render(ghost);
        }
        GlStateManager.enableLighting();
        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
    }

    /** Three offset additive copies in red, green and blue: a cheap chromatic split. */
    private static void chroma(MeleeWeapon weapon, GeoModel model, GeoPose pose,
                               ResourceLocation texture) {
        float spread = CONFIG.meleeChromaSpread / 2000f;
        float alpha = CONFIG.meleeChromaStrength / 100f;
        float[][] channels = {{1f, 0f, 0f}, {0f, 1f, 0f}, {0f, 0f, 1f}};

        GlStateManager.depthMask(false);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GlStateManager.disableLighting();
        mc.getTextureManager().bindTexture(texture);
        for (int i = 0; i < 3; i++) {
            GlStateManager.pushMatrix();
            GlStateManager.translate((i - 1) * spread * 16f, 0f, 0f);
            GlStateManager.color(channels[i][0], channels[i][1], channels[i][2], alpha);
            model.render(pose);
            GlStateManager.popMatrix();
        }
        GlStateManager.enableLighting();
        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
    }

    /** The colour the effects use: the theme's palette, or the weapon's own fixed colour. */
    static Color glowColour() {
        if (CONFIG.meleeGlowUseTheme) {
            return Palette.getColor(0);
        }
        return new Color(CONFIG.meleeGlowColor);
    }

    static Minecraft client() {
        return mc;
    }
}
