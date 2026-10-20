package me.Padej_.soupapi.gui;

import me.Padej_.soupapi.compat.DrawContext;
import me.Padej_.soupapi.compat.MatrixStack;
import me.Padej_.soupapi.compat.RenderSystem;
import me.Padej_.soupapi.compat.SoupScreen;
import me.Padej_.soupapi.compat.Texts;
import me.Padej_.soupapi.config.ConfigManager;
import me.Padej_.soupapi.font.FontRenderer;
import me.Padej_.soupapi.font.FontRenderers;
import me.Padej_.soupapi.gui.option.ConfigSchema;
import me.Padej_.soupapi.gui.option.Option;
import me.Padej_.soupapi.gui.option.OptionPage;
import me.Padej_.soupapi.render.Render2D;
import me.Padej_.soupapi.utils.Palette;
import net.minecraft.client.Minecraft;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * The mod's settings screen.
 *
 * <p>Rewritten for this port. The previous screen could only reach a second, parallel copy of five
 * modules' options — about thirty of the mod's two hundred and thirty-six — and drew them as flat
 * grey boxes with no scrolling, so most of them sat off the bottom of the screen with no way to
 * reach them. This one is driven by {@link ConfigSchema}, which reflects over the config itself, so
 * every option the mod has is here and every one of them is the value the mod actually reads.
 *
 * <p>Built for a touch screen as much as a mouse: rows are generously tall, the sidebar and the
 * option list scroll independently (wheel, or drag anywhere in the list), and nothing depends on a
 * modifier key. Right-clicking a row resets it, which is also reachable by a long press on
 * PojavLauncher since that sends a right click.
 */
public class SoupSettingsScreen extends SoupScreen {


    // Layout constants, in scaled GUI pixels.
    private static final int MARGIN = 6;
    private static final int HEADER_H = 20;
    private static final int FOOTER_H = 14;
    private static final int SIDEBAR_W = 92;
    private static final int ROW_H = 18;
    private static final int GROUP_H = 15;
    private static final int SIDE_ROW_H = 15;
    private static final int CONTROL_W = 78;
    /**
     * The scroll rail is wide enough to hit with a thumb.
     *
     * <p>PojavLauncher has no scroll wheel, and dragging the list works but fights with the rows
     * underneath it, so there is a real rail: two arrow buttons and a thumb you can drag or tap
     * past. Three pixels of decoration was not something anyone could use on a phone.
     */
    private static final int SCROLLBAR_W = 10;

    /** The square arrow buttons at each end of the rail. */
    private static final int ARROW_H = 12;

    /** True while the rail's thumb is being dragged. */
    private boolean draggingBar;
    private double barGrabOffset;

    private List<OptionPage> pages = new ArrayList<OptionPage>();
    private int selectedPage;

    private double contentScroll;
    private double sidebarScroll;

    private String searchQuery = "";
    private boolean searchFocused;

    /** The option being typed into, with its in-progress text. */
    private Option editing;
    private String editBuffer = "";

    /** The slider or value being dragged, plus where the drag last was. */
    private Option.Num draggingNum;
    private boolean draggingSlider;
    private double lastDragX;
    private boolean draggingList;
    private double lastDragY;

    private Option hovered;

    /** The key row waiting for a key press, or {@code null}. */
    private Option.Key listening;

    public SoupSettingsScreen() {
        super(Texts.translatable("soupapi.screen.soup_settings.title"));
    }


    @Override
    public void init() {
        ConfigManager.loadConfig();
        pages = ConfigSchema.build();

        String saved = ConfigManager.getMetadata("selectedPage");
        if (saved != null) {
            for (int i = 0; i < pages.size(); i++) {
                if (pages.get(i).id().equals(saved)) {
                    selectedPage = i;
                    break;
                }
            }
        }
        clampSelection();
    }

    private void clampSelection() {
        if (pages.isEmpty()) {
            selectedPage = 0;
        } else if (selectedPage >= pages.size()) {
            selectedPage = pages.size() - 1;
        } else if (selectedPage < 0) {
            selectedPage = 0;
        }
    }

    // ---- geometry --------------------------------------------------------------------------

    private int left() {
        return MARGIN;
    }

    private int top() {
        return MARGIN;
    }

    private int right() {
        return this.width - MARGIN;
    }

    private int bottom() {
        return this.height - MARGIN;
    }

    private int listTop() {
        return top() + HEADER_H;
    }

    private int listBottom() {
        return bottom() - FOOTER_H;
    }

