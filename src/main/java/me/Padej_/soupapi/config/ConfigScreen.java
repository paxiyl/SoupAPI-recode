package me.Padej_.soupapi.config;

/**
 * Replacement for Cloth Config's {@code me.shedaniel.clothconfig2.api.ConfigScreen}.
 *
 * <p>Only {@link #saveAll(boolean)} was ever called on it, by the screen-closing hook, so that is
 * all this declares. The mod's own settings screen implements it.
 */
public interface ConfigScreen {
    void saveAll(boolean openOtherScreens);
}
