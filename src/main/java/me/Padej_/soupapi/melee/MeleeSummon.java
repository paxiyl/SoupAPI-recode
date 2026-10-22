package me.Padej_.soupapi.melee;

import me.Padej_.soupapi.config.ConfigurableModule;
import me.Padej_.soupapi.render.SmokeFx;
import me.Padej_.soupapi.utils.Palette;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.Random;

import static me.Padej_.soupapi.melee.SummonFx.*;

/**
 * How each weapon arrives.
 *
 * <p>Every weapon used to come out of the same rift, which made eleven weapons look like one trick
 * with eleven skins. A weapon now names its own summon in its pack profile, and gets a piece of
 * choreography written for what it is:
 *
 * <ul>
 *   <li>{@code rift} — a fracture tears open, rune rings turn in it and the weapon is drawn out.
 *       The Weeping Eye's version opens a great eye in the tear, and the staff comes out of the
 *       pupil as the iris contracts.</li>
 *   <li>{@code wing} — no circle at all: feathers blow in from off-screen, tighten into a black
 *       whirl, and the claw is left behind when the murder scatters.</li>
 *   <li>{@code forge} — the blade is written in light: a hairline draws itself upward, throws
 *       sparks like a weld, flares white as it is quenched, and sheds a shockwave.</li>
 *   <li>{@code bloom} — a sigil is drawn flat in the air, rings of light rise off it one after
 *       another, and petals of light spiral up and close into the weapon.</li>
 * </ul>
 *
 * <p>All three are pure functions of the summon clip's own playhead, so nothing outlives the clip
 * and there is no state to leak when the weapon is put away or the world goes.
 */
public final class MeleeSummon extends ConfigurableModule {

    /** The styles a pack can ask for by name. */
    public enum Style {
        RIFT, EYE, WING, FORGE, BLOOM
    }

    private static final ResourceLocation GREAT_EYE =
            new ResourceLocation("soupapi", "textures/vfx/great_eye.png");
    private static final ResourceLocation SUMMON_ATLAS =
            new ResourceLocation("soupapi", "textures/vfx/summon_atlas.png");

    /** The summon atlas is 2x2: two crows, a sigil, a shockwave ring. */
    private static final float[] CROW_L = {0f, 0f, 0.5f, 0.5f};
    private static final float[] CROW_R = {0.5f, 0f, 1f, 0.5f};
    private static final float[] SIGIL = {0f, 0.5f, 0.5f, 1f};
    private static final float[] SHOCK = {0.5f, 0.5f, 1f, 1f};

    private static long seed = 1L;

    private MeleeSummon() {
    }

    /** A new summon: a new pattern of feathers, sparks and shards. */
    static void begin() {
        seed = new Random().nextLong();
        MeleePortal.begin();
    }

    static Style style(MeleeState state) {
        MeleeWeapon weapon = state == null ? null : state.weapon();
        String name = weapon == null ? null : weapon.profileText("summon_style");
        if (name != null) {
            for (Style s : Style.values()) {
                if (s.name().equalsIgnoreCase(name)) return s;
            }
        }
        return Style.RIFT;
    }

    static boolean active(MeleeState state) {
        return CONFIG.meleeSummonEnabled && state != null && state.summoning() && time(state) < END;
    }

    /**
     * Where the summon is in its own sequence.
     *
     * <p>Written against a {@value SummonFx#END}-second beat; a weapon whose summon clip is shorter
     * or longer is scaled onto it, so the effect and the animation can never drift apart.
     */
    static float time(MeleeState state) {
        me.Padej_.soupapi.bedrock.GeoClip clip = state.mainClip();
        float t = state.mainTime();
        if (clip == null || clip.length <= 0.1f) return t;
        return t / clip.length * END;
    }

