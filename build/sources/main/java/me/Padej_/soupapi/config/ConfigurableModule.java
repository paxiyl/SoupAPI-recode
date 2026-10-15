package me.Padej_.soupapi.config;

import me.Padej_.soupapi.SoupAPI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

/**
 * Base class giving every module static access to the client and the live config.
 *
 * <p>{@code CONFIG} is resolved once, at class-init, from the holder registered by
 * {@link SoupAPI_Main}. That ordering is why the main initialiser registers the config before
 * anything touches a module.
 */
public abstract class ConfigurableModule {
    public static Minecraft mc = Minecraft.getMinecraft();
    public static final SoupAPI_Config CONFIG = SoupAPI.configHolder.get();

    public static void saveConfig() {
        SoupAPI.configHolder.save();
    }

    public static void saveAll(GuiScreen screen) {
        if (screen instanceof ConfigScreen) {
            ((ConfigScreen) screen).saveAll(false);
        }
    }
}
