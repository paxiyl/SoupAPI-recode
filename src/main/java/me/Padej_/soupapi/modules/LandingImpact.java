package me.Padej_.soupapi.modules;

import me.Padej_.soupapi.config.ConfigurableModule;
import me.Padej_.soupapi.render.FxParticles;
import me.Padej_.soupapi.utils.MathUtility;
import me.Padej_.soupapi.utils.Palette;
import me.Padej_.soupapi.utils.TexturesManager;
import net.minecraft.entity.player.EntityPlayer;

import java.awt.Color;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * What happens where a player lands.
 *
 * <p>Scaled by the fall: a step off a block barely registers, a drop from height throws a wide ring
 * and a cloud of dust. {@code landingImpactMinFall} is the height below which nothing is drawn.
 */
public class LandingImpact extends ConfigurableModule {

    public enum Style {
        /** One expanding ground ring. */
        RING,
        /** Dust thrown up and outward. */
        DUST,
        /** A ring plus a low spray, the classic impact. */
        SHOCKWAVE,
        /** Shards radiating out like a cracked floor. */
        CRACK,
        /** A wet splash: droplets arcing up. */
        SPLASH,
        /** A dome of sprites expanding from the feet. */
        BURST,
        /** Rune symbols flashing on the ground. */
        RUNES
    }

    /** Airborne state and the height the fall started from, per player. */
    private static final Map<EntityPlayer, Double> FALL_START = new WeakHashMap<EntityPlayer, Double>();
    private static final Map<EntityPlayer, Boolean> AIRBORNE = new WeakHashMap<EntityPlayer, Boolean>();

    public static void onTick() {
        if (mc.theWorld == null || mc.thePlayer == null) return;
        if (!CONFIG.landingImpactEnabled) return;

        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (!isTarget(player)) continue;

            Boolean wasAirborne = AIRBORNE.get(player);
            boolean airborne = !player.onGround;

            if (airborne) {
                Double start = FALL_START.get(player);
                if (start == null || player.posY > start) {
                    FALL_START.put(player, player.posY);
                }
            } else if (wasAirborne != null && wasAirborne) {
                Double start = FALL_START.remove(player);
                double fall = start == null ? 0 : start - player.posY;
                if (fall >= CONFIG.landingImpactMinFall / 10.0) {
                    spawn(player, (float) Math.min(3.0, 0.6 + fall / 6.0));
                }
            }
            AIRBORNE.put(player, airborne);
        }
    }

    private static boolean isTarget(EntityPlayer player) {
        boolean self = player == mc.thePlayer;
        switch (CONFIG.landingImpactTarget) {
            case SELF:
                return self;
            case OTHERS:
                return !self;
            case BOTH:
            default:
                return true;
        }
    }

    public static void spawn(EntityPlayer player, float force) {
        double x = player.posX;
        double y = player.posY + 0.03;
        double z = player.posZ;
        float scale = (CONFIG.landingImpactScale / 100f) * force;
        int count = CONFIG.landingImpactCount;
        int life = Math.max(6, CONFIG.landingImpactDuration);

        switch (CONFIG.landingImpactStyle) {
            case RING:
                ring(x, y, z, 0.2f * scale, 2.2f * scale, life);
                break;

            case DUST:
                for (int i = 0; i < count * 2; i++) {
                    double angle = MathUtility.random(0f, (float) (Math.PI * 2));
                    double speed = MathUtility.random(0.06f, 0.2f) * scale;
                    FxParticles.spawn(new FxParticles.Particle(x, y, z, life, 0.3f * scale,
                            colour(i / (float) (count * 2)))
                            .motion(Math.cos(angle) * speed, MathUtility.random(0.02f, 0.08f),
                                    Math.sin(angle) * speed)
                            .gravity(0.006)
                            .texture(TexturesManager.themed(TexturesManager.FIREFLY))
                            .grow(0.25f * scale));
                }
                break;

            case SHOCKWAVE:
                ring(x, y, z, 0.2f * scale, 2.8f * scale, life);
                for (int i = 0; i < count; i++) {
                    double angle = i * (Math.PI * 2 / count);
                    FxParticles.spawn(new FxParticles.Particle(x, y, z, life, 0.22f * scale,
                            colour(i / (float) count))
                            .motion(Math.cos(angle) * 0.22 * scale, 0.04, Math.sin(angle) * 0.22 * scale)
                            .texture(TexturesManager.themed(TexturesManager.FIREFLY)));
                }
                break;

            case CRACK:
                for (int i = 0; i < count; i++) {
                    double angle = i * (Math.PI * 2 / count) + MathUtility.random(-0.1f, 0.1f);
                    double distance = MathUtility.random(0.5f, 1.4f) * scale;
                    FxParticles.spawn(new FxParticles.Particle(
                            x + Math.cos(angle) * distance, y, z + Math.sin(angle) * distance,
                            life, 0.45f * scale, colour(i / (float) count))
                            .texture(TexturesManager.GLYPH_DIAMOND_SUIT)
                            .flat());
                }
                break;

            case SPLASH:
                for (int i = 0; i < count * 2; i++) {
                    double angle = MathUtility.random(0f, (float) (Math.PI * 2));
                    double speed = MathUtility.random(0.08f, 0.22f) * scale;
                    FxParticles.spawn(new FxParticles.Particle(x, y, z, life, 0.2f * scale,
                            colour(i / (float) (count * 2)))
                            .motion(Math.cos(angle) * speed, MathUtility.random(0.14f, 0.3f),
                                    Math.sin(angle) * speed)
                            .gravity(0.03)
                            .texture(TexturesManager.themed(TexturesManager.GUI_BUBBLE)));
                }
                break;

            case BURST:
                for (int i = 0; i < count * 2; i++) {
                    double theta = MathUtility.random(0f, (float) (Math.PI * 2));
                    double phi = MathUtility.random(0f, (float) (Math.PI / 2));
                    double speed = 0.2 * scale;
                    FxParticles.spawn(new FxParticles.Particle(x, y, z, life, 0.22f * scale,
                            colour(i / (float) (count * 2)))
                            .motion(Math.sin(phi) * Math.cos(theta) * speed, Math.cos(phi) * speed,
                                    Math.sin(phi) * Math.sin(theta) * speed)
                            .texture(TexturesManager.themed(TexturesManager.FIREFLY)));
                }
                break;

            case RUNES:
                for (int i = 0; i < Math.max(3, count / 2); i++) {
                    double angle = i * (Math.PI * 2 / Math.max(3, count / 2));
                    double distance = 0.9 * scale;
                    FxParticles.spawn(new FxParticles.Particle(
                            x + Math.cos(angle) * distance, y, z + Math.sin(angle) * distance,
                            (int) (life * 1.5), 0.5f * scale, colour(i / (float) Math.max(3, count / 2)))
                            .texture(TexturesManager.getRandomGlyphParticle())
                            .flat()
                            .fadeIn());
                }
                ring(x, y, z, 0.9f * scale, 0.4f * scale, (int) (life * 1.5));
                break;

            default:
                break;
        }
    }

    private static void ring(double x, double y, double z, float radius, float expand, int life) {
        FxParticles.spawn(new FxParticles.Particle(x, y, z, life, radius, colour(0f))
                .expand(expand));
    }

    private static Color colour(float position) {
        if (CONFIG.landingImpactRandomColor) return Palette.getRandomColor();
        return Palette.getColor(position);
    }
}
