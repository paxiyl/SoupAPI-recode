package me.Padej_.soupapi.modules;

import me.Padej_.soupapi.config.ConfigurableModule;
import me.Padej_.soupapi.event.WorldRenderContext;
import me.Padej_.soupapi.render.Fx;
import me.Padej_.soupapi.render.SmokeFx;
import me.Padej_.soupapi.utils.FxClock;
import me.Padej_.soupapi.utils.Ambience;
import me.Padej_.soupapi.utils.MathUtility;
import me.Padej_.soupapi.utils.Palette;
import me.Padej_.soupapi.utils.Themes;
import net.minecraft.util.ResourceLocation;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * The air around the player: falling glyphs, snow, embers, whatever the theme asks for.
 *
 * <p>Kept as a volume that travels with the camera rather than a weather system. A column of Matrix
 * glyphs is only ever seen within a few blocks of the player, so simulating it out to the render
 * distance would cost everything and show nothing; particles that leave the box are wrapped back to
 * the far side of it, which reads as endless and costs a fixed number of sprites.
 */
public class WorldAmbience extends ConfigurableModule {

    private static final int MAX = 900;
    private static final List<Mote> MOTES = new ArrayList<Mote>();
    private static Ambience current = Ambience.NONE;

    private static final class Mote {
        double x, y, z;
        float vx, vy, vz;
        float size;
        float spin;
        float spinRate;
        /** Position within its column, 0 at the head; -1 for a scattered mote. */
        float columnDepth = -1f;
        float shade;
        /** Where in its own blink or pulse the mote is, so they do not all beat together. */
        float phase;
    }

    /** The ambience to draw: the theme's, unless the player has pinned one. */
    public static Ambience ambience() {
        if (!CONFIG.ambienceEnabled) return Ambience.NONE;
        if (CONFIG.ambienceStyle != Ambience.NONE) return CONFIG.ambienceStyle;
        return Themes.ambience();
    }

    public static void onTick() {
        Ambience style = ambience();
        if (style != current) {
            MOTES.clear();
            current = style;
        }
        if (style == Ambience.NONE || mc.thePlayer == null || mc.theWorld == null) {
            MOTES.clear();
            return;
        }

        float quality = CONFIG.ambienceQuality == null ? 1f : CONFIG.ambienceQuality.scale;
        int wanted = Math.min(MAX, Math.round(CONFIG.ambienceDensity * (style.columnar() ? 6 : 3) * quality
                * (style.smoky() ? 0.3f : 1f)));
        double radius = Math.max(4, CONFIG.ambienceRadius);
        double height = Math.max(3, CONFIG.ambienceHeight);

        while (MOTES.size() > wanted) {
            MOTES.remove(MOTES.size() - 1);
        }
        while (MOTES.size() < wanted) {
            Mote mote = spawn(style, radius, height, true);
            if (style == Ambience.CONSTELLATIONS && MOTES.size() % 5 != 0) {
                // Constellations come in fives: each star near the one before it.
                Mote previous = MOTES.get(MOTES.size() - 1);
                mote.x = previous.x + MathUtility.random(-2.2f, 2.2f);
                mote.y = previous.y + MathUtility.random(-1.4f, 1.4f);
                mote.z = previous.z + MathUtility.random(-2.2f, 2.2f);
                mote.vx = previous.vx;
                mote.vy = previous.vy;
                mote.vz = previous.vz;
            }
            MOTES.add(mote);
        }

        float speed = CONFIG.ambienceSpeed / 100f;
        if (style.smoky()) smoke(style, radius, quality);
        double px = mc.thePlayer.posX;
        double py = mc.thePlayer.posY + 1.2;
        double pz = mc.thePlayer.posZ;

        for (Mote mote : MOTES) {
            mote.x += mote.vx * speed;
            mote.y += mote.vy * speed;
            mote.z += mote.vz * speed;
            if (style.drift() > 0f && style != Ambience.CONSTELLATIONS) {
                mote.vx += MathUtility.random(-0.01f, 0.01f) * style.drift();
                mote.vz += MathUtility.random(-0.01f, 0.01f) * style.drift();
                mote.vx *= 0.96f;
                mote.vz *= 0.96f;
            }
            mote.spin += mote.spinRate;

            // Wrap rather than respawn, so a column keeps its shape as the player walks.
            if (mote.y - py > height) mote.y = py - height;
            if (py - mote.y > height) mote.y = py + height;
            if (Math.abs(mote.x - px) > radius) mote.x = px - Math.signum(mote.x - px) * radius;
            if (Math.abs(mote.z - pz) > radius) mote.z = pz - Math.signum(mote.z - pz) * radius;
        }
    }