    /** Drawn before the weapon, so the weapon comes out in front of it. */
    public static void renderBack(MeleeState state) {
        if (!active(state)) return;
        Style style = style(state);
        if (style == Style.RIFT || style == Style.EYE) {
            MeleePortal.renderBack(state, style == Style.EYE);
            return;
        }
        Palette.Element previous = Palette.scope(Palette.Element.WEAPON);
        begin3d();
        try {
            if (style == Style.WING) {
                wingBack(state);
            } else if (style == Style.BLOOM) {
                bloomBack(state);
            } else {
                forgeBack(state);
            }
        } finally {
            end3d();
            Palette.scope(previous);
        }
    }

    /** Drawn after the weapon, so the last burst flies in front of it. */
    public static void renderFront(MeleeState state) {
        if (!active(state)) return;
        Style style = style(state);
        if (style == Style.RIFT || style == Style.EYE) {
            MeleePortal.renderFront(state);
            return;
        }
        Palette.Element previous = Palette.scope(Palette.Element.WEAPON);
        begin3d();
        try {
            if (style == Style.WING) {
                wingFront(state);
            } else if (style == Style.BLOOM) {
                bloomFront(state);
            } else {
                forgeFront(state);
            }
        } finally {
            end3d();
            Palette.scope(previous);
        }
    }

    // ---- wing: the murder ------------------------------------------------------------------

    /**
     * The claw's summon.
     *
     * <p>Feathers blow in from beyond the edges of the view, tighten into a whirl that keeps its
     * own darkness, flash once as the claw takes their place, and blow out again. The only circle
     * in it is the one the feathers make themselves.
     */
    private static void wingBack(MeleeState state) {
        float t = time(state);
        float size = CONFIG.meleePortalSize / 100f;
        float glow = CONFIG.meleePortalGlow / 100f;
        float intensity = CONFIG.meleePortalIntensity / 100f;
        float density = CONFIG.meleePortalParticles / 100f;
        MeleeWeapon weapon = state.weapon();
        Color plume = weaponColour(weapon, "trail", 0.15f);
        Color bloom = weaponColour(weapon, "trail2", 0.65f);
        Random r = new Random(seed);

        // The whirl's own darkness, under everything: a soft black disc that grows and collapses.
        float dark = window(t, 0.35f, 0.8f, 1.5f, 2.1f);
        if (dark > 0f) {
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            bind(SmokeFx.PORTAL_CORE);
            WorldRenderer w = buffer();
            quad(w, PX, PY, PZ + 0.02f, 1.25f * size * dark, -t * 150f, 0f, 0f, 1f, 1f,
                    0.04f, 0.02f, 0.06f, dark * 0.9f * Math.min(1f, intensity));
            draw();
        }

        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

        // The feathers. Each one comes in from its own direction at its own moment, spirals down
        // to the centre and is held in the whirl until the claw takes over.
        bind(SmokeFx.FX_ATLAS);
        WorldRenderer w = buffer();
        int plumes = Math.round(34 * density);
        for (int i = 0; i < plumes; i++) {
            float start = 0.05f + r.nextFloat() * 0.45f;
            float arrive = start + 0.45f + r.nextFloat() * 0.35f;
            float k = clamp01((t - start) / (arrive - start));
            float held = window(t, start, start + 0.12f, 1.55f + r.nextFloat() * 0.3f, 2.15f);
            if (held <= 0f) continue;
            float entry = r.nextFloat() * 6.283f;
            // Spiralling in: the angle runs on while the radius falls away.
            float ang = entry + (1f - k) * 2.4f + t * (1.6f + r.nextFloat());
            float far = 2.6f + r.nextFloat() * 1.4f;
            float rad = (far * (1f - smooth(k)) + (0.30f + r.nextFloat() * 0.38f) * smooth(k)) * size;
            float x = PX + (float) Math.cos(ang) * rad;
            float y = PY + (float) Math.sin(ang) * rad * 0.9f;
            float spin = (float) Math.toDegrees(ang) + 90f + 40f * (float) Math.sin(t * 5f + i);
            sprite(w, x, y, PZ + 0.03f - 0.1f * r.nextFloat(), (0.26f + 0.16f * r.nextFloat()) * size,
                    spin, 3, i % 5 == 0 ? bloom : plume, held * (0.45f + 0.55f * k) * intensity * glow);
        }

        // Petals torn off the roses, riding the same whirl but slower and warmer.
        int petals = Math.round(14 * density);
        for (int i = 0; i < petals; i++) {
            float held = window(t, 0.6f + r.nextFloat() * 0.3f, 0.9f, 1.7f, 2.2f);
            if (held <= 0f) continue;
            float ang = r.nextFloat() * 6.283f + t * 1.1f;
            float rad = (0.5f + r.nextFloat() * 0.5f) * size;
            sprite(w, PX + (float) Math.cos(ang) * rad, PY + (float) Math.sin(ang) * rad * 0.85f, PZ + 0.04f,
                    0.1f * size, t * 140f + i * 50f, 4 + (i % 3), bloom, held * 0.8f * intensity);
        }
        draw();

        // The flash the claw arrives on.
        float flash = bump(t, 1.08f, 0.2f);
        if (flash > 0f) {
            bind(SmokeFx.GLOW);
            w = buffer();
            quad(w, PX, PY, PZ + 0.05f, 2.1f * size * (0.45f + flash), 0f, 0f, 0f, 1f, 1f,
                    Color.WHITE, flash * glow * intensity);
            draw();
        }
    }

