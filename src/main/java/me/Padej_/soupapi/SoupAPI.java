package me.Padej_.soupapi;

import me.Padej_.soupapi.compat.Caps;
import me.Padej_.soupapi.compat.SoupLog;
import me.Padej_.soupapi.config.AutoConfig;
import me.Padej_.soupapi.config.ConfigHolder;
import me.Padej_.soupapi.config.ConfigManager;
import me.Padej_.soupapi.config.GsonConfigSerializer;
import me.Padej_.soupapi.config.SoupAPI_Config;
import me.Padej_.soupapi.event.ForgeEventBridge;
import me.Padej_.soupapi.gui.SoupSettingsScreen;
import me.Padej_.soupapi.modules.HitParticle;
import me.Padej_.soupapi.modules.HitBubbles;
import me.Padej_.soupapi.modules.HitEffects;
import me.Padej_.soupapi.modules.Translator;
import me.Padej_.soupapi.utils.HitSound;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.common.Mod.EventHandler;

/**
 * Forge entry point.
 *
 * <p>Replaces the two Fabric entrypoints ({@code SoupAPI_Main} and {@code SoupAPI_Client}). The
 * mod is client-only, declared through {@code @Mod(clientSideOnly = true)}, so it does not need to
 * be installed on servers and will not refuse to join one that lacks it.
 *
 * <p>The config is registered in pre-init because {@code ConfigurableModule.CONFIG} is resolved
 * when that class is first loaded and every module extends it — so nothing may touch a module
 * before this runs.
 */
@Mod(modid = SoupAPI.MODID, name = SoupAPI.NAME, version = SoupAPI.VERSION,
        clientSideOnly = true, acceptedMinecraftVersions = "[1.8.9]")
public class SoupAPI {

    public static final String MODID = "soupapi";
    public static final String NAME = "SoupAPI";
    public static final String VERSION = "1.3.7-a-1.8.9";

    public static long initTime;
    /** Kept for parity with the previous entrypoint; some modules read it. */
    public static String ac = "";

    public static ConfigHolder<SoupAPI_Config> configHolder;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        initTime = System.currentTimeMillis();
        configHolder = AutoConfig.register(SoupAPI_Config.class, GsonConfigSerializer::new);
        SoupLog.info("SoupAPI config loaded");
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        ForgeEventBridge.register();
        me.Padej_.soupapi.screen.SoupSettingsShortcut.register();

        HitBubbles.registerOnHit();

        HitEffects.registerOnHit();
        HitSound.registerOnHit();
        HitParticle.registerOnHit();

        Translator.loadCache();
        ConfigManager.loadConfig();

        // The settings screen is the mod's config UI; AutoConfig.getConfigScreen() routes to it.
        AutoConfig.setScreenFactory(parent -> new SoupSettingsScreen());

        SoupLog.info("SoupAPI ready for Minecraft 1.8.9 Forge (" + Caps.describe() + ")");
    }
}
