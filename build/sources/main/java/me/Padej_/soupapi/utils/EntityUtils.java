package me.Padej_.soupapi.utils;

import me.Padej_.soupapi.SoupAPI;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.potion.Potion;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;

import static me.Padej_.soupapi.config.ConfigurableModule.mc;

public class EntityUtils {
    private static Entity targetEntity;
    private static EntityLivingBase lastHitEntity;

    public static void updateEntities(Minecraft client) {
        if (client.thePlayer == null) return;

        MovingObjectPosition hitResult = client.objectMouseOver;
        if (hitResult != null && hitResult.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY && hitResult.entityHit != null) {
            Entity checkedEntity = hitResult.entityHit;
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
        EntityPlayer player = mc.thePlayer;
        if (player == null) return false;
        // 1.9 introduced the attack cooldown; in 1.8.9 every swing is at full strength, so the
        // cooldown term of the 1.21.4 check has no counterpart and is dropped.
        return player.fallDistance > 0.0F &&
                !player.onGround &&
                !player.isOnLadder() &&
                !player.isInWater() &&
                !player.isPotionActive(Potion.blindness) &&
                !player.isRiding() &&
                !player.isSprinting();
    }

    /**
     * 1.8.9 declares {@code isSpectator()} on {@link EntityPlayer} rather than {@link Entity}, so
     * non-players are simply never spectators.
     */
    public static boolean isSpectator(Entity entity) {
        return entity instanceof EntityPlayer && ((EntityPlayer) entity).isSpectator();
    }

    public static boolean isFriend(Entity entity) {
        String entityName = entity.getName();
        if (entityName == null) return false;
        for (String friend : SoupAPI.configHolder.get().friends) {
            if (friend.equalsIgnoreCase(entityName)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isFriend(String name) {
        for (String friend : SoupAPI.configHolder.get().friends) {
            if (friend.equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    public static Entity getTargetEntity() {
        return targetEntity;
    }

    public static EntityLivingBase getLastHitEntity() {
        return lastHitEntity;
    }

    public static void clearLastHitEntity() {
        lastHitEntity = null;
    }
}