    /** The murder scatters: crows break out of the whirl and fly past the player. */
    private static void wingFront(MeleeState state) {
        float t = time(state);
        float burst = (t - 1.55f) / 0.9f;
        if (burst <= 0f || burst >= 1f) return;
        float size = CONFIG.meleePortalSize / 100f;
        float intensity = CONFIG.meleePortalIntensity / 100f;
        float density = CONFIG.meleePortalParticles / 100f;
        Color plume = weaponColour(state.weapon(), "trail", 0.2f);
        Random r = new Random(seed ^ 0x5DEECE66DL);
        float ease = 1f - (1f - burst) * (1f - burst);

        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        bind(SUMMON_ATLAS);
        WorldRenderer w = buffer();
        int birds = Math.max(3, Math.round(6 * density));
        for (int i = 0; i < birds; i++) {
            float ang = r.nextFloat() * 6.283f;
            float speed = (1.3f + r.nextFloat() * 1.6f) * size;
            float[] uv = (i % 2 == 0) ? CROW_L : CROW_R;
            // Wings beating: the sprite is squashed and stretched on its own clock.
            float beat = 0.72f + 0.28f * (float) Math.sin(t * 26f + i * 2.1f);
            float s = (0.34f + 0.1f * r.nextFloat()) * size * (0.6f + ease);
            quad(w, PX + (float) Math.cos(ang) * speed * ease,
                    PY + (float) Math.sin(ang) * speed * ease * 0.8f,
                    PZ + 0.5f * ease, s, s * beat,
                    (float) Math.toDegrees(ang) - 90f, uv[0], uv[1], uv[2], uv[3],
                    0.08f, 0.07f, 0.10f, (1f - burst) * 1.4f);
        }
        draw();

        bind(SmokeFx.FX_ATLAS);
        w = buffer();
        int motes = Math.round(30 * density);
        for (int i = 0; i < motes; i++) {
            float ang = r.nextFloat() * 6.283f;
            float speed = (0.7f + r.nextFloat() * 1.5f) * size;
            boolean feather = i % 2 == 0;
            sprite(w, PX + (float) Math.cos(ang) * speed * ease, PY + (float) Math.sin(ang) * speed * ease,
                    PZ + (0.1f + r.nextFloat() * 0.5f) * ease, (feather ? 0.18f : 0.07f) * size,
                    i * 41f + burst * 520f, feather ? 3 : 0, plume, (1f - burst) * intensity);
        }
        draw();
    }

    // ---- forge: the blade written in light --------------------------------------------------

