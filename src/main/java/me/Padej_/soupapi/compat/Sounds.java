package me.Padej_.soupapi.compat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.entity.Entity;

/**
 * Plays the mod's own sounds on 1.8.9.
 *
 * <p>1.21.4's {@code PlayerEntity.playSound(SoundEvent, volume, pitch)} is a client-and-server API
 * backed by the sound registry. 1.8.9 has neither, so sounds go straight to the client sound
 * manager as a positioned instance, which is what vanilla 1.8.9 does for UI and feedback sounds.
 *
 * <p>The sound is placed at the listener's own position so that volume is not attenuated by
 * distance — matching how the 1.21.4 code played hit sounds on the local player.
 */
public final class Sounds {

    private Sounds() {
    }

    public static void play(SoundEvent sound, float volume, float pitch) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getSoundManager() == null) return;

        Entity listener = mc.player != null ? mc.player : mc.getCameraEntity();
        double x = listener != null ? listener.x : 0.0;
        double y = listener != null ? listener.y : 0.0;
        double z = listener != null ? listener.z : 0.0;

        mc.getSoundManager().play(new PositionedSoundInstance(
                sound.getId(), volume, pitch, (float) x, (float) y, (float) z));
    }
}
