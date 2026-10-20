package me.Padej_.soupapi.gui.atlas;

import me.Padej_.soupapi.compat.DrawContext;
import me.Padej_.soupapi.compat.MatrixStack;
import me.Padej_.soupapi.compat.RenderSystem;
import me.Padej_.soupapi.compat.SoupScreen;
import me.Padej_.soupapi.config.ConfigManager;
import me.Padej_.soupapi.config.ConfigurableModule;
import me.Padej_.soupapi.font.FontRenderer;
import me.Padej_.soupapi.font.FontRenderers;
import me.Padej_.soupapi.gui.StringListScreen;
import me.Padej_.soupapi.gui.option.ConfigSchema;
import me.Padej_.soupapi.gui.option.Option;
import me.Padej_.soupapi.gui.option.OptionPage;
import me.Padej_.soupapi.utils.FxClock;
import me.Padej_.soupapi.utils.Palette;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The settings, as a port of the Atlas click GUI from Myau Atlas
 * (github.com/Marco-hacker666/Myau_Atlas, GPL-3.0 - the same licence as this mod).
 *
 * <p>What is ported is the design and how it behaves, not Myau's modules or anything of its gameplay:
 * three things on screen at once and nothing behind a hover. A sidebar of groups; a list of the
 * group's pages, each with the switch that turns it on; and a detail pane with every setting of the
 * selected page, laid out and editable in place. Typing anywhere searches every setting of every page
 * at once. Everything that moves is on a spring - the selection pills slide as droplets that stretch
 * with their speed and settle with a small overshoot, switches squash as they travel, the window pops
 * open - and every control is hit-tested against the rectangle it was actually drawn in, which is
 * what keeps a click and the eye in agreement.
 *
 * <p>Atlas draws its window as shader-refracted glass; that part is replaced with fixed-function
 * layers ({@link Glass}), because the mod runs on gl4es. The pages, widgets and values are the mod's
 * own {@link ConfigSchema}, so every edit goes through the same setter as the classic screen and is
 * saved the same way, and the theme logic sees it.
 */
public final class AtlasScreen extends SoupScreen {

    /** Which settings screen the open key brings up. */
    public enum Style { ATLAS, CLASSIC }

    private static final int HEADER = 34;
    private static final int FOOTER = 16;
    private static final int ROW = 22;
    private static final int MIN_W = 420, MIN_H = 250;

    private static final String[][] GROUPS = {
            {"Theme", "main", "rgb"},
            {"Weapons", "melee", "weapons"},
            {"World", "colored_sky", "world", "ambience", "smoke"},
            {"Combat", "kill_effects", "hit_effects", "hit_bubbles", "hit_particles", "hit_sound", "target_render", "target_hud", "hit_color"},
            {"Visuals", "trails", "player_effects", "halo", "jump_circles", "jump_particles", "landing_impact", "china_hat", "capes", "totem"},
            {"HUD", "hud", "crosshair", "mouse_move", "no_render", "trajectories", "aspect_ratio", "swing_hand"},
            {"System", "rpc", "keys"},
    };

    private final List<OptionPage> pages;
    private final Map<String, List<OptionPage>> grouped = new LinkedHashMap<String, List<OptionPage>>();
    private final List<String> groupNames = new ArrayList<String>();
    private String group;
    private OptionPage page;
    private String search = "";
    private boolean searchFocused;

    // Window
    private int winX = Integer.MIN_VALUE, winY, winW = 600, winH = 340;
    private int minW = MIN_W, minH = MIN_H;
    private boolean dragging, resizing;
    private int dragDX, dragDY;

    // Animation
    private final Map<String, Spring> springs = new HashMap<String, Spring>();
    private final Map<Object, Float> eased = new HashMap<Object, Float>();
    private final Spring open = new Spring(0.9f, 300f, 20f);
    private long lastFrame;
    private float delta = 0.016f, opened;

    // Scrolling
    private float listScroll, detailScroll, listMax, detailMax;

    // Interaction
    private static final class Hit {
        final String kind;
        final Object payload;
        final float x, y, x2, y2;

        Hit(String kind, Object payload, float x, float y, float x2, float y2) {
            this.kind = kind;
            this.payload = payload;
            this.x = x;
            this.y = y;
            this.x2 = x2;
            this.y2 = y2;
        }

        boolean contains(double mx, double my) {
            return mx >= x && mx <= x2 && my >= y && my <= y2;
        }
    }

    private final List<Hit> hits = new ArrayList<Hit>();
    private Option.Num slider;
    private Hit sliderHit;
    private Option editing;
    private String editBuffer = "";
    private Option.Key listening;
    private Option.Colour colourOpen;
    private Option.Choice dropdown;
    private float dropX, dropY, dropW;
    private String dropFilter = "";
    private float dropScroll;
    private String hint = "";

    // Palette, repainted every frame.
    private int accent, text, dim, faint, ink, tint;

    public AtlasScreen() {
        this.pages = ConfigSchema.build();
        Map<String, OptionPage> byId = new HashMap<String, OptionPage>();
        for (OptionPage p : pages) byId.put(p.id(), p);
        for (String[] g : GROUPS) {
            List<OptionPage> list = new ArrayList<OptionPage>();
            for (int i = 1; i < g.length; i++) {
                OptionPage p = byId.remove(g[i]);
                if (p != null) list.add(p);
            }
            if (!list.isEmpty()) {
                grouped.put(g[0], list);
                groupNames.add(g[0]);
            }
        }
        if (!byId.isEmpty()) {
            List<OptionPage> rest = new ArrayList<OptionPage>();
            for (OptionPage p : pages) if (byId.containsKey(p.id())) rest.add(p);
            grouped.put("Other", rest);
            groupNames.add("Other");
        }
        restore();
    }

    // ---- state -----------------------------------------------------------------------------------

