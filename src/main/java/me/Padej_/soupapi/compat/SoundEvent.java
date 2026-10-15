package me.Padej_.soupapi.compat;

import net.minecraft.util.Identifier;

/**
 * Replacement for {@code net.minecraft.sound.SoundEvent}.
 *
 * <p>1.8.9 has no sound registry: a sound is addressed directly by the {@link Identifier} that
 * {@code sounds.json} maps to an .ogg file. This wraps that identifier so the ported code keeps
 * talking about sound events, and {@link Sounds#play} turns one into a playing sound.
 */
public final class SoundEvent {

    private final Identifier id;

    private SoundEvent(Identifier id) {
        this.id = id;
    }

    /** Mirrors 1.21.4's {@code SoundEvent.of(Identifier)}. */
    public static SoundEvent of(Identifier id) {
        return new SoundEvent(id);
    }

    public Identifier getId() {
        return id;
    }

    @Override
    public String toString() {
        return "SoundEvent[" + id + "]";
    }
}
