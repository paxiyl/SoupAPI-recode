package me.Padej_.soupapi.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.IChatComponent;
import org.lwjgl.input.Mouse;

import java.io.IOException;

/**
 * Adapter from MCP 1.8.9's {@link GuiScreen} contract to the 1.21.4-style one the mod's screens
 * are written against.
 *
 * <p>The two differ substantially. 1.8.9 has:
 * <ul>
 *   <li>{@code drawScreen(int mouseX, int mouseY, float partialTicks)} with no draw context;</li>
 *   <li>{@code void mouseClicked(int, int, int)} — integer coordinates, no return value, so a
 *       screen cannot report whether it consumed the click, and it declares
 *       {@code throws IOException};</li>
 *   <li>{@code void keyTyped(char typedChar, int keyCode)} — one method for both the key and the
 *       character, where 1.21.4 split them into {@code keyPressed} and {@code charTyped};</li>
 *   <li>no scroll callback: the wheel is read from LWJGL inside {@code handleMouseInput()};</li>
 *   <li>no title, and {@code initGui()} rather than {@code init()}.</li>
 * </ul>
 *
 * <p>This class absorbs those differences so each screen keeps the newer method shapes. Return
 * values are accepted and discarded where 1.8.9 has nowhere to put them.
 *
 * <p>Escape handling is done here: 1.8.9 screens close themselves on key code 1 rather than having
 * a {@code shouldCloseOnEsc()} hook, so that hook is reproduced.
 */
public abstract class SoupScreen extends GuiScreen {

    /** LWJGL 2 reports one wheel notch as ±120. */
    private static final double WHEEL_NOTCH = 120.0;

    /** 1.8.9's {@link GuiScreen} has no title, so the port keeps one here. */
    private final IChatComponent title;

    protected SoupScreen(IChatComponent title) {
        this.title = title;
    }

    protected SoupScreen() {
        this(Texts.literal(""));
    }

    public IChatComponent getTitle() {
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
        Minecraft.getMinecraft().displayGuiScreen(null);
    }

    /** 1.21.4 name; 1.8.9 spells it {@code doesGuiPauseGame}. */
    public boolean shouldPause() {
        return false;
    }

    /** 1.21.4 name; 1.8.9 spells it {@code initGui}. */
    public void init() {
    }

    // ---- 1.8.9 contract --------------------------------------------------------------------

    @Override
    public void initGui() {
        super.initGui();
        init();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        render(new DrawContext(Minecraft.getMinecraft()), mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        super.mouseClicked(mouseX, mouseY, button);
        mouseClicked((double) mouseX, (double) mouseY, button);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int button) {
        super.mouseReleased(mouseX, mouseY, button);
        // 1.8.9 passes state -1 for "moved while held"; the newer API only reports real releases.
        if (button >= 0) {
            mouseReleased((double) mouseX, (double) mouseY, button);
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int button, long timeSinceClick) {
        super.mouseClickMove(mouseX, mouseY, button, timeSinceClick);
        // 1.8.9 gives no per-frame delta, and the mod's drag handlers track their own anchor
        // point, so zero is passed rather than a fabricated value.
        mouseDragged((double) mouseX, (double) mouseY, button, 0.0, 0.0);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1 && shouldCloseOnEsc()) {
            close();
            return;
        }
        // Key first, then the character, matching 1.21.4's ordering.
        if (keyPressed(keyCode, keyCode, modifiers())) {
            return;
        }
        if (typedChar != 0) {
            charTyped(typedChar, modifiers());
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel;
        try {
            wheel = Mouse.getEventDWheel();
        } catch (Throwable t) {
            // Pojav's LWJGL 2 stub may not report a wheel; touch devices have none.
            return;
        }
        if (wheel != 0) {
            int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
            int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
            mouseScrolled(mouseX, mouseY, 0.0, wheel / WHEEL_NOTCH);
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return shouldPause();
    }

    /** Packs the current modifier keys the way 1.21.4's {@code modifiers} bitmask did. */
    private static int modifiers() {
        int mods = 0;
        if (isShiftKeyDown()) mods |= 1;
        if (isCtrlKeyDown()) mods |= 2;
        if (isAltKeyDown()) mods |= 4;
        return mods;
    }
}
