package me.Padej_.soupapi.modules;

import me.Padej_.soupapi.config.ConfigurableModule;
import me.Padej_.soupapi.utils.Weather;

/**
 * Client-side time and weather.
 *
 * <p>Both were read by nothing before: the 1.21.4 version overrode them from mixins on the client
 * world. Here they are re-applied once per tick instead, which has the same visible effect — the
 * values are client-only state that the server overwrites on its own schedule, and re-applying each
 * tick wins that race for rendering purposes.
 *
 * <p>Nothing is sent to the server, so this changes only what this client draws.
 */
public class WorldChanger extends ConfigurableModule {

    public static void onTick() {
        if (mc.theWorld == null) return;

        if (CONFIG.timeChangerEnabled) {
            // The option is in ticks of the 24000-tick day.
            mc.theWorld.setWorldTime(CONFIG.timeChangerTime);
        }

        if (CONFIG.weatherChangerEnabled) {
            boolean rain = CONFIG.weatherType == Weather.RAIN;
            mc.theWorld.setRainStrength(rain ? 1.0f : 0.0f);
            try {
                mc.theWorld.setThunderStrength(0.0f);
            } catch (Throwable ignored) {
                // Not every world implementation exposes it; rain alone is the visible part.
            }
        }
    }
}
