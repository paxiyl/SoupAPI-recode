package me.Padej_.soupapi.modules;

import me.Padej_.soupapi.config.ConfigurableModule;
import me.Padej_.soupapi.event.WorldRenderContext;
import me.Padej_.soupapi.render.Fx;
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

        int wanted = Math.min(MAX, CONFIG.ambienceDensity * (style.columnar() ? 6 : 3));
        double radius = Math.max(4, CONFIG.ambienceRadius);
        double height = Math.max(3, CONFIG.ambienceHeight);

        while (MOTES.size() > wanted) {
            MOTES.remove(MOTES.size() - 1);
        }
        while (MOTES.size() < wanted) {
            MOTES.add(spawn(style, radius, height, true));
        }

        float speed = CONFIG.ambienceSpeed / 100f;
        double px = mc.thePlayer.posX;
        double py = mc.thePlayer.posY + 1.2;
        double pz = mc.thePlayer.posZ;

        for (Mote mote : MOTES) {
            mote.x += mote.vx * speed;
            mote.y += mote.vy * speed;
            mote.z += mote.vz * speed;
            if (style.drift() > 0f) {
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
        if (style.spins()) {
            mote.spinRate = MathUtility.random(-6f, 6f);
        }
        if (style.columnar()) {
            mote.columnDepth = MathUtility.random(0f, 1f);
        }
        return mote;
    }

    public static void render(WorldRenderContext context) {
        Ambience style = ambience();
        if (style == Ambience.NONE || MOTES.isEmpty()) return;

        ResourceLocation texture = new ResourceLocation("soupapi",
                "textures/particle/" + style.sprite() + ".png");
        float alpha = CONFIG.ambienceAlpha / 100f;

        me.Padej_.soupapi.compat.BufferBuilder buffer = Fx.beginSprites(texture);
        try {
            for (Mote mote : MOTES) {
                Color colour = colourOf(style, mote, alpha);
                Fx.appendSprite(buffer, mote.x, mote.y, mote.z, mote.size, colour, false);
            }
        } finally {
            Fx.endSprites(buffer);
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
