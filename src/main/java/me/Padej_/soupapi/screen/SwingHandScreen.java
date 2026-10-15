package me.Padej_.soupapi.screen;

import me.Padej_.soupapi.compat.DrawContext;
import me.Padej_.soupapi.compat.SoupScreen;
import me.Padej_.soupapi.compat.Texts;
import me.Padej_.soupapi.config.ConfigurableModule;
import me.Padej_.soupapi.modules.SwingHand;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

import static me.Padej_.soupapi.config.ConfigurableModule.CONFIG;

/**
 * Live editor for the first-person hand transform.
 *
 * <p>Rewritten with self-contained sliders. The 1.21.4 version used
 * {@code SliderWidget}'s {@code applyValue}/{@code updateMessage} hooks and
 * {@code GuiButton.builder(...).dimensions(...).build()}, none of which exist in 1.8.9 — its
 * {@code SliderWidget} is hard-wired to a {@code GameOptions} entry and its {@code GuiButton}
 * is an id-plus-message object dispatched through {@code buttonClicked}. Drawing and hit-testing
 * the sliders directly is both shorter than adapting those and behaves the same under
 * PojavLauncher's touch input, which synthesises ordinary click and drag events.
 */
public class SwingHandScreen extends SoupScreen {

    private static final int SLIDER_WIDTH = 100;
    private static final int SLIDER_HEIGHT = 20;
    private static final int PADDING = 5;

    private final List<Slider> sliders = new ArrayList<Slider>();
    private Slider dragging;

    private int buttonX, buttonY, buttonWidth, buttonHeight;

    public SwingHandScreen() {
        super(Texts.literal("Swing Hand GuiScreen"));
    }

    @Override
    public void init() {
        sliders.clear();

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int rowSpacing = SLIDER_HEIGHT + PADDING;
        int totalWidth = SLIDER_WIDTH * 3 + PADDING * 2;
        int startX = centerX - totalWidth / 2;
        int y = centerY - 90;

        // Position
        addSlider(startX, y, SLIDER_WIDTH, "X Pos", -2.0F, 2.0F, SwingHand.xPos, new Setter() {
            public void accept(float v) { SwingHand.xPos = v; }
        });
        addSlider(startX + SLIDER_WIDTH + PADDING, y, SLIDER_WIDTH, "Y Pos", -2.0F, 2.0F, SwingHand.yPos, new Setter() {
            public void accept(float v) { SwingHand.yPos = v; }
        });
        addSlider(startX + (SLIDER_WIDTH + PADDING) * 2, y, SLIDER_WIDTH, "Z Pos", -2.0F, 2.0F, SwingHand.zPos, new Setter() {
            public void accept(float v) { SwingHand.zPos = v; }
        });
        y += rowSpacing;

        // Rotation
        addSlider(startX, y, SLIDER_WIDTH, "X Rot", -180.0F, 180.0F, SwingHand.rotX, new Setter() {
            public void accept(float v) { SwingHand.rotX = (int) v; }
        });
        addSlider(startX + SLIDER_WIDTH + PADDING, y, SLIDER_WIDTH, "Y Rot", -180.0F, 180.0F, SwingHand.rotY, new Setter() {
            public void accept(float v) { SwingHand.rotY = (int) v; }
        });
        addSlider(startX + (SLIDER_WIDTH + PADDING) * 2, y, SLIDER_WIDTH, "Z Rot", -180.0F, 180.0F, SwingHand.rotZ, new Setter() {
            public void accept(float v) { SwingHand.rotZ = (int) v; }
        });
        y += rowSpacing;

        // Swing rotation
        addSlider(startX, y, SLIDER_WIDTH, "X Swing Rot", -90.0F, 90.0F, SwingHand.xSwingRot, new Setter() {
            public void accept(float v) { SwingHand.xSwingRot = (int) v; }
        });
        addSlider(startX + SLIDER_WIDTH + PADDING, y, SLIDER_WIDTH, "Y Swing Rot", -90.0F, 90.0F, SwingHand.ySwingRot, new Setter() {
            public void accept(float v) { SwingHand.ySwingRot = (int) v; }
        });
        addSlider(startX + (SLIDER_WIDTH + PADDING) * 2, y, SLIDER_WIDTH, "Z Swing Rot", -90.0F, 90.0F, SwingHand.zSwingRot, new Setter() {
            public void accept(float v) { SwingHand.zSwingRot = (int) v; }
        });
        y += rowSpacing;

        // Scale and speed, each one and a half slots wide.
        int wideWidth = (int) (SLIDER_WIDTH * 1.5f);
        addSlider((int) (centerX - SLIDER_WIDTH * 1.5f) - PADDING, y, wideWidth, "Scale", 0.0F, 2.0F,
                SwingHand.scale, new Setter() {
                    public void accept(float v) { SwingHand.scale = v; }
                });
        addSlider(centerX + PADDING, y, wideWidth, "Speed", 0.0F, 100.0F, SwingHand.speed, new Setter() {
            public void accept(float v) { SwingHand.speed = (int) v; }
        });
        y += rowSpacing + 10;

        buttonX = centerX - 50;
        buttonY = y;
        buttonWidth = 100;
        buttonHeight = 20;
    }

