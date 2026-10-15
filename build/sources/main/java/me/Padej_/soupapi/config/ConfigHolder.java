package me.Padej_.soupapi.config;

import me.Padej_.soupapi.compat.SoupLog;

/**
 * Holds the live config instance, replacing Cloth Config's {@code ConfigHolder}.
 *
 * <p>Keeps both {@link #get()} and {@link #getConfig()} because the 1.21.4 sources used each in
 * different places.
 */
public final class ConfigHolder<T extends ConfigData> {

    private final Class<T> type;
    private final ConfigSerializer<T> serializer;
    private T config;

    ConfigHolder(Class<T> type, ConfigSerializer<T> serializer) {
        this.type = type;
        this.serializer = serializer;
        load();
    }

    public Class<T> getType() {
        return type;
    }

    public T get() {
        return config;
    }

    /** Alias of {@link #get()}; both spellings appear in the sources. */
    public T getConfig() {
        return config;
    }

    public void load() {
        try {
            config = serializer.deserialize();
        } catch (Exception e) {
            SoupLog.error("Failed to load config, using defaults", e);
            config = serializer.createDefault();
        }
        config.validatePostLoad();
    }

    public boolean save() {
        try {
            config.validatePostLoad();
            serializer.serialize(config);
            return true;
        } catch (Exception e) {
            SoupLog.error("Failed to save config", e);
            return false;
        }
    }

    /** Discards the in-memory config and reloads defaults, then persists them. */
    public void resetToDefault() {
        config = serializer.createDefault();
        save();
    }
}
