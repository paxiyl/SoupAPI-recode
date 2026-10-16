package me.Padej_.soupapi.utils;

import me.Padej_.soupapi.modules.Halo;
import me.Padej_.soupapi.modules.HitBubbles;
import me.Padej_.soupapi.modules.JumpCircles;
import me.Padej_.soupapi.modules.PlayerEffects;
import me.Padej_.soupapi.modules.Trails;

import static me.Padej_.soupapi.config.ConfigurableModule.CONFIG;

/**
 * Where the rest of the mod asks "what style should I use?".
 *
 * <p>Each accessor returns the theme's choice when a theme is active and theme visuals are on, and
 * the player's own setting otherwise. Keeping the decision here means no module has to know that
 * themes exist.
 */
public final class Themes {

    private Themes() {
    }

    /** True when a theme should decide the shapes, not just the colours. */
    public static boolean visuals() {
        return Palette.getTheme() != Theme.CUSTOM && CONFIG.themeVisuals;
    }

    /** True when a theme should also colour the sky and the fog. */
    public static boolean world() {
        return Palette.getTheme() != Theme.CUSTOM && CONFIG.themeWorld;
    }

    public static JumpCircles.JumCircleStyle circleStyle() {
        return visuals() ? Palette.getTheme().circleStyle() : CONFIG.jumpCirclesStyle;
    }

    public static HitBubbles.Style bubbleStyle() {
        return visuals() ? Palette.getTheme().bubbleStyle() : CONFIG.hitBubblesStyle;
    }

    public static Trails.Style trailStyle() {
        return visuals() ? Palette.getTheme().trailStyle() : CONFIG.trailsStyle;
    }

    public static PlayerEffects.Style effectStyle() {
        return visuals() ? Palette.getTheme().effectStyle() : CONFIG.playerEffectsStyle;
    }

    public static Halo.Style haloStyle() {
        return visuals() ? Palette.getTheme().haloStyle() : CONFIG.haloStyle;
    }
}
