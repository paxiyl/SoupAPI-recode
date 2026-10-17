package me.Padej_.soupapi.utils;

import me.Padej_.soupapi.SoupAPI;
import me.Padej_.soupapi.config.SoupAPI_Config;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Applies themes, and notices when you have edited one.
 *
 * <p>This class used to answer "what style should I use?" for every module, returning the theme's
 * choice over the player's whenever a theme was active. That made a theme feel like a lock: with
 * Halloween picked, switching the particle or a colour in the settings changed nothing at all,
 * because the theme went on winning. Themes are presets now —
 * {@link Theme#apply(SoupAPI_Config)} copies the look into the settings when one is picked, the
 * modules read the settings as they always wanted to, and {@link #onEdited} turns the theme into
 * {@link Theme#CUSTOM} the moment you change something it had filled in. The look stays; only the
 * label gives way, which is the honest thing for it to say once the settings are yours.
 */
public final class Themes {

    /**
     * The settings a theme fills in.
     *
     * <p>Editing one of these is what makes a theme custom. Everything else — a radius, a speed, a
     * toggle a theme never touches — leaves the theme's name alone, so tuning the halo's size does
     * not claim you have left Halloween.
     */
    private static final Set<String> OWNED = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
            "c1", "c2", "c3", "c4", "textColor", "backColor",
            "jumpCirclesStyle", "hitBubblesStyle", "trailsStyle", "playerEffectsStyle", "haloStyle",
            "particleSpriteOverride", "particleSymbol",
            "ambienceStyle", "coloredSkyColor", "coloredSkyAlpha",
            "skyStyle", "skyColor2", "skyDarkColor")));

    /** The switches that decide how much of a theme gets copied; changing one re-applies it. */
    private static final Set<String> REAPPLY = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
            "themeStyle", "themeVisuals", "themeParticles", "themeWorld", "themeAmbience")));

    private Themes() {
    }

    /**
     * Called after any config field is written from the settings screen.
     *
     * <p>Three cases: picking a theme (or changing what a theme applies) copies it in; editing
     * something the active theme filled in to a different value drops the theme to Custom; anything
     * else is none of a theme's business.
     */
    public static void onEdited(String field, SoupAPI_Config config) {
        if (config == null || field == null) return;

        if (REAPPLY.contains(field)) {
            apply(config);
            return;
        }

        if (!OWNED.contains(field)) return;

        Theme theme = config.themeStyle;
        if (theme == null || theme == Theme.CUSTOM) return;
        if (!theme.agreesWith(field, config)) {
            config.themeStyle = Theme.CUSTOM;
        }
    }

    /** What the active theme puts in the air, or {@link Ambience#NONE}. */
    public static Ambience ambience() {
        Theme theme = Palette.getTheme();
        return theme == null ? Ambience.NONE : theme.ambience();
    }

    /** What the active theme's sky does, or {@link SkyStyle#PLAIN}. */
    public static SkyStyle skyStyle() {
        Theme theme = Palette.getTheme();
        return theme == null ? SkyStyle.PLAIN : theme.skyStyle();
    }

    /** Copies the active theme's look into the settings. Does nothing for {@link Theme#CUSTOM}. */
    public static void apply(SoupAPI_Config config) {
        if (config == null) return;
        Theme theme = config.themeStyle;
        if (theme == null || theme == Theme.CUSTOM) return;
        theme.apply(config);
    }

    /**
     * Copies the saved theme in once, at startup.
     *
     * <p>A config written before themes became presets can name a theme while the settings still
     * hold the defaults the theme used to override. Applying it on load means such a config keeps
     * the look it had instead of reverting to blue-and-orange with the theme's name on it.
     */
    public static void applySaved() {
        try {
            apply(SoupAPI.configHolder.get());
        } catch (Throwable ignored) {
            // A theme is not worth taking the client down for.
        }
    }
}
