package me.Padej_.soupapi.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.ResourceLocation;

/**
 * Replacement for {@code net.minecraft.client.gui.DrawContext}.
 *
 * <p>1.8.9 draws GUIs through static {@code Gui} calls with no context object and no
 * matrix stack, so this bundles the two things 1.21.4's context carried: the {@link MatrixStack}
 * (read by the ported code 146 times) and the scaled window size.
 *
 * <p>Vanilla calls reached from here — item models, the item count/durability overlay — render
 * through fixed-function GL, so the CPU-side matrix stack is pushed onto the GL stack around them
 * via {@link MatrixStack#applyToGl()}. Geometry the mod draws itself instead transforms its
 * vertices on the CPU and leaves GL's matrices alone.
 */
public final class DrawContext {

    private final Minecraft client;
    private final MatrixStack matrices;
    private final ScaledResolution window;

    public DrawContext(Minecraft client, MatrixStack matrices) {
        this.client = client;
        this.matrices = matrices;
        this.window = new ScaledResolution(client);
    }

    public DrawContext(Minecraft client) {
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
        // Route through the mod's own renderer so the matrix stack is honoured; Gui.fill
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

    public void drawText(FontRenderer renderer, String text, int x, int y, int color, boolean shadow) {
        matrices.applyToGl();
        try {
            if (shadow) {
                renderer.drawStringWithShadow(text, x, y, color);
            } else {
                renderer.drawString(text, x, y, color);
            }
        } finally {
            matrices.undoGl();
        }
    }

    public void drawText(FontRenderer renderer, IChatComponent text, int x, int y, int color, boolean shadow) {
        drawText(renderer, Texts.toPlainString(text), x, y, color, shadow);
    }

    public void drawTextWithShadow(FontRenderer renderer, String text, int x, int y, int color) {
        drawText(renderer, text, x, y, color, true);
    }

    public void drawTextWithShadow(FontRenderer renderer, IChatComponent text, int x, int y, int color) {
        drawText(renderer, Texts.toPlainString(text), x, y, color, true);
    }

    public void drawCenteredTextWithShadow(FontRenderer renderer, String text, int centerX, int y, int color) {
        drawText(renderer, text, centerX - renderer.getStringWidth(text) / 2, y, color, true);
    }

    // ---- textures --------------------------------------------------------------------------

    public void drawTexture(ResourceLocation texture, int x, int y, float u, float v,
                            int width, int height, int textureWidth, int textureHeight) {
        RenderSystem.setShader(ShaderKey.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, texture);
        Render2DBridge.texturedQuad(matrices, x, y, width, height, u, v,
                width, height, textureWidth, textureHeight);
    }

    public void drawTexture(ResourceLocation texture, int x, int y, int width, int height,
                            float u, float v, int regionWidth, int regionHeight,
                            int textureWidth, int textureHeight) {
        RenderSystem.setShader(ShaderKey.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, texture);
        Render2DBridge.texturedQuad(matrices, x, y, width, height, u, v,
                regionWidth, regionHeight, textureWidth, textureHeight);
    }

    /** Stretches a full texture over the given rectangle. */
    public void drawSpriteStretched(ResourceLocation texture, int x, int y, int width, int height) {
        RenderSystem.setShader(ShaderKey.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, texture);
        Render2DBridge.texturedQuad(matrices, x, y, width, height, 0f, 0f, 1, 1, 1, 1);
    }

    // ---- items -----------------------------------------------------------------------------

    public void drawItem(ItemStack stack, int x, int y) {
        if (ItemStacks.isEmpty(stack)) return;
        matrices.applyToGl();
        try {
            RenderHelper.enableGUIStandardItemLighting();
            client.getRenderItem().zLevel = 0f;
            client.getRenderItem().renderItemAndEffectIntoGUI(stack, x, y);
            RenderHelper.disableStandardItemLighting();
        } finally {
            matrices.undoGl();
        }
    }

    /** The stack count and durability bar drawn over an item. */
    public void drawStackOverlay(FontRenderer renderer, ItemStack stack, int x, int y) {
        if (ItemStacks.isEmpty(stack)) return;
        matrices.applyToGl();
        try {
            client.getRenderItem().renderItemOverlays(renderer, stack, x, y);
        } finally {
            matrices.undoGl();
        }
    }

    public void drawStackOverlay(FontRenderer renderer, ItemStack stack, int x, int y, String countLabel) {
        if (ItemStacks.isEmpty(stack)) return;
        matrices.applyToGl();
        try {
            client.getRenderItem().renderItemOverlayIntoGUI(renderer, stack, x, y, countLabel);
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
        Gui.drawRect(x1, y1, x2, y2, color);
    }
}
