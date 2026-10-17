package me.Padej_.soupapi.melee;

import me.Padej_.soupapi.compat.DrawContext;
import me.Padej_.soupapi.compat.MatrixStack;
import me.Padej_.soupapi.compat.RenderSystem;
import me.Padej_.soupapi.compat.SoupScreen;
import me.Padej_.soupapi.compat.Texts;
import me.Padej_.soupapi.config.ConfigurableModule;
import me.Padej_.soupapi.font.FontRenderers;
import me.Padej_.soupapi.gui.option.ConfigSchema;
import me.Padej_.soupapi.render.Render2D;
import me.Padej_.soupapi.utils.Palette;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Picks the carried weapon, and switches individual ones on or off.
 *
 * <p>A weapon list cannot be an enum: the weapons come from whatever packs are on disk, so they are
 * only known once those are read. Clicking a row carries that weapon; the switch on the right drops
 * it out of the list the cycle key walks, which is what "toggle or remove" asks for without
 * deleting anything from the pack.
 */
public class MeleeSelectScreen extends SoupScreen {

    private static final int ROW_H = 20;

    private final SoupScreen parent;
    private String query = "";
    private double scroll;

    public MeleeSelectScreen(SoupScreen parent) {
        super(Texts.literal("Melee weapons"));
        this.parent = parent;
    }

    private List<MeleeWeapon> matches() {
        List<MeleeWeapon> out = new ArrayList<MeleeWeapon>();
        String needle = query.toLowerCase(Locale.ROOT);
        for (MeleeWeapon weapon : MeleePack.weapons()) {
            if (needle.isEmpty()
                    || plain(weapon.displayName).toLowerCase(Locale.ROOT).contains(needle)
                    || weapon.id.toLowerCase(Locale.ROOT).contains(needle)) {
                out.add(weapon);
            }
        }
        return out;
    }

    /** Pack names carry Minecraft colour codes; the mod's own font draws them as glyphs. */
    private static String plain(String text) {
        if (text == null) return "";
        return text.replaceAll("§[0-9a-fk-or]", "");
    }

    private int left() {
        return Math.max(8, this.width / 2 - 150);
    }

    private int right() {
        return Math.min(this.width - 8, this.width / 2 + 150);
    }

    private int top() {
        return 8;
    }

    private int bottom() {
        return this.height - 8;
    }

    private int listTop() {
        return top() + 44;
    }

    private int toggleX() {
        return right() - 34;
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

        FontRenderers.sf_bold.drawString(ms, "Melee weapons", left() + 8, top() + 7, accent.getRGB());

        List<MeleePack> packs = MeleePack.packs();
        String subtitle = packs.isEmpty()
                ? "No packs found - put one in .minecraft/soupapi/melee"
                : packs.size() + (packs.size() == 1 ? " pack, " : " packs, ")
                        + MeleePack.weapons().size() + " weapons";
        FontRenderers.sf_bold_mini.drawString(ms, subtitle, left() + 8, top() + 19, 0xFF8A90A0);

        int fx = left() + 6;
        int fw = right() - left() - 12;
        Render2D.drawRound(ms, fx, top() + 31, fw, 10, 3, new Color(255, 255, 255, 22));
        String shown = query.isEmpty() ? "Type to filter..." : query;
        FontRenderers.sf_bold_mini.drawString(ms, shown, fx + 4, top() + 33,
                query.isEmpty() ? 0xFF8A90A0 : text);

        List<MeleeWeapon> weapons = matches();
        int view = bottom() - listTop() - 4;
        int total = weapons.size() * ROW_H;
        scroll = Math.max(0, Math.min(Math.max(0, total - view), scroll));

        RenderSystem.enableScissor(left() + 4, listTop(), right() - 4, bottom() - 4);
        int y = (int) (listTop() - scroll);
        for (MeleeWeapon weapon : weapons) {
            if (y + ROW_H >= listTop() && y <= bottom()) {
                row(ms, context, weapon, y, mouseX, mouseY, accent, text);
            }
            y += ROW_H;
        }
        RenderSystem.disableScissor();

        if (weapons.isEmpty()) {
            String reason = MeleePack.lastError();
            FontRenderers.sf_bold_mini.drawString(ms,
                    reason == null ? "Nothing to show" : reason,
                    left() + 10, listTop() + 6, 0xFF8A90A0);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private void row(MatrixStack ms, DrawContext context, MeleeWeapon weapon, int y,
                     int mouseX, int mouseY, Color accent, int text) {
        boolean carried = weapon.id.equals(ConfigurableModule.CONFIG.meleeWeapon);
        boolean off = MeleePack.isDisabled(weapon);
        boolean hover = mouseX >= left() + 6 && mouseX <= right() - 6
                && mouseY >= Math.max(listTop(), y) && mouseY <= Math.min(bottom(), y + ROW_H - 2);

        if (carried) {
            Render2D.drawRound(ms, left() + 6, y, right() - left() - 12, ROW_H - 3, 3,
                    new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 70));
        } else if (hover) {
            Render2D.drawRound(ms, left() + 6, y, right() - left() - 12, ROW_H - 3, 3,
                    new Color(255, 255, 255, 18));
        }

        ResourceLocation icon = weapon.slotIcon();
        if (icon != null) {
            GlStateManager.color(1f, 1f, 1f, off ? 0.35f : 1f);
            context.drawSpriteStretched(icon, left() + 9, y + 1, 15, 15);
            GlStateManager.color(1f, 1f, 1f, 1f);
        }

        int label = off ? 0xFF5A6070 : (carried ? accent.getRGB() : text);
        FontRenderers.sf_bold_mini.drawString(ms, plain(weapon.displayName), left() + 28, y + 2, label);
        FontRenderers.sf_bold_mini.drawString(ms, weapon.pack.namespace + ":" + weapon.id,
                left() + 28, y + 10, 0xFF6A7080);

        // The on/off switch.
        Color pill = off ? new Color(90, 96, 112, 160)
                : new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 200);
        Render2D.drawRound(ms, toggleX(), y + 4, 26, 10, 5, pill);
        Render2D.drawRound(ms, toggleX() + (off ? 2 : 16), y + 6, 8, 6, 3,
                new Color(255, 255, 255, 230));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseY >= listTop() && mouseY <= bottom()
                && mouseX >= left() + 6 && mouseX <= right() - 6) {
            int index = (int) ((mouseY - listTop() + scroll) / ROW_H);
            List<MeleeWeapon> weapons = matches();
            if (index >= 0 && index < weapons.size()) {
                MeleeWeapon weapon = weapons.get(index);
                if (mouseX >= toggleX() - 2) {
                    MeleePack.setDisabled(weapon, !MeleePack.isDisabled(weapon));
                } else {
                    MeleePack.setDisabled(weapon, false);
                    ConfigurableModule.CONFIG.meleeWeapon = weapon.id;
                    ConfigurableModule.CONFIG.meleeEnabled = true;
                }
                ConfigSchema.save();
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
