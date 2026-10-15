package me.Padej_.soupapi.gui.settings;

import me.Padej_.soupapi.compat.DrawContext;
import me.Padej_.soupapi.compat.Texts;
import me.Padej_.soupapi.settings.impl.StringSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.TextFieldWidget;

/**
 * A text entry for a {@link StringSetting}.
 *
 * <p>1.8.9's {@link TextFieldWidget} differs from the 1.21.4 one in several ways this works around:
 *
 * <ul>
 *   <li>It takes a widget id and no label, and its width and height are fixed at construction with
 *       no setters — so the field is built once at the component's size and only ever moved by
 *       writing its public {@code x}/{@code y} fields.</li>
 *   <li>There is no {@code setChangedListener}, so the text is polled each frame and pushed to the
 *       setting when it actually changes. That also avoids writing the config on every keystroke.</li>
 *   <li>{@code render()} takes no arguments and draws at the widget's own position.</li>
 *   <li>Input arrives as a single {@code keyPressed(char, int)} rather than separate
 *       {@code keyPressed}/{@code charTyped} calls, and there are no drag or release callbacks.</li>
 * </ul>
 */
public class StringComponent extends SettingComponent<String> {

    private final TextFieldWidget textField;
    private final TextRenderer textRenderer;
    private String lastText;

    public StringComponent(StringSetting setting, int x, int y, int width, int height) {
        super(setting, x, y, width, height);
        MinecraftClient mc = MinecraftClient.getInstance();
        this.textRenderer = mc.textRenderer;

        this.textField = new TextFieldWidget(0, textRenderer, x, y, width, height);
        this.textField.setMaxLength(1024);
        this.textField.setText(setting.getValue() == null ? "" : setting.getValue());
        this.textField.setEditable(true);
        this.lastText = this.textField.getText();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        int labelHeight = textRenderer.fontHeight;
        context.drawText(textRenderer, Texts.translatable(setting.getTranslationKey()),
                x, y, 0xFFFFFFFF, false);

        // Two pixels of padding between the label and the field.
        textField.x = x;
        textField.y = y + labelHeight + 2;
        textField.render();

        // Stands in for the changed-listener 1.8.9's widget does not have.
        String current = textField.getText();
        if (!current.equals(lastText)) {
            lastText = current;
            setting.setValue(current);
        }
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        boolean inside = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        if (button == 1 && inside) {
            textField.setText(setting.getDefaultValue());
            setting.setValue(setting.getDefaultValue());
            lastText = textField.getText();
            setFocused(false);
        } else if (inside) {
            setFocused(true);
            textField.mouseClicked((int) mouseX, (int) mouseY, button);
        } else {
            setFocused(false);
        }
    }

    @Override
    public void mouseReleased(double mouseX, double mouseY, int button) {
        // 1.8.9's text field has no release handling.
    }

    @Override
    public void mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        // 1.8.9's text field has no drag handling (no drag-to-select).
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // 1.8.9 routes control keys through the same entry point as characters; characters are
        // handled in charTyped so they are not delivered twice.
        return textField.isFocused() && textField.keyPressed('\0', keyCode);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        return textField.isFocused() && textField.keyPressed(chr, 0);
    }

    public void setFocused(boolean focused) {
        textField.setFocused(focused);
    }
}
