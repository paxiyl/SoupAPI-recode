package me.Padej_.soupapi.compat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.util.Window;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Replacement for {@code net.minecraft.client.gui.DrawContext}.
 *
 * <p>1.8.9 draws GUIs through static {@code DrawableHelper} calls with no context object and no
 * matrix stack, so this bundles the two things 1.21.4's context carried: the {@link MatrixStack}
 * (read by the ported code 146 times) and the scaled window size.
 *
 * <p>Vanilla calls reached from here — item models, the item count/durability overlay — render
 * through fixed-function GL, so the CPU-side matrix stack is pushed onto the GL stack around them
 * via {@link MatrixStack#applyToGl()}. Geometry the mod draws itself instead transforms its
 * vertices on the CPU and leaves GL's matrices alone.
 */
public final class DrawContext {

    private final MinecraftClient client;
    private final MatrixStack matrices;
    private final Window window;

    public DrawContext(MinecraftClient client, MatrixStack matrices) {
        this.client = client;
        this.matrices = matrices;
        this.window = new Window(client);
    }

    public DrawContext(MinecraftClient client) {
        this(client, new MatrixStack());
    }

    public MatrixStack getMatrices() {
        return matrices;
    }

    public int getScaledWindowWidth() {
        return (int) window.getScaledWidth();
    }

    public int getScaledWindowHeight() {
        return (int) window.getScaledHeight();
    }

    // ---- primitives ------------------------------------------------------------------------

    /** Axis-aligned filled rectangle in ARGB. */
    public void fill(int x1, int y1, int x2, int y2, int color) {
        // Route through the mod's own renderer so the matrix stack is honoured; DrawableHelper.fill
        // ignores it entirely.
        Render2DBridge.fill(matrices, x1, y1, x2, y2, color);
    }

    public void fill(int x1, int y1, int x2, int y2, int z, int color) {
        fill(x1, y1, x2, y2, color);
    }

    /** One-pixel outline, matching 1.21.4's {@code drawBorder}. */
    public void drawBorder(int x, int y, int width, int height, int color) {
        fill(x, y, x + width, y + 1, color);
        fill(x, y + height - 1, x + width, y + height, color);
        fill(x, y + 1, x + 1, y + height - 1, color);
        fill(x + width - 1, y + 1, x + width, y + height - 1, color);
    }

    // ---- text ------------------------------------------------------------------------------

    public void drawText(TextRenderer renderer, String text, int x, int y, int color, boolean shadow) {
        matrices.applyToGl();
        try {
            if (shadow) {
                renderer.drawWithShadow(text, x, y, color);
            } else {
                renderer.draw(text, x, y, color);
            }
        } finally {
            matrices.undoGl();
        }
    }

    public void drawText(TextRenderer renderer, Text text, int x, int y, int color, boolean shadow) {
        drawText(renderer, Texts.toPlainString(text), x, y, color, shadow);
    }

    public void drawTextWithShadow(TextRenderer renderer, String text, int x, int y, int color) {
        drawText(renderer, text, x, y, color, true);
    }

    public void drawTextWithShadow(TextRenderer renderer, Text text, int x, int y, int color) {
        drawText(renderer, Texts.toPlainString(text), x, y, color, true);
    }

    public void drawCenteredTextWithShadow(TextRenderer renderer, String text, int centerX, int y, int color) {
        drawText(renderer, text, centerX - renderer.getStringWidth(text) / 2, y, color, true);
    }

    // ---- textures --------------------------------------------------------------------------

    public void drawTexture(Identifier texture, int x, int y, float u, float v,
                            int width, int height, int textureWidth, int textureHeight) {
        RenderSystem.setShader(ShaderKey.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, texture);
        Render2DBridge.texturedQuad(matrices, x, y, width, height, u, v,
                width, height, textureWidth, textureHeight);
    }

    public void drawTexture(Identifier texture, int x, int y, int width, int height,
                            float u, float v, int regionWidth, int regionHeight,
                            int textureWidth, int textureHeight) {
        RenderSystem.setShader(ShaderKey.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, texture);
        Render2DBridge.texturedQuad(matrices, x, y, width, height, u, v,
                regionWidth, regionHeight, textureWidth, textureHeight);
    }

    /** Stretches a full texture over the given rectangle. */
    public void drawSpriteStretched(Identifier texture, int x, int y, int width, int height) {
        RenderSystem.setShader(ShaderKey.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, texture);
        Render2DBridge.texturedQuad(matrices, x, y, width, height, 0f, 0f, 1, 1, 1, 1);
    }

    // ---- items -----------------------------------------------------------------------------

    public void drawItem(ItemStack stack, int x, int y) {
        if (ItemStacks.isEmpty(stack)) return;
        matrices.applyToGl();
        try {
            DiffuseLighting.enable();
            client.getItemRenderer().zOffset = 0f;
            client.getItemRenderer().renderInGuiWithOverrides(stack, x, y);
            DiffuseLighting.disable();
        } finally {
            matrices.undoGl();
        }
    }

    /** The stack count and durability bar drawn over an item. */
    public void drawStackOverlay(TextRenderer renderer, ItemStack stack, int x, int y) {
        if (ItemStacks.isEmpty(stack)) return;
        matrices.applyToGl();
        try {
            client.getItemRenderer().renderGuiItemOverlay(renderer, stack, x, y);
        } finally {
            matrices.undoGl();
        }
    }

    public void drawStackOverlay(TextRenderer renderer, ItemStack stack, int x, int y, String countLabel) {
        if (ItemStacks.isEmpty(stack)) return;
        matrices.applyToGl();
        try {
            client.getItemRenderer().renderGuiItemOverlay(renderer, stack, x, y, countLabel);
        } finally {
            matrices.undoGl();
        }
    }

    // ---- scissor ---------------------------------------------------------------------------

    public void enableScissor(int x1, int y1, int x2, int y2) {
        GlCompat.enableScissor(x1, y1, x2, y2);
    }

    public void disableScissor() {
        GlCompat.disableScissor();
    }

    /** Kept so ported code that only needed vanilla's helper still compiles. */
    public static void vanillaFill(int x1, int y1, int x2, int y2, int color) {
        DrawableHelper.fill(x1, y1, x2, y2, color);
    }
}
