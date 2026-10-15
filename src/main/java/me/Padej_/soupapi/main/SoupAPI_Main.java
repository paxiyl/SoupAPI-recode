package me.Padej_.soupapi.main;

import me.Padej_.soupapi.compat.Caps;
import me.Padej_.soupapi.compat.SoupLog;
import me.Padej_.soupapi.config.AutoConfig;
import me.Padej_.soupapi.config.ConfigHolder;
import me.Padej_.soupapi.config.GsonConfigSerializer;
import me.Padej_.soupapi.config.SoupAPI_Config;
import net.fabricmc.api.ModInitializer;

/**
 * Main entrypoint.
 *
 * <p>Registers the config before anything else, because {@code ConfigurableModule.CONFIG} is
 * resolved when that class is first loaded and every module extends it.
 *
 * <p>The 1.21.4 version also registered a {@code SimpleParticleType} ("star") with a physics
 * particle factory. 1.8.9 has no particle registry to extend, and that particle was never
 * spawned — the factory was registered but nothing ever called {@code addParticle} with it — so
 * the type and {@code CustomPhysicParticle} are dropped rather than reimplemented. The particle
 * effects the mod actually shows (hit, jump, totem, ambient) are drawn by its own renderers and
 * are unaffected.
 */
public class SoupAPI_Main implements ModInitializer {
    public static ConfigHolder<SoupAPI_Config> configHolder;
    public static long initTime;
    public static String ac = "";

    @Override
    public void onInitialize() {
        initTime = System.currentTimeMillis();
        configHolder = AutoConfig.register(SoupAPI_Config.class, GsonConfigSerializer::new);
        SoupLog.info("SoupAPI initialised for Minecraft 1.8.9 (" + Caps.describe() + ")");
    }
}
