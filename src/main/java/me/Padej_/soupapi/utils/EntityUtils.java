package me.Padej_.soupapi.utils;

import me.Padej_.soupapi.main.SoupAPI_Main;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.BlockHitResult;

import static me.Padej_.soupapi.config.ConfigurableModule.mc;

public class EntityUtils {
    private static Entity targetEntity;
    private static LivingEntity lastHitEntity;

    public static void updateEntities(MinecraftClient client) {
        if (client.player == null) return;

        BlockHitResult hitResult = client.result;
        if (hitResult != null && hitResult.type == BlockHitResult.Type.ENTITY && hitResult.entity != null) {
            Entity checkedEntity = hitResult.entity;
            if (!isFriend(checkedEntity) && !isSpectator(checkedEntity)) {
                targetEntity = checkedEntity;
            } else {
                targetEntity = null;
            }
        } else {
            targetEntity = null;
        }
    }

    public static boolean isCrit() {
        PlayerEntity player = mc.player;
        if (player == null) return false;
        // 1.9 introduced the attack cooldown; in 1.8.9 every swing is at full strength, so the
        // cooldown term of the 1.21.4 check has no counterpart and is dropped.
        return player.fallDistance > 0.0F &&
                !player.onGround &&
                !player.isClimbing() &&
                !player.isTouchingWater() &&
                !player.hasStatusEffect(StatusEffect.BLINDNESS) &&
                !player.hasVehicle() &&
                !player.isSprinting();
    }

    /**
     * 1.8.9 declares {@code isSpectator()} on {@link PlayerEntity} rather than {@link Entity}, so
     * non-players are simply never spectators.
     */
    public static boolean isSpectator(Entity entity) {
        return entity instanceof PlayerEntity && ((PlayerEntity) entity).isSpectator();
    }

    public static boolean isFriend(Entity entity) {
        String entityName = entity.getName() == null ? null : entity.getName().asUnformattedString();
        if (entityName == null) return false;
        for (String friend : SoupAPI_Main.configHolder.get().friends) {
            if (friend.equalsIgnoreCase(entityName)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isFriend(String name) {
        for (String friend : SoupAPI_Main.configHolder.get().friends) {
            if (friend.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    public static Entity getTargetEntity() {
        return targetEntity;
    }

    public static LivingEntity getLastHitEntity() {
        return lastHitEntity;
    }

    public static void clearLastHitEntity() {
        lastHitEntity = null;
    }
}