    private int contentLeft() {
        return left() + SIDEBAR_W + 4;
    }

    private int contentRight() {
        return right() - 6 - SCROLLBAR_W;
    }

    private int barX() {
        return right() - 4 - SCROLLBAR_W;
    }

    private int barTop() {
        return listTop() + ARROW_H;
    }

    private int barBottom() {
        return listBottom() - ARROW_H;
    }

    private int searchX() {
        return right() - 96;
    }

    // ---- rendering -------------------------------------------------------------------------

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        FontRenderers.ensureLoaded();
        clampSelection();
        hovered = null;

        MatrixStack ms = context.getMatrices();
        Color back = Palette.getBackColor();
        Color accent = Palette.getColor(0f);
        int text = Palette.getTextColor();

        // Dim the world, then the window itself: a soft shadow, the panel, a hairline edge.
        context.fill(0, 0, this.width, this.height, 0xC0000000);
        for (int i = 4; i >= 1; i--) {
            Render2D.drawRound(ms, left() - i, top() - i,
                    right() - left() + i * 2, bottom() - top() + i * 2, 5 + i,
                    new Color(0, 0, 0, 26 - i * 4));
        }
        Render2D.drawRound(ms, left(), top(), right() - left(), bottom() - top(), 5,
                new Color(back.getRed(), back.getGreen(), back.getBlue(), 242));
        outline(context, left(), top(), right(), bottom(),
                new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 46).getRGB());

        renderHeader(context, ms, mouseX, mouseY, accent, text);
        renderSidebar(context, ms, mouseX, mouseY, accent, text);
        renderOptions(context, ms, mouseX, mouseY, accent, text);
        renderFooter(context, ms, text);

        super.render(context, mouseX, mouseY, delta);
    }

    private void renderHeader(DrawContext context, MatrixStack ms, int mouseX, int mouseY,
                              Color accent, int text) {
        FontRenderer font = FontRenderers.sf_bold;
        font.drawString(ms, "SoupAPI", left() + 8, top() + 7, accent.getRGB());
        float titleWidth = font.getStringWidth("SoupAPI");
        context.fill(left() + 8, top() + 16, (int) (left() + 8 + titleWidth), top() + 17,
                new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 140).getRGB());
        FontRenderers.sf_bold_mini.drawString(ms, me.Padej_.soupapi.SoupAPI.VERSION,
                left() + 12 + titleWidth, top() + 8, 0xFF7A8090);

        // Search field.
        int x = searchX();
        int y = top() + 4;
        boolean hover = inside(mouseX, mouseY, x, y, 90, 12);
        Render2D.drawRound(ms, x, y, 90, 12, 3,
                new Color(255, 255, 255, searchFocused ? 38 : (hover ? 26 : 16)));
        String shown = searchQuery.isEmpty() && !searchFocused ? "Search..." : searchQuery;
        int colour = searchQuery.isEmpty() && !searchFocused ? 0xFF8A90A0 : text;
        FontRenderers.sf_bold_mini.drawString(ms, clip(shown, 82, FontRenderers.sf_bold_mini),
                x + 4, y + 3, colour);
        if (searchFocused && caretVisible()) {
            float caret = x + 4 + FontRenderers.sf_bold_mini.getStringWidth(
                    clip(searchQuery, 82, FontRenderers.sf_bold_mini));
            context.fill((int) caret, y + 2, (int) caret + 1, y + 10, text);
        }
        if (!searchQuery.isEmpty()) {
            String found = matchCount() + " found";
            FontRenderers.sf_bold_mini.drawString(ms, found,
                    x - 6 - FontRenderers.sf_bold_mini.getStringWidth(found), y + 3, 0xFF7A8090);
        }

        context.fill(left() + 4, listTop() - 2, right() - 4, listTop() - 1, 0x22FFFFFF);
    }

    private void renderSidebar(DrawContext context, MatrixStack ms, int mouseX, int mouseY,
                               Color accent, int text) {
        int x = left() + 4;
        int w = SIDEBAR_W - 4;
        int top = listTop();
        int bottom = listBottom();

        sidebarScroll = clampScroll(sidebarScroll, pages.size() * SIDE_ROW_H, bottom - top);

        RenderSystem.enableScissor(x, top, x + w, bottom);
        int y = (int) (top - sidebarScroll);
        for (int i = 0; i < pages.size(); i++) {
            OptionPage page = pages.get(i);
            boolean selected = i == selectedPage;
            boolean hover = inside(mouseX, mouseY, x, y, w, SIDE_ROW_H - 2)
                    && mouseY >= top && mouseY <= bottom;
            if (selected) {
                Render2D.drawRound(ms, x, y, w, SIDE_ROW_H - 2, 3,
                        new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 60));
                context.fill(x, y + 2, x + 2, y + SIDE_ROW_H - 4, accent.getRGB());
            } else if (hover) {
                Render2D.drawRound(ms, x, y, w, SIDE_ROW_H - 2, 3, new Color(255, 255, 255, 18));
            }
            String count = String.valueOf(page.interactiveCount());
            float countWidth = FontRenderers.sf_bold_mini.getStringWidth(count);
            FontRenderers.sf_bold_mini.drawString(ms,
                    clip(page.title(), w - 14 - countWidth, FontRenderers.sf_bold_mini),
                    x + 6, y + 4, selected ? accent.getRGB() : text);
            FontRenderers.sf_bold_mini.drawString(ms, count, x + w - 5 - countWidth, y + 4,
                    selected ? 0xFFB0B6C6 : 0xFF6A7080);
            y += SIDE_ROW_H;
        }
        RenderSystem.disableScissor();

        context.fill(left() + SIDEBAR_W, top, left() + SIDEBAR_W + 1, bottom, 0x18FFFFFF);
    }

    private void renderOptions(DrawContext context, MatrixStack ms, int mouseX, int mouseY,
                               Color accent, int text) {
        if (pages.isEmpty()) return;
        List<Option> rows = visibleRows();
        int x = contentLeft();
        int w = contentRight() - x;
        int top = listTop();
        int bottom = listBottom();

        contentScroll = clampScroll(contentScroll, totalHeight(rows), bottom - top);

        RenderSystem.enableScissor(x, top, x + w, bottom);
        int y = (int) (top - contentScroll);
        int index = 0;
        for (Option row : rows) {
            int rowHeight = row.interactive() ? ROW_H : GROUP_H;
            if (row.interactive()) index++;
            if (y + rowHeight >= top && y <= bottom) {
                boolean hover = mouseX >= x && mouseX <= x + w && mouseY >= Math.max(top, y)
                        && mouseY <= Math.min(bottom, y + rowHeight);
                if (hover && row.interactive()) {
                    hovered = row;
                }
                renderRow(context, ms, row, x, y, w, rowHeight, mouseX, mouseY, hover,
                        (index & 1) == 1, accent, text);
            }
            y += rowHeight;
        }
        RenderSystem.disableScissor();

        renderScrollbar(context, barX(), top, bottom, totalHeight(rows), contentScroll, accent,
                mouseX, mouseY);
    }

    private void renderRow(DrawContext context, MatrixStack ms, Option row, int x, int y, int w,
                           int rowHeight, int mouseX, int mouseY, boolean hover, boolean banded,
                           Color accent, int text) {
        if (!row.interactive()) {
            // Group caption.
            FontRenderers.sf_bold_mini.drawString(ms, row.label().toUpperCase(java.util.Locale.ROOT),
                    x + 2, y + 5, accent.getRGB());
            float captionWidth = FontRenderers.sf_bold_mini.getStringWidth(
                    row.label().toUpperCase(java.util.Locale.ROOT));
            context.fill((int) (x + captionWidth + 6), y + 8, x + w - 2, y + 9, 0x22FFFFFF);
            return;
        }

        boolean unavailable = row.unavailableReason() != null;
        if (banded) {
            Render2D.drawRound(ms, x, y, w, rowHeight - 1, 3, new Color(255, 255, 255, 6));
        }
        if (hover) {
            Render2D.drawRound(ms, x, y, w, rowHeight - 1, 3, new Color(255, 255, 255, 14));
        }
        int controlX = x + w - CONTROL_W - 4;
        if (unavailable) {
            // Readable, but clearly not something to reach for; the footer says why.
            text = 0xFF6E7380;
            accent = new Color(110, 115, 128);
        }
        FontRenderers.sf_bold_mini.drawString(ms,
                clip(row.label(), controlX - x - 8, FontRenderers.sf_bold_mini),
                x + 4, y + 6, text);
        if (!row.isDefault()) {
            context.fill(x + 1, y + 5, x + 2, y + rowHeight - 6, accent.getRGB());
        }

        int cy = y + 3;
        int ch = rowHeight - 7;

        if (row instanceof Option.Toggle) {
            Option.Toggle toggle = (Option.Toggle) row;
            boolean on = toggle.get();
            int tw = 22;
            int tx = x + w - tw - 4;
            Color fill = on ? accent : new Color(255, 255, 255, 40);
            Render2D.drawRound(ms, tx, cy + 1, tw, ch - 2, (ch - 2) / 2f, fill);
            float knob = on ? tx + tw - (ch - 4) - 2 : tx + 2;
            Render2D.drawRound(ms, knob, cy + 3, ch - 6, ch - 6, (ch - 6) / 2f, Color.WHITE);
            return;
        }

        if (row instanceof Option.Num) {
            Option.Num num = (Option.Num) row;
            if (num.bounded()) {
                int trackW = CONTROL_W - 30;
                int tx = controlX;
                int ty = cy + ch / 2 - 1;
                Render2D.drawRound(ms, tx, ty, trackW, 2, 1, new Color(255, 255, 255, 40));
                float fill = (float) (trackW * num.fraction());
                Render2D.drawRound(ms, tx, ty, Math.max(1f, fill), 2, 1, accent);
                Render2D.drawRound(ms, tx + fill - 2, ty - 2.5f, 5, 7, 2.5f, Color.WHITE);
                String value = num.display();
                FontRenderers.sf_bold_mini.drawString(ms, value,
                        x + w - 4 - FontRenderers.sf_bold_mini.getStringWidth(value), y + 6, text);
            } else {
                String value = editing == row ? editBuffer : num.display();
                int bw = 46;
                int bx = x + w - bw - 4;
                Render2D.drawRound(ms, bx, cy, bw, ch, 3,
                        new Color(255, 255, 255, editing == row ? 38 : 20));
                FontRenderers.sf_bold_mini.drawString(ms, clip(value, bw - 8, FontRenderers.sf_bold_mini),
                        bx + 4, y + 6, text);
                if (editing == row && caretVisible()) {
                    float caret = bx + 4 + FontRenderers.sf_bold_mini.getStringWidth(value);
                    context.fill((int) caret, cy + 2, (int) caret + 1, cy + ch - 2, text);
                }
            }
            return;
        }

        if (row instanceof Option.Key) {
            Option.Key key = (Option.Key) row;
            int bx = controlX;
            boolean waiting = listening == key;
            boolean hoverControl = inside(mouseX, mouseY, bx, cy, CONTROL_W, ch);
            Render2D.drawRound(ms, bx, cy, CONTROL_W, ch, 3,
                    waiting ? new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 90)
                            : new Color(255, 255, 255, hoverControl ? 34 : 20));
            String value = waiting ? "Press a key..." : key.display();
            FontRenderers.sf_bold_mini.drawString(ms,
                    clip(value, CONTROL_W - 8, FontRenderers.sf_bold_mini),
                    bx + CONTROL_W / 2f - Math.min(CONTROL_W - 8,
                            FontRenderers.sf_bold_mini.getStringWidth(value)) / 2f,
                    y + 6, waiting ? accent.getRGB() : text);
            return;
        }

        if (row instanceof Option.Choice) {
            Option.Choice choice = (Option.Choice) row;
            int bx = controlX;
            boolean hoverControl = inside(mouseX, mouseY, bx, cy, CONTROL_W, ch);
            Render2D.drawRound(ms, bx, cy, CONTROL_W, ch, 3,
                    new Color(255, 255, 255, hoverControl ? 34 : 20));
            String value = choice.display() + (choice.needsPicker() ? " ..." : "");
            FontRenderers.sf_bold_mini.drawString(ms,
                    clip(value, CONTROL_W - 8, FontRenderers.sf_bold_mini), bx + 4, y + 6, text);
            return;
        }

        if (row instanceof Option.Colour) {
            Option.Colour colour = (Option.Colour) row;
            int sw = 12;
            int bx = controlX;
            int rgb = colour.get();
            Render2D.drawRound(ms, bx, cy, sw, ch, 2,
                    new Color((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, 255));
            int fx = bx + sw + 3;
            int fw = CONTROL_W - sw - 3;
            String value = editing == row ? editBuffer : colour.hex();
            Render2D.drawRound(ms, fx, cy, fw, ch, 3,
                    new Color(255, 255, 255, editing == row ? 38 : 20));
            FontRenderers.sf_bold_mini.drawString(ms, "#" + value, fx + 3, y + 6, text);
            if (editing == row && caretVisible()) {
                float caret = fx + 3 + FontRenderers.sf_bold_mini.getStringWidth("#" + value);
                context.fill((int) caret, cy + 2, (int) caret + 1, cy + ch - 2, text);
            }
            return;
        }

        if (row instanceof Option.Text) {
            Option.Text field = (Option.Text) row;
            int fw = CONTROL_W + 30;
            int fx = x + w - fw - 4;
            String value = editing == row ? editBuffer : field.get();
            Render2D.drawRound(ms, fx, cy, fw, ch, 3,
                    new Color(255, 255, 255, editing == row ? 38 : 20));
            String shown = clipEnd(value, fw - 8, FontRenderers.sf_bold_mini);
            FontRenderers.sf_bold_mini.drawString(ms, shown, fx + 4, y + 6, text);
            if (editing == row && caretVisible()) {
                float caret = fx + 4 + FontRenderers.sf_bold_mini.getStringWidth(shown);
                context.fill((int) Math.min(caret, fx + fw - 3), cy + 2,
                        (int) Math.min(caret, fx + fw - 3) + 1, cy + ch - 2, text);
            }
            return;
        }

        if (row instanceof Option.TextList) {
            Option.TextList list = (Option.TextList) row;
            int bx = controlX;
            Render2D.drawRound(ms, bx, cy, CONTROL_W, ch, 3, new Color(255, 255, 255, 20));
            String value = "Edit (" + list.get().length + ")";
            FontRenderers.sf_bold_mini.drawString(ms, value,
                    bx + CONTROL_W / 2f - FontRenderers.sf_bold_mini.getStringWidth(value) / 2f,
                    y + 6, text);
            return;
        }

        if (row instanceof Option.Action) {
            Option.Action action = (Option.Action) row;
            int bx = controlX;
            boolean hoverControl = inside(mouseX, mouseY, bx, cy, CONTROL_W, ch);
            Render2D.drawRound(ms, bx, cy, CONTROL_W, ch, 3,
                    new Color(accent.getRed(), accent.getGreen(), accent.getBlue(),
                            hoverControl ? 150 : 90));
            String value = action.buttonText();
            FontRenderers.sf_bold_mini.drawString(ms, value,
                    bx + CONTROL_W / 2f - FontRenderers.sf_bold_mini.getStringWidth(value) / 2f,
                    y + 6, 0xFFFFFFFF);
        }
    }

    /**
     * The scroll rail: an up arrow, a draggable thumb, a down arrow.
     *
     * <p>Drawn even when everything fits, so the control does not appear and disappear under the
     * thumb as a page is filtered.
     */
    private void renderScrollbar(DrawContext context, int x, int top, int bottom, int total,
                                 double scroll, Color accent, int mouseX, int mouseY) {
        MatrixStack ms = new MatrixStack();
        int view = bottom - top;
        boolean needed = total > view;

        Render2D.drawRound(ms, x, top, SCROLLBAR_W, view, SCROLLBAR_W / 2f,
                new Color(255, 255, 255, 16));

        arrow(context, ms, x, top, true, accent, mouseX, mouseY, needed);
        arrow(context, ms, x, bottom - ARROW_H, false, accent, mouseX, mouseY, needed);

        if (!needed) return;
        int railTop = top + ARROW_H;
        int railHeight = view - ARROW_H * 2;
        int thumbHeight = Math.max(18, (int) ((double) railHeight * view / total));
        int travel = railHeight - thumbHeight;
        int thumbY = railTop + (int) (travel * (scroll / (double) (total - view)));
        boolean hover = inside(mouseX, mouseY, x, thumbY, SCROLLBAR_W, thumbHeight);
        Render2D.drawRound(ms, x + 1, thumbY, SCROLLBAR_W - 2, thumbHeight, (SCROLLBAR_W - 2) / 2f,
                new Color(accent.getRed(), accent.getGreen(), accent.getBlue(),
                        draggingBar || hover ? 235 : 180));
    }

    private void arrow(DrawContext context, MatrixStack ms, int x, int y, boolean up, Color accent,
                       int mouseX, int mouseY, boolean enabled) {
        boolean hover = enabled && inside(mouseX, mouseY, x, y, SCROLLBAR_W, ARROW_H);
        Render2D.drawRound(ms, x, y, SCROLLBAR_W, ARROW_H, 3,
                new Color(255, 255, 255, hover ? 46 : 26));
        int colour = enabled
                ? new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 230).getRGB()
                : 0x55FFFFFF;
        // A small triangle, drawn as three rows of pixels so it stays crisp at any GUI scale.
        int cx = x + SCROLLBAR_W / 2;
        int cy = y + ARROW_H / 2;
        for (int i = 0; i < 3; i++) {
            int half = up ? i : 2 - i;
            int row = up ? cy - 2 + i : cy - 2 + i;
            context.fill(cx - half, row, cx + half + 1, row + 1, colour);
        }
    }

    /** How far one arrow press or one rail tap moves the list. */
    private int scrollStep() {
        return ROW_H * 3;
    }

    /** A one-pixel frame, drawn as four fills so it stays crisp at any GUI scale. */
    private void outline(DrawContext context, int left, int top, int right, int bottom, int colour) {
        context.fill(left, top, right, top + 1, colour);
        context.fill(left, bottom - 1, right, bottom, colour);
        context.fill(left, top, left + 1, bottom, colour);
        context.fill(right - 1, top, right, bottom, colour);
    }

    /** How many options the search matches, across every page. */
    private int matchCount() {
        int count = 0;
        for (OptionPage page : pages) {
            for (Option row : page.rows()) {
                if (row.interactive()
                        && row.label().toLowerCase(java.util.Locale.ROOT)
                                .contains(searchQuery.toLowerCase(java.util.Locale.ROOT))) {
                    count++;
                }
            }
        }
        return count;
    }

    private void renderFooter(DrawContext context, MatrixStack ms, int text) {
        int y = listBottom() + 3;
        context.fill(left() + 4, y - 2, right() - 4, y - 1, 0x18FFFFFF);
        String hint;
        if (hovered != null && hovered.unavailableReason() != null) {
            hint = "Not available on 1.8.9 - " + hovered.unavailableReason();
        } else if (hovered != null && hovered.tooltip() != null && !hovered.tooltip().isEmpty()) {
            hint = hovered.tooltip();
        } else if (hovered != null) {
            hint = hovered.label() + "  -  right-click to reset";
        } else {
            hint = pages.isEmpty() ? "" : pages.get(selectedPage).interactiveCount()
                    + " options  -  scroll or drag to move, right-click a row to reset";
        }
        FontRenderers.sf_bold_mini.drawString(ms,
                clip(hint, right() - left() - 16, FontRenderers.sf_bold_mini),
                left() + 6, y + 2, 0xFF9AA0B0);
    }

    // ---- input -----------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Search box.
        if (inside(mouseX, mouseY, searchX(), top() + 4, 90, 12)) {
            searchFocused = true;
            stopEditing(true);
            return true;
        }
        searchFocused = false;

        // The scroll rail: arrows, thumb, or a tap on the track to page.
        if (mouseX >= barX() - 2 && mouseX <= barX() + SCROLLBAR_W + 2
                && mouseY >= listTop() && mouseY <= listBottom()) {
            stopEditing(true);
            List<Option> rows = visibleRows();
            int view = listBottom() - listTop();
            int total = totalHeight(rows);
            if (mouseY <= listTop() + ARROW_H) {
                contentScroll -= scrollStep();
            } else if (mouseY >= listBottom() - ARROW_H) {
                contentScroll += scrollStep();
            } else if (total > view) {
                int railTop = barTop();
                int railHeight = barBottom() - barTop();
                int thumbHeight = Math.max(18, (int) ((double) railHeight * view / total));
                int travel = railHeight - thumbHeight;
                int thumbY = railTop + (int) (travel * (contentScroll / (double) (total - view)));
                if (mouseY >= thumbY && mouseY <= thumbY + thumbHeight) {
                    draggingBar = true;
                    barGrabOffset = mouseY - thumbY;
                } else {
                    contentScroll += mouseY < thumbY ? -view * 0.8 : view * 0.8;
                }
            }
            contentScroll = clampScroll(contentScroll, total, view);
            return true;
        }

        // Sidebar.
        if (mouseX >= left() && mouseX <= left() + SIDEBAR_W
                && mouseY >= listTop() && mouseY <= listBottom()) {
            int index = (int) ((mouseY - listTop() + sidebarScroll) / SIDE_ROW_H);
            if (index >= 0 && index < pages.size()) {
                if (index != selectedPage) {
                    selectedPage = index;
                    contentScroll = 0;
                }
                stopEditing(true);
            }
            lastDragY = mouseY;
            draggingList = true;
            return true;
        }

        // Option rows.
        if (mouseX >= contentLeft() && mouseX <= contentRight()
                && mouseY >= listTop() && mouseY <= listBottom()) {
            List<Option> rows = visibleRows();
            int x = contentLeft();
            int w = contentRight() - x;
            int y = (int) (listTop() - contentScroll);
            for (Option row : rows) {
                int rowHeight = row.interactive() ? ROW_H : GROUP_H;
                if (row.interactive() && mouseY >= y && mouseY < y + rowHeight) {
                    if (row.unavailableReason() != null) {
                        stopEditing(true);
                        return true;
                    }
                    if (button == 1) {
                        row.reset();
                        stopEditing(false);
                        ConfigSchema.save();
                        return true;
                    }
                    clickRow(row, x, y, w, rowHeight, mouseX, mouseY, button);
                    return true;
                }
                y += rowHeight;
            }
            stopEditing(true);
            lastDragY = mouseY;
            draggingList = true;
            return true;
        }

        stopEditing(true);
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void clickRow(Option row, int x, int y, int w, int rowHeight,
                          double mouseX, double mouseY, int button) {
        if (row instanceof Option.Key) {
            // Tap to listen, tap again to give up without changing it.
            listening = listening == row ? null : (Option.Key) row;
            return;
        }
        int controlX = x + w - CONTROL_W - 4;
        int cy = y + 3;
        int ch = rowHeight - 7;

        if (row instanceof Option.Toggle) {
            ((Option.Toggle) row).toggle();
            stopEditing(false);
            ConfigSchema.save();
            return;
        }

        if (row instanceof Option.Num) {
            Option.Num num = (Option.Num) row;
            if (num.bounded()) {
                int trackW = CONTROL_W - 30;
                if (mouseX >= controlX - 4 && mouseX <= controlX + trackW + 6) {
                    num.setFraction((mouseX - controlX) / (double) trackW);
                    draggingNum = num;
                    draggingSlider = true;
                    stopEditing(false);
                }
            } else {
                startEditing(row, num.display());
                draggingNum = num;
                draggingSlider = false;
                lastDragX = mouseX;
            }
            return;
        }

        if (row instanceof Option.Choice) {
            Option.Choice choice = (Option.Choice) row;
            stopEditing(false);
            if (choice.needsPicker()) {
                Minecraft.getMinecraft().displayGuiScreen(new EnumPickerScreen(this, choice));
            } else {
                choice.cycle(button == 1 ? -1 : 1);
                ConfigSchema.save();
            }
            return;
        }

        if (row instanceof Option.Colour) {
            Option.Colour colour = (Option.Colour) row;
            startEditing(row, colour.hex());
            return;
        }

        if (row instanceof Option.Text) {
            startEditing(row, ((Option.Text) row).get());
            return;
        }

        if (row instanceof Option.TextList) {
            stopEditing(false);
            Minecraft.getMinecraft().displayGuiScreen(
                    new StringListScreen(this, (Option.TextList) row));
            return;
        }

        if (row instanceof Option.Action) {
            stopEditing(true);
            ((Option.Action) row).run();
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (draggingNum != null) {
            if (draggingSlider) {
                int x = contentLeft();
                int w = contentRight() - x;
                int controlX = x + w - CONTROL_W - 4;
                int trackW = CONTROL_W - 30;
                draggingNum.setFraction((mouseX - controlX) / (double) trackW);
            } else {
                double dx = mouseX - lastDragX;
                if (Math.abs(dx) >= 1.0) {
                    draggingNum.set(draggingNum.get() + dx * draggingNum.dragStep());
                    lastDragX = mouseX;
                    if (editing == draggingNum) {
                        editBuffer = draggingNum.display();
                    }
                }
            }
            return true;
        }
        if (draggingBar) {
            List<Option> rows = visibleRows();
            int view = listBottom() - listTop();
            int total = totalHeight(rows);
            if (total > view) {
                int railTop = barTop();
                int railHeight = barBottom() - barTop();
                int thumbHeight = Math.max(18, (int) ((double) railHeight * view / total));
                int travel = railHeight - thumbHeight;
                if (travel > 0) {
                    double fraction = (mouseY - barGrabOffset - railTop) / (double) travel;
                    contentScroll = clampScroll(fraction * (total - view), total, view);
                }
            }
            return true;
        }

        if (draggingList) {
            double dy = mouseY - lastDragY;
            lastDragY = mouseY;
            if (mouseX <= left() + SIDEBAR_W) {
                sidebarScroll -= dy;
            } else {
                contentScroll -= dy;
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingNum != null) {
            draggingNum = null;
            draggingSlider = false;
            ConfigSchema.save();
            return true;
        }
        draggingList = false;
        draggingBar = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        double amount = vertical * ROW_H * 2;
        if (mouseX <= left() + SIDEBAR_W) {
            sidebarScroll -= amount;
        } else {
            contentScroll -= amount;
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (listening != null) {
            // Escape unbinds, anything else binds; either way the row stops listening.
            listening.set(keyCode == 1 ? 0 : keyCode);
            listening = null;
            ConfigSchema.save();
            return true;
        }
        // LWJGL 2 key codes: 14 backspace, 28 enter, 156 numpad enter, 1 escape (handled upstream).
        if (searchFocused) {
            if (keyCode == 14) {
                if (!searchQuery.isEmpty()) {
                    searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                    contentScroll = 0;
                }
                return true;
            }
            if (keyCode == 28 || keyCode == 156) {
                searchFocused = false;
                return true;
            }
            return true;
        }
        if (editing != null) {
            if (keyCode == 14) {
                if (!editBuffer.isEmpty()) {
                    editBuffer = editBuffer.substring(0, editBuffer.length() - 1);
                }
                return true;
            }
            if (keyCode == 28 || keyCode == 156) {
                stopEditing(true);
                return true;
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (chr < 32 || chr == 127) return false;
        if (searchFocused) {
            searchQuery += chr;
            contentScroll = 0;
            return true;
        }
        if (editing instanceof Option.Colour) {
            if (Character.digit(chr, 16) >= 0 && editBuffer.length() < (((Option.Colour) editing).allowAlpha() ? 8 : 6)) {
                editBuffer += Character.toUpperCase(chr);
            }
            return true;
        }
        if (editing instanceof Option.Num) {
            if (Character.isDigit(chr) || chr == '-' || chr == '.') {
                editBuffer += chr;
            }
            return true;
        }
        if (editing instanceof Option.Text) {
            editBuffer += chr;
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    // ---- editing ---------------------------------------------------------------------------

    private void startEditing(Option option, String initial) {
        editing = option;
        editBuffer = initial == null ? "" : initial;
        searchFocused = false;
    }

    /** Ends the current edit, optionally committing what was typed. */
    private void stopEditing(boolean commit) {
        if (editing == null) return;
        Option option = editing;
        String buffer = editBuffer;
        editing = null;
        editBuffer = "";
        if (!commit) return;

        try {
            if (option instanceof Option.Colour) {
                Option.Colour colour = (Option.Colour) option;
                if (!buffer.isEmpty()) {
                    long parsed = Long.parseLong(buffer, 16);
                    colour.set(colour.allowAlpha() ? (int) parsed : (int) (parsed & 0xFFFFFF));
                }
            } else if (option instanceof Option.Num) {
                Option.Num num = (Option.Num) option;
                if (!buffer.isEmpty() && !buffer.equals("-")) {
                    num.set(Double.parseDouble(buffer));
                }
            } else if (option instanceof Option.Text) {
                ((Option.Text) option).set(buffer);
            }
        } catch (NumberFormatException ignored) {
            // Leave the value as it was; the field simply reverts.
        }
        ConfigSchema.save();
    }

    // ---- helpers ---------------------------------------------------------------------------

    private List<Option> visibleRows() {
        if (pages.isEmpty()) return new ArrayList<Option>();
        return pages.get(selectedPage).search(searchQuery);
    }

    private int totalHeight(List<Option> rows) {
        int total = 0;
        for (Option row : rows) {
            total += row.interactive() ? ROW_H : GROUP_H;
        }
        return total + 4;
    }

    private static double clampScroll(double scroll, int total, int view) {
        double max = Math.max(0, total - view);
        return Math.max(0, Math.min(max, scroll));
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    private static boolean caretVisible() {
        return (System.currentTimeMillis() / 500) % 2 == 0;
    }

    /** Truncates from the right with an ellipsis so a long label cannot run under its control. */
    private static String clip(String text, float maxWidth, FontRenderer font) {
        if (text == null) return "";
        if (font.getStringWidth(text) <= maxWidth) return text;
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            if (font.getStringWidth(out.toString() + text.charAt(i) + "..") > maxWidth) break;
            out.append(text.charAt(i));
        }
        return out + "..";
    }

    /** Keeps the <i>end</i> of the text visible, which is what a path or URL needs. */
    private static String clipEnd(String text, float maxWidth, FontRenderer font) {
        if (text == null) return "";
        if (font.getStringWidth(text) <= maxWidth) return text;
        String out = text;
        while (out.length() > 1 && font.getStringWidth(".." + out) > maxWidth) {
            out = out.substring(1);
        }
        return ".." + out;
    }

    @Override
    public void close() {
        stopEditing(true);
        if (!pages.isEmpty()) {
            ConfigManager.setMetadata("selectedPage", pages.get(selectedPage).id());
        }
        ConfigSchema.save();
        super.close();
    }
}
