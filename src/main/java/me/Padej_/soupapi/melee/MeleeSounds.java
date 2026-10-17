package me.Padej_.soupapi.melee;

import me.Padej_.soupapi.config.ConfigurableModule;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.util.ResourceLocation;

/**
 * Plays the sounds an animation asks for.
 *
 * <p>A clip's cues name the pack's own files — {@code delta_wt:melee/mercy/draw} — and the bundler
 * puts each of those under {@code assets/soupapi/sounds/} with an entry in the mod's
 * {@code sounds.json}, so {@code delta_wt:melee/mercy/draw} resolves to the sound event
 * {@code soupapi:melee.mercy.draw}. Nothing is registered at runtime: 1.8.9 builds its sound
 * registry when resources load, and a sound that is not in it when the player presses the key does
 * not exist.
 */
public final class MeleeSounds extends ConfigurableModule {

    private MeleeSounds() {
    }

    /** Plays one cue from a clip, at the player's position. */
    static void play(String effect) {
        if (!CONFIG.meleeSounds || effect == null || effect.isEmpty()) return;
        try {
            ResourceLocation event = toEvent(effect);
            if (event == null) return;
            float volume = CONFIG.meleeSoundVolume / 100f;
            if (volume <= 0f) return;
            if (mc.thePlayer == null) return;
            mc.getSoundHandler().playSound(new PositionedSoundRecord(event, volume, 1f,
                    (float) mc.thePlayer.posX, (float) mc.thePlayer.posY, (float) mc.thePlayer.posZ));
        } catch (Throwable ignored) {
            // A missing sound should never stop the animation that asked for it.
        }
    }

    /**
     * Turns a pack reference into one of the mod's sound events.
     *
     * <p>{@code delta_wt:melee/mercy/draw} and a bare {@code melee/mercy/draw} both become
     * {@code soupapi:melee.mercy.draw}; the pack's namespace is dropped because the files no longer
     * live under it.
     */
    static ResourceLocation toEvent(String effect) {
        String path = effect;
        int colon = path.indexOf(':');
        if (colon >= 0) path = path.substring(colon + 1);
        path = path.trim();
        if (path.isEmpty()) return null;
        return new ResourceLocation("soupapi", path.replace('/', '.'));
    }
}
