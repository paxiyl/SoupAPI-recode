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
        SPIKES
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

            if (CONFIG.haloStyle == Style.SOULS) {
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

        float scale = CONFIG.haloSoulRadius / 100f * 1.6f;
        float alpha = CONFIG.haloSoulStartSize / 100f;
        double spin = (mc.theWorld.getTotalWorldTime() + tickDelta) * 0.06;
        double headY = y + entity.height + 0.35;
        int segments = 28;

        switch (CONFIG.haloStyle) {
            case RING:
                Fx.ring(x, headY, z, 0.42 * scale, segments, Fx.withAlpha(Palette.getColor(0f), alpha));
                Fx.ring(x, headY + 0.02, z, 0.38 * scale, segments,
                        Fx.withAlpha(Palette.getColor(0.5f), alpha * 0.7f));
                break;
            case DOUBLE_RING:
                Fx.tiltedRing(x, headY, z, 0.45 * scale, segments, 0.0, spin,
                        Fx.withAlpha(Palette.getColor(0f), alpha));
                Fx.tiltedRing(x, headY + 0.12, z, 0.3 * scale, segments, 0.45, -spin * 1.4,
                        Fx.withAlpha(Palette.getColor(0.6f), alpha * 0.8f));
                break;
            case RUNE_DISC: {
                ResourceLocation glyph = TexturesManager.GLYPH_CIRCLE;
                BufferBuilder buffer = Fx.beginSprites(glyph);
                int runes = 7;
                for (int i = 0; i < runes; i++) {
                    double angle = spin * 2 + i * (Math.PI * 2 / runes);
                    Fx.appendSprite(buffer,
                            x + Math.cos(angle) * 0.4 * scale,
                            headY,
                            z + Math.sin(angle) * 0.4 * scale,
                            0.28f * scale,
                            Fx.withAlpha(Palette.getColor(i / (float) runes), alpha), true);
                }
                Fx.endSprites(buffer);
                Fx.ring(x, headY - 0.01, z, 0.55 * scale, segments,
                        Fx.withAlpha(Palette.getColor(0.2f), alpha * 0.6f));
                break;
            }
            case SATURN:
                Fx.tiltedRing(x, y + entity.height * 0.55, z, 0.95 * scale, segments * 2, 0.38, spin,
                        Fx.withAlpha(Palette.getColor(0f), alpha));
                Fx.tiltedRing(x, y + entity.height * 0.55, z, 1.12 * scale, segments * 2, 0.38, spin,
                        Fx.withAlpha(Palette.getColor(0.5f), alpha * 0.55f));
                break;
            case SPIKES: {
                int spikes = 10;
                double radius = 0.4 * scale;
                for (int i = 0; i < spikes; i++) {
                    double angle = spin + i * (Math.PI * 2 / spikes);
                    double sx = x + Math.cos(angle) * radius;
                    double sz = z + Math.sin(angle) * radius;
                    Color colour = Fx.withAlpha(Palette.getColor(i / (float) spikes), alpha);
                    Fx.line(sx, headY, sz, sx, headY + 0.3 * scale, sz, colour);
                }
                Fx.ring(x, headY, z, radius, segments, Fx.withAlpha(Palette.getColor(0f), alpha * 0.8f));
                break;
            }
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
