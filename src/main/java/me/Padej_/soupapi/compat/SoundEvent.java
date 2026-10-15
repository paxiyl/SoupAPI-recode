package me.Padej_.soupapi.compat;

import net.minecraft.util.ResourceLocation;

/**
 * Replacement for 1.21.4's {@code SoundEvent}.
 *
 * <p>1.8.9 has no sound registry: a sound is addressed directly by the {@link ResourceLocation}
 * that {@code sounds.json} maps to an .ogg file. This wraps that identifier so the ported code
 * keeps talking about sound events, and {@link Sounds#play} turns one into a playing sound.
 */
public final class SoundEvent {

    private final ResourceLocation id;

    private SoundEvent(ResourceLocation id) {
        this.id = id;
    }

    /** Mirrors 1.21.4's {@code SoundEvent.of(Identifier)}. */
    public static SoundEvent of(ResourceLocation id) {
        return new SoundEvent(id);
    }

    public ResourceLocation getId() {
        return id;
    }

    @Override
    public String toString() {
        return "SoundEvent[" + id + "]";
    }
}
