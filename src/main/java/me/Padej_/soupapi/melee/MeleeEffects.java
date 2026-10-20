package me.Padej_.soupapi.melee;

import me.Padej_.soupapi.bedrock.GeoModel;
import me.Padej_.soupapi.bedrock.GeoPose;
import me.Padej_.soupapi.config.ConfigurableModule;
import me.Padej_.soupapi.utils.MathUtility;
import me.Padej_.soupapi.utils.Palette;
import me.Padej_.soupapi.utils.Symbol;
import me.Padej_.soupapi.utils.TexturesManager;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The things that happen around the weapon: the smear behind a swing, the sparks off the edge, the
 * symbols orbiting it, the ring that leaves the blade when it lands.
 *
 * <p>All of it lives in the weapon's own model space and is drawn inside the same matrix as the
 * model, so it follows the animation exactly. A world-space particle would not: a first-person hand
 * is drawn after the world, in the camera's frame, and anything placed in the world drifts off the
 * blade the moment the player turns.
 */
public final class MeleeEffects extends ConfigurableModule {

    /** How many past poses are kept for the phantom smear. */
    private static final int HISTORY = 12;

    private static final GeoPose[] HISTORY_POSES = new GeoPose[HISTORY];
    private static GeoModel historyModel;
    private static int historyHead;
    private static int historyFilled;

    /** The blade tip's path, in model units, newest first. */
    private static final List<float[]> TRAIL = new ArrayList<float[]>();
    private static final List<Spark> SPARKS = new ArrayList<Spark>();
    private static final List<Ring> RINGS = new ArrayList<Ring>();
    private static String bladeBone;
    private static MeleeWeapon tracked;
    private static float runeSpin;

    private MeleeEffects() {
    }

    private static final class Spark {
        float x, y, z, vx, vy, vz, life, maxLife, size, spin, spinRate;
        Color colour;
        /** A cell of the FX atlas (glass shard, feather, rune), or -1 for the round spark. */
        int cell = -1;
    }

    private static final class Ring {
        float x, y, z, life, maxLife;
        Color colour;
    }

    /** Called once per frame, inside the hand pass, before anything is drawn. */
    static void track(MeleeWeapon weapon, float delta) {
        if (weapon != tracked) {
            reset();
            tracked = weapon;
            bladeBone = weapon.bladeBone();
        }
        GeoModel model = weapon.model();
        GeoPose pose = weapon.pose();
        if (model == null || pose == null) return;

        rememberPose(model, pose);
        rememberTip(model, pose);
        advance(delta);
    }

    private static void reset() {
        TRAIL.clear();
        SPARKS.clear();
        RINGS.clear();
        historyHead = 0;
        historyFilled = 0;
        historyModel = null;
        bladeBone = null;
    }

    private static void rememberPose(GeoModel model, GeoPose pose) {
        if (historyModel != model) {
            historyModel = model;
            for (int i = 0; i < HISTORY; i++) HISTORY_POSES[i] = model.newPose();
            historyHead = 0;
            historyFilled = 0;
        }
        HISTORY_POSES[historyHead].copyFrom(pose);
        historyHead = (historyHead + 1) % HISTORY;
        if (historyFilled < HISTORY) historyFilled++;
    }

    /**
     * The pose {@code age} steps back, for a phantom copy.
     *
     * <p>Spaced by the phantom gap so a handful of copies can cover a long swing instead of
     * bunching up in the last few frames of it.
     */
    static GeoPose pastPose(int age) {
        if (historyFilled == 0) return null;
        int gap = Math.max(1, CONFIG.meleePhantomGap);
        int steps = (age + 1) * gap;
        if (steps >= historyFilled) return null;
        int index = ((historyHead - 1 - steps) % HISTORY + HISTORY) % HISTORY;
        return HISTORY_POSES[index];
    }

