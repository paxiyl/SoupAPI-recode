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
import java.util.List;
import java.util.Locale;

/**
 * Picks one constant from a long enum.
 *
 * <p>Cycling is fine for a handful of styles, but the cape list alone has over a hundred entries —
 * clicking through those one at a time is not a usable control, which is part of why so much of the
 * mod felt unreachable. This is a filtered list instead.
 */
public class EnumPickerScreen extends SoupScreen {

    private static final int ROW_H = 14;

    private final SoupScreen parent;
    private final Option.Choice option;

    private String query = "";
    private double scroll;

    public EnumPickerScreen(SoupScreen parent, Option.Choice option) {
        super(Texts.literal(option.label()));
        this.parent = parent;
        this.option = option;
    }

    private List<Enum<?>> matches() {
        List<Enum<?>> out = new ArrayList<Enum<?>>();
        String needle = query.toLowerCase(Locale.ROOT);
        for (Enum<?> value : option.values()) {
            if (needle.isEmpty()
                    || Option.Choice.prettify(value).toLowerCase(Locale.ROOT).contains(needle)) {
                out.add(value);
            }
        }
        return out;
    }

    private int left() {
        return Math.max(8, this.width / 2 - 110);
    }

    private int right() {
        return Math.min(this.width - 8, this.width / 2 + 110);
    }

    private int top() {
        return 8;
    }

    private int bottom() {
        return this.height - 8;
    }

    private int listTop() {
        return top() + 30;
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

        FontRenderers.sf_bold.drawString(ms, option.label(), left() + 8, top() + 7, accent.getRGB());

        // Filter box.
        int fx = left() + 6;
        int fw = right() - left() - 12;
        Render2D.drawRound(ms, fx, top() + 18, fw, 10, 3, new Color(255, 255, 255, 22));
        String shown = query.isEmpty() ? "Type to filter..." : query;
        FontRenderers.sf_bold_mini.drawString(ms, shown, fx + 4, top() + 20,
                query.isEmpty() ? 0xFF8A90A0 : text);

        List<Enum<?>> values = matches();
        int view = bottom() - listTop() - 4;
        int total = values.size() * ROW_H;
        scroll = Math.max(0, Math.min(Math.max(0, total - view), scroll));

        RenderSystem.enableScissor(left() + 4, listTop(), right() - 4, bottom() - 4);
        int y = (int) (listTop() - scroll);
        for (Enum<?> value : values) {
            if (y + ROW_H >= listTop() && y <= bottom()) {
                boolean selected = value == option.get();
                boolean hover = mouseX >= left() + 6 && mouseX <= right() - 6
                        && mouseY >= Math.max(listTop(), y) && mouseY <= Math.min(bottom(), y + ROW_H - 1);
                if (selected) {
                    Render2D.drawRound(ms, left() + 6, y, right() - left() - 12, ROW_H - 2, 3,
                            new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 70));
                } else if (hover) {
                    Render2D.drawRound(ms, left() + 6, y, right() - left() - 12, ROW_H - 2, 3,
                            new Color(255, 255, 255, 18));
                }
                FontRenderers.sf_bold_mini.drawString(ms, Option.Choice.prettify(value),
                        left() + 12, y + 3, selected ? accent.getRGB() : text);
            }
            y += ROW_H;
        }
        RenderSystem.disableScissor();

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseY >= listTop() && mouseY <= bottom()
                && mouseX >= left() + 6 && mouseX <= right() - 6) {
            int index = (int) ((mouseY - listTop() + scroll) / ROW_H);
            List<Enum<?>> values = matches();
            if (index >= 0 && index < values.size()) {
                option.set(values.get(index));
                ConfigSchema.save();
                back();
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
            if (!query.isEmpty()) {
                query = query.substring(0, query.length() - 1);
                scroll = 0;
            }
            return true;
        }
        if (keyCode == 28 || keyCode == 156) {
            back();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (chr >= 32 && chr != 127) {
            query += chr;
            scroll = 0;
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public void close() {
        back();
    }

    private void back() {
        Minecraft.getMinecraft().displayGuiScreen(parent);
    }
}
