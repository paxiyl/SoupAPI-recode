package me.Padej_.soupapi.config;

/** Reads and writes a config instance. Replaces Cloth Config's serializer interface. */
public interface ConfigSerializer<T extends ConfigData> {

    void serialize(T config) throws Exception;

    /** Returns the stored config, or a fresh default instance if there is nothing valid to load. */
    T deserialize() throws Exception;

    T createDefault();
}
