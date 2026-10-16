package me.Padej_.soupapi.modules;

import me.Padej_.soupapi.config.ConfigurableModule;
import me.Padej_.soupapi.render.FxParticles;
import me.Padej_.soupapi.utils.EntityUtils;
import me.Padej_.soupapi.utils.MathUtility;
import me.Padej_.soupapi.utils.Palette;
import me.Padej_.soupapi.utils.TexturesManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;

import java.awt.Color;
import java.util.HashSet;
import java.util.Set;

/**
 * What happens where something dies.
 *
 * <p>1.8.9 sends no "entity died" packet a client can hook; the model simply stops being updated and
 * is removed. So deaths are spotted by watching: every living entity in range is remembered while it
 * is alive, and the effect fires the first tick one of them is found dead or gone.
 */
public class KillEffects extends ConfigurableModule {

    public enum Style {
        /** A column of flame rising from the body. */
        FLAME_PILLAR,
        /** A sphere of sprites thrown outward, then drawn back in. */
        SOUL_BURST,
        /** A fast, wide blast with a ground ring. */
        EXPLOSION,
        /** A flat ring that opens on the ground, like a closing portal. */
        PORTAL,
        /** A spinning funnel that narrows as it rises. */
        VORTEX,
        /** A hard flash with falling sparks. */
        LIGHTNING,
        /** Three rings rising one after another. */
        RINGS,
        /** Slow embers drifting up and out. */
        ASH
    }

    /** Entities seen alive, so their disappearance can be noticed. */
    private static final Set<Integer> ALIVE = new HashSet<Integer>();

    public static void onTick() {
        if (mc.theWorld == null || mc.thePlayer == null) return;
        if (!CONFIG.killEffectsEnabled) {
            if (!ALIVE.isEmpty()) ALIVE.clear();
            return;
        }

        Set<Integer> stillAlive = new HashSet<Integer>();
        for (Object o : mc.theWorld.loadedEntityList) {
            if (!(o instanceof EntityLivingBase)) continue;
            EntityLivingBase living = (EntityLivingBase) o;
            if (living == mc.thePlayer && !CONFIG.killEffectsIncludeSelf) continue;
            if (CONFIG.killEffectsOnlyPlayers && !(living instanceof EntityPlayer)) continue;
            if (EntityUtils.isSpectator(living)) continue;
            if (living.getDistanceToEntity(mc.thePlayer) > 48) continue;

            if (living.getHealth() > 0f && !living.isDead) {
                stillAlive.add(living.getEntityId());
            } else if (ALIVE.contains(living.getEntityId())) {
                spawn(living);
            }
        }

        // Anything that was alive and is no longer in the list died out of sight or despawned; only
        // fire for entities still present, so leaving an area does not set off a dozen effects.
        ALIVE.clear();
        ALIVE.addAll(stillAlive);
    }

