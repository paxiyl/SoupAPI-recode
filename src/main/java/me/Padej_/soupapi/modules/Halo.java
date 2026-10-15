package me.Padej_.soupapi.modules;

import me.Padej_.soupapi.compat.BlendFactor;

import net.minecraft.client.renderer.GlStateManager;
import me.Padej_.soupapi.compat.RenderSystem;
import me.Padej_.soupapi.config.ConfigurableModule;
import me.Padej_.soupapi.render.Render3D;
import me.Padej_.soupapi.utils.EntityUtils;
import me.Padej_.soupapi.event.WorldRenderContext;
import net.minecraft.entity.Entity;

public class Halo extends ConfigurableModule {

    public static void render(WorldRenderContext context) {
        float tickDelta = context.tickCounter().getTickDelta(true);

        if (mc.theWorld == null || mc.thePlayer == null || !CONFIG.haloEnabled) return;

        boolean isFirstPerson = mc.gameSettings.perspective == 0;

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (EntityUtils.isFriend(entity) || entity.equals(mc.thePlayer)) {
                if (entity == mc.thePlayer && isFirstPerson) continue;
                if (!mc.thePlayer.canEntityBeSeen(entity) || entity.isInvisible()) return;

                Render3D.renderSoulPair(tickDelta, entity);
            }
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
                        RenderSystem.blendFunc(BlendFactor.SrcFactor.SRC_ALPHA, BlendFactor.DstFactor.ONE_MINUS_SRC_COLOR);
                        break;
                    case PLASMA:
                        RenderSystem.blendFunc(BlendFactor.SrcFactor.SRC_ALPHA, BlendFactor.DstFactor.ONE);
                        break;
                    default:
                        RenderSystem.defaultBlendFunc();
                        break;
                }
            }
        }
    }
}
