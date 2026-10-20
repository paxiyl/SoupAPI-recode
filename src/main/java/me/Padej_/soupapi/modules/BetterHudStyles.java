package me.Padej_.soupapi.modules;

import me.Padej_.soupapi.config.ConfigurableModule;

import me.Padej_.soupapi.screen.ConfigHudPositionsScreen;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

public class BetterHudStyles extends ConfigurableModule {



    /**
     * Whether the mod draws this HUD element itself, so Forge should suppress vanilla's.
     *
     * <p>On Forge each element raises its own cancellable event, which is what makes the custom
     * bars possible at all: 1.8.9 itself draws health, armour, food and air inside a single
     * private method, so they cannot be replaced individually without bytecode surgery.
     */
    public static boolean shouldCancelVanilla(RenderGameOverlayEvent.ElementType type) {
        if (!CONFIG.hudBetterHotbarEnabled) return false;
        return type == RenderGameOverlayEvent.ElementType.ARMOR
                || type == RenderGameOverlayEvent.ElementType.HEALTH
                || type == RenderGameOverlayEvent.ElementType.FOOD
                || type == RenderGameOverlayEvent.ElementType.AIR
                || type == RenderGameOverlayEvent.ElementType.EXPERIENCE
                || type == RenderGameOverlayEvent.ElementType.HOTBAR;
    }

    public enum HotbarStyle {
        GLOW,
        SIMPLE,
        /** A translucent pane with a rim, the Atlas look. */
        GLASS,
        /** Every slot its own pill, the bar floating with gaps between. */
        FLOATING,
        /** A thin RGB-lit outline round a dark bar, and a bright underline under the selection. */
        NEON,
        /** No panel at all: items, and a dot under the selected one. */
        MINIMAL
    }
}
