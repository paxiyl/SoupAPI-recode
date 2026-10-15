package me.Padej_.soupapi.modules;

import me.Padej_.soupapi.compat.ItemStacks;

import me.Padej_.soupapi.compat.BlendFactor;

import net.minecraft.client.renderer.GlStateManager;
import me.Padej_.soupapi.compat.RenderSystem;
import me.Padej_.soupapi.config.ConfigurableModule;
import me.Padej_.soupapi.render.Render3D;
import me.Padej_.soupapi.utils.EntityUtils;
import me.Padej_.soupapi.event.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;

public class TargetRender extends ConfigurableModule {

    private static Entity lastTargetEntity = null;
    private static long lastTargetUpdateTime = 0;

    private static boolean updateOrKeepTarget() {
        Minecraft client = Minecraft.getMinecraft();
        if (client.thePlayer == null || !CONFIG.targetRenderEnabled) return false;

        long currentTime = System.currentTimeMillis();
        Entity currentTarget = EntityUtils.getTargetEntity();

        // Проверка: если включён флаг — только игроки
        if (CONFIG.targetRenderOnlyPlayers && !(currentTarget instanceof EntityPlayer)) {
            currentTarget = null;
        }

        boolean visibleNow = false;
        if (currentTarget != null && client.thePlayer.canEntityBeSeen(currentTarget)) {
            if (!currentTarget.isInvisible()) {
                visibleNow = true;
            // 1.21.4 also treated a glowing entity as visible through invisibility. The
            // Glowing effect and entity flag arrived in 1.9, so on 1.8.9 armour is the only
            // remaining tell that an invisible entity is there.
            } else if (currentTarget instanceof EntityLivingBase && hasAnyArmor((EntityLivingBase) currentTarget)) {
                visibleNow = true;
            }
        }

        if (visibleNow) {
            if (currentTarget != lastTargetEntity) {
                lastTargetEntity = currentTarget;
            }
            lastTargetUpdateTime = currentTime;
        }

        if (lastTargetEntity != null) {
            if (currentTime - lastTargetUpdateTime > CONFIG.targetRenderLiveTime * 1000L
                    || lastTargetEntity.isDead) {
                lastTargetEntity = null;
                return false;
            }

            if (!client.thePlayer.canEntityBeSeen(lastTargetEntity)) {
                return false;
            }

            if (CONFIG.targetRenderOnlyPlayers && !(lastTargetEntity instanceof EntityPlayer)) {
                lastTargetEntity = null;
                return false;
            }
        }

        return lastTargetEntity != null;
    }

    /**
     * 1.8.9 has no {@code EquipmentSlot} enum: armour is indexed 0..3 (feet, legs, chest, head)
     * through {@code getArmorSlot}, and an empty slot is a null stack rather than an empty one.
     */
    private static boolean hasAnyArmor(EntityLivingBase entity) {
        for (int slot = 0; slot < 4; slot++) {
            if (!ItemStacks.isEmpty(entity.getCurrentArmor(slot))) {
                return true;
            }
        }
        return false;
    }

    public static void renderTarget(WorldRenderContext context) {
        if (!updateOrKeepTarget()) return;

        float tickDelta = context.tickCounter().getTickDelta(true);

        switch (CONFIG.targetRenderStyle) {
            case SOUL:
                Render3D.renderSoulsEsp(tickDelta, lastTargetEntity);
                break;
            case SPIRAL:
                Render3D.drawSpiralsEsp(context.matrixStack(), lastTargetEntity);
                break;
            case TOPKA:
                Render3D.drawScanEsp(context.matrixStack(), lastTargetEntity);
                break;
            case LEGACY:
                Render3D.drawLegacy(tickDelta, lastTargetEntity);
                break;
        }
    }

    public enum Style {
        LEGACY, SOUL, SPIRAL, TOPKA
    }

    public enum LegacyTexture {
        LEGACY, MARKER, BO, SIMPLE, SCIFI, AMONGUS, SKULL, JEKA, VEGAS
    }

    public enum SoulTexture {
        FIREFLY, ALT
    }

    public enum TargetRenderSoulStyle {
        SMOKE, PLASMA;

        public static void setupBlendFunc() {
            if (CONFIG.targetRenderSoulStyle == null) {
                RenderSystem.defaultBlendFunc();
            } else {
    switch (CONFIG.targetRenderSoulStyle) {
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
