package me.Padej_.soupapi.modules;

import me.Padej_.soupapi.config.ConfigurableModule;

/**
 * Tints the sky with the palette colour.
 *
 * <p>The tint used to be a far-plane quad painted from the end of the world pass. It is now part of
 * the sky itself: with Colored Sky on and no moving style picked, {@link
 * me.Padej_.soupapi.render.SkyRenderer} draws the vanilla sky colour leaned toward the palette by
 * {@code coloredSkyAlpha}, under the world instead of over it, with the sun and moon on top.
 */
public class ColoredSky extends ConfigurableModule {

    /**
     * Kept as the entry point the event bridge used to call; the tint is now drawn by
     * {@link me.Padej_.soupapi.render.SkyRenderer} as part of the sky itself.
     *
     * <p>Painting it here, from the end of the world pass, is what covered the mod's own effects
     * wherever they sat against the sky, and it is also where the lighting state leaked from.
     */
    public static void render() {
    }
}
