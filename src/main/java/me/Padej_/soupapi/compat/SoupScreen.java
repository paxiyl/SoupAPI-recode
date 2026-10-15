package me.Padej_.soupapi.compat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Mouse;

/**
 * Adapter from 1.8.9's {@link Screen} contract to the 1.21.4-style one the mod's screens are
 * written against.
 *
 * <p>The two are quite different. 1.8.9 has:
 * <ul>
 *   <li>{@code render(int mouseX, int mouseY, float delta)} with no draw context;</li>
 *   <li>{@code void mouseClicked(int, int, int)} — integer coordinates, no return value, so a
 *       screen cannot report whether it consumed the click;</li>
 *   <li>{@code void keyPressed(char typedChar, int keyCode)} — one method for both the key and the
 *       character, where 1.21.4 split them into {@code keyPressed} and {@code charTyped};</li>
 *   <li>no scroll callback at all: the wheel is read from LWJGL inside {@code handleMouse()}.</li>
 * </ul>
 *
 * <p>This class absorbs those differences so each screen keeps the newer method shapes. Return
 * values are accepted and discarded where 1.8.9 has nowhere to put them.
 *
 * <p>Escape handling is done here too: 1.8.9 screens close themselves on key code 1 rather than
 * having a {@code shouldCloseOnEsc()} hook, so that hook is reproduced.
 */
public abstract class SoupScreen extends Screen {

    /** LWJGL 2 reports one wheel notch as ±120. */
    private static final double WHEEL_NOTCH = 120.0;

    /**
     * 1.8.9's {@link Screen} has no title, so the port keeps one here. Screens pass theirs to
     * {@code super(...)} exactly as they did on 1.21.4.
     */
    private final net.minecraft.text.Text title;

    protected SoupScreen(net.minecraft.text.Text title) {
        this.title = title;
    }

    protected SoupScreen() {
        this(Texts.literal(""));
    }

    public net.minecraft.text.Text getTitle() {
        return title;
    }

    // ---- 1.21.4-style methods for subclasses to override -----------------------------------

    /** Draw the screen. Called once per frame with a fresh {@link DrawContext}. */
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    public boolean charTyped(char chr, int modifiers) {
        return false;
    }

    public boolean shouldCloseOnEsc() {
        return true;
    }

    public void close() {
        MinecraftClient.getInstance().setScreen(null);
    }

    /** 1.21.4 name; 1.8.9 spells it {@code shouldPauseGame}. */
    public boolean shouldPause() {
        return false;
    }

    // ---- 1.8.9 contract --------------------------------------------------------------------

    @Override
    public void render(int mouseX, int mouseY, float delta) {
        super.render(mouseX, mouseY, delta);
        render(new DrawContext(MinecraftClient.getInstance()), mouseX, mouseY, delta);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        super.mouseClicked(mouseX, mouseY, button);
        mouseClicked((double) mouseX, (double) mouseY, button);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int button) {
        super.mouseReleased(mouseX, mouseY, button);
        // 1.8.9 passes button -1 for "moved while held"; the newer API only reports real releases.
        if (button >= 0) {
            mouseReleased((double) mouseX, (double) mouseY, button);
        }
    }

    @Override
    protected void mouseDragged(int mouseX, int mouseY, int button, long timeSinceClick) {
        super.mouseDragged(mouseX, mouseY, button, timeSinceClick);
        // 1.8.9 gives no per-frame delta, and the mod's drag handlers track their own anchor
        // point, so zero is passed rather than a fabricated value.
        mouseDragged((double) mouseX, (double) mouseY, button, 0.0, 0.0);
    }

    @Override
    protected void keyPressed(char typedChar, int keyCode) {
        if (keyCode == 1 && shouldCloseOnEsc()) {
            close();
            return;
        }
        // Key first, then the character, matching 1.21.4's ordering.
        if (keyPressed(keyCode, keyCode, modifiers())) {
            return;
        }
        if (typedChar != 0 && typedChar != '\0') {
            charTyped(typedChar, modifiers());
        }
    }

    @Override
    public void handleMouse() {
        super.handleMouse();
        int wheel;
        try {
            wheel = Mouse.getEventDWheel();
        } catch (Throwable t) {
            // Pojav's LWJGL 2 stub may not report a wheel; touch devices have none.
            return;
        }
        if (wheel != 0) {
            int mouseX = Mouse.getEventX() * this.width / this.client.width;
            int mouseY = this.height - Mouse.getEventY() * this.height / this.client.height - 1;
            mouseScrolled(mouseX, mouseY, 0.0, wheel / WHEEL_NOTCH);
        }
    }

    @Override
    public boolean shouldPauseGame() {
        return shouldPause();
    }

    /** Packs the current modifier keys the way 1.21.4's {@code modifiers} bitmask did. */
    private static int modifiers() {
        int mods = 0;
        if (hasShiftDown()) mods |= 1;
        if (hasControlDown()) mods |= 2;
        if (hasAltDown()) mods |= 4;
        return mods;
    }
}
