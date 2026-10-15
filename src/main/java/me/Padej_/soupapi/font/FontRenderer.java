package me.Padej_.soupapi.font;

import me.Padej_.soupapi.compat.ShaderKey;
import me.Padej_.soupapi.compat.Windows;

import me.Padej_.soupapi.compat.RenderSystem;
import me.Padej_.soupapi.config.ConfigurableModule;
import me.Padej_.soupapi.render.Render2D;
import net.minecraft.client.Minecraft;
import me.Padej_.soupapi.compat.BufferBuilder;
import me.Padej_.soupapi.compat.BufferRenderer;
import me.Padej_.soupapi.compat.Tessellator;
import me.Padej_.soupapi.compat.VertexFormat;
import me.Padej_.soupapi.compat.VertexFormats;
import me.Padej_.soupapi.compat.VertexConsumer;
import me.Padej_.soupapi.compat.VertexConsumerProvider;
import me.Padej_.soupapi.compat.RenderLayer;
import me.Padej_.soupapi.compat.Camera;
import me.Padej_.soupapi.compat.RenderTickCounter;
import me.Padej_.soupapi.compat.MatrixStack;
import net.minecraft.util.ResourceLocation;
import me.Padej_.soupapi.compat.Mat4;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.io.Closeable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class FontRenderer extends ConfigurableModule implements Closeable {
    private static final Minecraft mc = Minecraft.getMinecraft();
    /** Legacy section-sign colour codes, keyed by the upper-case code character. */
    private static final Map<Character, Integer> colorCodes = new HashMap<Character, Integer>();

    static {
        colorCodes.put('0', 0x000000); colorCodes.put('1', 0x0000AA);
        colorCodes.put('2', 0x00AA00); colorCodes.put('3', 0x00AAAA);
        colorCodes.put('4', 0xAA0000); colorCodes.put('5', 0xAA00AA);
        colorCodes.put('6', 0xFFAA00); colorCodes.put('7', 0xAAAAAA);
        colorCodes.put('8', 0x555555); colorCodes.put('9', 0x5555FF);
        colorCodes.put('A', 0x55FF55); colorCodes.put('B', 0x55FFFF);
        colorCodes.put('C', 0xFF5555); colorCodes.put('D', 0xFF55FF);
        colorCodes.put('E', 0xFFFF55); colorCodes.put('F', 0xFFFFFF);
    }

    private static final ExecutorService ASYNC_WORKER = Executors.newCachedThreadPool();
    private final Map<ResourceLocation, List<DrawEntry>> GLYPH_PAGE_CACHE = new LinkedHashMap<ResourceLocation, List<DrawEntry>>();
    private final float originalSize;
    private final List<GlyphMap> maps = new ArrayList<GlyphMap>();
    private final Map<Character, Glyph> allGlyphs = new HashMap<Character, Glyph>();
    private final int charsPerPage;
    private final int padding;
    private final String prebakeGlyphs;
    private int scaleMul = 0;
    private Font font;
    private int previousGameScale = -1;
    private Future<Void> prebakeGlyphsFuture;
    private boolean initialized;

    /**
     * When true, drawing and measuring are delegated to vanilla's {@code FontRenderer} instead of
     * an AWT-rasterised atlas.
     *
     * <p>1.8.9 needs {@code BufferedImage} and {@code ImageIO} to boot at all, so those are always
     * present — but {@code java.awt.Font} glyph rasterisation goes through libfontmanager/freetype,
     * which is a separate native library and is not reliably functional in PojavLauncher's JREs.
     * Rather than lose all text, the renderer falls back to the bitmap font vanilla already has.
     * See {@link me.Padej_.soupapi.compat.Caps#canRasteriseAwtFonts()}.
     */
    private final boolean useVanillaFallback;

    public FontRenderer(Font font, float sizePx, int charactersPerPage, int paddingBetweenCharacters, String prebakeCharacters) {
        this.originalSize = sizePx;
        this.charsPerPage = charactersPerPage;
        this.padding = paddingBetweenCharacters;
        this.prebakeGlyphs = prebakeCharacters;
        this.useVanillaFallback = false;
        init(font, sizePx);
    }

    /** Constructs a renderer that delegates to vanilla's font; see {@link #useVanillaFallback}. */
    private FontRenderer(float sizePx) {
        this.originalSize = sizePx;
        this.charsPerPage = 0;
        this.padding = 0;
        this.prebakeGlyphs = null;
        this.useVanillaFallback = true;
        this.initialized = true;
        this.scaleMul = 1;
    }

    /**
     * A renderer backed by vanilla's bitmap font, scaled to approximately {@code sizePx} tall.
     * Used when AWT cannot rasterise glyphs.
     */
    public static FontRenderer vanillaBacked(float sizePx) {
        return new FontRenderer(sizePx);
    }

    /** Vanilla's font is 9px tall; scale it towards this renderer's requested size. */
    private float fallbackScale() {
        return originalSize / 9f;
    }

    public FontRenderer(Font font, float sizePx) {
        this(font, sizePx, 256, 5, null);
    }

    private static int floorNearestMulN(int x, int n) {
        return n * (int) Math.floor((double) x / (double) n);
    }

    public static String stripControlCodes(String text) {
        char[] chars = text.toCharArray();
        StringBuilder f = new StringBuilder();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (c == '§') {
                i++;
                continue;
            }
            f.append(c);
        }
        return f.toString();
    }

    private void sizeCheck() {
        int gs = (int) Windows.scaleFactor();
        if (gs != this.previousGameScale) {
            close();
            init(this.font, this.originalSize);
        }
    }

    private void init(Font font, float sizePx) {
        if (initialized) throw new IllegalStateException("Double call to init()");
        initialized = true;
        this.previousGameScale = (int) Windows.scaleFactor();
        this.scaleMul = this.previousGameScale;
        this.font = font.deriveFont(sizePx * this.scaleMul);
        if (prebakeGlyphs != null && !prebakeGlyphs.isEmpty()) {
            prebakeGlyphsFuture = this.prebake();
        }
    }

    private Future<Void> prebake() {
        return ASYNC_WORKER.submit(() -> {
            for (char c : prebakeGlyphs.toCharArray()) {
                if (Thread.interrupted()) break;
                locateGlyph1(c);
            }
            return null;
        });
    }

    private GlyphMap generateMap(char from, char to) {
        GlyphMap gm = new GlyphMap(from, to, this.font, randomIdentifier(), padding);
        maps.add(gm);
        return gm;
    }

    private Glyph locateGlyph0(char glyph) {
        for (GlyphMap map : maps) {
            if (map.contains(glyph)) {
                return map.getGlyph(glyph);
            }
        }
        int base = floorNearestMulN(glyph, charsPerPage);
        GlyphMap glyphMap = generateMap((char) base, (char) (base + charsPerPage));
        return glyphMap.getGlyph(glyph);
    }

    private Glyph locateGlyph1(char glyph) {
        Character key = Character.valueOf(glyph);
        Glyph cached = allGlyphs.get(key);
        if (cached == null) {
            cached = locateGlyph0(glyph);
            if (cached != null) {
                allGlyphs.put(key, cached);
            }
        }
        return cached;
    }

    public void drawString(MatrixStack stack, String s, double x, double y, int color) {
        float r = ((color >> 16) & 0xff) / 255f;
        float g = ((color >> 8) & 0xff) / 255f;
        float b = ((color) & 0xff) / 255f;
        float a = ((color >> 24) & 0xff) / 255f;
        drawString(stack, s, (float) x, (float) y, r, g, b, a);
    }

    public void drawString(MatrixStack stack, String s, double x, double y, Color color) {
        drawString(stack, s, (float) x, (float) y, color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha());
    }

    public void drawString(MatrixStack stack, String s, float x, float y, float r, float g, float b, float a) {
        drawString(stack, s, x, y, r, g, b, a, false, 0);
    }

    public void drawString(MatrixStack stack, String s, float x, float y, float r, float g, float b, float a, boolean gradient, int offset) {
        if (useVanillaFallback) {
            drawStringVanilla(stack, s, x, y, r, g, b, a, gradient);
            return;
        }
        if (prebakeGlyphsFuture != null && !prebakeGlyphsFuture.isDone()) {
            try {
                prebakeGlyphsFuture.get();
            } catch (InterruptedException | ExecutionException ignored) {
            }
        }

        sizeCheck();
        float r2 = r, g2 = g, b2 = b;
        stack.push();
        y -= 3f;
        stack.translate(roundToDecimal(x, 1), roundToDecimal(y, 1), 0);
        stack.scale(1f / this.scaleMul, 1f / this.scaleMul, 1f);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        RenderSystem.setShader(ShaderKey.POSITION_TEX_COLOR);
        BufferBuilder bb;
        Mat4 mat = stack.peek().getPositionMatrix();
        char[] chars = s.toCharArray();
        float xOffset = 0;
        float yOffset = 0;
        boolean inSel = false;
        int lineStart = 0;
        synchronized (GLYPH_PAGE_CACHE) {
            for (int i = 0; i < chars.length; i++) {
                char c = chars[i];
                if (inSel) {
                    inSel = false;
                    char c1 = Character.toUpperCase(c);
                    if (colorCodes.containsKey(c1)) {
                        int ii = colorCodes.get(c1).intValue();
                        int[] col = RGBIntToRGB(ii);
                        r2 = col[0] / 255f;
                        g2 = col[1] / 255f;
                        b2 = col[2] / 255f;
                    } else if (c1 == 'R') {
                        r2 = r;
                        g2 = g;
                        b2 = b;
                    }
                    continue;
                }

                if (gradient) {
                    Color color = new Color(CONFIG.c1);
                    r2 = color.getRed() / 255f;
                    g2 = color.getGreen() / 255f;
                    b2 = color.getBlue() / 255f;
                    a = color.getAlpha() / 255f;
                }

                if (c == '§') {
                    inSel = true;
                    continue;
                } else if (c == '\n') {
                    yOffset += getStringHeight(s.substring(lineStart, i)) * scaleMul;
                    xOffset = 0;
                    lineStart = i + 1;
                    continue;
                }
                Glyph glyph = locateGlyph1(c);
                if (glyph != null) {
                    if (glyph.value() != ' ') {
                        ResourceLocation i1 = glyph.owner().bindToTexture;
                        DrawEntry entry = new DrawEntry(xOffset, yOffset, r2, g2, b2, glyph);
                        List<DrawEntry> page = GLYPH_PAGE_CACHE.get(i1);
                        if (page == null) {
                            page = new ArrayList<DrawEntry>();
                            GLYPH_PAGE_CACHE.put(i1, page);
                        }
                        page.add(entry);
                    }
                    xOffset += glyph.width();
                }
            }
            for (ResourceLocation identifier : GLYPH_PAGE_CACHE.keySet()) {
                RenderSystem.setShaderTexture(0, identifier);
                List<DrawEntry> objects = GLYPH_PAGE_CACHE.get(identifier);

                bb = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

                for (DrawEntry object : objects) {
                    float xo = object.atX;
                    float yo = object.atY;
                    float cr = object.r;
                    float cg = object.g;
                    float cb = object.b;
                    Glyph glyph = object.toDraw;
                    GlyphMap owner = glyph.owner();
                    float w = glyph.width();
                    float h = glyph.height();
                    float u1 = (float) glyph.u() / owner.width;
                    float v1 = (float) glyph.v() / owner.height;
                    float u2 = (float) (glyph.u() + glyph.width()) / owner.width;
                    float v2 = (float) (glyph.v() + glyph.height()) / owner.height;

                    bb.vertex(mat, xo + 0, yo + h, 0).texture(u1, v2).color(cr, cg, cb, a);
                    bb.vertex(mat, xo + w, yo + h, 0).texture(u2, v2).color(cr, cg, cb, a);
                    bb.vertex(mat, xo + w, yo + 0, 0).texture(u2, v1).color(cr, cg, cb, a);
                    bb.vertex(mat, xo + 0, yo + 0, 0).texture(u1, v1).color(cr, cg, cb, a);
                }
                endBuilding(bb);
            }

            GLYPH_PAGE_CACHE.clear();
        }
        RenderSystem.disableBlend();
        stack.pop();
    }

    public void drawCenteredString(MatrixStack stack, String s, double x, double y, int color) {
        float r = ((color >> 16) & 0xff) / 255f;
        float g = ((color >> 8) & 0xff) / 255f;
        float b = ((color) & 0xff) / 255f;
        float a = ((color >> 24) & 0xff) / 255f;
        drawString(stack, s, (float) (x - getStringWidth(s) / 2f), (float) y, r, g, b, a);
    }

    public void drawCenteredString(MatrixStack stack, String s, double x, double y, Color color) {
        drawString(stack, s, (float) (x - getStringWidth(s) / 2f), (float) y, color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f);
    }

    public void drawCenteredString(MatrixStack stack, String s, float x, float y, float r, float g, float b, float a) {
        drawString(stack, s, x - getStringWidth(s) / 2f, y, r, g, b, a);
    }

    public float getStringWidth(String text) {
        if (useVanillaFallback) {
            float scale = fallbackScale();
            float widest = 0f;
            for (String line : stripControlCodes(text).split("\n", -1)) {
                widest = Math.max(widest, mc.fontRendererObj.getStringWidth(line) * scale);
            }
            return widest;
        }
        char[] c = stripControlCodes(text).toCharArray();
        float currentLine = 0;
        float maxPreviousLines = 0;
        for (char c1 : c) {
            if (c1 == '\n') {
                maxPreviousLines = Math.max(currentLine, maxPreviousLines);
                currentLine = 0;
                continue;
            }
            Glyph glyph = locateGlyph1(c1);
            currentLine += glyph == null ? 0 : (glyph.width() / (float) this.scaleMul);
        }
        return Math.max(currentLine, maxPreviousLines);
    }

    public float getStringHeight(String text) {
        if (useVanillaFallback) {
            int lines = Math.max(1, stripControlCodes(text).split("\n", -1).length);
            return mc.fontRendererObj.FONT_HEIGHT * fallbackScale() * lines;
        }
        char[] c = stripControlCodes(text).toCharArray();
        if (c.length == 0) {
            c = new char[]{' '};
        }
        float currentLine = 0;
        float previous = 0;
        for (char c1 : c) {
            if (c1 == '\n') {
                if (currentLine == 0) {
                    currentLine = (locateGlyph1(' ') == null ? 0 : (Objects.requireNonNull(locateGlyph1(' ')).height() / (float) this.scaleMul));
                }
                previous += currentLine;
                currentLine = 0;
                continue;
            }
            Glyph glyph = locateGlyph1(c1);
            currentLine = Math.max(

                    glyph == null ? 0 : (glyph.height() / (float) this.scaleMul)

                    , currentLine);
        }
        return currentLine + previous;
    }


    /**
     * Draws through vanilla's {@code FontRenderer}, scaled to match this renderer's size.
     *
     * <p>Colour-code handling differs slightly: vanilla parses the section-sign codes itself, so
     * the string is passed through intact rather than being split into per-run colours. Gradient
     * mode collapses to the first palette colour, which is the closest vanilla can express.
     */
    private void drawStringVanilla(MatrixStack stack, String s, float x, float y,
                                   float r, float g, float b, float a, boolean gradient) {
        int argb;
        if (gradient) {
            Color c = new Color(CONFIG.c1);
            argb = (Math.round(a * 255f) << 24) | (c.getRed() << 16) | (c.getGreen() << 8) | c.getBlue();
        } else {
            argb = (Math.round(a * 255f) << 24)
                    | (Math.round(r * 255f) << 16)
                    | (Math.round(g * 255f) << 8)
                    | Math.round(b * 255f);
        }

        float scale = fallbackScale();
        stack.push();
        stack.translate(x, y - 3f, 0f);
        stack.scale(scale, scale, 1f);
        stack.applyToGl();
        try {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            mc.fontRendererObj.draw(s, 0, 0, argb, false);
        } finally {
            stack.undoGl();
            stack.pop();
        }
    }

    @Override
    public void close() {
        if (useVanillaFallback) {
            return;
        }
        try {
            if (prebakeGlyphsFuture != null && !prebakeGlyphsFuture.isDone() && !prebakeGlyphsFuture.isCancelled()) {
                prebakeGlyphsFuture.cancel(true);
                prebakeGlyphsFuture.get();
                prebakeGlyphsFuture = null;
            }
            for (GlyphMap map : maps) {
                map.destroy();
            }
            maps.clear();
            allGlyphs.clear();
            initialized = false;
        } catch (Exception ignored) {
        }
    }

    public static ResourceLocation randomIdentifier() {
        return new ResourceLocation("soupapi", "temp/" + randomString());
    }

    private static String randomString() {
        Random random = new Random();
        StringBuilder sb = new StringBuilder(32);
        for (int i = 0; i < 32; i++) {
            sb.append((char) ('a' + random.nextInt(26)));
        }
        return sb.toString();
    }

    public static int [] RGBIntToRGB(int in) {
        int red = in >> 8 * 2 & 0xFF;
        int green = in >> 8 & 0xFF;
        int blue = in & 0xFF;
        return new int[]{red, green, blue};
    }

    public float getFontHeight(String str) {
        return getStringHeight(str);
    }

    public void drawGradientString(MatrixStack stack, String s, float x, float y, int offset) {
        drawString(stack, s, x, y, 255, 255, 255, 255, true, offset);
    }

    public void drawGradientCenteredString(MatrixStack matrices, String s, float x, float y, int i) {
        drawGradientString(matrices, s, x - getStringWidth(s) / 2f, y, i);
    }
