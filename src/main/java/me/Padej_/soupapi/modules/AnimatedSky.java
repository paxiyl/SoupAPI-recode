package me.Padej_.soupapi.modules;

import me.Padej_.soupapi.config.ConfigurableModule;
import me.Padej_.soupapi.render.SkyRenderer;
import me.Padej_.soupapi.utils.SkyStyle;
import me.Padej_.soupapi.utils.Themes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.world.WorldProvider;
import net.minecraftforge.client.IRenderHandler;

/**
 * A sky that actually replaces the sky.
 *
 * <p>This class decides <em>whether</em> there is one and keeps it installed; {@link SkyRenderer}
 * draws it. The sky is put in place through Forge's own hook — {@code WorldProvider.setSkyRenderer},
 * which vanilla's {@code renderSky} defers to — so it is drawn at the sky's point in the frame, before
 * the world, in world directions. Switching it off hands back whatever renderer the provider had
 * before (for vanilla, none), which is exactly vanilla's sky again.
 *
 * <p>A new world brings a new provider with no renderer set, so this re-checks every tick rather
 * than installing once: world changes, dimension changes and the settings screen all take effect on
 * the next tick without anything else needing to know.
 */
public class AnimatedSky extends ConfigurableModule {

    /** Draws nothing, for when the style asks for the clouds to go. */
    private static final IRenderHandler NO_CLOUDS = new IRenderHandler() {
        @Override
        public void render(float partialTicks, WorldClient world, Minecraft mc) {
        }
    };

    private static WorldProvider installedOn;
    private static IRenderHandler previousSky;
    private static IRenderHandler previousClouds;
    private static boolean cloudsReplaced;

    /** The style to draw: the player's explicit pick, or the theme's. */
    public static SkyStyle style() {
        if (!CONFIG.skyEnabled) return SkyStyle.PLAIN;
        if (CONFIG.skyStyle != null && CONFIG.skyStyle != SkyStyle.PLAIN) return CONFIG.skyStyle;
        if (!CONFIG.coloredSkyEnabled) return SkyStyle.PLAIN;
        SkyStyle themed = Themes.skyStyle();
        return themed == null ? SkyStyle.PLAIN : themed;
    }

    /** True when a moving sky is being drawn. */
    public static boolean active() {
        return style() != SkyStyle.PLAIN;
    }

    /** True when the mod should own the sky at all: a moving sky, or the flat tint. */
    private static boolean wanted() {
        return CONFIG.skyEnabled && (active() || CONFIG.coloredSkyEnabled);
    }

    public static void onTick() {
        WorldClient world = mc.theWorld;
        if (world == null || world.provider == null) {
            forget();
            return;
        }
        WorldProvider provider = world.provider;
        if (provider != installedOn) {
            // A different world: what we replaced belonged to the old provider.
            forget();
        }

        boolean surface = provider.isSurfaceWorld();
        if (wanted() && surface) {
            if (provider.getSkyRenderer() != SkyRenderer.INSTANCE) {
                previousSky = provider.getSkyRenderer();
                provider.setSkyRenderer(SkyRenderer.INSTANCE);
            }
            // Shattered Glass is a pane the sky is seen through; clouds drifting in front of it
            // read as holes in it, so that style hides them whatever the option says.
            boolean hideClouds = active() && (CONFIG.skyHideClouds || style() == SkyStyle.SHATTERED_GLASS);
            if (hideClouds && provider.getCloudRenderer() != NO_CLOUDS) {
                previousClouds = provider.getCloudRenderer();
                provider.setCloudRenderer(NO_CLOUDS);
                cloudsReplaced = true;
            } else if (!hideClouds) {
                restoreClouds(provider);
            }
            installedOn = provider;
        } else {
            restore(provider);
        }
    }

    private static void restore(WorldProvider provider) {
        if (provider.getSkyRenderer() == SkyRenderer.INSTANCE) {
            provider.setSkyRenderer(previousSky);
        }
        restoreClouds(provider);
        previousSky = null;
        installedOn = null;
    }

    private static void restoreClouds(WorldProvider provider) {
        if (cloudsReplaced && provider.getCloudRenderer() == NO_CLOUDS) {
            provider.setCloudRenderer(previousClouds);
        }
        cloudsReplaced = false;
        previousClouds = null;
    }

    private static void forget() {
        installedOn = null;
        previousSky = null;
        previousClouds = null;
        cloudsReplaced = false;
    }
}
