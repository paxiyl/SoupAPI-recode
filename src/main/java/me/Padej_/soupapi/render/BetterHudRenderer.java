package me.Padej_.soupapi.render;

import me.Padej_.soupapi.compat.DrawContext;
import me.Padej_.soupapi.compat.MatrixStack;
import me.Padej_.soupapi.compat.RenderSystem;
import me.Padej_.soupapi.config.ConfigurableModule;
import me.Padej_.soupapi.font.FontRenderers;
import me.Padej_.soupapi.modules.BetterHudStyles;
import me.Padej_.soupapi.utils.Palette;
import me.Padej_.soupapi.utils.TexturesManager;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;

import java.awt.Color;

/**
 * Draws the mod's own hotbar and status bars.
 *
 * <p>This is the other half of {@code BetterHudStyles}. That class already cancelled vanilla's
 * hotbar, health, armour, food, air and experience elements whenever the option was on — but
 * nothing drew replacements, so turning "Better Hotbar" on simply deleted the HUD. The 1.21.4
 * version drew these from a mixin on {@code InGameHud}; the equivalent here is a renderer called
 * from the overlay event, using the mod's own palette, fonts and the {@code gui/} icons that were
 * clearly cut for exactly this (heart, ham, shield, bubble).
 *
 * <p>Both styles are honoured: {@code GLOW} adds the blurred shadow the rest of the mod's HUD uses,
 * {@code SIMPLE} is the flat panel. {@code hudBetterHotbarSmooth} eases the selected-slot highlight
 * between slots instead of snapping, and {@code hudBetterHotbarShowArmor} adds the armour bar.
 */
public final class BetterHudRenderer extends ConfigurableModule {

    private static final int SLOT = 20;
    private static final int SLOTS = 9;
    private static final int BAR_W = 70;
    private static final int BAR_H = 4;

    /** Eased position of the selection highlight, in slots. */
    private static float highlight = -1f;

    private BetterHudRenderer() {
    }

    public static void render(DrawContext context) {
        if (!CONFIG.hudBetterHotbarEnabled) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.gameSettings.hideGUI) return;
        if (mc.playerController != null && mc.playerController.isSpectator()) return;

        EntityPlayer player = mc.thePlayer;
        MatrixStack ms = context.getMatrices();
        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();

        boolean glow = CONFIG.hudBetterHotbarStyle == BetterHudStyles.HotbarStyle.GLOW;
        Color accent = Palette.getColor(0f);
        Color accent2 = Palette.getColor(0.5f);
        Color back = Palette.getBackColor();
        Color panel = new Color(back.getRed(), back.getGreen(), back.getBlue(),
                Math.round(255 * (CONFIG.backAlpha / 100f)));

        int barsWidth = SLOT * SLOTS;
        int x = (width - barsWidth) / 2;
        int y = height - SLOT - 4;

        // ---- hotbar ----
        if (glow) {
            Render2D.drawGradientBlurredShadow1(ms, x - 2, y - 2, barsWidth + 4, SLOT + 4, 8,
                    accent, accent2, accent2, accent);
        }
        Render2D.drawRound(ms, x - 2, y - 2, barsWidth + 4, SLOT + 4, 4, panel);

        // Selected slot, eased when smooth scrolling is on.
        int selected = player.inventory.currentItem;
        if (highlight < 0f || !CONFIG.hudBetterHotbarSmooth) {
            highlight = selected;
        } else {
            // Shortest way round the nine slots, so wrapping from 8 to 0 slides one step.
            float delta = selected - highlight;
            if (delta > SLOTS / 2f) delta -= SLOTS;
            if (delta < -SLOTS / 2f) delta += SLOTS;
            highlight += delta * 0.35f;
            if (highlight < 0f) highlight += SLOTS;
            if (highlight >= SLOTS) highlight -= SLOTS;
        }
        float hx = x + highlight * SLOT;
        Render2D.drawRound(ms, hx, y - 2, SLOT, SLOT + 4, 4,
                new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 110));

        for (int slot = 0; slot < SLOTS; slot++) {
            ItemStack stack = player.inventory.mainInventory[slot];
            if (stack == null) continue;
            int ix = x + slot * SLOT + 2;
            int iy = y + 1;
            context.drawItem(stack, ix, iy);
            context.drawStackOverlay(mc.fontRendererObj, stack, ix, iy);
        }

        // ---- status bars ----
        RenderSystem.restoreDefaults();
        int barY = y - 10;
        int leftX = (width / 2) - BAR_W - 6;
        int rightX = (width / 2) + 6;

        float health = MathHelper.clamp_float(player.getHealth() / player.getMaxHealth(), 0f, 1f);
        drawBar(context, ms, leftX, barY, health, TexturesManager.GUI_HEART, panel, accent, accent2, glow);

        float food = MathHelper.clamp_float(player.getFoodStats().getFoodLevel() / 20f, 0f, 1f);
        drawBar(context, ms, rightX, barY, food, TexturesManager.GUI_HAM, panel, accent, accent2, glow);

        int row = 0;
        if (CONFIG.hudBetterHotbarShowArmor && player.getTotalArmorValue() > 0) {
            row++;
            float armour = MathHelper.clamp_float(player.getTotalArmorValue() / 20f, 0f, 1f);
            drawBar(context, ms, leftX, barY - row * 8, armour, TexturesManager.GUI_SHIELD,
                    panel, accent, accent2, glow);
        }
        if (player.isInsideOfMaterial(net.minecraft.block.material.Material.water)
                || player.getAir() < 300) {
            float air = MathHelper.clamp_float(player.getAir() / 300f, 0f, 1f);
            drawBar(context, ms, rightX, barY - 8, air, TexturesManager.GUI_BUBBLE,
                    panel, accent, accent2, glow);
        }

        // ---- experience ----
        if (player.experienceLevel > 0 || player.experience > 0f) {
            int xpW = barsWidth;
            int xpY = y - 16 - (row * 8);
            Render2D.drawRound(ms, x, xpY, xpW, 3, 1.5f, new Color(0, 0, 0, 120));
            Render2D.renderRoundedGradientRect(ms, accent, accent2, accent2, accent,
                    x, xpY, Math.max(1f, xpW * MathHelper.clamp_float(player.experience, 0f, 1f)),
                    3, 1.5f);
            if (player.experienceLevel > 0) {
                String level = String.valueOf(player.experienceLevel);
                FontRenderers.sf_bold_mini.drawString(ms, level,
                        width / 2f - FontRenderers.sf_bold_mini.getStringWidth(level) / 2f,
                        xpY - 9, Palette.getTextColor());
            }
        }

        RenderSystem.restoreDefaults();
    }

    private static void drawBar(DrawContext context, MatrixStack ms, int x, int y, float fraction,
                                net.minecraft.util.ResourceLocation icon, Color panel,
                                Color accent, Color accent2, boolean glow) {
        if (glow) {
            Render2D.drawGradientBlurredShadow1(ms, x + 10, y, BAR_W, BAR_H, 6,
                    accent, accent2, accent2, accent);
        }
        Render2D.drawRound(ms, x + 10, y, BAR_W, BAR_H, BAR_H / 2f, panel);
        Render2D.renderRoundedGradientRect(ms, accent, accent2, accent2, accent,
                x + 10, y, Math.max(1f, BAR_W * fraction), BAR_H, BAR_H / 2f);

        // The icons are white-on-alpha, so they take the palette colour directly.
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(accent.getRed() / 255f, accent.getGreen() / 255f,
                accent.getBlue() / 255f, 1f);
        context.drawTexture(icon, x, y - 2, 8, 8, 0, 0, 512, 512, 512, 512);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }
}
