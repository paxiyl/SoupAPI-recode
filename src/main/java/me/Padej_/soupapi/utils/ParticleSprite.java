package me.Padej_.soupapi.utils;

import net.minecraft.util.ResourceLocation;

/**
 * One sprite for every particle in the mod, overriding each effect's own choice.
 *
 * <p>This is what a theme writes when it wants the particles to look like it — a skull for
 * Halloween, a snowflake for Frost — and it used to be an override buried inside the theme, which
 * meant it could not be chosen on its own and could not be changed once a theme was picked. It is
 * an ordinary setting now: a theme fills it in, and so can you.
 *
 * <p>{@link #NONE} leaves every effect its own sprite.
 */
public enum ParticleSprite {

    NONE(null),
    FIREFLY("firefly"),
    FIREFLY_SOFT("firefly_alt"),
    DOLLAR("dollar"),
    SNOWFLAKE("snowflake"),
    HEART("heart"),
    STAR("star"),
    /** Whatever {@code particleSymbol} is set to, which may itself be Random. */
    SYMBOL(null);

    private final String file;

    ParticleSprite(String file) {
        this.file = file;
    }

    /** The sprite to draw, or {@code null} when the caller should keep its own. */
    public ResourceLocation texture() {
        if (this == SYMBOL) {
            return TexturesManager.getRandomGlyphParticle();
        }
        if (file == null) return null;
        return new ResourceLocation("soupapi", "textures/particle/" + file + ".png");
    }
}
