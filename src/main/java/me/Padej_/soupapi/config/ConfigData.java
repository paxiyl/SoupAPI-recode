package me.Padej_.soupapi.config;

/**
 * Marker for config classes, replacing Cloth Config's {@code me.shedaniel.autoconfig.ConfigData}.
 *
 * <p>{@link #validatePostLoad()} is called after a config is deserialised, giving the config a
 * chance to repair values that are out of range or structurally invalid (for example a saved file
 * from an older version).
 */
public interface ConfigData {
    default void validatePostLoad() {
    }
}
