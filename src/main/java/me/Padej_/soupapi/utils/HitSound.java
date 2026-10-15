package me.Padej_.soupapi.utils;

import me.Padej_.soupapi.config.ConfigurableModule;
import me.Padej_.soupapi.sounds.CustomSounds;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.LivingEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.ActionResult;

import java.util.Random;

public class HitSound extends ConfigurableModule {
    private static long lastHitTime = 0;

    public static void registerOnHit() {
        AttackEntityCallback.EVENT.register((playerEntity, world, hand, entity, hitResult) -> {
            if ((entity instanceof LivingEntity && !EntityUtils.isFriend(entity) && !entity.isSpectator()) && CONFIG.hitSoundEnabled) {
                long now = System.currentTimeMillis();
                if (now - lastHitTime < 150) return ActionResult.PASS;
                lastHitTime = now;

                if (!CONFIG.hitSoundOnlyCrit) {
                    playSound();
                } else {
                    if (EntityUtils.isCrit()) {
                        playSound();
                    }
                }
            }
            return ActionResult.PASS;
        });
    }

    public static void playSound() {
        Random random = new Random();
        double pitch = 0.9 + (random.nextInt(51) / 100.0);

        mc.player.playSound(getSound(),
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
