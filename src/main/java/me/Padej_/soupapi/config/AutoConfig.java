package me.Padej_.soupapi.config;

import net.minecraft.client.gui.screen.Screen;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Registry of config holders, replacing Cloth Config's {@code AutoConfig}.
 *
 * <p>Cloth also generated a settings screen by reflecting over the config class. That part is not
 * reproduced here: the mod already ships its own settings UI
 * ({@code me.Padej_.soupapi.gui.SoupSettingsScreen}), which is what
 * {@link #getConfigScreen(Class, Screen)} returns. Keeping the method means the ported call sites
 * and the removed ModMenu entrypoint do not have to be restructured.
 */
public final class AutoConfig {

    private AutoConfig() {
    }

    private static final Map<Class<?>, ConfigHolder<?>> HOLDERS = new HashMap<Class<?>, ConfigHolder<?>>();

    /** Factory for the screen opened by {@link #getConfigScreen}; set by the client initialiser. */
    private static Function<Screen, Screen> screenFactory;

    public static <T extends ConfigData> ConfigHolder<T> register(
            Class<T> type, Function<Class<T>, ConfigSerializer<T>> serializerFactory) {
        ConfigHolder<T> holder = new ConfigHolder<T>(type, serializerFactory.apply(type));
        HOLDERS.put(type, holder);
        return holder;
    }

    @SuppressWarnings("unchecked")
    public static <T extends ConfigData> ConfigHolder<T> getConfigHolder(Class<T> type) {
        ConfigHolder<?> holder = HOLDERS.get(type);
        if (holder == null) {
            throw new IllegalStateException("Config " + type.getName() + " was never registered");
        }
        return (ConfigHolder<T>) holder;
    }

    public static void setScreenFactory(Function<Screen, Screen> factory) {
        screenFactory = factory;
    }

    /**
     * Returns a supplier of the mod's settings screen. Mirrors Cloth's signature (which returned a
     * {@code Supplier<Screen>}) so call sites keep their trailing {@code .get()}.
     */
    public static <T extends ConfigData> java.util.function.Supplier<Screen> getConfigScreen(
            Class<T> type, final Screen parent) {
        return new java.util.function.Supplier<Screen>() {
            @Override
            public Screen get() {
                return screenFactory != null ? screenFactory.apply(parent) : parent;
            }
        };
    }
}
