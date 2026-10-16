package me.Padej_.soupapi.modules;

import me.Padej_.soupapi.compat.BlendFactor;
import me.Padej_.soupapi.compat.BufferBuilder;
import me.Padej_.soupapi.compat.RenderSystem;
import me.Padej_.soupapi.config.ConfigurableModule;
import me.Padej_.soupapi.event.WorldRenderContext;
import me.Padej_.soupapi.render.Fx;
import me.Padej_.soupapi.render.Render3D;
import me.Padej_.soupapi.utils.EntityUtils;
import me.Padej_.soupapi.utils.Palette;
import me.Padej_.soupapi.utils.TexturesManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

import java.awt.Color;

/**
 * A halo above the player — and, now, five other things it can be instead.
 *
 * <p>The only style this had was the pair of orbiting "souls", which is busy and reads badly at a
 * distance. The rest are drawn from {@link Fx}: plain rings, a stack of them, a spinning disc of
 * runes, a tilted Saturn ring and a crown of spikes.
 */
public class Halo extends ConfigurableModule {

    public enum Style {
        /** The original: two trails of sprites orbiting the body. */
        SOULS,
        /** One classic ring above the head. */
        RING,
        /** Two rings, counter-rotating. */
        DOUBLE_RING,
        /** A flat disc of runes spinning overhead. */
        RUNE_DISC,
        /** A ring tilted off horizontal, turning slowly. */
        SATURN,
        /** Spikes standing up from a ring, like a crown. */
        SPIKES,
        /** A dark disc with a burning rim. */
        ECLIPSE,
        /** Two spiral arms of sprites turning overhead. */
        GALAXY,
        /** A stack of rings rising around the body. */
        AURA,
        /** A ring of shards hanging in the air above the head. */
        SHARDS,
        /** Three rings at different angles, turning against each other. */
        GYROSCOPE
    }

    public static void render(WorldRenderContext context) {
        float tickDelta = context.tickCounter().getTickDelta(true);

        if (mc.theWorld == null || mc.thePlayer == null || !CONFIG.haloEnabled) return;

        boolean isFirstPerson = mc.gameSettings.thirdPersonView == 0;

        for (Entity entity : mc.theWorld.loadedEntityList) {
            boolean self = entity == mc.thePlayer;
            if (!self && !EntityUtils.isFriend(entity)) continue;
            if (self && isFirstPerson) continue;
            if (entity.isInvisible()) continue;

            if (me.Padej_.soupapi.utils.Themes.haloStyle() == Style.SOULS) {
                Render3D.renderSoulPair(tickDelta, entity);
            } else {
                renderHalo(entity, tickDelta);
            }
        }
    }