    public static void spawn(Entity target) {
        double x = target.posX;
        double y = target.posY;
        double z = target.posZ;
        float scale = CONFIG.killEffectsScale / 100f;
        int count = CONFIG.killEffectsCount;
        int life = Math.max(8, CONFIG.killEffectsDuration);

        switch (CONFIG.killEffectsStyle) {
            case FLAME_PILLAR:
                for (int i = 0; i < count * 3; i++) {
                    double angle = MathUtility.random(0f, (float) (Math.PI * 2));
                    double radius = MathUtility.random(0f, 0.45f) * scale;
                    FxParticles.spawn(new FxParticles.Particle(
                            x + Math.cos(angle) * radius, y + 0.1, z + Math.sin(angle) * radius,
                            life + (int) MathUtility.random(0f, 10f), 0.5f * scale, colour(i / (float) (count * 3)))
                            .motion(0, MathUtility.random(0.08f, 0.2f), 0)
                            .texture(TexturesManager.themed(TexturesManager.GLYPH_FLAME))
                            .grow(0.2f * scale));
                }
                ring(x, y + 0.05, z, 0.3f * scale, 2.2f * scale, life, 0);
                break;

            case SOUL_BURST:
                for (int i = 0; i < count * 4; i++) {
                    double theta = MathUtility.random(0f, (float) (Math.PI * 2));
                    double phi = Math.acos(MathUtility.random(-1f, 1f));
                    double speed = MathUtility.random(0.12f, 0.3f) * scale;
                    FxParticles.spawn(new FxParticles.Particle(x, y + target.height * 0.5, z,
                            life, 0.3f * scale, colour(i / (float) (count * 4)))
                            .motion(Math.sin(phi) * Math.cos(theta) * speed, Math.cos(phi) * speed,
                                    Math.sin(phi) * Math.sin(theta) * speed)
                            .drag(0.9)
                            .texture(TexturesManager.themed(TexturesManager.FIREFLY)));
                }
                break;

            case EXPLOSION:
                for (int i = 0; i < count * 5; i++) {
                    double angle = MathUtility.random(0f, (float) (Math.PI * 2));
                    double speed = MathUtility.random(0.2f, 0.5f) * scale;
                    FxParticles.spawn(new FxParticles.Particle(x, y + 0.6, z, life, 0.35f * scale,
                            colour(i / (float) (count * 5)))
                            .motion(Math.cos(angle) * speed, MathUtility.random(0.05f, 0.35f), Math.sin(angle) * speed)
                            .gravity(0.03)
                            .texture(TexturesManager.themed(TexturesManager.FIREFLY)));
                }
                ring(x, y + 0.05, z, 0.2f * scale, 4.0f * scale, (int) (life * 0.7), 0);
                break;

            case PORTAL:
                ring(x, y + 0.04, z, 2.4f * scale, -2.2f * scale, life * 2, 0);
                for (int i = 0; i < count * 2; i++) {
                    double angle = i * (Math.PI * 2 / (count * 2));
                    FxParticles.spawn(new FxParticles.Particle(
                            x + Math.cos(angle) * 2.0 * scale, y + 0.1, z + Math.sin(angle) * 2.0 * scale,
                            life * 2, 0.3f * scale, colour(i / (float) (count * 2)))
                            .motion(-Math.cos(angle) * 0.06, 0.02, -Math.sin(angle) * 0.06)
                            .texture(TexturesManager.getRandomGlyphParticle()));
                }
                break;

            case VORTEX:
                for (int i = 0; i < count * 4; i++) {
                    double t = i / (double) (count * 4);
                    double angle = t * Math.PI * 6;
                    double radius = (1.2 - t) * scale;
                    FxParticles.spawn(new FxParticles.Particle(
                            x + Math.cos(angle) * radius, y + t * 3.0, z + Math.sin(angle) * radius,
                            life, 0.26f * scale, colour((float) t))
                            .motion(0, 0.06, 0)
                            .texture(TexturesManager.themed(TexturesManager.FIREFLY))
                            .fadeIn());
                }
                break;

            case LIGHTNING:
                for (int i = 0; i < count; i++) {
                    FxParticles.spawn(new FxParticles.Particle(
                            x + MathUtility.random(-0.4f, 0.4f), y + 3.0, z + MathUtility.random(-0.4f, 0.4f),
                            life, 0.7f * scale, colour(i / (float) count))
                            .motion(0, -0.45, 0)
                            .texture(TexturesManager.GLYPH_BOLT));
                }
                ring(x, y + 0.05, z, 0.2f * scale, 3.0f * scale, (int) (life * 0.6), 0);
                break;

            case RINGS:
                for (int i = 0; i < 3; i++) {
                    FxParticles.spawn(new FxParticles.Particle(x, y + 0.05 + i * 0.5, z,
                            life + i * 4, 0.3f * scale, colour(i / 3f))
                            .expand(2.0f * scale)
                            .motion(0, 0.04, 0));
                }
                break;

            case ASH:
                for (int i = 0; i < count * 3; i++) {
                    FxParticles.spawn(new FxParticles.Particle(
                            x + MathUtility.random(-0.6f, 0.6f), y + 0.2,
                            z + MathUtility.random(-0.6f, 0.6f),
                            life * 3, 0.18f * scale, colour(i / (float) (count * 3)))
                            .motion(MathUtility.random(-0.02f, 0.02f), MathUtility.random(0.01f, 0.05f),
                                    MathUtility.random(-0.02f, 0.02f))
                            .texture(TexturesManager.themed(TexturesManager.FIREFLY))
                            .fadeIn());
                }
                break;

            default:
                break;
        }
    }

    private static void ring(double x, double y, double z, float radius, float expand, int life,
                             double tilt) {
        FxParticles.spawn(new FxParticles.Particle(x, y, z, life, radius, colour(0f))
                .expand(expand).tilt(tilt));
    }

    private static Color colour(float position) {
        if (CONFIG.killEffectsRandomColor) return Palette.getRandomColor();
        return Palette.getColor(position);
    }
}
