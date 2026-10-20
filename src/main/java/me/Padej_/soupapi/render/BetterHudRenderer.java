package me.Padej_.soupapi.render;

import me.Padej_.soupapi.compat.DrawContext;
import me.Padej_.soupapi.compat.MatrixStack;
import me.Padej_.soupapi.compat.RenderSystem;
import me.Padej_.soupapi.config.ConfigurableModule;
import me.Padej_.soupapi.font.FontRenderers;
import me.Padej_.soupapi.gui.atlas.Glass;
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

    /** Sprung position of the selection highlight, in slots, and its speed. */
    private static float highlight = -1f, highlightVelocity;
    private static long lastFrame;
    private static int lastSelected = -1;
    /** Seconds since the selection last changed, for the pop. */
    private static float popAge = 10f;

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
        long now = System.nanoTime();
        float dt = lastFrame == 0L ? 0.016f : Math.min(0.1f, (now - lastFrame) / 1.0E9f);
        lastFrame = now;
        int selected = player.inventory.currentItem;
        if (selected != lastSelected) {
            lastSelected = selected;
            popAge = 0f;
        }
        popAge += dt;
        stepHighlight(selected, dt);
        hotbar(context, ms, player, x, y, barsWidth, accent, accent2, back, glow, dt);

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

    /**
     * The highlight on a spring, integrated in fixed sub-steps so it moves the same at 30 fps as at
     * 300. Wrapping from slot 8 to 0 travels one step, not eight.
     */
    private static void stepHighlight(int selected, float dt) {
        if (highlight < 0f || !CONFIG.hudBetterHotbarSmooth) {
            highlight = selected;
            highlightVelocity = 0f;
            return;
        }
        float target = selected;
        float delta = target - highlight;
        if (delta > SLOTS / 2f) highlight += SLOTS;
        if (delta < -SLOTS / 2f) highlight -= SLOTS;
        int steps = Math.max(1, (int) Math.ceil(dt / 0.004f));
        float h = dt / steps;
        for (int i = 0; i < steps; i++) {
            float accel = -420f * (highlight - target) - 34f * highlightVelocity;
            highlightVelocity += accel * h;
            highlight += highlightVelocity * h;
        }
    }

    private static void hotbar(DrawContext context, MatrixStack ms, EntityPlayer player, int x, int y, int barsWidth,
                               Color accent, Color accent2, Color back, boolean glow, float dt) {
        Minecraft mc = Minecraft.getMinecraft();
        BetterHudStyles.HotbarStyle style = CONFIG.hudBetterHotbarStyle;
        float opacity = CONFIG.hudBetterHotbarOpacity / 100f;
        int a1 = accent.getRGB() | 0xFF000000, a2 = accent2.getRGB() | 0xFF000000;
        int panel = Glass.alpha(back.getRGB() | 0xFF000000, opacity);
        int x2 = x + barsWidth;
        // Where the highlight is, wrapped back onto the bar, and how far it is stretched by its speed.
        float hs = ((highlight % SLOTS) + SLOTS) % SLOTS;
        float stretch = Math.min(6f, Math.abs(highlightVelocity) * 0.25f);
        float pop = CONFIG.hudBetterHotbarPop ? popCurve(popAge) : 0f;
        float hx = x + hs * SLOT;

        switch (style) {
            case FLOATING:
                for (int slot = 0; slot < SLOTS; slot++) {
                    float sx = x + slot * SLOT;
                    if (glow) Glass.shadow(ms, sx, y, sx + SLOT - 2, y + SLOT, 6, Glass.alpha(0xFF000000, 0.5f * opacity));
                    Glass.rect(ms, sx + 1, y, sx + SLOT - 1, y + SLOT, 5f, Glass.alpha(panel, 1f), Glass.alpha(panel, 0.8f));
                }
                Glass.pill(ms, hx + 1 - stretch / 2 - pop * 2, y - pop * 2, hx + SLOT - 1 + stretch / 2 + pop * 2, y + SLOT + pop * 2,
                        5f, a1, 0xFFFFFFFF, 1f);
                break;
            case GLASS:
                Glass.shadow(ms, x - 3, y - 3, x2 + 3, y + SLOT + 3, 10, Glass.alpha(0xFF000000, 0.45f * opacity));
                Glass.pane(ms, x - 3, y - 3, x2 + 3, y + SLOT + 3, 7f, Glass.alpha(panel, 1f), 0xFFFFFFFF);
                Glass.pill(ms, hx - stretch / 2 - pop, y - 2 - pop, hx + SLOT + stretch / 2 + pop, y + SLOT + 2 + pop, 6f, a1, 0xFFFFFFFF, 1f);
                break;
            case NEON:
                Glass.rect(ms, x - 2, y - 2, x2 + 2, y + SLOT + 2, 4f, Glass.alpha(0xFF05060A, opacity));
                if (glow) Render2D.drawGradientBlurredShadow1(ms, x - 2, y - 2, barsWidth + 4, SLOT + 4, 8, accent, accent2, accent2, accent);
                Glass.rect(ms, hx - stretch / 2, y - 2, hx + SLOT + stretch / 2, y + SLOT + 2, 4f, Glass.alpha(a1, 0.0f), Glass.alpha(a1, 0.45f + pop));
                Glass.rect(ms, hx + 3 - stretch / 2, y + SLOT, hx + SLOT - 3 + stretch / 2, y + SLOT + 1.5f, 0.75f, 0xFFFFFFFF);
                break;
            case MINIMAL:
                Glass.dot(ms, hx + SLOT / 2f, y + SLOT + 1.5f, 1.6f + pop * 3f, a1);
                break;
            default:
                if (glow) Render2D.drawGradientBlurredShadow1(ms, x - 2, y - 2, barsWidth + 4, SLOT + 4, 8, accent, accent2, accent2, accent);
                Glass.rect(ms, x - 2, y - 2, x2 + 2, y + SLOT + 2, 4f, panel);
                Glass.rect(ms, hx - stretch / 2 - pop, y - 2 - pop, hx + SLOT + stretch / 2 + pop, y + SLOT + 2 + pop, 4f, Glass.alpha(a1, 0.45f));
                break;
        }
        // An outline that runs the theme round the bar: each edge its own slice of the gradient.
        if (CONFIG.hudBetterHotbarRgbBorder || style == BetterHudStyles.HotbarStyle.NEON) {
            float t = 0.8f;
            int c0 = Palette.getColor(0f).getRGB() | 0xFF000000, c1 = Palette.getColor(0.33f).getRGB() | 0xFF000000;
            int c2 = Palette.getColor(0.66f).getRGB() | 0xFF000000;
            Glass.rect(ms, x - 2, y - 2, x2 + 2, y - 2 + t, 0f, c0);
            Glass.rect(ms, x - 2, y + SLOT + 2 - t, x2 + 2, y + SLOT + 2, 0f, c2);
            Glass.rect(ms, x - 2, y - 2, x - 2 + t, y + SLOT + 2, 0f, c0, c2);
            Glass.rect(ms, x2 + 2 - t, y - 2, x2 + 2, y + SLOT + 2, 0f, c1, c2);
        }

        RenderSystem.restoreDefaults();
        for (int slot = 0; slot < SLOTS; slot++) {
            ItemStack stack = player.inventory.mainInventory[slot];
            if (stack == null) continue;
            int ix = x + slot * SLOT + 2;
            int iy = y + 2;
            boolean popping = slot == lastSelected && pop > 0.001f;
            if (popping) {
                ms.push();
                ms.translate(ix + 8, iy + 8, 0);
                ms.scale(1f + pop, 1f + pop, 1f);
                ms.translate(-(ix + 8), -(iy + 8), 0);
            }
            context.drawItem(stack, ix, iy);
            context.drawStackOverlay(mc.fontRendererObj, stack, ix, iy);
            if (popping) ms.pop();
        }
        RenderSystem.restoreDefaults();

        // The held tool's wear, in a small pill at the right end of the bar.
        ItemStack held = player.inventory.mainInventory[player.inventory.currentItem];
        if (CONFIG.hudBetterHotbarHeldInfo && held != null && held.isItemStackDamageable() && held.getMaxDamage() > 0) {
            int left = held.getMaxDamage() - held.getItemDamage();
            float f = MathHelper.clamp_float(left / (float) held.getMaxDamage(), 0f, 1f);
            String text = left + "";
            float tw = FontRenderers.sf_bold_mini.getStringWidth(text) + 10;
            float px = x2 + 6, py = y + SLOT / 2f - 6;
            Glass.rect(ms, px, py, px + tw, py + 12, 6f, panel);
            int wear = f > 0.5f ? a1 : Glass.blend(0xFFE5484D, a1, f * 2f);
            Glass.rect(ms, px + 3, py + 10, px + 3 + (tw - 6) * f, py + 11, 0.5f, wear);
            FontRenderers.sf_bold_mini.drawString(ms, text, px + 5, py + 2.5f, Palette.getTextColor());
        }
    }

    /** A quick overshoot when the selection changes: up, a little past, settle. */
    private static float popCurve(float age) {
        if (age > 0.35f) return 0f;
        float t = age / 0.35f;
        return 0.22f * (float) (Math.sin(t * Math.PI) * Math.exp(-t * 2.2));
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
