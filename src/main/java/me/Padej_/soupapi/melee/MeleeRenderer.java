package me.Padej_.soupapi.melee;

import me.Padej_.soupapi.bedrock.GeoModel;
import me.Padej_.soupapi.bedrock.GeoPose;
import me.Padej_.soupapi.bedrock.Mat4;
import me.Padej_.soupapi.config.ConfigurableModule;
import me.Padej_.soupapi.utils.MathUtility;
import me.Padej_.soupapi.utils.Palette;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import org.lwjgl.BufferUtils;

import java.awt.Color;
import java.nio.FloatBuffer;

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

    private static final FloatBuffer MATRIX = BufferUtils.createFloatBuffer(16);

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

        // One matrix, built once: pushed onto GL for the weapon, handed to MeleeHands so it can
        // work out where in the view the grip ended up.
        Mat4 view = placement(weapon, partialTicks);

        GlStateManager.pushMatrix();
        try {
            GlStateManager.disableCull();           // the X mirror flips the winding
            GlStateManager.enableDepth();
            GlStateManager.enableAlpha();
            RenderHelper.enableStandardItemLighting();

            MATRIX.clear();
            MATRIX.put(view.m);
            MATRIX.flip();
            GL11.glMultMatrix(MATRIX);

            // Anything additive that should sit *behind* the weapon is drawn first, without
            // writing depth; the textured pass then covers the middle of it and only what sticks
            // out past the silhouette survives. Drawing these afterwards instead - which is what
            // the first version did - paints a flat sheet of accent colour over the artwork and
            // buries the weapon in its own glow.
            if (CONFIG.meleeOutline) outline(weapon, model, pose);
            if (CONFIG.meleePhantom) phantoms(weapon, model, texture);

            // The pack's own hand markers are never drawn: they are arm-shaped boxes with no arm
            // texture on them, there so the mod it was built for knows where to put the player's
            // real arm. MeleeHands puts the real arm there instead.
            model.setHidden(MeleeWeapon.ARM_MESH, true);

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

        // The arms are drawn outside that matrix, in the view, because they are not placed by the
        // pack at all - only their target is. See MeleeHands.
        MeleeHands.render(weapon, view);
        return true;
    }

    /**
     * The transform from the model's own units into view space.
     *
     * <p>Built as a matrix rather than pushed straight onto the GL stack because the arms need the
     * same transform: {@link MeleeHands} aims each arm at a point on the weapon, and to do that it
     * has to put a model-space point into view space itself. One matrix, used twice, so the two
     * cannot drift apart.
     *
     * <p>The placement proper is three lines: mirror X, scale model units to blocks, and move the
     * pack's own view anchor onto the camera. Everything else is something the player asked for.
     * The sliders start at zero on purpose — out of the box the weapon sits where the pack puts it.
     */
    public static Mat4 placement(MeleeWeapon weapon, float partialTicks) {
        MeleeWeapon.Fit fit = weapon.fit();
        MeleePlacement.Values placement = MeleePlacement.of(weapon);
        float size = placement.size / 100f;
        Mat4 m = new Mat4();

        if (CONFIG.meleeBob) bob(m, partialTicks);
        if (CONFIG.meleeSway) sway(m);

        // Nudges, in hundredths of a block, on top of the pack's own placement.
        m.translate(
                placement.offsetX / 100f,
                placement.offsetY / 100f,
                (placement.offsetZ - placement.distance) / 100f);

        // Turns about the eye, which is where the player is looking from, so a pitch or a yaw here
        // swings the weapon across the view rather than tumbling it about its own middle.
        m.rotateDeg(placement.yaw, 0f, 1f, 0f);
        m.rotateDeg(placement.pitch, 1f, 0f, 0f);
        m.rotateDeg(placement.roll, 0f, 0f, 1f);

        shake(m, weapon);

        // Model units to blocks, mirrored in X: the pack holds the weapon on the left of its own
        // space and the player holds it on the right of theirs. Checked by where the hand markers
        // land - mirrored, the right hand is right of centre and the left hand left of it.
        float scale = size / 16f;
        m.scale(-scale, scale, scale);

        // And the pack's view anchor onto the camera.
        m.translate(-fit.eyeX, -fit.eyeY, -fit.eyeZ);
        return m;
    }

    /**
     * The pack's own camera track, applied to the weapon rather than the camera.
     *
     * <p>Every attack animation moves a bone called {@code camera}: on the mod it was built for
     * that drives a screen shake. A first-person hand hook runs after the view matrix is set, so
     * the shake cannot be put back into the camera from here — but rotating the weapon by the same
     * amount reads as the same kick, and it cannot make the player motion-sick on a phone.
     */
    private static void shake(Mat4 m, MeleeWeapon weapon) {
        if (CONFIG.meleeCameraShake <= 0) return;
        GeoModel model = weapon.model();
        GeoPose pose = weapon.pose();
        me.Padej_.soupapi.bedrock.GeoBone camera = model.bone("camera");
        if (camera == null) return;
        float strength = CONFIG.meleeCameraShake / 100f;
        int i = camera.index();
        m.rotateDeg(pose.rotZ(i) * strength, 0f, 0f, 1f);
        m.rotateDeg(pose.rotY(i) * strength, 0f, 1f, 0f);
        m.rotateDeg(pose.rotX(i) * strength, 1f, 0f, 0f);
        m.translate(
                pose.posX(i) * strength / 64f,
                pose.posY(i) * strength / 64f,
                pose.posZ(i) * strength / 64f);
    }

    private static void bob(Mat4 m, float partialTicks) {
        if (mc == null || mc.thePlayer == null) return;
        float distance = mc.thePlayer.distanceWalkedModified
                - mc.thePlayer.prevDistanceWalkedModified;
        float walked = -(mc.thePlayer.distanceWalkedModified + distance * partialTicks);
        float amount = (mc.thePlayer.prevCameraYaw
                + (mc.thePlayer.cameraYaw - mc.thePlayer.prevCameraYaw) * partialTicks);
        float strength = CONFIG.meleeBobStrength / 100f;
        m.translate(
                MathUtility.sin(walked * (float) Math.PI) * amount * 0.5f * strength,
                -Math.abs(MathUtility.cos(walked * (float) Math.PI) * amount) * strength,
                0f);
        m.rotateDeg(MathUtility.sin(walked * (float) Math.PI) * amount * 3f * strength,
                0f, 0f, 1f);
    }

    private static float swayYaw;
    private static float swayPitch;

    /** Lags the weapon behind the view, the way a heavy thing in a hand would. */
    private static void sway(Mat4 m) {
        if (mc == null || mc.thePlayer == null) return;
        float yawDelta = MathUtility.wrapDegrees(mc.thePlayer.rotationYaw - mc.thePlayer.prevRotationYaw);
        float pitchDelta = mc.thePlayer.rotationPitch - mc.thePlayer.prevRotationPitch;
        float strength = CONFIG.meleeSwayStrength / 100f;
        swayYaw += (Math.max(-12f, Math.min(12f, yawDelta)) - swayYaw) * 0.25f;
        swayPitch += (Math.max(-12f, Math.min(12f, pitchDelta)) - swayPitch) * 0.25f;
        m.translate(swayYaw * 0.004f * strength, swayPitch * 0.004f * strength, 0f);
        m.rotateDeg(-swayYaw * 0.35f * strength, 0f, 1f, 0f);
        m.rotateDeg(-swayPitch * 0.35f * strength, 1f, 0f, 0f);
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
