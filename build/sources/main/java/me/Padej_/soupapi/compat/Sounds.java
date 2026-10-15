package me.Padej_.soupapi.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;

/**
 * Plays the mod's own sounds on 1.8.9.
 *
 * <p>1.21.4's {@code PlayerEntity.playSound(SoundEvent, volume, pitch)} is backed by the sound
 * registry. 1.8.9 has neither, so sounds go straight to the client sound handler as a positioned
 * record, which is what vanilla 1.8.9 does for UI and feedback sounds.
 *
 * <p>{@code PositionedSoundRecord.create(ResourceLocation, pitch)} is a master-channel,
 * non-attenuated sound — exactly the behaviour the hit sound wants — but it gives no volume
 * control, so the positional constructor is used at the listener's own position instead, which
 * keeps volume configurable without distance attenuation.
 */
public final class Sounds {

    private Sounds() {
    }

    public static void play(SoundEvent sound, float volume, float pitch) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.getSoundHandler() == null) return;

        net.minecraft.entity.Entity listener =
                mc.thePlayer != null ? mc.thePlayer : mc.getRenderViewEntity();
        float x = listener != null ? (float) listener.posX : 0f;
        float y = listener != null ? (float) listener.posY : 0f;
        float z = listener != null ? (float) listener.posZ : 0f;

        mc.getSoundHandler().playSound(
                new PositionedSoundRecord(sound.getId(), volume, pitch, x, y, z));
    }
}