static final class DrawEntry {
    private final float atX;
    private final float atY;
    private final float r;
    private final float g;
    private final float b;
    private final Glyph toDraw;

    DrawEntry(float atX, float atY, float r, float g, float b, Glyph toDraw) {
        this.atX = atX;
        this.atY = atY;
        this.r = r;
        this.g = g;
        this.b = b;
        this.toDraw = toDraw;
    }

    public float atX() {
        return atX;
    }

    public float atY() {
        return atY;
    }

    public float r() {
        return r;
    }

    public float g() {
        return g;
    }

    public float b() {
        return b;
    }

    public Glyph toDraw() {
        return toDraw;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DrawEntry)) return false;
        DrawEntry other = (DrawEntry) o;
        if (Float.compare(atX, other.atX) != 0) return false;
        if (Float.compare(atY, other.atY) != 0) return false;
        if (Float.compare(r, other.r) != 0) return false;
        if (Float.compare(g, other.g) != 0) return false;
        if (Float.compare(b, other.b) != 0) return false;
        if (toDraw == null ? other.toDraw != null : !toDraw.equals(other.toDraw)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + Float.floatToIntBits(atX);
        result = 31 * result + Float.floatToIntBits(atY);
        result = 31 * result + Float.floatToIntBits(r);
        result = 31 * result + Float.floatToIntBits(g);
        result = 31 * result + Float.floatToIntBits(b);
        result = 31 * result + (toDraw == null ? 0 : toDraw.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "DrawEntry[" + "atX=" + atX + ", " + "atY=" + atY + ", " + "r=" + r + ", " + "g=" + g + ", " + "b=" + b + ", " + "toDraw=" + toDraw + "]";
    }
    }

    private static double roundToDecimal(double n, int point) {
        if (point == 0) {
            return Math.floor(n);
        }
        double factor = Math.pow(10, point);
        return Math.round(n * factor) / factor;
    }

    /**
     * Flushes a glyph page.
     *
     * <p>1.21.4 used {@code endNullable()} to skip the draw when no vertices were written. 1.8.9's
     * tessellator has no such check, but the caller only reaches here with at least one glyph
     * queued, so the draw is unconditional.
     */
    private static void endBuilding(BufferBuilder bb) {
        BufferRenderer.drawWithGlobalProgram(bb);
        Render2D.endRender();
    }
}
