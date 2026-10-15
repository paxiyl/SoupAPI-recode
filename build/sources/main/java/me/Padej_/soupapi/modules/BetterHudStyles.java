package me.Padej_.soupapi.modules;

import me.Padej_.soupapi.SoupModule;
import me.Padej_.soupapi.screen.ConfigHudPositionsScreen;
import me.Padej_.soupapi.settings.impl.BooleanSetting;
import me.Padej_.soupapi.settings.impl.ButtonSetting;
import me.Padej_.soupapi.settings.impl.EnumSetting;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

public class BetterHudStyles extends SoupModule {
    @SuppressWarnings("unused")
    public static ButtonSetting openHudCfg = new ButtonSetting("Config HUD", "das", () -> mc.displayGuiScreen(new ConfigHudPositionsScreen()));

    public static BooleanSetting betterHotbar = new BooleanSetting("Better Hotbar", "dasdas", false);
    public static BooleanSetting betterHotbarArmor = new BooleanSetting("Show Armor", "dasdas", false);
    public static BooleanSetting betterHotbarSmoothScroll = new BooleanSetting("Smooth Scroll", "dasdas", false);
    public static final EnumSetting<HotbarStyle> betterHotbarStyle = new EnumSetting<>("Style", "descripdstion", HotbarStyle.SIMPLE, HotbarStyle.class);

    public BetterHudStyles() {
        super("Better HUD", Category.HUD);
    }

    /**
     * Whether the mod draws this HUD element itself, so Forge should suppress vanilla's.
     *
     * <p>On Forge each element raises its own cancellable event, which is what makes the custom
     * bars possible at all: 1.8.9 itself draws health, armour, food and air inside a single
     * private method, so they cannot be replaced individually without bytecode surgery.
     */
    public static boolean shouldCancelVanilla(RenderGameOverlayEvent.ElementType type) {
        if (!betterHotbar.getValue()) return false;
        return type == RenderGameOverlayEvent.ElementType.ARMOR
                || type == RenderGameOverlayEvent.ElementType.HEALTH
                || type == RenderGameOverlayEvent.ElementType.FOOD
                || type == RenderGameOverlayEvent.ElementType.AIR
                || type == RenderGameOverlayEvent.ElementType.EXPERIENCE
                || type == RenderGameOverlayEvent.ElementType.HOTBAR;
    }

    public enum HotbarStyle {
        GLOW,
        SIMPLE
    }
}
