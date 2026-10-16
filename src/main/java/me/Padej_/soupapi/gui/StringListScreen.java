package me.Padej_.soupapi.gui;

import me.Padej_.soupapi.compat.DrawContext;
import me.Padej_.soupapi.compat.MatrixStack;
import me.Padej_.soupapi.compat.RenderSystem;
import me.Padej_.soupapi.compat.SoupScreen;
import me.Padej_.soupapi.compat.Texts;
import me.Padej_.soupapi.font.FontRenderers;
import me.Padej_.soupapi.gui.option.ConfigSchema;
import me.Padej_.soupapi.gui.option.Option;
import me.Padej_.soupapi.render.Render2D;
import me.Padej_.soupapi.utils.Palette;
import net.minecraft.client.Minecraft;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Edits a list of strings — in practice the friends list.
 *
 * <p>A {@code String[]} has no sensible inline control, and the old screen had no control for it at
 * all, so the list could only be edited by hand in the config file.
 */
public class StringListScreen extends SoupScreen {

    private static final int ROW_H = 14;

    private final SoupScreen parent;
    private final Option.TextList option;
    private final List<String> entries;

    private String input = "";
    private double scroll;

    public StringListScreen(SoupScreen parent, Option.TextList option) {
        super(Texts.literal(option.label()));
        this.parent = parent;
        this.option = option;
        this.entries = new ArrayList<String>(Arrays.asList(option.get()));
    }

    private int left() {
        return Math.max(8, this.width / 2 - 100);
    }

    private int right() {
        return Math.min(this.width - 8, this.width / 2 + 100);
    }

    private int top() {
        return 8;
    }

    private int bottom() {
        return this.height - 8;
    }

    private int listTop() {
        return top() + 32;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        FontRenderers.ensureLoaded();
        MatrixStack ms = context.getMatrices();
        Color back = Palette.getBackColor();
        Color accent = Palette.getColor(0f);
        int text = Palette.getTextColor();

        context.fill(0, 0, this.width, this.height, 0xB4000000);
        Render2D.drawRound(ms, left(), top(), right() - left(), bottom() - top(), 4,
                new Color(back.getRed(), back.getGreen(), back.getBlue(), 235));

        FontRenderers.sf_bold.drawString(ms, option.label(), left() + 8, top() + 6, accent.getRGB());

        int fx = left() + 6;
        int fw = right() - left() - 12;
        Render2D.drawRound(ms, fx, top() + 18, fw, 11, 3, new Color(255, 255, 255, 26));
        String shown = input.isEmpty() ? "Type a name, press Enter to add" : input;
        FontRenderers.sf_bold_mini.drawString(ms, shown, fx + 4, top() + 21,
                input.isEmpty() ? 0xFF8A90A0 : text);

        int view = bottom() - listTop() - 4;
        int total = entries.size() * ROW_H;
        scroll = Math.max(0, Math.min(Math.max(0, total - view), scroll));

        RenderSystem.enableScissor(left() + 4, listTop(), right() - 4, bottom() - 4);
        int y = (int) (listTop() - scroll);
        for (String entry : entries) {
            if (y + ROW_H >= listTop() && y <= bottom()) {
                boolean hover = mouseX >= left() + 6 && mouseX <= right() - 6
                        && mouseY >= Math.max(listTop(), y) && mouseY <= Math.min(bottom(), y + ROW_H - 1);
                if (hover) {
                    Render2D.drawRound(ms, left() + 6, y, right() - left() - 12, ROW_H - 2, 3,
                            new Color(255, 255, 255, 18));
                }
                FontRenderers.sf_bold_mini.drawString(ms, entry, left() + 12, y + 3, text);
                FontRenderers.sf_bold_mini.drawString(ms, "remove",
                        right() - 12 - FontRenderers.sf_bold_mini.getStringWidth("remove"), y + 3,
                        hover ? 0xFFFF6B6B : 0xFF8A90A0);
            }
            y += ROW_H;
        }
        RenderSystem.disableScissor();

        if (entries.isEmpty()) {
            FontRenderers.sf_bold_mini.drawString(ms, "No entries yet.", left() + 12, listTop() + 4,
                    0xFF8A90A0);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseY >= listTop() && mouseY <= bottom()
                && mouseX >= left() + 6 && mouseX <= right() - 6) {
            int index = (int) ((mouseY - listTop() + scroll) / ROW_H);
            if (index >= 0 && index < entries.size()) {
                entries.remove(index);
                commit();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        scroll -= vertical * ROW_H * 2;
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 14) {
            if (!input.isEmpty()) {
                input = input.substring(0, input.length() - 1);
            }
            return true;
        }
        if (keyCode == 28 || keyCode == 156) {
            String name = input.trim();
            if (!name.isEmpty() && !entries.contains(name)) {
                entries.add(name);
                commit();
            }
            input = "";
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (chr >= 32 && chr != 127) {
            input += chr;
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    private void commit() {
        option.set(entries.toArray(new String[0]));
        ConfigSchema.save();
    }

    @Override
    public void close() {
        commit();
        Minecraft.getMinecraft().displayGuiScreen(parent);
    }
}