    /**
     * The spectral blade's summon.
     *
     * <p>Nothing opens; something is drawn. A point of light at the hand runs upward into a
     * hairline, which throws sparks the way a weld does, and the blade is quenched out of it in a
     * white flare. The sigil behind it is edge-on and turning, so it reads as a plane the blade is
     * being drawn on rather than a ring the blade is coming through.
     */
    private static void forgeBack(MeleeState state) {
        float t = time(state);
        float size = CONFIG.meleePortalSize / 100f;
        float glow = CONFIG.meleePortalGlow / 100f;
        float intensity = CONFIG.meleePortalIntensity / 100f;
        float density = CONFIG.meleePortalParticles / 100f;
        MeleeWeapon weapon = state.weapon();
        Color light = weaponColour(weapon, "trail", 0.3f);
        Color hot = weaponColour(weapon, "trail2", 0.8f);
        Random r = new Random(seed);

        float base = PY - 0.42f * size;
        float full = PY + 0.78f * size;

        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

        // The sigil: turned nearly edge-on and spinning about its upright, a plane rather than a hoop.
        float sig = window(t, 0.25f, 0.6f, 1.6f, 2.2f);
        if (sig > 0f) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(PX, PY, PZ);
            GlStateManager.rotate(74f, 1f, 0f, 0f);
            GlStateManager.rotate(t * 150f, 0f, 0f, 1f);
            bind(SUMMON_ATLAS);
            WorldRenderer w = buffer();
            quad(w, 0f, 0f, 0f, 1.5f * size * (0.55f + 0.45f * sig), 0f,
                    SIGIL[0], SIGIL[1], SIGIL[2], SIGIL[3], light, sig * glow * intensity);
            draw();
            GlStateManager.popMatrix();
        }

        // The hairline: drawn from the hand upward, thickening as it goes.
        float drawn = smooth((t - 0.08f) / 0.75f);
        float alive = 1f - smooth((t - 1.5f) / 0.5f);
        if (drawn > 0f && alive > 0f) {
            bind(SmokeFx.GLOW);
            WorldRenderer w = buffer();
            float tip = base + (full - base) * drawn;
            float width = (0.018f + 0.10f * smooth((t - 0.75f) / 0.5f)) * size;
            band(w, PX, base, PX, tip, PZ + 0.03f, width, 0.5f, 0.02f, 0.5f, 0.98f,
                    Color.WHITE, alive * glow * intensity);
            band(w, PX, base, PX, tip, PZ + 0.02f, width * 5.5f, 0.02f, 0.5f, 0.98f, 0.5f,
                    light, alive * 0.55f * glow * intensity);
            // The point it is being drawn by.
            if (drawn < 1f) {
                quad(w, PX, tip, PZ + 0.04f, 0.42f * size, 0f, 0f, 0f, 1f, 1f, hot,
                        alive * glow * intensity);
            }
            draw();
        }

        // Sparks falling off the line the way they fall off a weld.
        bind(SmokeFx.FX_ATLAS);
        WorldRenderer w = buffer();
        int sparks = Math.round(40 * density);
        for (int i = 0; i < sparks; i++) {
            float born = 0.12f + r.nextFloat() * 1.15f;
            float age = (t - born) / 0.55f;
            if (age <= 0f || age >= 1f) continue;
            float from = base + (full - base) * r.nextFloat() * drawn;
            float vx = (r.nextFloat() - 0.5f) * 1.5f;
            float x = PX + vx * age * 0.55f * size;
            float y = from + (0.5f - age * age * 1.9f) * 0.42f * size;
            sprite(w, x, y, PZ + 0.03f, (0.05f + 0.03f * r.nextFloat()) * size,
                    i * 53f + t * 300f, i % 4 == 0 ? 2 : 0, i % 3 == 0 ? hot : light,
                    (1f - age) * intensity);
        }
        draw();