    private void restore() {
        // The side-car file holds the remembered page and window rectangle. Nothing else has
        // necessarily read it yet - the classic screen loads it in init(), and this screen may well
        // be the first thing opened - and reading it unloaded would silently lose the layout.
        try {
            ConfigManager.loadConfig();
        } catch (Throwable ignored) {
        }
        try {
            String g = ConfigManager.getMetadata("atlasGroup");
            String p = ConfigManager.getMetadata("atlasPage");
            if (g != null && grouped.containsKey(g)) group = g;
            if (group == null && !groupNames.isEmpty()) group = groupNames.get(0);
            if (group != null) {
                for (OptionPage op : grouped.get(group)) if (op.id().equals(p)) page = op;
                if (page == null) page = grouped.get(group).get(0);
            }
            String rect = ConfigManager.getMetadata("atlasWindow");
            if (rect != null) {
                String[] parts = rect.split(",");
                winX = Integer.parseInt(parts[0]);
                winY = Integer.parseInt(parts[1]);
                winW = Integer.parseInt(parts[2]);
                winH = Integer.parseInt(parts[3]);
            }
        } catch (Throwable ignored) {
            // A bad saved layout just means the default one.
        }
        if (group == null && !groupNames.isEmpty()) group = groupNames.get(0);
        if (page == null && group != null) page = grouped.get(group).get(0);
    }

    private void persist() {
        try {
            ConfigManager.setMetadata("atlasGroup", group == null ? "" : group);
            ConfigManager.setMetadata("atlasPage", page == null ? "" : page.id());
            ConfigManager.setMetadata("atlasWindow", winX + "," + winY + "," + winW + "," + winH);
        } catch (Throwable ignored) {
        }
    }

    // ---- easing ----------------------------------------------------------------------------------

    private Spring spring(String key, float target, float stiffness, float damping) {
        Spring s = springs.get(key);
        if (s == null) {
            s = new Spring(target, stiffness, damping);
            springs.put(key, s);
        }
        s.target = target;
        s.step(delta);
        return s;
    }

    /** Exponential approach to 0 or 1: frame-rate independent fades for hovers and highlights. */
    private float ease(Object key, boolean on, float rate) {
        Float held = eased.get(key);
        float target = on ? 1f : 0f;
        float v = held == null ? target : held;
        v += (target - v) * (1f - (float) Math.exp(-rate * delta));
        eased.put(key, v);
        return v;
    }

    private void hit(String kind, Object payload, float x, float y, float x2, float y2) {
        hits.add(new Hit(kind, payload, x, y, x2, y2));
    }

    private Hit hitAt(double mx, double my) {
        for (int i = hits.size() - 1; i >= 0; i--) {
            if (hits.get(i).contains(mx, my)) return hits.get(i);
        }
        return null;
    }

