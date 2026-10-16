package me.Padej_.soupapi.modules;

import me.Padej_.soupapi.config.ConfigurableModule;
import me.Padej_.soupapi.event.AttackEntityCallback;
import me.Padej_.soupapi.render.FxParticles;
import me.Padej_.soupapi.utils.EntityUtils;
import me.Padej_.soupapi.utils.MathUtility;
import me.Padej_.soupapi.utils.Palette;
import me.Padej_.soupapi.utils.Symbol;
import me.Padej_.soupapi.utils.TexturesManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;

import java.awt.Color;

/**
 * What appears on the target when you land a hit.
 *
 * <p>Ten shapes, all built from the same idea: throw a burst of sprites and rings at the point of
 * impact and let them age out. {@code hitEffectsFollow} sticks the burst to the entity's current
 * position instead of where it was hit, which reads better on a target that is being knocked back.
 */
public class HitEffects extends ConfigurableModule {

    public enum Style {
        /** Triangular shards thrown outward. */
        SHARDS,
        /** Dense shards with a flat ring, like breaking glass. */
        GLASS,
        /** Arrow heads driven into the body. */
        ARROWS,
        /** A tight spray of sparks. */
        SPARKS,
        /** Symbols that pop and fade. */
        RUNES,
        /** Two rings at right angles. */
        RINGS,
        /** A cross of four shards. */
        CROSS,
        /** An even sphere of sprites. */
        BURST,
        /** Stars that arc away under gravity. */
        STARS,
        /** A single slash across the body. */
        SLASH
    }

    public static void registerOnHit() {
        AttackEntityCallback.EVENT.register((playerEntity, world, hand, entity, hitResult) -> {
            if (!CONFIG.hitEffectsEnabled) return AttackEntityCallback.Result.PASS;
            if (!(entity instanceof EntityLivingBase)) return AttackEntityCallback.Result.PASS;
            if (EntityUtils.isFriend(entity) || EntityUtils.isSpectator(entity)) {
                return AttackEntityCallback.Result.PASS;
            }
            if (CONFIG.hitEffectsOnlyPlayers && !(entity instanceof net.minecraft.entity.player.EntityPlayer)) {
                return AttackEntityCallback.Result.PASS;
            }
            if (CONFIG.hitEffectsOnlyCrit && !EntityUtils.isCrit()) {
                return AttackEntityCallback.Result.PASS;
            }
            spawn(entity);
            return AttackEntityCallback.Result.PASS;
        });
    }

    public static void spawn(Entity target) {
        double x = target.posX;
        double y = target.posY + target.height * 0.6;
        double z = target.posZ;
        // With Follow Target on, the burst inherits the target's velocity, so it travels with them
        // through the knockback instead of being left behind in mid-air.
        driftX = CONFIG.hitEffectsFollow ? target.motionX * 0.8 : 0.0;
        driftY = CONFIG.hitEffectsFollow ? target.motionY * 0.8 : 0.0;
        driftZ = CONFIG.hitEffectsFollow ? target.motionZ * 0.8 : 0.0;

        int count = CONFIG.hitEffectsCount;
        float size = CONFIG.hitEffectsScale / 100f;
        float speed = CONFIG.hitEffectsSpeed / 100f;
        int life = Math.max(4, CONFIG.hitEffectsDuration);

        switch (CONFIG.hitEffectsStyle) {
            case SHARDS:
                if (CONFIG.hitEffectsStick) {
                    stick(target, count, size * 0.4f, life * 4, TexturesManager.GLYPH_TRIANGLE);
                } else {
                    burst(x, y, z, count, 0.22 * speed, size * 0.35f, life,
                            TexturesManager.GLYPH_TRIANGLE, 0.012, false);
                }
                break;
            case GLASS:
                if (CONFIG.hitEffectsStick) {
                    stick(target, count * 2, size * 0.26f, life * 4, TexturesManager.GLYPH_DIAMOND_SUIT);
                } else {
                    burst(x, y, z, count * 2, 0.26 * speed, size * 0.22f, life,
                            TexturesManager.GLYPH_DIAMOND_SUIT, 0.02, false);
                }
                ring(x, y, z, size * 0.5f, size * 1.6f, life, 0.0);
                break;
            case ARROWS:
                if (CONFIG.hitEffectsStick) {
                    stick(target, count, size * 0.5f, life * 5, TexturesManager.GLYPH_ARROW_HEAVY);
                } else {
                    burst(x, y, z, count, 0.1 * speed, size * 0.45f, (int) (life * 1.6),
                            TexturesManager.GLYPH_ARROW_HEAVY, 0.0, false);
                }
                break;
            case SPARKS:
                burst(x, y, z, count * 2, 0.34 * speed, size * 0.16f, life,
                        TexturesManager.FIREFLY, 0.03, false);
                break;
            case RUNES:
                if (CONFIG.hitEffectsStick) {
                    stick(target, Math.max(3, count / 2), size * 0.55f, life * 4,
                            TexturesManager.getRandomGlyphParticle());
                } else {
                    burst(x, y, z, Math.max(3, count / 2), 0.14 * speed, size * 0.5f, (int) (life * 1.4),
                            TexturesManager.getRandomGlyphParticle(), -0.004, false);
                }
                break;
            case RINGS:
                ring(x, y, z, size * 0.3f, size * 1.4f, life, 0.0);
                ring(x, y, z, size * 0.3f, size * 1.4f, life, Math.PI / 2);
                break;
            case CROSS:
                for (int i = 0; i < 4; i++) {
                    double angle = i * Math.PI / 2 + Math.PI / 4;
                    FxParticles.spawn(new FxParticles.Particle(x, y, z, life, size * 0.4f, colour(i / 4f))
                            .motion(Math.cos(angle) * 0.2 * speed, Math.sin(angle) * 0.2 * speed, 0)
                            .texture(TexturesManager.GLYPH_TRIANGLE));
                }
                break;
            case BURST:
                sphere(x, y, z, count * 2, 0.2 * speed, size * 0.22f, life,
                        TexturesManager.themed(TexturesManager.FIREFLY));
                break;
            case STARS:
                burst(x, y, z, count, 0.26 * speed, size * 0.35f, (int) (life * 1.5),
                        TexturesManager.STAR, 0.035, false);
                break;
            case SLASH:
                for (int i = 0; i < Math.max(6, count); i++) {
                    double t = i / (double) Math.max(6, count) - 0.5;
                    FxParticles.spawn(new FxParticles.Particle(
                            x + t * 1.2 * size, y + t * 0.9 * size, z, life, size * 0.3f, colour((float) (t + 0.5)))
                            .motion(0, 0, 0)
                            .texture(TexturesManager.GLYPH_DIAMOND_SUIT));
                }
                break;
            default:
                break;
        }
    }