        // The quench: a white flare and a flat shockwave across the blade's line.
        float quench = bump(t, 1.02f, 0.26f);
        if (quench > 0f) {
            bind(SmokeFx.GLOW);
            w = buffer();
            quad(w, PX, PY, PZ + 0.05f, 2.6f * size * quench, 0.55f * size * quench, 0f,
                    0f, 0f, 1f, 1f, 1f, 1f, 1f, quench * glow * intensity);
            draw();
        }
    }

    /** What the quench sheds: a ring going out, and embers falling away in front of the blade. */
    private static void forgeFront(MeleeState state) {
        float t = time(state);
        float size = CONFIG.meleePortalSize / 100f;
        float intensity = CONFIG.meleePortalIntensity / 100f;
        float density = CONFIG.meleePortalParticles / 100f;
        Color light = weaponColour(state.weapon(), "trail", 0.3f);
        Random r = new Random(seed ^ 0x27BB2EE6L);

        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        float ring = (t - 0.95f) / 0.7f;
        if (ring > 0f && ring < 1f) {
            bind(SUMMON_ATLAS);
            WorldRenderer w = buffer();
            float ease = 1f - (1f - ring) * (1f - ring);
            GlStateManager.pushMatrix();
            GlStateManager.translate(PX, PY, PZ);
            GlStateManager.rotate(74f, 1f, 0f, 0f);
            quad(w, 0f, 0f, 0f, 3.4f * size * ease, 0f, SHOCK[0], SHOCK[1], SHOCK[2], SHOCK[3],
                    Color.WHITE, (1f - ring) * intensity);
            draw();
            GlStateManager.popMatrix();
        }

        float shed = (t - 1.15f) / 1.1f;
        if (shed > 0f && shed < 1f) {
            bind(SmokeFx.FX_ATLAS);
            WorldRenderer w = buffer();
            int embers = Math.round(26 * density);
            for (int i = 0; i < embers; i++) {
                float born = r.nextFloat() * 0.5f;
                float age = clamp01((shed - born) / (1f - born));
                if (age <= 0f) continue;
                float x = PX + (r.nextFloat() - 0.5f) * 1.1f * size;
                float y = PY + (0.6f - age * 1.5f) * size + (r.nextFloat() - 0.5f) * 0.2f;
                sprite(w, x, y, PZ + 0.3f * age, 0.06f * size * (1f - age * 0.5f),
                        i * 37f + t * 180f, 0, light, (1f - age) * intensity);
            }
            draw();
        }
    }

    // ---- bloom: the flower of light ----------------------------------------------------------

    /**
     * The scepter's and the lotus's summon.
     *
     * <p>A sigil is drawn flat in the air below the hand and rings of light climb off it, one after
     * another, each turning the other way. Petals of light spiral up out of the sigil and close
     * over the top, and the weapon is what is left standing in them. Nothing tears and nothing
     * burns: it grows.
     */
    private static void bloomBack(MeleeState state) {
        float t = time(state);
        float size = CONFIG.meleePortalSize / 100f;
        float glow = CONFIG.meleePortalGlow / 100f;
        float intensity = CONFIG.meleePortalIntensity / 100f;
        float density = CONFIG.meleePortalParticles / 100f;
        MeleeWeapon weapon = state.weapon();
        Color petal = weaponColour(weapon, "trail", 0.25f);
        Color light = weaponColour(weapon, "trail2", 0.75f);
        Random r = new Random(seed);

        float floor = PY - 0.52f * size;

        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

        // The sigil, lying flat: drawn edge-on so it reads as a plate the weapon stands on.
        float sig = window(t, 0.05f, 0.45f, 1.7f, 2.3f);
        if (sig > 0f) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(PX, floor, PZ);
            GlStateManager.rotate(80f, 1f, 0f, 0f);
            GlStateManager.rotate(-t * 46f, 0f, 0f, 1f);
            bind(SUMMON_ATLAS);
            WorldRenderer w = buffer();
            quad(w, 0f, 0f, 0f, 2.2f * size * (0.35f + 0.65f * sig), 0f,
                    SIGIL[0], SIGIL[1], SIGIL[2], SIGIL[3], light, sig * glow * intensity);
            draw();
            GlStateManager.popMatrix();
        }

        // Rings climbing off it, each one turning against the one before.
        bind(SUMMON_ATLAS);
        WorldRenderer w = buffer();
        for (int i = 0; i < 4; i++) {
            float born = 0.25f + i * 0.2f;
            float climb = clamp01((t - born) / 0.95f);
            if (climb <= 0f) continue;
            float fade = smooth(climb / 0.2f) * (1f - smooth((climb - 0.65f) / 0.35f));
            float y = floor + climb * 1.3f * size;
            GlStateManager.pushMatrix();
            GlStateManager.translate(PX, y, PZ);
            GlStateManager.rotate(80f, 1f, 0f, 0f);
            GlStateManager.rotate(t * (i % 2 == 0 ? 90f : -90f), 0f, 0f, 1f);
            quad(w, 0f, 0f, 0f, 2.0f * size * (1f - climb * 0.55f), 0f,
                    SHOCK[0], SHOCK[1], SHOCK[2], SHOCK[3], i % 2 == 0 ? light : petal,
                    fade * glow * intensity);
            draw();
            GlStateManager.popMatrix();
            w = buffer();
        }
        draw();

        // Petals of light: they spiral up out of the sigil and lean in over the top.
        bind(SmokeFx.FX_ATLAS);
        w = buffer();
        int leaves = Math.round(26 * density);
        for (int i = 0; i < leaves; i++) {
            float born = 0.3f + r.nextFloat() * 0.6f;
            float climb = clamp01((t - born) / 1.0f);
            if (climb <= 0f) continue;
            float fade = smooth(climb / 0.15f) * (1f - smooth((climb - 0.6f) / 0.4f));
            float ang = r.nextFloat() * 6.283f + climb * 3.4f;
            // Out, then in: the radius opens and closes, which is what makes it a flower.
            float rad = (float) Math.sin(climb * Math.PI) * (0.55f + r.nextFloat() * 0.35f) * size;
            float y = floor + climb * 1.45f * size;
            sprite(w, PX + (float) Math.cos(ang) * rad, y, PZ + (float) Math.sin(ang) * rad * 0.35f,
                    (0.16f + 0.08f * r.nextFloat()) * size,
                    (float) Math.toDegrees(ang) + climb * 220f, 3,
                    i % 3 == 0 ? light : petal, fade * intensity * glow);
        }
        draw();

        // The close: everything gathers and there is one bright moment as the weapon takes its place.
        float close = bump(t, 1.12f, 0.24f);
        if (close > 0f) {
            bind(SmokeFx.GLOW);
            w = buffer();
            quad(w, PX, PY, PZ + 0.04f, 1.9f * size * (0.5f + close), 0f, 0f, 0f, 1f, 1f,
                    Color.WHITE, close * glow * intensity);
            draw();
        }
    }

    /** What the bloom sheds: seeds of light drifting up past the weapon. */
    private static void bloomFront(MeleeState state) {
        float t = time(state);
        float shed = (t - 1.1f) / 1.2f;
        if (shed <= 0f || shed >= 1f) return;
        float size = CONFIG.meleePortalSize / 100f;
        float intensity = CONFIG.meleePortalIntensity / 100f;
        float density = CONFIG.meleePortalParticles / 100f;
        Color light = weaponColour(state.weapon(), "trail2", 0.7f);
        Random r = new Random(seed ^ 0x1234567L);

        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        bind(SmokeFx.FX_ATLAS);
        WorldRenderer w = buffer();
        int seeds = Math.round(22 * density);
        for (int i = 0; i < seeds; i++) {
            float born = r.nextFloat() * 0.55f;
            float age = clamp01((shed - born) / (1f - born));
            if (age <= 0f) continue;
            float ang = r.nextFloat() * 6.283f;
            float rad = (0.2f + r.nextFloat() * 0.5f) * size;
            float y = PY - 0.3f * size + age * 1.3f * size;
            sprite(w, PX + (float) Math.cos(ang) * rad, y, PZ + 0.25f * age,
                    0.055f * size * (1f - age * 0.4f), i * 29f + t * 120f, 0, light,
                    (1f - age) * intensity);
        }
        draw();
    }
}