    /**
     * Records where the edge is, while a swing is running.
     *
     * <p>The tip is the posed mesh vertex furthest from the weapon's centre — the same point the
     * placement uses to decide which way the blade faces. The first version took the pivot of the
     * furthest bone instead, and a pivot can sit anywhere: on some weapons that was a point out in
     * space beside the model, so the trail drew a bright band running parallel to the weapon and
     * never touching it.
     */
    private static void rememberTip(GeoModel model, GeoPose pose) {
        if (!Melee.state().attacking()) {
            // Nothing to trail between swings, and a stale trail is a glowing streak hanging in
            // mid-air next to the weapon.
            if (!TRAIL.isEmpty()) TRAIL.clear();
            return;
        }

        float[] box = model.bounds(pose, null, MeleeWeapon.ARM_MESH);
        if (box == null) return;
        float[] centre = {(box[0] + box[3]) / 2f, (box[1] + box[4]) / 2f, (box[2] + box[5]) / 2f};
        float[] tip = model.furthestFrom(pose, centre, MeleeWeapon.ARM_MESH);
        if (tip == null) return;

        TRAIL.add(0, new float[]{tip[0], tip[1], tip[2], 1f});
        if (CONFIG.meleeSparks) spawnSparks(tip);
        int limit = Math.max(2, CONFIG.meleeTrailLength);
        while (TRAIL.size() > limit) {
            TRAIL.remove(TRAIL.size() - 1);
        }
    }

    private static void spawnSparks(float[] tip) {
        int count = Math.max(1, CONFIG.meleeSparkCount / 4);
        for (int i = 0; i < count; i++) {
            Spark spark = new Spark();
            spark.x = tip[0];
            spark.y = tip[1];
            spark.z = tip[2];
            spark.vx = MathUtility.random(-0.6f, 0.6f);
            spark.vy = MathUtility.random(-0.2f, 0.9f);
            spark.vz = MathUtility.random(-0.6f, 0.6f);
            spark.maxLife = MathUtility.random(0.25f, 0.6f);
            spark.life = spark.maxLife;
            spark.size = MathUtility.random(0.3f, 1.1f) * (CONFIG.meleeSparkSize / 100f);
            spark.colour = colour();
            SPARKS.add(spark);
        }
        while (SPARKS.size() > 400) {
            SPARKS.remove(0);
        }
    }

    /** Ages everything by real time, so a trail is the same length at 30 fps and at 240. */
    private static void advance(float realDelta) {
        float delta = Math.max(0f, Math.min(0.1f, realDelta));
        for (Iterator<float[]> it = TRAIL.iterator(); it.hasNext(); ) {
            float[] point = it.next();
            point[3] -= delta * (100f / Math.max(10, CONFIG.meleeTrailFade));
            if (point[3] <= 0f) it.remove();
        }
        for (Iterator<Spark> it = SPARKS.iterator(); it.hasNext(); ) {
            Spark spark = it.next();
            spark.life -= delta;
            if (spark.life <= 0f) {
                it.remove();
                continue;
            }
            spark.x += spark.vx * delta * 20f;
            spark.y += spark.vy * delta * 20f;
            spark.z += spark.vz * delta * 20f;
            spark.vy -= 0.06f * delta * 20f;
            spark.spin += spark.spinRate * delta;
        }
        for (Iterator<Ring> it = RINGS.iterator(); it.hasNext(); ) {
            Ring ring = it.next();
            ring.life -= delta;
            if (ring.life <= 0f) it.remove();
        }
        runeSpin += 28f * delta;
    }

    /** Called when a swing starts, so the ring and the burst fire with the animation. */
    static void onSwing() {
        if (!CONFIG.meleeEnabled) return;
        MeleeWeapon weapon = Melee.active();
        if (weapon == null) return;
        if (!CONFIG.meleePulse) {
            GeoModel model = weapon.model();
            GeoPose pose = weapon.pose();
            if (model == null || pose == null) return;
            float[] box = model.bounds(pose, null, MeleeWeapon.ARM_MESH);
            if (box == null) return;
            float[] tip = model.furthestFrom(pose, new float[]{(box[0] + box[3]) / 2f, (box[1] + box[4]) / 2f,
                    (box[2] + box[5]) / 2f}, MeleeWeapon.ARM_MESH);
            if (tip != null) fragments(weapon, tip);
            return;
        }
        GeoModel model = weapon.model();
        GeoPose pose = weapon.pose();
        if (model == null || pose == null) return;
        float[] box = model.bounds(pose, null, MeleeWeapon.ARM_MESH);
        if (box == null) return;
        float[] centre = {(box[0] + box[3]) / 2f, (box[1] + box[4]) / 2f, (box[2] + box[5]) / 2f};
        float[] tip = model.furthestFrom(pose, centre, MeleeWeapon.ARM_MESH);
        if (tip == null) return;
        fragments(weapon, tip);
        Ring ring = new Ring();
        ring.x = tip[0];
        ring.y = tip[1];
        ring.z = tip[2];
        ring.maxLife = 0.5f;
        ring.life = ring.maxLife;
        ring.colour = colour();
        RINGS.add(ring);
    }

