package me.Padej_.soupapi.screen;

import me.Padej_.soupapi.gui.SoupSettingsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import org.lwjgl.input.Keyboard;

/**
 * Opens the settings screen.
 *
 * <p>Two ways in:
 *
 * <ul>
 *   <li>A proper Forge {@link KeyBinding}, default <b>Right Shift</b>, which shows up in
 *       Options &rarr; Controls under "SoupAPI" and can be rebound. The 1.21.4 version had only a
 *       hard-coded combination, which is close to unusable on PojavLauncher where the modifier keys
 *       live behind an on-screen keyboard.</li>
 *   <li>The original <b>Right Ctrl + Right Alt</b> combination, kept for parity.</li>
 * </ul>
 *
 * <p>Both are edge-triggered: the 1.21.4 version re-opened the screen on every tick the combination
 * was held. The raw key read is guarded because PojavLauncher's LWJGL 2 stub does not implement
 * every query.
 */
public final class SoupSettingsShortcut {

    private SoupSettingsShortcut() {
    }

    public static final KeyBinding OPEN_SETTINGS = new KeyBinding(
            "key.soupapi.settings", Keyboard.KEY_RSHIFT, "key.categories.soupapi");

    private static boolean held;

    /** Called from the mod's init; registers the binding with Forge's controls screen. */
    public static void register() {
        ClientRegistry.registerKeyBinding(OPEN_SETTINGS);
    }

    public static void tick(Minecraft mc) {
        GuiScreen current = mc.currentScreen;
        if (current instanceof GuiChat) return;

        // isPressed() consumes one queued press, so it is already edge-triggered, and it only
        // fires while no screen is open - which is what we want for opening one.
        boolean pressed = false;
        try {
            pressed = OPEN_SETTINGS.isPressed();
        } catch (Throwable ignored) {
        }

        boolean down;
        try {
            down = Keyboard.isKeyDown(Keyboard.KEY_RCONTROL) && Keyboard.isKeyDown(Keyboard.KEY_RMENU);
        } catch (Throwable t) {
            down = false;
        }

        if ((pressed || (down && !held)) && !(current instanceof SoupSettingsScreen)) {
            mc.displayGuiScreen(new SoupSettingsScreen());
        }
        held = down;
    }
}
