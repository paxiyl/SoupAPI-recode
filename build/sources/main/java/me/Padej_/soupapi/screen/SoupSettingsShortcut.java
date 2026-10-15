package me.Padej_.soupapi.screen;

import me.Padej_.soupapi.gui.SoupSettingsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

/**
 * Opens the settings screen on right-ctrl + right-alt.
 *
 * <p>Edge-triggered: the 1.21.4 version re-opened the screen on every tick the combination was
 * held. Input is read through LWJGL 2's {@code Keyboard}, which is how 1.8.9 and PojavLauncher's
 * LWJGL 2 stub report it, and the read is guarded because that stub does not implement every
 * query.
 */
public final class SoupSettingsShortcut {

    private SoupSettingsShortcut() {
    }

    private static boolean held;

    public static void tick(Minecraft mc) {
        GuiScreen current = mc.currentScreen;
        if (current instanceof GuiChat) return;

        boolean down;
        try {
            down = Keyboard.isKeyDown(Keyboard.KEY_RCONTROL) && Keyboard.isKeyDown(Keyboard.KEY_RMENU);
        } catch (Throwable t) {
            return;
        }

        if (down && !held && !(current instanceof SoupSettingsScreen)) {
            mc.displayGuiScreen(new SoupSettingsScreen());
        }
        held = down;
    }
}