    private static void renderHalo(Entity entity, float tickDelta) {
        double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * tickDelta;
        double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * tickDelta;
        double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * tickDelta;

        float scale = Math.max(0.2f, CONFIG.haloSoulRadius / 100f) * 1.6f;
        float alpha = Math.max(0.05f, CONFIG.haloSoulStartSize / 100f);
        double spin = (mc.theWorld.getTotalWorldTime() + tickDelta) * 0.05;
        double headY = y + entity.height + 0.35;
        int segments = 40;

        switch (me.Padej_.soupapi.utils.Themes.haloStyle()) {
            case RING:
                // A band, not a hairline: wide, bright, and with a dimmer echo under it.
                Fx.glowRing(x, headY, z, 0.45 * scale, 0.12 * scale, segments, 0.0, 0.0,
                        Fx.withAlpha(Palette.getColor(0f), alpha));
                Fx.glowRing(x, headY - 0.05, z, 0.52 * scale, 0.06 * scale, segments, 0.0, 0.0,
                        Fx.withAlpha(Palette.getColor(0.5f), alpha * 0.45f));
                break;

            case DOUBLE_RING:
                Fx.glowRing(x, headY, z, 0.5 * scale, 0.1 * scale, segments, 0.25, spin,
                        Fx.withAlpha(Palette.getColor(0f), alpha));
                Fx.glowRing(x, headY, z, 0.5 * scale, 0.1 * scale, segments, -0.25, -spin,
                        Fx.withAlpha(Palette.getColor(0.6f), alpha));
                break;

            case RUNE_DISC: {
                ResourceLocation glyph = TexturesManager.getRandomGlyphParticle();
                BufferBuilder buffer = Fx.beginSprites(glyph);
                int runes = 8;
                for (int i = 0; i < runes; i++) {
                    double angle = spin * 2 + i * (Math.PI * 2 / runes);
                    Fx.appendSprite(buffer,
                            x + Math.cos(angle) * 0.46 * scale, headY,
                            z + Math.sin(angle) * 0.46 * scale,
                            0.34f * scale,
                            Fx.withAlpha(Palette.getColor(i / (float) runes), alpha), true);
                }
                Fx.endSprites(buffer);
                Fx.glowRing(x, headY - 0.01, z, 0.62 * scale, 0.05 * scale, segments, 0.0, 0.0,
                        Fx.withAlpha(Palette.getColor(0.2f), alpha * 0.7f));
                break;
            }

            case SATURN:
                Fx.glowRing(x, y + entity.height * 0.55, z, 1.0 * scale, 0.16 * scale, segments * 2,
                        0.38, spin, Fx.withAlpha(Palette.getColor(0f), alpha));
                Fx.glowRing(x, y + entity.height * 0.55, z, 1.22 * scale, 0.07 * scale, segments * 2,
                        0.38, spin, Fx.withAlpha(Palette.getColor(0.5f), alpha * 0.5f));
                break;

            case SPIKES: {
                int spikes = 12;
                double radius = 0.42 * scale;
                for (int i = 0; i < spikes; i++) {
                    double angle = spin + i * (Math.PI * 2 / spikes);
                    double sx = x + Math.cos(angle) * radius;
                    double sz = z + Math.sin(angle) * radius;
                    double lift = 0.22 + 0.1 * Math.sin(spin * 4 + i);
                    Fx.glowLine(sx, headY, sz, sx, headY + lift * scale, sz, 0.07 * scale,
                            Fx.withAlpha(Palette.getColor(i / (float) spikes), alpha));
                }
                Fx.glowRing(x, headY, z, radius, 0.07 * scale, segments,
                        0.0, 0.0, Fx.withAlpha(Palette.getColor(0f), alpha * 0.8f));
                break;
            }

            case ECLIPSE:
                // A bright rim with nothing inside it, like a corona.
                Fx.glowRing(x, headY + 0.1, z, 0.6 * scale, 0.05 * scale, segments * 2, 1.2, spin,
                        Fx.withAlpha(Palette.getColor(0f), alpha));
                Fx.glowRing(x, headY + 0.1, z, 0.66 * scale, 0.14 * scale, segments * 2, 1.2, spin,
                        Fx.withAlpha(Palette.getColor(0.4f), alpha * 0.35f));
                break;

            case GALAXY: {
                BufferBuilder buffer = Fx.beginSprites(
                        TexturesManager.themed(TexturesManager.FIREFLY));
                int arms = 2;
                int perArm = 14;
                for (int arm = 0; arm < arms; arm++) {
                    for (int i = 0; i < perArm; i++) {
                        double t = i / (double) perArm;
                        double angle = spin * 2 + t * Math.PI * 1.8 + arm * Math.PI;
                        double radius = (0.15 + t * 0.75) * scale;
                        Fx.appendSprite(buffer,
                                x + Math.cos(angle) * radius,
                                headY + Math.sin(t * Math.PI) * 0.06,
                                z + Math.sin(angle) * radius,
                                (0.26f - (float) t * 0.12f) * scale,
                                Fx.withAlpha(Palette.getColor((float) t), alpha * (1f - (float) t * 0.5f)),
                                true);
                    }
                }
                Fx.endSprites(buffer);
                break;
            }

            case AURA: {
                int rings = 5;
                for (int i = 0; i < rings; i++) {
                    double phase = ((spin * 0.8) + i / (double) rings) % 1.0;
                    double ringY = y + phase * (entity.height + 0.6);
                    double radius = (0.75 - phase * 0.35) * scale;
                    Fx.glowRing(x, ringY, z, radius, 0.09 * scale, segments, 0.0, 0.0,
                            Fx.withAlpha(Palette.getColor((float) phase),
                                    alpha * (1f - (float) phase) * 0.9f));
                }
                break;
            }

            case SHARDS: {
                BufferBuilder buffer = Fx.beginSprites(TexturesManager.GLYPH_TRIANGLE);
                int shards = 6;
                for (int i = 0; i < shards; i++) {
                    double angle = -spin * 1.6 + i * (Math.PI * 2 / shards);
                    double radius = 0.5 * scale;
                    Fx.appendSprite(buffer,
                            x + Math.cos(angle) * radius,
                            headY + 0.12 + Math.sin(spin * 3 + i) * 0.08,
                            z + Math.sin(angle) * radius,
                            0.3f * scale,
                            Fx.withAlpha(Palette.getColor(i / (float) shards), alpha), false);
                }
                Fx.endSprites(buffer);
                break;
            }

            case GYROSCOPE:
                Fx.glowRing(x, headY, z, 0.52 * scale, 0.08 * scale, segments, 0.0, spin,
                        Fx.withAlpha(Palette.getColor(0f), alpha));
                Fx.glowRing(x, headY, z, 0.52 * scale, 0.08 * scale, segments, Math.PI / 2, spin * 1.3,
                        Fx.withAlpha(Palette.getColor(0.33f), alpha));
                Fx.glowRing(x, headY, z, 0.52 * scale, 0.08 * scale, segments, Math.PI / 4, -spin * 0.8,
                        Fx.withAlpha(Palette.getColor(0.66f), alpha));
                break;

            default:
                break;
        }
    }

    public enum SoulStyle {
        SMOKE, PLASMA;

        public static void setupBlendFunc() {
            if (CONFIG.haloSoulRenderSoulStyle == null) {
                RenderSystem.defaultBlendFunc();
            } else {
                switch (CONFIG.haloSoulRenderSoulStyle) {
                    case SMOKE:
                        RenderSystem.blendFunc(BlendFactor.SrcFactor.SRC_ALPHA,
                                BlendFactor.DstFactor.ONE_MINUS_SRC_COLOR);
                        break;
                    case PLASMA:
                        RenderSystem.blendFunc(BlendFactor.SrcFactor.SRC_ALPHA,
                                BlendFactor.DstFactor.ONE);
                        break;
                    default:
                        RenderSystem.defaultBlendFunc();
                        break;
                }
            }
        }
    }
}