    /**
     * What breaks off a weapon when it swings: glass shards for the fractured blade, feathers for the
     * claw, runes for the spectral blade. Thrown from the edge in the weapon's own space.
     */
    private static void fragments(MeleeWeapon weapon, float[] tip) {
        int cell;
        if (weapon.flag("shards") && CONFIG.meleeShards) cell = 4;
        else if (weapon.flag("feathers")) cell = 3;
        else if (weapon.count("ribbons") > 0) cell = 8;
        else return;
        for (int i = 0; i < 9; i++) {
            Spark spark = new Spark();
            spark.x = tip[0];
            spark.y = tip[1];
            spark.z = tip[2];
            spark.vx = MathUtility.random(-0.5f, 0.5f);
            spark.vy = MathUtility.random(-0.1f, 0.7f);
            spark.vz = MathUtility.random(-0.5f, 0.5f);
            spark.maxLife = MathUtility.random(0.5f, 0.9f);
            spark.life = spark.maxLife;
            spark.size = MathUtility.random(0.6f, 1.3f);
            spark.spin = MathUtility.random(0f, 360f);
            spark.spinRate = MathUtility.random(-400f, 400f);
            spark.cell = cell == 4 ? 4 + (i % 3) : (cell == 8 ? 8 + (i & 7) : cell);
            spark.colour = colour();
            SPARKS.add(spark);
        }
    }

    // ---- drawing ---------------------------------------------------------------------------

    /** Draws everything, inside the model's matrix, in model units. */
    static void render(MeleeWeapon weapon, float partialTicks) {
        GlStateManager.depthMask(false);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GlStateManager.disableLighting();
        try {
            if (CONFIG.meleeTrail) trail();
            if (CONFIG.meleeSparks) sparks();
            if (CONFIG.meleePulse) rings();
            if (CONFIG.meleeRunes) runes(weapon);
        } finally {
            GlStateManager.enableLighting();
            GlStateManager.depthMask(true);
            GlStateManager.disableBlend();
            GlStateManager.enableTexture2D();
            GlStateManager.color(1f, 1f, 1f, 1f);
        }
    }