    private static Mote spawn(Ambience style, double radius, double height, boolean anywhere) {
        Mote mote = new Mote();
        double px = mc.thePlayer.posX;
        double py = mc.thePlayer.posY + 1.2;
        double pz = mc.thePlayer.posZ;
        mote.x = px + MathUtility.random((float) -radius, (float) radius);
        mote.z = pz + MathUtility.random((float) -radius, (float) radius);
        mote.y = py + (anywhere
                ? MathUtility.random((float) -height, (float) height)
                : (style.gravity() < 0f ? height : -height));
        mote.vy = style.gravity() * MathUtility.random(0.6f, 1.4f) * 0.25f;
        mote.vx = MathUtility.random(-1f, 1f) * style.drift() * 0.1f;
        mote.vz = MathUtility.random(-1f, 1f) * style.drift() * 0.1f;
        mote.size = style.size() * MathUtility.random(0.6f, 1.3f)
                * (CONFIG.ambienceSize / 100f);
        mote.shade = MathUtility.random(0.45f, 1f);
        mote.phase = MathUtility.random(0f, (float) (Math.PI * 2));
        if (style == Ambience.SPECTRAL_CURRENTS) {
            // Currents all flow one way, so they read as a current and not as wind.
            mote.vx = 0.05f * MathUtility.random(0.7f, 1.3f);
            mote.vz = 0.015f * MathUtility.random(-1f, 1f);
        }
        if (style.spins()) {
            mote.spinRate = MathUtility.random(-6f, 6f);
        }
        if (style.columnar()) {
            mote.columnDepth = MathUtility.random(0f, 1f);
        }
        return mote;
    }

    /**
     * Ground smoke for the smoky styles: a few puffs a tick, low around the player, fed to the shared
     * smoke system so they sort, light and fade like every other smoke in the mod (and obey its
     * quality cap and switch).
     */
    private static void smoke(Ambience style, double radius, float quality) {
        int n = Math.max(1, Math.round(CONFIG.ambienceDensity / 30f * quality));
        double px = mc.thePlayer.posX, py = mc.thePlayer.posY, pz = mc.thePlayer.posZ;
        int tint = style.tint();
        Color accent = new Color(tint);
        for (int i = 0; i < n; i++) {
            if (MathUtility.random(0f, 1f) > 0.55f) continue;
            double a = MathUtility.random(0f, (float) (Math.PI * 2));
            double r = MathUtility.random(2f, (float) radius);
            SmokeFx.P p = SmokeFx.particle()
                    .kind(style == Ambience.SPECTRAL_SMOKE || i % 3 == 0 ? SmokeFx.Kind.WISP : SmokeFx.Kind.SMOKE)
                    .at(px + Math.cos(a) * r, py + MathUtility.random(-0.2f, 0.7f), pz + Math.sin(a) * r)
                    .vel(MathUtility.random(-0.01f, 0.01f), style == Ambience.INFERNAL_SMOKE ? 0.012 : 0.003,
                            MathUtility.random(-0.01f, 0.01f))
                    .life(MathUtility.random(70f, 120f))
                    .size(1.0f, MathUtility.random(2.2f, 3.4f))
                    .spin(MathUtility.random(0f, 360f), MathUtility.random(-0.8f, 0.8f))
                    .alpha(style == Ambience.SPECTRAL_SMOKE ? 0.35f : 0.55f)
                    .edge(0.6f).fade(0.3f, 0.6f).turbulence(0.4f);
            if (style == Ambience.SPECTRAL_SMOKE) p.colour(0.55f, 0.63f, 0.72f);
            else if (style == Ambience.INFERNAL_SMOKE) p.colour(0.09f, 0.03f, 0.02f);
            else p.colour(0.03f, 0.02f, 0.05f);
            if (CONFIG.ambienceUseTheme && Palette.animates(Palette.Element.AMBIENCE)) {
                p.accentPalette(MathUtility.random(0f, 1f), Palette.Element.AMBIENCE);
            } else {
                p.accent(accent);
            }
            SmokeFx.spawn(p);
        }
    }

