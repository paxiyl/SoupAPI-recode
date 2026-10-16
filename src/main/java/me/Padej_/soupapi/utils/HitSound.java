package me.Padej_.soupapi.utils;

import me.Padej_.soupapi.utils.EntityUtils;

import me.Padej_.soupapi.config.ConfigurableModule;
import me.Padej_.soupapi.sounds.CustomSounds;
import me.Padej_.soupapi.event.AttackEntityCallback;
import net.minecraft.entity.EntityLivingBase;
import me.Padej_.soupapi.compat.SoundEvent;
import me.Padej_.soupapi.compat.Sounds;

import java.util.Random;

public class HitSound extends ConfigurableModule {
    private static long lastHitTime = 0;

    public static void registerOnHit() {
        AttackEntityCallback.EVENT.register((playerEntity, world, hand, entity, hitResult) -> {
            if ((entity instanceof EntityLivingBase && !EntityUtils.isFriend(entity) && !EntityUtils.isSpectator(entity)) && CONFIG.hitSoundEnabled) {
                long now = System.currentTimeMillis();
                if (now - lastHitTime < 150) return AttackEntityCallback.Result.PASS;
                lastHitTime = now;

                lastHitTime = now;
                if (!CONFIG.hitSoundOnlyCrit) {
                    playSound();
                } else {
                    if (EntityUtils.isCrit()) {
                        playSound();
                    }
                }
            }
            return AttackEntityCallback.Result.PASS;
        });
    }

    /**
     * How long ago the mod played its own hit sound, in milliseconds.
     *
     * <p>Used to scope {@code hitSoundOverwriteEnabled}: vanilla's hurt sound is muted only in the
     * window right after one of the player's own hits, so taking damage still sounds normal.
     */
    public static long sinceLastHitMs() {
        return System.currentTimeMillis() - lastHitTime;
    }

    public static void playSound() {
        Random random = new Random();
        double pitch = 0.9 + (random.nextInt(51) / 100.0);

        Sounds.play(getSound(),
                CONFIG.hitSoundVolume / 100f,
                CONFIG.hitSoundRandomPitch ? (float) pitch : 1);
    }

    private static SoundEvent getSound() {
        switch (CONFIG.hitSoundType) {
            case ON:
                return CustomSounds.ON;
            case OFF:
                return CustomSounds.OFF;
            case GET:
                return CustomSounds.GET;
            case BUBBLE:
                return CustomSounds.BUBBLE;
            case BELL:
                return CustomSounds.BELL;
            case BONK:
                return CustomSounds.BONK;
            case POK:
                return CustomSounds.POK;
            case MAGIC_POK:
                return CustomSounds.MAGIC_POK;
        }
        throw new IllegalStateException("Unexpected value: " + CONFIG.hitSoundType);
    }
}