    /** The smear the edge leaves: a camera-facing ribbon through the tip's recent path. */
    private static void trail() {
        if (TRAIL.size() < 2) return;
        float width = CONFIG.meleeTrailWidth / 100f * 2f;

        GlStateManager.disableTexture2D();
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer buffer = tessellator.getWorldRenderer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i + 1 < TRAIL.size(); i++) {
            float[] a = TRAIL.get(i);
            float[] b = TRAIL.get(i + 1);
            float alphaA = a[3] * CONFIG.meleeTrailStrength / 100f;
            float alphaB = b[3] * CONFIG.meleeTrailStrength / 100f;
            float taperA = width * a[3];
            float taperB = width * b[3];
            quad(buffer, a, b, taperA, taperB, trailColour(i / (float) TRAIL.size()), alphaA, alphaB);
        }
        tessellator.draw();
        GlStateManager.enableTexture2D();
    }

    /**
     * One segment of the ribbon.
     *
     * <p>Widened across the segment's own direction rather than toward the camera: in the hand pass
     * the camera is at the origin looking down -Z, so a ribbon built in the plane that holds the
     * segment and the view direction always faces the player.
     */
    private static void quad(WorldRenderer buffer, float[] a, float[] b, float widthA, float widthB,
                             Color colour, float alphaA, float alphaB) {
        float dx = b[0] - a[0];
        float dy = b[1] - a[1];
        float dz = b[2] - a[2];
        // side = direction x view, with the view direction taken as the segment's midpoint.
        float mx = (a[0] + b[0]) * 0.5f;
        float my = (a[1] + b[1]) * 0.5f;
        float mz = (a[2] + b[2]) * 0.5f;
        float sx = dy * mz - dz * my;
        float sy = dz * mx - dx * mz;
        float sz = dx * my - dy * mx;
        float length = (float) Math.sqrt(sx * sx + sy * sy + sz * sz);
        if (length < 1.0E-5f) return;
        sx /= length;
        sy /= length;
        sz /= length;

        int r = colour.getRed();
        int g = colour.getGreen();
        int bl = colour.getBlue();
        buffer.pos(a[0] + sx * widthA, a[1] + sy * widthA, a[2] + sz * widthA)
                .color(r, g, bl, (int) (alphaA * 255)).endVertex();
        buffer.pos(a[0] - sx * widthA, a[1] - sy * widthA, a[2] - sz * widthA)
                .color(r, g, bl, (int) (alphaA * 255)).endVertex();
        buffer.pos(b[0] - sx * widthB, b[1] - sy * widthB, b[2] - sz * widthB)
                .color(r, g, bl, (int) (alphaB * 255)).endVertex();
        buffer.pos(b[0] + sx * widthB, b[1] + sy * widthB, b[2] + sz * widthB)
                .color(r, g, bl, (int) (alphaB * 255)).endVertex();
    }

    private static void sparks() {
        if (SPARKS.isEmpty()) return;
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer buffer = tessellator.getWorldRenderer();
        for (int pass = 0; pass < 2; pass++) {
            boolean atlas = pass == 1;
            mc.getTextureManager().bindTexture(atlas ? me.Padej_.soupapi.render.SmokeFx.FX_ATLAS
                    : TexturesManager.themed(TexturesManager.FIREFLY));
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
            for (Spark spark : SPARKS) {
                if ((spark.cell >= 0) != atlas) continue;
                float fade = Math.max(0f, spark.life / spark.maxLife);
                int alpha = (int) (fade * 255);
                float size = spark.size * (0.4f + fade);
                if (atlas) {
                    atlasSprite(buffer, spark, size, alpha);
                } else {
                    sprite(buffer, spark.x, spark.y, spark.z, size, spark.colour, alpha);
                }
            }
            tessellator.draw();
        }
    }

    private static void atlasSprite(WorldRenderer buffer, Spark spark, float size, int alpha) {
        float u0 = (spark.cell % 4) * 0.25f, v0 = (spark.cell / 4) * 0.25f, u1 = u0 + 0.25f, v1 = v0 + 0.25f;
        double rad = Math.toRadians(spark.spin);
        float c = (float) Math.cos(rad) * size, s = (float) Math.sin(rad) * size;
        int r = spark.colour.getRed(), g = spark.colour.getGreen(), b = spark.colour.getBlue();
        buffer.pos(spark.x - c + s, spark.y - s - c, spark.z).tex(u0, v1).color(r, g, b, alpha).endVertex();
        buffer.pos(spark.x + c + s, spark.y + s - c, spark.z).tex(u1, v1).color(r, g, b, alpha).endVertex();
        buffer.pos(spark.x + c - s, spark.y + s + c, spark.z).tex(u1, v0).color(r, g, b, alpha).endVertex();
        buffer.pos(spark.x - c - s, spark.y - s + c, spark.z).tex(u0, v0).color(r, g, b, alpha).endVertex();
    }

    /** A flat, camera-facing sprite in model space. */
    private static void sprite(WorldRenderer buffer, float x, float y, float z, float size,
                               Color colour, int alpha) {
        int r = colour.getRed();
        int g = colour.getGreen();
        int b = colour.getBlue();
        buffer.pos(x - size, y - size, z).tex(0, 1).color(r, g, b, alpha).endVertex();
        buffer.pos(x + size, y - size, z).tex(1, 1).color(r, g, b, alpha).endVertex();
        buffer.pos(x + size, y + size, z).tex(1, 0).color(r, g, b, alpha).endVertex();
        buffer.pos(x - size, y + size, z).tex(0, 0).color(r, g, b, alpha).endVertex();
    }

    /** The ring that leaves the edge when a swing starts. */
    private static void rings() {
        if (RINGS.isEmpty()) return;
        GlStateManager.disableTexture2D();
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer buffer = tessellator.getWorldRenderer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        for (Ring ring : RINGS) {
            float progress = 1f - ring.life / ring.maxLife;
            float radius = progress * CONFIG.meleePulseSize / 10f;
            float band = Math.max(0.05f, radius * 0.18f);
            int alpha = (int) ((1f - progress) * 255 * (CONFIG.meleePulseStrength / 100f));
            int segments = 28;
            for (int i = 0; i < segments; i++) {
                double a0 = i / (double) segments * Math.PI * 2;
                double a1 = (i + 1) / (double) segments * Math.PI * 2;
                ringQuad(buffer, ring, a0, a1, radius - band, radius + band, alpha);
            }
        }
        tessellator.draw();
        GlStateManager.enableTexture2D();
    }

    private static void ringQuad(WorldRenderer buffer, Ring ring, double a0, double a1,
                                 float inner, float outer, int alpha) {
        int r = ring.colour.getRed();
        int g = ring.colour.getGreen();
        int b = ring.colour.getBlue();
        float c0 = (float) Math.cos(a0);
        float s0 = (float) Math.sin(a0);
        float c1 = (float) Math.cos(a1);
        float s1 = (float) Math.sin(a1);
        buffer.pos(ring.x + c0 * inner, ring.y + s0 * inner, ring.z).color(r, g, b, 0).endVertex();
        buffer.pos(ring.x + c1 * inner, ring.y + s1 * inner, ring.z).color(r, g, b, 0).endVertex();
        buffer.pos(ring.x + c1 * outer, ring.y + s1 * outer, ring.z).color(r, g, b, alpha).endVertex();
        buffer.pos(ring.x + c0 * outer, ring.y + s0 * outer, ring.z).color(r, g, b, alpha).endVertex();
    }

    /** Symbols circling the weapon while it is held. */
    private static void runes(MeleeWeapon weapon) {
        Symbol symbol = CONFIG.particleSymbol;
        ResourceLocation texture = symbol == null || symbol == Symbol.RANDOM
                ? TexturesManager.GLYPH_PENTAGRAM : symbol.texture();
        mc.getTextureManager().bindTexture(texture);

        int count = Math.max(1, CONFIG.meleeRuneCount);
        float radius = CONFIG.meleeRuneRadius / 10f;
        float size = CONFIG.meleeRuneSize / 100f * 1.5f;
        Color colour = colour();
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer buffer = tessellator.getWorldRenderer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        for (int i = 0; i < count; i++) {
            double angle = Math.toRadians(runeSpin * (CONFIG.meleeRuneSpeed / 100f))
                    + i * Math.PI * 2 / count;
            float x = (float) Math.cos(angle) * radius;
            float z = (float) Math.sin(angle) * radius;
            float y = MathUtility.sin((float) angle * 2f) * radius * 0.2f;
            sprite(buffer, x, y + 8f, z, size, colour, (int) (CONFIG.meleeRuneStrength / 100f * 255));
        }
        tessellator.draw();
    }

    /**
     * The effect colour: the theme's (moving with it when RGB is on), or else the weapon's own trail
     * colour from its profile, or the fixed glow colour.
     */
    private static Color colour() {
        if (CONFIG.meleeGlowUseTheme) return Palette.getColor(MathUtility.random(0f, 1f));
        MeleeWeapon weapon = tracked;
        Color own = weapon == null ? null : weapon.colour(MathUtility.random(0f, 1f) < 0.5f ? "trail" : "trail2");
        return own != null ? own : MeleeRenderer.glowColour();
    }

    /** The trail's colour at a point along it: an animated gradient from the head to the tail. */
    private static Color trailColour(float along) {
        if (CONFIG.meleeGlowUseTheme) return Palette.getColor(along * 0.6f + me.Padej_.soupapi.utils.FxClock.cycle(3f));
        MeleeWeapon weapon = tracked;
        Color a = weapon == null ? null : weapon.colour("trail");
        Color b = weapon == null ? null : weapon.colour("trail2");
        if (a == null) a = MeleeRenderer.glowColour();
        if (b == null) b = a;
        return me.Padej_.soupapi.utils.OklabUtils.interpolate(a, b, Math.max(0f, Math.min(1f, along)));
    }
}