    // ---- drawing ---------------------------------------------------------------------------------

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float partial) {
        // The fonts are built lazily, from the render and tick hooks. Those only run in game, so a
        // screen that can be opened before them has to ask for itself.
        FontRenderers.ensureLoaded();
        Palette.Element previous = Palette.scope(Palette.Element.GUI);
        try {
            draw(context, mouseX, mouseY);
        } finally {
            Palette.scope(previous);
            context.disableScissor();
            RenderSystem.restoreDefaults();
        }
    }

    private void draw(DrawContext context, int mouseX, int mouseY) {
        long now = System.nanoTime();
        delta = lastFrame == 0L ? 0.016f : Math.min(0.05f, (now - lastFrame) / 1.0E9f);
        lastFrame = now;
        opened += delta;
        hits.clear();
        hint = "";
        MatrixStack ms = context.getMatrices();
        int sw = context.getScaledWindowWidth(), sh = context.getScaledWindowHeight();
        layout(sw, sh);
        paintPalette();

        // The world dimmed behind, and the starfield Atlas calls its cosmos.
        float fade = Math.min(1f, opened / 0.18f);
        float dimAmount = ConfigurableModule.CONFIG.guiDim / 100f;
        Glass.rect(ms, 0, 0, sw, sh, 0f, Glass.alpha(0xFF050810, dimAmount * 0.8f * fade), Glass.alpha(0xFF050810, dimAmount * fade));
        if (ConfigurableModule.CONFIG.guiCosmos) cosmos(ms, sw, sh, fade);

        // The window pops open on a spring from 90%.
        open.target = 1f;
        float scale = open.step(delta);
        float px = winX + winW / 2f, py = winY + winH / 2f;
        ms.push();
        ms.translate(px, py, 0);
        ms.scale(scale, scale, 1f);
        ms.translate(-px, -py, 0);
        double mx = px + (mouseX - px) / scale, my = py + (mouseY - py) / scale;

        int x = winX, y = winY, w = winW, h = winH;
        Glass.shadow(ms, x, y, x + w, y + h, 18, Glass.alpha(0xFF000000, 0.55f * fade));
        Glass.pane(ms, x, y, x + w, y + h, 14f, tint, ink);
        hit("move", null, x, y, x + w, y + HEADER - 2);

        int side = sidebarWidth(), detail = detailWidth();
        header(ms, x, y, w, mx, my);
        Glass.rect(ms, x + 12, y + HEADER - 1, x + w - 12, y + HEADER - 0.5f, 0f, Glass.alpha(ink, 0.08f));
        Glass.rect(ms, x + side, y + HEADER + 8, x + side + 0.5f, y + h - FOOTER - 8, 0f, Glass.alpha(ink, 0.07f));
        Glass.rect(ms, x + w - detail, y + HEADER + 8, x + w - detail + 0.5f, y + h - FOOTER - 8, 0f, Glass.alpha(ink, 0.07f));

        int bodyTop = y + HEADER, bodyH = h - HEADER - FOOTER;
        sidebar(context, ms, x, bodyTop, side, bodyH, mx, my);
        list(context, ms, x + side, bodyTop, w - side - detail, bodyH, mx, my);
        detail(context, ms, x + w - detail, bodyTop, detail, bodyH, mx, my);
        footer(ms, x, y + h - FOOTER, w);
        grip(ms, x + w, y + h, mx, my);
        if (dropdown != null) dropdownOverlay(context, ms, mx, my);
        ms.pop();
    }

    private void layout(int sw, int sh) {
        // The preferred minimum, but never wider than the screen: at GUI scale 3 or 4 - which is
        // what a phone ends up on - the scaled width can be under 400, and a window pinned to 420
        // hangs off the side with its detail pane unreachable.
        minW = Math.min(MIN_W, Math.max(200, sw - 8));
        minH = Math.min(MIN_H, Math.max(140, sh - 8));
        winW = Math.max(minW, Math.min(winW, sw - 8));
        winH = Math.max(minH, Math.min(winH, sh - 8));
        if (winX == Integer.MIN_VALUE) {
            winX = (sw - winW) / 2;
            winY = (sh - winH) / 2;
        }
        winX = Math.max(4 - winW + 60, Math.min(winX, sw - 60));
        winY = Math.max(4, Math.min(winY, sh - 30));
    }

    private int sidebarWidth() {
        return Math.max(64, Math.min(120, winW / 6));
    }

    /**
     * The detail pane's width: a little over half of what the sidebar leaves, but never so much
     * that the page list in between is squeezed to nothing on a narrow window.
     */
    private int detailWidth() {
        int rest = winW - sidebarWidth();
        int detail = Math.round(rest * 0.56f);
        detail = Math.max(detail, Math.min(220, Math.round(rest * 0.5f)));
        return Math.min(detail, rest - Math.min(110, Math.round(rest * 0.35f)));
    }

    private void paintPalette() {
        accent = Palette.getColor(0f).getRGB() | 0xFF000000;
        int back = Palette.getBackColor().getRGB() & 0xFFFFFF;
        tint = 0xFF000000 | back;
        ink = 0xFFFFFFFF;
        text = 0xFFF5F8FF;
        dim = 0xD8C3CEDF;
        faint = 0xA093A3BA;
    }

    private void cosmos(MatrixStack ms, int sw, int sh, float fade) {
        float t = FxClock.seconds();
        for (int i = 0; i < 70; i++) {
            float sx = (float) ((i * 97.13 + t * (2 + (i % 5))) % sw);
            float sy = (float) ((i * 53.71 + Math.sin(t * 0.2 + i) * 6) % sh);
            float tw = 0.35f + 0.35f * (float) Math.sin(t * (1.2 + (i % 7) * 0.3) + i);
            float r = i % 9 == 0 ? 1.2f : 0.6f;
            Glass.dot(ms, sx, sy, r, Glass.alpha(i % 4 == 0 ? accent : 0xFFFFFFFF, tw * fade));
        }
    }

    private void header(MatrixStack ms, int x, int y, int w, double mx, double my) {
        FontRenderer title = FontRenderers.sf_bold;
        Glass.dot(ms, x + 18, y + HEADER / 2f, 5f, accent);
        Glass.dot(ms, x + 18, y + HEADER / 2f, 2.2f, Glass.alpha(0xFFFFFFFF, 0.85f));
        title.drawString(ms, "SoupAPI", x + 29, y + HEADER / 2f - 4, text);
        FontRenderers.sf_bold_12.drawString(ms, "Atlas", x + 31 + title.getStringWidth("SoupAPI"), y + HEADER / 2f - 2, faint);

        // Search, in the middle of the header.
        float sw = Math.min(220, w * 0.36f), sx = x + (w - sw) / 2f, sy = y + 8;
        float focus = ease("searchFocus", searchFocused, 12f);
        Glass.rect(ms, sx, sy, sx + sw, sy + 18, 9f, Glass.blend(Glass.alpha(ink, 0.06f), Glass.alpha(accent, 0.18f), focus));
        Glass.rim(ms, sx, sy, sx + sw, sy + 18, 9f, 0.8f, Glass.alpha(ink, 0.18f + 0.3f * focus), Glass.alpha(ink, 0.05f));
        FontRenderer small = FontRenderers.sf_bold_mini;
        String shown = search.isEmpty() && !searchFocused ? t("search", "Search every setting...") : search + (searchFocused && caret() ? "_" : "");
        small.drawString(ms, shown, sx + 10, sy + 5, search.isEmpty() ? faint : text);
        hit("search", null, sx, sy, sx + sw, sy + 18);

        // Classic screen switch and close.
        float cx = x + w - 16;
        float hover = ease("close", inside(mx, my, cx - 7, y + 10, cx + 7, y + 24), 12f);
        Glass.dot(ms, cx, y + HEADER / 2f, 6f, Glass.blend(Glass.alpha(ink, 0.10f), 0xFFE5484D, hover));
        hit("close", null, cx - 8, y + 8, cx + 8, y + 26);
        String classic = t("classic", "Classic");
        float bw = small.getStringWidth(classic) + 14, bx = cx - 14 - bw;
        float ch = ease("classic", inside(mx, my, bx, y + 9, bx + bw, y + 25), 12f);
        Glass.rect(ms, bx, y + 9, bx + bw, y + 25, 8f, Glass.alpha(ink, 0.06f + 0.08f * ch));
        small.drawString(ms, classic, bx + 7, y + 13, ch > 0.5f ? text : dim);
        hit("classic", null, bx, y + 9, bx + bw, y + 25);
    }

    private void sidebar(DrawContext context, MatrixStack ms, int x, int y, int width, int height, double mx, double my) {
        FontRenderer font = FontRenderers.sf_bold_mini;
        float top = y + 10;
        int index = groupNames.indexOf(group);
        Spring pill = spring("groupPill", index * ROW, 380f, 26f);
        float stretch = Math.min(6f, Math.abs(pill.velocity) * 0.012f);
        if (index >= 0 && search.isEmpty()) {
            Glass.pill(ms, x + 8, top + pill.value - stretch / 2, x + width - 8, top + pill.value + ROW - 2 + stretch / 2, 8f, accent, ink, 1f);
        }
        for (int i = 0; i < groupNames.size(); i++) {
            String g = groupNames.get(i);
            float ry = top + i * ROW;
            boolean hov = inside(mx, my, x + 8, ry, x + width - 8, ry + ROW - 2);
            float lit = ease("g:" + g, hov && !g.equals(group), 10f);
            if (lit > 0.01f) Glass.rect(ms, x + 8, ry, x + width - 8, ry + ROW - 2, 8f, Glass.alpha(ink, 0.06f * lit));
            font.drawString(ms, groupLabel(g), x + 18, ry + 6, g.equals(group) && search.isEmpty() ? text : dim);
            int on = 0;
            for (OptionPage p : grouped.get(g)) {
                Option.Toggle master = master(p);
                if (master != null && master.get()) on++;
            }
            if (on > 0) {
                String n = String.valueOf(on);
                FontRenderers.sf_bold_12.drawString(ms, n, x + width - 16 - FontRenderers.sf_bold_12.getStringWidth(n), ry + 7, faint);
            }
            hit("group", g, x + 8, ry, x + width - 8, ry + ROW - 2);
        }
    }

    /** A page's master switch: its first setting, when that is a toggle named "...Enabled". */
    private static Option.Toggle master(OptionPage p) {
        for (Option o : p.rows()) {
            if (!o.interactive()) continue;
            return o instanceof Option.Toggle && o.key() != null && o.key().endsWith("Enabled") ? (Option.Toggle) o : null;
        }
        return null;
    }

    private void list(DrawContext context, MatrixStack ms, int x, int y, int width, int height, double mx, double my) {
        FontRenderer big = FontRenderers.sf_bold;
        FontRenderer font = FontRenderers.sf_bold_mini;
        boolean searching = !search.trim().isEmpty();
        List<Object[]> rows = new ArrayList<Object[]>();
        if (searching) {
            for (OptionPage p : pages) {
                List<Option> found = p.search(search);
                int count = 0;
                for (Option o : found) if (o.interactive()) count++;
                if (count > 0 || p.title().toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT))) {
                    rows.add(new Object[]{p, count});
                }
            }
        } else if (group != null) {
            for (OptionPage p : grouped.get(group)) rows.add(new Object[]{p, p.interactiveCount()});
        }
        big.drawString(ms, searching ? t("search_title", "Search") : groupLabel(group), x + 14, y + 10, text);
        FontRenderers.sf_bold_12.drawString(ms, searching ? t("pages_match", "{n} pages match").replace("{n}", String.valueOf(rows.size())) : t("pages", "{n} pages").replace("{n}", String.valueOf(rows.size())),
                x + 14, y + 24, faint);

        float top = y + 38, bottom = y + height - 4;
        listMax = Math.max(0, rows.size() * ROW - (bottom - top));
        listScroll = Math.max(0, Math.min(listScroll, listMax));
        float shown = spring("listScroll", listScroll, 260f, 28f).value;
        // Registered before the rows: hitAt() walks the list backwards, so whatever is added last
        // wins, and a pane-sized rectangle added after the rows would swallow every click in it.
        hit("listArea", null, x, top, x + width, bottom);
        scissor(context, x, top - 2, x + width, bottom);
        int sel = -1;
        for (int i = 0; i < rows.size(); i++) if (rows.get(i)[0] == page) sel = i;
        if (sel >= 0) {
            Spring pill = spring("pagePill", sel * ROW, 380f, 26f);
            float stretch = Math.min(6f, Math.abs(pill.velocity) * 0.012f);
            Glass.pill(ms, x + 8 - stretch * 0.3f, top + pill.value - shown - stretch / 2, x + width - 8 + stretch * 0.3f,
                    top + pill.value - shown + ROW - 2 + stretch / 2, 8f, accent, ink, 1f);
        }
        for (int i = 0; i < rows.size(); i++) {
            OptionPage p = (OptionPage) rows.get(i)[0];
            float ry = top + i * ROW - shown;
            if (ry > bottom || ry + ROW < top) continue;
            boolean hov = inside(mx, my, x + 8, ry, x + width - 8, ry + ROW - 2) && my >= top && my <= bottom;
            float lit = ease("p:" + p.id(), hov && p != page, 10f);
            if (lit > 0.01f) Glass.rect(ms, x + 8, ry, x + width - 8, ry + ROW - 2, 8f, Glass.alpha(ink, 0.06f * lit));
            Option.Toggle master = master(p);
            float on = ease("on:" + p.id(), master != null && master.get(), 12f);
            Glass.dot(ms, x + 18, ry + ROW / 2f - 1, 2.5f, Glass.blend(Glass.alpha(ink, 0.25f), accent, on));
            font.drawString(ms, clip(p.title(), width - 80, font), x + 27, ry + 6, master == null || master.get() ? text : dim);
            String count = String.valueOf(rows.get(i)[1]);
            float sx = x + width - 16 - 22;
            FontRenderers.sf_bold_12.drawString(ms, count, sx - 6 - FontRenderers.sf_bold_12.getStringWidth(count), ry + 7, faint);
            hit("page", p, x + 8, Math.max(ry, top), x + width - 8, Math.min(ry + ROW - 2, bottom));
            if (master != null) {
                drawSwitch(ms, "pg:" + p.id(), sx, ry + ROW / 2f - 1, 22f, 12f, master.get());
                hit("toggle", master, sx - 4, Math.max(ry + 2, top), sx + 26, Math.min(ry + ROW - 4, bottom));
            }
        }
        context.disableScissor();
        scrollbar(ms, x + width - 5, top, bottom, shown, listMax);
    }

    /** A switch whose knob travels on a spring and stretches with its speed. */
    private void drawSwitch(MatrixStack ms, String key, float x, float cy, float w, float h, boolean on) {
        Spring s = spring("sw:" + key, on ? 1f : 0f, 520f, 24f);
        float t = Math.max(-0.08f, Math.min(1.08f, s.value));
        float lit = Math.max(0f, Math.min(1f, t));
        float y = cy - h / 2f;
        Glass.rect(ms, x, y, x + w, y + h, h / 2f, Glass.alpha(ink, 0.12f * (1f - lit)));
        if (lit > 0.01f) Glass.rect(ms, x, y, x + w, y + h, h / 2f, Glass.alpha(accent, 0.95f * lit), Glass.alpha(accent, 0.8f * lit));
        float pad = 1.5f, knob = h - pad * 2f;
        float stretch = Math.min(knob * 0.45f, Math.abs(s.velocity) * 0.018f * knob);
        float kx = x + pad + (w - pad * 2f - knob) * t;
        Glass.rect(ms, kx - stretch / 2f, y + pad, kx + knob + stretch / 2f, y + pad + knob, knob / 2f, 0xFFFFFFFF, 0xFFE9EEF6);
    }

    private void detail(DrawContext context, MatrixStack ms, int x, int y, int width, int height, double mx, double my) {
        FontRenderer big = FontRenderers.sf_bold;
        FontRenderer font = FontRenderers.sf_bold_mini;
        if (page == null) {
            big.drawString(ms, t("select_page", "Select a page"), x + 14, y + 10, text);
            return;
        }
        big.drawString(ms, clip(page.title(), width - 30, big), x + 14, y + 10, text);
        List<Option> rows = page.search(search);
        FontRenderers.sf_bold_12.drawString(ms, (search.isEmpty() ? t("settings", "{n} settings").replace("{n}", String.valueOf(page.interactiveCount())) : t("matching", "matching \"{n}\"").replace("{n}", search))
                + "  ·  " + t("reset_hint", "right-click resets"), x + 14, y + 24, faint);
        float top = y + 38, bottom = y + height - 4;
        float total = 0;
        for (Option o : rows) total += rowHeight(o);
        detailMax = Math.max(0, total - (bottom - top) + 6);
        detailScroll = Math.max(0, Math.min(detailScroll, detailMax));
        float shown = spring("detailScroll", detailScroll, 260f, 28f).value;
        hit("detailArea", null, x, top, x + width, bottom);
        scissor(context, x, top - 2, x + width, bottom);
        float ry = top - shown;
        for (Option o : rows) {
            float rh = rowHeight(o);
            if (ry + rh >= top && ry <= bottom) row(ms, font, o, x + 10, ry, width - 22, rh, mx, my, top, bottom);
            ry += rh;
        }
        context.disableScissor();
        scrollbar(ms, x + width - 5, top, bottom, shown, detailMax);
    }

    private float rowHeight(Option o) {
        if (!o.interactive()) return 18;
        if (o instanceof Option.Num && ((Option.Num) o).bounded()) return 30;
        if (o == colourOpen) return 74;
        return ROW;
    }

    private void row(MatrixStack ms, FontRenderer font, Option o, float x, float y, float w, float h,
                     double mx, double my, float top, float bottom) {
        if (!o.interactive()) {
            String label = o.label().toUpperCase(Locale.ROOT);
            FontRenderers.sf_bold_12.drawString(ms, label, x + 4, y + 8, faint);
            Glass.rect(ms, x + 8 + FontRenderers.sf_bold_12.getStringWidth(label), y + 11, x + w, y + 11.5f, 0f, Glass.alpha(ink, 0.08f));
            return;
        }
        // The hover highlight and the footer hint still follow the cursor, and only while it is
        // inside the pane rather than over the header or footer above and below it.
        boolean hov = my >= top && my <= bottom && inside(mx, my, x, y, x + w, y + h);
        float lit = ease(o, hov, 10f);
        if (lit > 0.01f) Glass.rect(ms, x, y + 1, x + w, y + h - 1, 7f, Glass.alpha(ink, 0.05f * lit));
        boolean unavailable = o.unavailableReason() != null;
        int labelColour = unavailable ? faint : text;
        font.drawString(ms, clip(o.label(), w * 0.55f, font), x + 6, y + 6, labelColour);
        if (!o.isDefault()) Glass.dot(ms, x + 2, y + 10, 1.4f, accent);
        if (hov) {
            hint = unavailable ? o.unavailableReason() : (o.tooltip() != null ? o.tooltip() : o.label());
        }
        // Hit rectangles come from where the row was drawn, clipped to the pane. They used to be
        // registered only while the cursor was inside the pane, which tied them to where the mouse
        // was on the previous frame: a click that arrived in the same frame as the move into the
        // pane found no rectangle and did nothing.
        hit("row", o, x, Math.max(y, top), x + w, Math.min(y + h, bottom));
        float right = x + w - 4;
        if (o instanceof Option.Toggle) {
            drawSwitch(ms, "opt:" + System.identityHashCode(o), right - 22, y + h / 2f, 22f, 12f, ((Option.Toggle) o).get());
        } else if (o instanceof Option.Num) {
            Option.Num n = (Option.Num) o;
            String value = editing == o ? editBuffer + (caret() ? "_" : "") : n.display();
            font.drawString(ms, value, right - font.getStringWidth(value), y + 6, accent);
            if (n.bounded()) {
                float sx = x + 6, sx2 = right, sy = y + 21;
                float f = (float) Math.max(0, Math.min(1, n.fraction()));
                float shownF = spring("sl:" + System.identityHashCode(o), f, 600f, 34f).value;
                Glass.rect(ms, sx, sy - 1.5f, sx2, sy + 1.5f, 1.5f, Glass.alpha(ink, 0.14f));
                Glass.rect(ms, sx, sy - 1.5f, sx + (sx2 - sx) * shownF, sy + 1.5f, 1.5f, accent);
                float kx = sx + (sx2 - sx) * shownF;
                float grab = ease("grab" + System.identityHashCode(o), slider == o, 14f);
                Glass.dot(ms, kx, sy, 4f + grab * 1.5f, 0xFFFFFFFF);
                hit("slider", o, sx - 3, Math.max(y + 15, top), sx2 + 3, Math.min(y + h, bottom));
            }
        } else if (o instanceof Option.Choice) {
            String value = ((Option.Choice) o).display();
            float vw = Math.min(w * 0.42f, font.getStringWidth(value) + 18);
            Glass.rect(ms, right - vw, y + 3, right, y + h - 3, 7f, Glass.alpha(accent, 0.22f), Glass.alpha(accent, 0.14f));
            font.drawString(ms, clip(value, vw - 16, font), right - vw + 7, y + 6, text);
            font.drawString(ms, "v", right - 9, y + 5, dim);
        } else if (o instanceof Option.Colour) {
            Option.Colour col = (Option.Colour) o;
            int argb = 0xFF000000 | col.get();
            Glass.rect(ms, right - 30, y + 4, right, y + 18, 5f, argb);
            Glass.rim(ms, right - 30, y + 4, right, y + 18, 5f, 0.8f, Glass.alpha(ink, 0.4f), Glass.alpha(ink, 0.15f));
            String hex = editing == o ? "#" + editBuffer + (caret() ? "_" : "") : "#" + col.hex();
            float hexW = FontRenderers.sf_bold_12.getStringWidth(hex);
            FontRenderers.sf_bold_12.drawString(ms, hex, right - 36 - hexW, y + 8, dim);
            // Only the text itself, so the rest of the row still opens the channel editor.
            hit("hex", o, right - 40 - hexW, Math.max(y + 2, top), right - 33, Math.min(y + 20, bottom));
            if (o == colourOpen) {
                String[] names = {"R", "G", "B"};
                int[] shifts = {16, 8, 0};
                int[] tints = {0xFFE5484D, 0xFF46A758, 0xFF3E63DD};
                for (int k = 0; k < 3; k++) {
                    float sy = y + 32 + k * 14;
                    FontRenderers.sf_bold_12.drawString(ms, names[k], x + 8, sy - 3, dim);
                    float sx = x + 20, sx2 = right - 30;
                    float f = col.component(shifts[k]) / 255f;
                    Glass.rect(ms, sx, sy - 1.5f, sx2, sy + 1.5f, 1.5f, Glass.alpha(ink, 0.14f));
                    Glass.rect(ms, sx, sy - 1.5f, sx + (sx2 - sx) * f, sy + 1.5f, 1.5f, tints[k]);
                    Glass.dot(ms, sx + (sx2 - sx) * f, sy, 3.5f, 0xFFFFFFFF);
                    String v = String.valueOf(col.component(shifts[k]));
                    FontRenderers.sf_bold_12.drawString(ms, v, right - FontRenderers.sf_bold_12.getStringWidth(v), sy - 3, dim);
                    hit("channel", new Object[]{o, shifts[k], sx, sx2}, sx - 3, Math.max(sy - 6, top), sx2 + 3, Math.min(sy + 6, bottom));
                }
            }
        } else if (o instanceof Option.Text) {
            String value = editing == o ? editBuffer + (caret() ? "_" : "") : ((Option.Text) o).get();
            float vw = w * 0.42f;
            Glass.rect(ms, right - vw, y + 3, right, y + h - 3, 6f, Glass.alpha(ink, editing == o ? 0.14f : 0.07f));
            font.drawString(ms, clip(value == null || value.isEmpty() ? "-" : value, vw - 10, font), right - vw + 5, y + 6, dim);
        } else if (o instanceof Option.TextList) {
            String value = t("entries", "{n} entries").replace("{n}", String.valueOf(((Option.TextList) o).get().length)) + "  >";
            font.drawString(ms, value, right - font.getStringWidth(value), y + 6, dim);
        } else if (o instanceof Option.Key) {
            String value = listening == o ? t("press_key", "press a key...") : ((Option.Key) o).display();
            float vw = font.getStringWidth(value) + 14;
            Glass.rect(ms, right - vw, y + 3, right, y + h - 3, 7f, Glass.alpha(listening == o ? accent : ink, listening == o ? 0.35f : 0.08f));
            font.drawString(ms, value, right - vw + 7, y + 6, text);
        } else if (o instanceof Option.Action) {
            String value = ((Option.Action) o).buttonText();
            float vw = font.getStringWidth(value) + 16;
            float press = ease("act" + System.identityHashCode(o), hov, 12f);
            Glass.rect(ms, right - vw, y + 3, right, y + h - 3, 7f, Glass.alpha(accent, 0.25f + 0.3f * press), Glass.alpha(accent, 0.15f + 0.2f * press));
            font.drawString(ms, value, right - vw + 8, y + 6, text);
        }
    }

    /** The open dropdown's values, less the ones its filter rules out. */
    private List<Enum<?>> dropValues() {
        List<Enum<?>> values = new ArrayList<Enum<?>>();
        if (dropdown == null) return values;
        String needle = dropFilter.toLowerCase(Locale.ROOT);
        for (Enum<?> e : dropdown.values()) {
            if (needle.isEmpty() || Option.Choice.prettify(e).toLowerCase(Locale.ROOT).contains(needle)) {
                values.add(e);
            }
        }
        return values;
    }

    private void dropdownOverlay(DrawContext context, MatrixStack ms, double mx, double my) {
        FontRenderer font = FontRenderers.sf_bold_mini;
        List<Enum<?>> values = dropValues();
        int visible = Math.min(10, Math.max(1, values.size()));
        float h = visible * 16 + (dropdown.values().length > 8 ? 22 : 6);
        float x = dropX, y = dropY, x2 = x + dropW;
        if (y + h > winY + winH - 4) y = Math.max(winY + 4, winY + winH - 4 - h);
        Glass.shadow(ms, x, y, x2, y + h, 10, 0x88000000);
        Glass.rect(ms, x, y, x2, y + h, 9f, Glass.alpha(tint, 0.98f));
        Glass.rim(ms, x, y, x2, y + h, 9f, 0.8f, Glass.alpha(ink, 0.25f), Glass.alpha(ink, 0.08f));
        hit("dropArea", null, x, y, x2, y + h);
        float listTop = y + 3;
        if (dropdown.values().length > 8) {
            Glass.rect(ms, x + 5, y + 4, x2 - 5, y + 18, 6f, Glass.alpha(ink, 0.08f));
            font.drawString(ms, dropFilter.isEmpty() ? t("filter", "type to filter") : dropFilter + (caret() ? "_" : ""), x + 10, y + 7, dropFilter.isEmpty() ? faint : text);
            listTop = y + 21;
        }
        float maxScroll = Math.max(0, values.size() * 16 - visible * 16);
        dropScroll = Math.max(0, Math.min(dropScroll, maxScroll));
        scissor(context, x, listTop, x2, listTop + visible * 16);
        Enum<?> current = dropdown.get();
        for (int i = 0; i < values.size(); i++) {
            float ry = listTop + i * 16 - dropScroll;
            if (ry + 16 < listTop || ry > listTop + visible * 16) continue;
            Enum<?> e = values.get(i);
            boolean hov = inside(mx, my, x + 3, ry, x2 - 3, ry + 16);
            if (e == current) Glass.pill(ms, x + 3, ry, x2 - 3, ry + 15, 6f, accent, ink, 1f);
            else if (hov) Glass.rect(ms, x + 3, ry, x2 - 3, ry + 15, 6f, Glass.alpha(ink, 0.08f));
            font.drawString(ms, clip(Option.Choice.prettify(e), dropW - 16, font), x + 9, ry + 4, e == current ? text : dim);
            hit("choose", e, x + 3, Math.max(ry, listTop), x2 - 3, Math.min(ry + 15, listTop + visible * 16));
        }
        context.disableScissor();
    }

    /**
     * A scissor box given in the window's own coordinates.
     *
     * <p>The window is drawn through a scale about its centre while it pops open, and the scissor
     * box is in screen pixels, so the same transform has to be applied by hand - otherwise the
     * panes clip against the wrong rectangle for the first few frames and the rows appear cut off.
     */
    private void scissor(DrawContext context, float x, float y, float x2, float y2) {
        float scale = Math.max(0.01f, open.value);
        float px = winX + winW / 2f, py = winY + winH / 2f;
        context.enableScissor(
                (int) Math.floor(px + (x - px) * scale),
                (int) Math.floor(py + (y - py) * scale),
                (int) Math.ceil(px + (x2 - px) * scale),
                (int) Math.ceil(py + (y2 - py) * scale));
    }

    private void scrollbar(MatrixStack ms, float x, float top, float bottom, float shown, float max) {
        if (max <= 0) return;
        float track = bottom - top;
        float thumb = Math.max(18f, track * track / (track + max));
        float along = (track - thumb) * (shown / max);
        Glass.rect(ms, x, top + along, x + 2.5f, top + along + thumb, 1.25f, Glass.alpha(ink, 0.3f));
    }

    private void footer(MatrixStack ms, int x, int y, int w) {
        String text = hint.isEmpty() ? t("hint", "Drag the title bar to move  ·  corner to resize  ·  type to search  ·  Esc closes") : hint;
        FontRenderers.sf_bold_12.drawString(ms, clip(text, w - 30, FontRenderers.sf_bold_12), x + 14, y + 3, faint);
    }

    private void grip(MatrixStack ms, int right, int bottom, double mx, double my) {
        float lit = ease("grip", resizing || inside(mx, my, right - 14, bottom - 14, right, bottom), 12f);
        for (int i = 0; i < 3; i++) {
            Glass.dot(ms, right - 5 - i * 3.5f, bottom - 5, 0.9f, Glass.alpha(ink, 0.25f + 0.5f * lit));
            if (i < 2) Glass.dot(ms, right - 5, bottom - 8.5f - i * 3.5f, 0.9f, Glass.alpha(ink, 0.25f + 0.5f * lit));
        }
        hit("grip", null, right - 14, bottom - 14, right, bottom);
    }

    // ---- input -----------------------------------------------------------------------------------

    private double toLayoutX(double mx) {
        float px = winX + winW / 2f;
        return px + (mx - px) / Math.max(0.01f, open.value);
    }

    private double toLayoutY(double my) {
        float py = winY + winH / 2f;
        return py + (my - py) / Math.max(0.01f, open.value);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double mx = toLayoutX(mouseX), my = toLayoutY(mouseY);
        Hit h = hitAt(mx, my);
        if (listening != null) {
            listening = null;
            return true;
        }
        if (editing != null && (h == null || h.payload != editing)) stopEditing(true);
        if (dropdown != null) {
            if (h != null && "choose".equals(h.kind)) {
                dropdown.set((Enum<?>) h.payload);
                ConfigSchema.save();
            }
            if (h == null || !"dropArea".equals(h.kind) || "choose".equals(h.kind)) {
                dropdown = null;
                dropFilter = "";
            }
            return true;
        }
        searchFocused = h != null && "search".equals(h.kind);
        if (h == null) return true;
        String kind = h.kind;
        if (button == 1) {
            // "Right-click resets", as the header says - including on a switch, a slider track, a
            // hex field or one of a colour's channels, which are all hit before the row is.
            Option target = null;
            if (h.payload instanceof Option) {
                target = (Option) h.payload;
            } else if ("channel".equals(kind)) {
                target = (Option) ((Object[]) h.payload)[0];
            }
            if (target != null) {
                stopEditing(false);
                target.reset();
                ConfigSchema.save();
                return true;
            }
        }
        if ("close".equals(kind)) {
            close();
        } else if ("classic".equals(kind)) {
            persist();
            ConfigSchema.save();
            mc.displayGuiScreen(new me.Padej_.soupapi.gui.SoupSettingsScreen());
        } else if ("move".equals(kind)) {
            dragging = true;
            dragDX = (int) (mx - winX);
            dragDY = (int) (my - winY);
        } else if ("grip".equals(kind)) {
            resizing = true;
        } else if ("group".equals(kind)) {
            group = (String) h.payload;
            page = grouped.get(group).get(0);
            search = "";
            listScroll = detailScroll = 0;
            colourOpen = null;
        } else if ("page".equals(kind)) {
            page = (OptionPage) h.payload;
            detailScroll = 0;
            colourOpen = null;
            for (String g : groupNames) if (grouped.get(g).contains(page) && search.isEmpty()) group = g;
        } else if ("toggle".equals(kind)) {
            ((Option.Toggle) h.payload).toggle();
            ConfigSchema.save();
        } else if ("slider".equals(kind)) {
            slider = (Option.Num) h.payload;
            sliderHit = h;
            dragSlider(mx);
        } else if ("channel".equals(kind)) {
            Object[] p = (Object[]) h.payload;
            channel(p, mx);
            sliderHit = h;
        } else if ("hex".equals(kind)) {
            startEditing((Option) h.payload, ((Option.Colour) h.payload).hex());
        } else if ("row".equals(kind)) {
            clickRow((Option) h.payload, h, mx, my);
        }
        return true;
    }

    private void clickRow(Option o, Hit h, double mx, double my) {
        if (o.unavailableReason() != null) return;
        if (o instanceof Option.Toggle) {
            ((Option.Toggle) o).toggle();
            ConfigSchema.save();
        } else if (o instanceof Option.Choice) {
            dropdown = (Option.Choice) o;
            dropFilter = "";
            dropScroll = 0;
            dropW = Math.min(200, h.x2 - h.x);
            dropX = h.x2 - dropW;
            dropY = h.y2;
            // Start the list at the current value.
            Enum<?>[] values = dropdown.values();
            for (int i = 0; i < values.length; i++) if (values[i] == dropdown.get()) dropScroll = Math.max(0, (i - 4) * 16);
        } else if (o instanceof Option.Colour) {
            colourOpen = colourOpen == o ? null : (Option.Colour) o;
        } else if (o instanceof Option.Num) {
            startEditing(o, ((Option.Num) o).display());
        } else if (o instanceof Option.Text) {
            startEditing(o, ((Option.Text) o).get());
        } else if (o instanceof Option.TextList) {
            persist();
            mc.displayGuiScreen(new StringListScreen(this, (Option.TextList) o));
        } else if (o instanceof Option.Key) {
            listening = (Option.Key) o;
        } else if (o instanceof Option.Action) {
            persist();
            ConfigSchema.save();
            ((Option.Action) o).run();
            ConfigSchema.save();
        }
    }

    private void dragSlider(double mx) {
        if (slider == null || sliderHit == null) return;
        float x0 = sliderHit.x + 3, x1 = sliderHit.x2 - 3;
        slider.setFraction(Math.max(0, Math.min(1, (mx - x0) / Math.max(1, x1 - x0))));
    }

    private void channel(Object[] p, double mx) {
        Option.Colour col = (Option.Colour) p[0];
        int shift = (Integer) p[1];
        float x0 = (Float) p[2], x1 = (Float) p[3];
        int v = (int) Math.round(Math.max(0, Math.min(1, (mx - x0) / Math.max(1, x1 - x0))) * 255);
        col.setComponent(shift, v);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        double mx = toLayoutX(mouseX), my = toLayoutY(mouseY);
        if (dragging) {
            winX = (int) (mx - dragDX);
            winY = (int) (my - dragDY);
        } else if (resizing) {
            winW = (int) Math.max(minW, mx - winX);
            winH = (int) Math.max(minH, my - winY);
        } else if (slider != null) {
            dragSlider(mx);
        } else if (sliderHit != null && "channel".equals(sliderHit.kind)) {
            channel((Object[]) sliderHit.payload, mx);
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (slider != null || sliderHit != null) ConfigSchema.save();
        dragging = resizing = false;
        slider = null;
        sliderHit = null;
        persist();
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        double mx = toLayoutX(mouseX), my = toLayoutY(mouseY);
        if (dropdown != null) {
            dropScroll -= (float) vertical * 16;
            return true;
        }
        Hit h = null;
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit c = hits.get(i);
            if (("listArea".equals(c.kind) || "detailArea".equals(c.kind)) && c.contains(mx, my)) {
                h = c;
                break;
            }
        }
        if (h == null) return true;
        if ("listArea".equals(h.kind)) listScroll = Math.max(0, Math.min(listMax, listScroll - (float) vertical * ROW));
        else detailScroll = Math.max(0, Math.min(detailMax, detailScroll - (float) vertical * ROW));
        return true;
    }

    /**
     * Keys.
     *
     * <p>Returns true only for keys this screen really handled. Everything else has to fall
     * through, because the character that belongs to the press arrives separately and is what
     * types into the search box, a value field or a dropdown's filter - a blanket "handled" here
     * is what left every field in the screen unable to receive a single character.
     */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // LWJGL 2 key codes: 1 escape, 14 backspace, 28 enter, 156 numpad enter.
        if (listening != null) {
            // Escape unbinds, anything else binds; the character of that key press is dropped so
            // it does not also land in the search box.
            listening.set(keyCode == 1 ? 0 : keyCode);
            listening = null;
            ConfigSchema.save();
            swallowNextChar();
            return true;
        }
        if (dropdown != null) {
            if (keyCode == 1) {
                dropdown = null;
                dropFilter = "";
                return true;
            }
            if (keyCode == 14) {
                if (!dropFilter.isEmpty()) dropFilter = dropFilter.substring(0, dropFilter.length() - 1);
                return true;
            }
            if (keyCode == 28 || keyCode == 156) {
                // Enter takes the first value the filter leaves, so a dropdown can be used from
                // the keyboard alone.
                List<Enum<?>> filtered = dropValues();
                if (!filtered.isEmpty()) {
                    dropdown.set(filtered.get(0));
                    ConfigSchema.save();
                }
                dropdown = null;
                dropFilter = "";
                return true;
            }
            return false;
        }
        if (editing != null) {
            if (keyCode == 14) {
                if (!editBuffer.isEmpty()) editBuffer = editBuffer.substring(0, editBuffer.length() - 1);
                return true;
            }
            if (keyCode == 28 || keyCode == 156) {
                stopEditing(true);
                return true;
            }
            if (keyCode == 1) {
                stopEditing(false);
                return true;
            }
            return false;
        }
        if (keyCode == 14) {
            if (!search.isEmpty()) search = search.substring(0, search.length() - 1);
            return true;
        }
        if (keyCode == 1 && !search.isEmpty()) {
            search = "";
            searchFocused = false;
            return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (chr < 32 || chr == 127) return false;
        if (dropdown != null) {
            dropFilter += chr;
            dropScroll = 0;
            return true;
        }
        if (editing instanceof Option.Colour) {
            int digits = ((Option.Colour) editing).allowAlpha() ? 8 : 6;
            if (Character.digit(chr, 16) >= 0 && editBuffer.length() < digits) editBuffer += Character.toUpperCase(chr);
            return true;
        }
        if (editing instanceof Option.Num) {
            if (Character.isDigit(chr) || chr == '-' || chr == '.') editBuffer += chr;
            return true;
        }
        if (editing instanceof Option.Text) {
            editBuffer += chr;
            return true;
        }
        // Typing anywhere searches, the way Atlas does.
        searchFocused = true;
        search += chr;
        listScroll = detailScroll = 0;
        if (!search.trim().isEmpty()) {
            for (OptionPage p : pages) {
                if (!p.search(search).isEmpty()) {
                    page = p;
                    break;
                }
            }
        }
        return true;
    }

    private void startEditing(Option o, String initial) {
        editing = o;
        editBuffer = initial == null ? "" : initial;
    }

    private void stopEditing(boolean commit) {
        Option o = editing;
        String buffer = editBuffer;
        editing = null;
        editBuffer = "";
        if (o == null || !commit) return;
        try {
            if (o instanceof Option.Colour && !buffer.isEmpty()) {
                Option.Colour col = (Option.Colour) o;
                long parsed = Long.parseLong(buffer, 16);
                // Masking off the alpha byte of a colour that has one makes it fully transparent,
                // which reads as the setting having been turned off.
                col.set(col.allowAlpha() ? (int) parsed : (int) (parsed & 0xFFFFFF));
            } else if (o instanceof Option.Num && !buffer.isEmpty() && !buffer.equals("-")) {
                ((Option.Num) o).set(Double.parseDouble(buffer));
            } else if (o instanceof Option.Text) {
                ((Option.Text) o).set(buffer);
            }
        } catch (NumberFormatException ignored) {
            // The field reverts.
        }
        ConfigSchema.save();
    }

    @Override
    public void close() {
        stopEditing(true);
        persist();
        ConfigSchema.save();
        super.close();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    // ---- helpers ---------------------------------------------------------------------------------

    private static boolean inside(double mx, double my, float x, float y, float x2, float y2) {
        return mx >= x && mx <= x2 && my >= y && my <= y2;
    }

    /** The screen's own text, from the lang files when they have it. */
    private static String t(String key, String fallback) {
        return ConfigSchema.translate("soupapi.atlas." + key, fallback);
    }

    private static String groupLabel(String group) {
        return group == null ? "" : t("group." + group.toLowerCase(Locale.ROOT), group);
    }

    private static boolean caret() {
        return (System.currentTimeMillis() / 500) % 2 == 0;
    }

    private static String clip(String text, float max, FontRenderer font) {
        if (text == null) return "";
        if (font.getStringWidth(text) <= max) return text;
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            if (font.getStringWidth(out.toString() + text.charAt(i) + "..") > max) break;
            out.append(text.charAt(i));
        }
        return out + "..";
    }

    /** The settings screen the configured style asks for. */
    public static SoupScreen create() {
        return ConfigurableModule.CONFIG.guiStyle == Style.CLASSIC
                ? new me.Padej_.soupapi.gui.SoupSettingsScreen() : new AtlasScreen();
    }
}