    private void addSlider(int x, int y, int width, String label, float min, float max,
                           float current, Setter setter) {
        sliders.add(new Slider(x, y, width, SLIDER_HEIGHT, label, min, max, current, setter));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        for (int i = 0; i < sliders.size(); i++) {
            sliders.get(i).render(context, mouseX, mouseY);
        }

        boolean buttonHovered = mouseX >= buttonX && mouseX <= buttonX + buttonWidth
                && mouseY >= buttonY && mouseY <= buttonY + buttonHeight;
        context.fill(buttonX, buttonY, buttonX + buttonWidth, buttonY + buttonHeight,
                buttonHovered ? 0xFF5A5A5A : 0xFF3C3C3C);
        context.drawBorder(buttonX, buttonY, buttonWidth, buttonHeight, 0xFF000000);
        context.drawCenteredTextWithShadow(this.textRenderer, "Swing Hand",
                buttonX + buttonWidth / 2, buttonY + (buttonHeight - 8) / 2, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;

        for (int i = 0; i < sliders.size(); i++) {
            Slider slider = sliders.get(i);
            if (slider.isOver(mouseX, mouseY)) {
                dragging = slider;
                slider.setFromMouse(mouseX);
                return true;
            }
        }

        if (mouseX >= buttonX && mouseX <= buttonX + buttonWidth
                && mouseY >= buttonY && mouseY <= buttonY + buttonHeight) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.thePlayer != null) {
                // 1.8.9 has only one hand, so swingHand() takes no argument.
                mc.thePlayer.swingItem();
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging != null) {
            dragging.setFromMouse(mouseX);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = null;
        return false;
    }

    @Override
    public void close() {
        super.close();
        CONFIG.swingHand_xPos = SwingHand.xPos;
        CONFIG.swingHand_yPos = SwingHand.yPos;
        CONFIG.swingHand_zPos = SwingHand.zPos;
        CONFIG.swingHand_scale = SwingHand.scale;
        CONFIG.swingHand_rotX = SwingHand.rotX;
        CONFIG.swingHand_rotY = SwingHand.rotY;
        CONFIG.swingHand_rotZ = SwingHand.rotZ;
        CONFIG.swingHand_xSwingRot = SwingHand.xSwingRot;
        CONFIG.swingHand_ySwingRot = SwingHand.ySwingRot;
        CONFIG.swingHand_zSwingRot = SwingHand.zSwingRot;
        CONFIG.swingHand_speed = SwingHand.speed;
        ConfigurableModule.saveConfig();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    /** Receives a slider's value as it changes. */
    private interface Setter {
        void accept(float value);
    }

    /** A horizontal slider that draws and hit-tests itself. */
    private final class Slider {
        private final int x, y, width, height;
        private final String label;
        private final float min, max;
        private final Setter setter;
        /** Normalised position in [0, 1]. */
        private float value;

        Slider(int x, int y, int width, int height, String label,
               float min, float max, float current, Setter setter) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.label = label;
            this.min = min;
            this.max = max;
            this.setter = setter;
            this.value = max == min ? 0f : clamp01((current - min) / (max - min));
        }

        boolean isOver(double mouseX, double mouseY) {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        }

        void setFromMouse(double mouseX) {
            value = clamp01((float) ((mouseX - x) / (double) width));
            setter.accept(min + value * (max - min));
        }

        void render(DrawContext context, int mouseX, int mouseY) {
            context.fill(x, y, x + width, y + height, 0xFF2B2B2B);
            int handleX = x + (int) (value * (width - 8));
            context.fill(handleX, y, handleX + 8, y + height,
                    isOver(mouseX, mouseY) ? 0xFFAAAAAA : 0xFF8B8B8B);
            context.drawBorder(x, y, width, height, 0xFF000000);

            String text = label + ": " + String.format("%.1f", min + value * (max - min));
            context.drawCenteredTextWithShadow(SwingHandScreen.this.textRenderer, text,
                    x + width / 2, y + (height - 8) / 2, 0xFFFFFFFF);
        }

        private float clamp01(float v) {
            return v < 0f ? 0f : (v > 1f ? 1f : v);
        }
    }
}
