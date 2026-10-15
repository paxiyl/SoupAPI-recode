package me.Padej_.soupapi.modules;

import me.Padej_.soupapi.config.ConfigurableModule;
import me.Padej_.soupapi.font.FontRenderers;
import me.Padej_.soupapi.render.Render2D;
import me.Padej_.soupapi.render.TargetHudRenderer;
import me.Padej_.soupapi.utils.MathUtility;
import me.Padej_.soupapi.utils.Palette;
import me.Padej_.soupapi.utils.TexturesManager;
import net.minecraft.client.Minecraft;
import me.Padej_.soupapi.compat.DrawContext;
import me.Padej_.soupapi.compat.RenderLayer;
import me.Padej_.soupapi.compat.MatrixStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;

import java.awt.*;
import java.util.List;
import java.util.*;

public class PotionsHud extends ConfigurableModule {
    private static final Map<Potion, Float> effectAlphas = new HashMap<>();
    private static final Map<Potion, Integer> maxDurations = new HashMap<>();
    /** Vanilla's inventory sheet, which carries the 18x18 status-effect icons at y=198. */
    private static final ResourceLocation INVENTORY_ICONS =
            new ResourceLocation("textures/gui/container/inventory.png");

    private static float currentWidth = 0f;
    private static float currentHeight = 0f;

    /**
     * Localised name for an effect.
     *
     * <p>The 1.21.4 version parsed {@code lang/en_us.json} out of the resource manager to get
     * English names regardless of the player's language. 1.8.9 has no such file — it ships
     * {@code lang/en_US.lang} — so that load always failed and the HUD fell back to raw keys like
     * {@code potion.moveSpeed}. Vanilla's own localisation is used instead, which also means the
     * HUD follows the player's selected language.
     */
    private static String effectName(Potion potion) {
        String key = potion.getName();
        String name = net.minecraft.client.resources.I18n.format(key);
        return name == null || name.equals(key) ? key : name;
    }

    public static void render(DrawContext context) {
        if (mc.thePlayer == null || mc.theWorld == null) return;

        float x = CONFIG.hudBetterPotionsHudX;
        float y = CONFIG.hudBetterPotionsHudY;
        float spacing = 19;
        float targetWidth = 0f;
        float targetHeight;

        List<PotionEffect> rawEffects = new ArrayList<PotionEffect>(mc.thePlayer.getActivePotionEffects());
        rawEffects.sort(Comparator.comparing(a -> effectName(potionOf(a))));

        if (rawEffects.isEmpty()) {
            currentWidth = fast(currentWidth, 0, 15f);
            currentHeight = fast(currentHeight, 0, 15f);
            effectAlphas.clear();
            maxDurations.clear();
            if (currentWidth < 0.1f && currentHeight < 0.1f) return;
        }

        // 1.8.9 looks effects up by Potion directly; there is no registry entry wrapper.
        maxDurations.keySet().removeIf(effect -> mc.thePlayer.getActivePotionEffect(effect) == null);

        List<EffectElement> effects = new ArrayList<>();
        for (PotionEffect instance : rawEffects) {
            Potion effect = potionOf(instance);
            maxDurations.putIfAbsent(effect, instance.getDuration());
            effects.add(new EffectElement(instance));
        }

        for (EffectElement element : effects) {
            Potion effect = element.getEffect();
            float currentAlpha = effectAlphas.getOrDefault(effect, 0f);
            float newAlpha = fast(currentAlpha, 255f, 10f);
            effectAlphas.put(effect, newAlpha);
            targetWidth = Math.max(targetWidth, Math.max(element.getWidth(), 80f));
        }

        targetHeight = effects.size() * spacing + 4;

        // Анимируем только если есть эффекты
        if (!effects.isEmpty()) {
            currentWidth = fast(currentWidth, targetWidth, 15f);
            currentHeight = fast(currentHeight, targetHeight, 15f);
        }

        float headerHeight = 12f;
        float headerYOffset = 5f;
        int radius = 3;

        if (currentWidth > 0.1f || currentHeight > 0.1f) {
            float headerY = y - headerHeight - headerYOffset;

            // Заголовок
            MatrixStack matrices = context.getMatrices();
            Render2D.drawGradientBlurredShadow1(matrices, x - 2.5f, headerY, currentWidth, headerHeight, 5, TargetHudRenderer.bottomLeft, TargetHudRenderer.bottomRight, TargetHudRenderer.topRight, TargetHudRenderer.topLeft);
            Render2D.drawRound(matrices, x - 3f, headerY, currentWidth, headerHeight, radius, Palette.getBackColor());

            String title = "Potions";
            float textWidth = FontRenderers.sf_bold.getStringWidth(title);
            FontRenderers.sf_bold.drawString(matrices, title,
                    x - 3f + currentWidth / 2f - textWidth / 2f,
                    headerY + 3f, Palette.getTextColor()
            );

            // Фон под эффекты
            Render2D.drawGradientBlurredShadow1(matrices, x - 2.5f, y + 1, currentWidth, currentHeight, 5, TargetHudRenderer.bottomLeft, TargetHudRenderer.bottomRight, TargetHudRenderer.topRight, TargetHudRenderer.topLeft);
            Render2D.drawRound(matrices, x - 3f, y + 0.5f, currentWidth, currentHeight, radius, Palette.getBackColor());

            matrices.push();
            matrices.translate(x, headerY + 1.5f, 0);
            matrices.scale(0.5f, 0.5f, 0.5f);
            context.drawTexture(TexturesManager.GUI_POTION, 0, 0, 16, 16, 0, 0, 1024, 1024, 1024, 1024);
            matrices.pop();
        }

        for (int i = 0; i < effects.size(); i++) {
            EffectElement e = effects.get(i);
            float alpha = effectAlphas.getOrDefault(e.getEffect(), 255f);
            e.render(context, x - 2, y + 2 + i * spacing, alpha);
        }
    }