    public static void render(WorldRenderContext context) {
        Ambience style = ambience();
        if (style == Ambience.NONE || MOTES.isEmpty()) return;

        ResourceLocation texture = new ResourceLocation("soupapi",
                "textures/particle/" + style.sprite() + ".png");
        float alpha = CONFIG.ambienceAlpha / 100f;
        float time = FxClock.seconds();
        double reach = CONFIG.ambienceDrawDistance;
        double cx = mc.getRenderManager().viewerPosX, cy = mc.getRenderManager().viewerPosY;
        double cz = mc.getRenderManager().viewerPosZ;

        me.Padej_.soupapi.compat.BufferBuilder buffer = Fx.beginSprites(texture);
        try {
            for (Mote mote : MOTES) {
                double dx = mote.x - cx, dy = mote.y - cy, dz = mote.z - cz;
                double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
                if (distance > reach) continue;
                // Thin out toward the edge of the draw distance instead of popping.
                float edge = (float) Math.min(1.0, (reach - distance) / 3.0);
                float size = mote.size;
                float pulse = 1f;
                if (style == Ambience.VOID_DISTORTIONS) {
                    float cycle = (float) (((time * 0.35f + mote.phase) % 1f + 1f) % 1f);
                    size *= 0.4f + 1.2f * cycle;
                    pulse = (float) Math.sin(cycle * Math.PI);
                }
                Color colour = colourOf(style, mote, alpha * edge * pulse * twinkle(style, mote, time));
                Fx.appendSprite(buffer, mote.x, mote.y, mote.z, size, colour, false);
            }
        } finally {
            Fx.endSprites(buffer);
        }
        if (style == Ambience.CONSTELLATIONS) constellations(alpha, reach, cx, cy, cz);
    }

    /** Fireflies blink, stars twinkle, glass flashes as it turns. */
    private static float twinkle(Ambience style, Mote mote, float time) {
        if (!style.twinkles()) return 1f;
        float wave = (float) Math.sin(time * (style == Ambience.CONSTELLATIONS ? 2.2f : 1.3f) + mote.phase * 3f);
        if (style == Ambience.VIOLET_FIREFLIES || style == Ambience.FIREFLIES) {
            return Math.max(0.05f, wave);
        }
        if (style == Ambience.GLASS_FRAGMENTS) {
            return 0.35f + 0.65f * (float) Math.pow(Math.abs(Math.sin(Math.toRadians(mote.spin))), 6);
        }
        return 0.55f + 0.45f * wave;
    }

    /** Stars joined in fives by faint lines, each link only while its two stars are near. */
    private static void constellations(float alpha, double reach, double cx, double cy, double cz) {
        Color base = CONFIG.ambienceUseTheme ? Palette.getColor(0.3f) : new Color(CONFIG.ambienceColor);
        Color line = new Color(base.getRed(), base.getGreen(), base.getBlue(), Math.round(150 * alpha));
        for (int i = 0; i + 1 < MOTES.size(); i++) {
            if (i % 5 == 4) continue;
            Mote a = MOTES.get(i), b = MOTES.get(i + 1);
            double dx = a.x - b.x, dy = a.y - b.y, dz = a.z - b.z;
            if (dx * dx + dy * dy + dz * dz > 16) continue;
            double ax = a.x - cx, ay = a.y - cy, az = a.z - cz;
            if (ax * ax + ay * ay + az * az > reach * reach) continue;
            Fx.glowLine(a.x, a.y, a.z, b.x, b.y, b.z, 0.05, line);
        }
    }

    /**
     * A mote's colour.
     *
     * <p>The head of a glyph column is drawn near white and the tail falls away to the theme's
     * colour, which is what makes a column read as a column rather than a line of dots.
     */
    private static Color colourOf(Ambience style, Mote mote, float alpha) {
        Color base = CONFIG.ambienceUseTheme
                ? Palette.getColor(mote.shade)
                : new Color(CONFIG.ambienceColor);
        // Styles with a colour of their own keep it, unless the theme is cycling (RGB) for ambience.
        if (style.tint() >= 0 && CONFIG.ambienceUseTheme && !Palette.animates(Palette.Element.AMBIENCE)) {
            base = new Color(style.tint());
        }
        float brightness = mote.shade;
        if (mote.columnDepth >= 0f) {
            float head = 1f - mote.columnDepth;
            brightness = 0.25f + 0.75f * head * head;
            if (head > 0.92f) {
                return new Color(255, 255, 255, clamp(alpha * 255f));
            }
        }
        return new Color(
                clamp(base.getRed() * brightness),
                clamp(base.getGreen() * brightness),
                clamp(base.getBlue() * brightness),
                clamp(alpha * 255f * (0.4f + 0.6f * brightness)));
    }

    private static int clamp(float value) {
        return Math.max(0, Math.min(255, Math.round(value)));
    }

    /** How many motes are alive, for the settings screen's readout. */
    public static int count() {
        return MOTES.size();
    }
}
