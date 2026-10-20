package me.Padej_.soupapi.render;

import me.Padej_.soupapi.compat.SoupLog;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderPlayer;

/**
 * Attaches the mod's player feature layers once the render manager exists.
 *
 * <p>These layers — the cape and the china hat — were mixin-injected on 1.21.4, and the port left
 * them written but never registered, so both features were dead however they were configured.
 * Forge 1.8.9 exposes {@code RenderPlayer.addLayer} and the skin map, so they can simply be added.
 *
 * <p>Registration waits for the first client tick: the render manager's skin map does not exist
 * while the mod is still being constructed.
 */
public final class PlayerLayers {

    private static boolean registered;

    private PlayerLayers() {
    }

    public static void ensureRegistered() {
        if (registered) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.getRenderManager() == null || mc.getRenderManager().getSkinMap() == null
                || mc.getRenderManager().getSkinMap().isEmpty()) {
            return;
        }
        try {
            for (RenderPlayer renderer : mc.getRenderManager().getSkinMap().values()) {
                renderer.addLayer(new CapeFeatureRenderer(renderer));
                renderer.addLayer(new ChinaHatFeatureRenderer(renderer));
            }
            registered = true;
            SoupLog.info("[SoupAPI] player layers registered (cape, china hat)");
        } catch (Throwable t) {
            // Another mod replacing the player renderer could make this fail; the rest of the mod
            // does not depend on it.
            registered = true;
            SoupLog.warn("[SoupAPI] could not attach the player layers", t);
        }
    }
}