    private static String getDuration(PotionEffect statusEffect) {
        // Infinite effects arrived in 1.19; in 1.8.9 every effect has a finite duration.
        int duration = statusEffect.getDuration();
        int minutes = duration / 1200;
        int seconds = (duration % 1200) / 20;
        return minutes + ":" + String.format("%02d", seconds);
    }

    /**
     * Resolves a {@link PotionEffect}'s numeric id to its {@link Potion}.
     *
     * <p>1.21.4 carried a registry entry on the instance; 1.8.9 stores a raw id and looks it up in
     * {@code Potion.potionTypes}. Returns the "unknown" potion rather than null for an id outside
     * the table, which a modded effect could produce.
     */
    static Potion potionOf(PotionEffect instance) {
        int id = instance.getPotionID();
        if (id >= 0 && id < Potion.potionTypes.length && Potion.potionTypes[id] != null) {
            return Potion.potionTypes[id];
        }
        return Potion.moveSpeed;
    }

    private static String toRoman(int number) {
        if (number < 1 || number > 3999) return String.valueOf(number);
        int[] values = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
        String[] symbols = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            while (number >= values[i]) {
                result.append(symbols[i]);
                number -= values[i];
            }
        }
        return result.toString();
    }

    private static float fast(float end, float start, float multiple) {
        float clampedDelta = MathUtility.clamp((1f / net.minecraft.client.Minecraft.getDebugFPS()) * multiple, 0f, 1f);
        return (1f - clampedDelta) * end + clampedDelta * start;
    }

    private static class EffectElement {
        private final PotionEffect instance;
        private final String displayText;
        private final float textWidth;

        public EffectElement(PotionEffect instance) {
            this.instance = instance;
            int amplifier = instance.getAmplifier();

            // Получение англоязычного имени эффекта (en_us)
            String name = effectName(potionOf(instance));

            String levelPart = amplifier > 0
                    ? " " + (CONFIG.hudBetterPotionsHudToRoman ? toRoman(amplifier + 1) : amplifier + 1)
                    : "";

            this.displayText = name + levelPart;
            this.textWidth = FontRenderers.sf_bold_mini.getStringWidth(displayText);
        }

        public void render(DrawContext context, float x, float y, float alpha) {
            float scale = 0.8f;

            MatrixStack matrices = context.getMatrices();

            matrices.push();
            matrices.translate(x + (1 + scale), y + (1 + scale), 0);
            matrices.scale(scale, scale, 0);
            // 1.8.9 has no status-effect sprite atlas: icons live on the inventory sheet,
            // indexed by Potion.getStatusIconIndex() in an 8-wide grid of 18x18 cells.
            Potion potion = potionOf(instance);
            if (potion.hasStatusIcon()) {
                int icon = potion.getStatusIconIndex();
                context.drawTexture(INVENTORY_ICONS, 0, 0, 18, 18,
                        (icon % 8) * 18, 198 + (icon / 8) * 18, 18, 18, 256, 256);
            }
            matrices.pop();

            FontRenderers.sf_bold_mini.drawString(matrices, displayText, x + 20, y + 2, Palette.getTextColor());
            FontRenderers.sf_bold_mini.drawString(matrices, "§7" + getDuration(instance), x + 20, y + 9, Palette.getTextColor());

            {
                Potion effect = potionOf(instance);
                int maxDuration = maxDurations.getOrDefault(effect, instance.getDuration());
                float progress = MathUtility.clamp(instance.getDuration() / (float) maxDuration, 0f, 1f);

                float barX = x + 20;
                float barY = y + 15;
                float barWidth = textWidth;
                float barHeight = 2;

                Color fillStart = Palette.getColor(0f).darker();
                Color fillEnd = Palette.getColor(0.33f).darker();
                Color background = new Color(0, 0, 0, (int) (alpha * 0.3f));

                Render2D.drawGradientBlurredShadow1(matrices, barX, barY, barWidth * progress, barHeight, 3, fillStart, fillEnd, fillStart, fillEnd);
                Render2D.drawRound(matrices, barX, barY, barWidth, barHeight, 1, background);
                Render2D.renderRoundedGradientRect(matrices, fillStart, fillEnd, fillEnd, fillStart, barX, barY, barWidth * progress, barHeight, 1);
            }
        }

        public float getWidth() {
            return 24 + 2 + textWidth;
        }

        public Potion getEffect() {
            return potionOf(instance);
        }
    }
}