    private static double driftX;
    private static double driftY;
    private static double driftZ;

    /**
     * Drives sprites into the target and leaves them there.
     *
     * <p>Spawn points sit on the surface of the body's cylinder - a random angle and a height over
     * the torso and legs - and each one is attached to the entity, so the shards ride the target
     * through knockback and turns instead of being left where the hit landed.
     */
    private static void stick(Entity target, int count, float size, int life,
                              ResourceLocation texture) {
        double radius = Math.max(0.2, target.width * 0.52);
        for (int i = 0; i < count; i++) {
            double angle = MathUtility.random(0f, (float) (Math.PI * 2));
            double height = MathUtility.random(0.2f, 0.95f) * target.height;
            double jitter = MathUtility.random(0.9f, 1.15f);
            FxParticles.spawn(new FxParticles.Particle(
                    target.posX, target.posY + height, target.posZ,
                    life, size * (float) jitter, colour(i / (float) Math.max(1, count)))
                    .texture(texture)
                    .fadeIn()
                    .attach(target, Math.cos(angle) * radius, height, Math.sin(angle) * radius));
        }
    }

    private static void burst(double x, double y, double z, int count, double speed, float size,
                              int life, ResourceLocation texture, double gravity, boolean flat) {
        for (int i = 0; i < count; i++) {
            double angle = MathUtility.random(0f, (float) (Math.PI * 2));
            double lift = MathUtility.random(-0.5f, 1.0f);
            FxParticles.Particle p = new FxParticles.Particle(x, y, z, life, size,
                    colour(i / (float) Math.max(1, count)))
                    .motion(Math.cos(angle) * speed + driftX, lift * speed + driftY,
                            Math.sin(angle) * speed + driftZ)
                    .gravity(gravity)
                    .texture(texture);
            if (flat) p.flat();
            FxParticles.spawn(p);
        }
    }

    private static void sphere(double x, double y, double z, int count, double speed, float size,
                               int life, ResourceLocation texture) {
        for (int i = 0; i < count; i++) {
            double theta = MathUtility.random(0f, (float) (Math.PI * 2));
            double phi = Math.acos(MathUtility.random(-1f, 1f));
            FxParticles.spawn(new FxParticles.Particle(x, y, z, life, size,
                    colour(i / (float) Math.max(1, count)))
                    .motion(Math.sin(phi) * Math.cos(theta) * speed + driftX,
                            Math.cos(phi) * speed + driftY,
                            Math.sin(phi) * Math.sin(theta) * speed + driftZ)
                    .texture(texture));
        }
    }

    private static void ring(double x, double y, double z, float radius, float expand, int life,
                             double tilt) {
        FxParticles.spawn(new FxParticles.Particle(x, y, z, life, radius, colour(0f))
                .expand(expand)
                .tilt(tilt));
    }

    private static Color colour(float position) {
        if (CONFIG.hitEffectsRandomColor) return Palette.getRandomColor();
        return Palette.getColor(position);
    }

    /** The symbol picker also drives the rune style, so it is worth exposing here. */
    public static Symbol symbol() {
        return CONFIG.particleSymbol;
    }
}
