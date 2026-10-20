package me.Padej_.soupapi.modules;

import me.Padej_.soupapi.compat.DrawContext;
import me.Padej_.soupapi.compat.MatrixStack;
import me.Padej_.soupapi.compat.RenderSystem;
import me.Padej_.soupapi.compat.RotationAxis;
import me.Padej_.soupapi.config.ConfigurableModule;
import me.Padej_.soupapi.event.AttackEntityCallback;
import me.Padej_.soupapi.gui.atlas.Glass;
import me.Padej_.soupapi.render.Render2D;
import me.Padej_.soupapi.utils.FxClock;
import me.Padej_.soupapi.utils.Palette;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

import java.awt.Color;

/**
 * A crosshair drawn by the mod in place of vanilla's.
 *
 * <p>Everything is built from rounded quads in GUI space, so it is the same on gl4es as anywhere,
 * and it is drawn from the overlay event after vanilla's {@code CROSSHAIRS} element has been
 * cancelled. It can spin, spread while the player moves or swings, take the theme (and so RGB), and
 * flash and kick outward when an attack lands.
 */
public final class Crosshair extends ConfigurableModule {

    public enum Style {
        /** A plain cross. */
        MINIMAL,
        /** Four bars with a gap that spreads. */
        FOUR_SEGMENT,
        /** A single dot. */
        DOT,
        /** A thin ring with a dot in it. */
        CIRCULAR,
        /** A ring of rune ticks round a diamond. */
        RUNE,
        /** Two blade-like chevrons and a dot, for the melee weapons. */
        WEAPON
    }

    private static float spread, hit, lastHitAt = -10f;
    private static long lastFrame;

    private Crosshair() {
    }

    public static void register() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (CONFIG.crosshairEnabled && CONFIG.crosshairHitConfirm && entity instanceof EntityLivingBase) {
                lastHitAt = FxClock.seconds();
            }
            return AttackEntityCallback.Result.PASS;
        });
    }

    public static boolean shouldCancelVanilla(RenderGameOverlayEvent.ElementType type) {
        return CONFIG.crosshairEnabled && type == RenderGameOverlayEvent.ElementType.CROSSHAIRS;
    }

    public static void render(DrawContext context) {
        if (!CONFIG.crosshairEnabled || mc.thePlayer == null || mc.gameSettings.hideGUI) return;
        if (mc.gameSettings.thirdPersonView != 0 && !CONFIG.crosshairShowInThirdPerson) return;
        if (mc.currentScreen != null) return;

        long now = System.nanoTime();
        float dt = lastFrame == 0L ? 0.016f : Math.min(0.1f, (now - lastFrame) / 1.0E9f);
        lastFrame = now;

        // Spread: how fast the player is moving and whether they are swinging, eased.
        float target = 0f;
        if (CONFIG.crosshairDynamic) {
            double mx = mc.thePlayer.posX - mc.thePlayer.prevPosX, mz = mc.thePlayer.posZ - mc.thePlayer.prevPosZ;
            target = (float) Math.min(1.0, Math.sqrt(mx * mx + mz * mz) / 0.28);
            if (!mc.thePlayer.onGround) target = Math.max(target, 0.6f);
            if (mc.thePlayer.isSwingInProgress) target = Math.max(target, 0.5f);
        }
        spread += (target - spread) * (1f - (float) Math.exp(-12f * dt));
        float since = FxClock.seconds() - lastHitAt;
        hit = since >= 0f && since < 0.3f ? 1f - since / 0.3f : 0f;

        MatrixStack ms = context.getMatrices();
        float cx = context.getScaledWindowWidth() / 2f, cy = context.getScaledWindowHeight() / 2f;
        float angle = CONFIG.crosshairRotation + FxClock.seconds() * CONFIG.crosshairSpinSpeed;

        int colour = colour();
        int hitColour = 0xFFFF3B47;
        int main = hit > 0f ? Glass.blend(colour, hitColour, hit) : colour;
        main = Glass.alpha(main, CONFIG.crosshairOpacity / 100f);

        ms.push();
        ms.translate(cx, cy, 0);
        ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(angle));
        if (CONFIG.crosshairGlow) {
            float r = CONFIG.crosshairSize + CONFIG.crosshairGap + 3;
            Render2D.drawBlurredShadow(ms, -r, -r, r * 2, r * 2, 6, Glass.c(Glass.alpha(main, 0.5f)));
        }
        if (CONFIG.crosshairOutline) draw(ms, Glass.alpha(0xFF000000, 0.55f * CONFIG.crosshairOpacity / 100f), 1f);
        draw(ms, main, 0f);
        ms.pop();
        RenderSystem.restoreDefaults();
    }

    private static int colour() {
        if (CONFIG.crosshairUseTheme) return Palette.getColor(0f).getRGB() | 0xFF000000;
        return CONFIG.crosshairColor | 0xFF000000;
    }

    /** One pass of the shape; the outline pass is the same shape grown by {@code grow} on every side. */
    private static void draw(MatrixStack ms, int c, float grow) {
        float t = CONFIG.crosshairThickness;
        float half = t / 2f + grow;
        float size = CONFIG.crosshairSize;
        float gap = CONFIG.crosshairGap + spread * 4f + hit * 3f;
        switch (CONFIG.crosshairStyle) {
            case DOT:
                Glass.dot(ms, 0, 0, Math.max(1f, t) + 0.5f + grow, c);
                break;
            case MINIMAL:
                Glass.rect(ms, -size - grow, -half, size + grow, half, 0f, c);
                Glass.rect(ms, -half, -size - grow, half, -half, 0f, c);
                Glass.rect(ms, -half, half, half, size + grow, 0f, c);
                break;
            case CIRCULAR:
                ring(ms, gap + size * 0.6f, t + grow * 2f, 40, c);
                Glass.dot(ms, 0, 0, 0.8f + grow, c);
                break;
            case RUNE:
                // Eight ticks, the cardinal ones longer, round a small diamond.
                for (int i = 0; i < 8; i++) {
                    float len = i % 2 == 0 ? size : size * 0.5f;
                    ms.push();
                    ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i * 45f));
                    Glass.rect(ms, -half, -gap - len - grow, half, -gap + grow, 0f, c);
                    ms.pop();
                }
                ms.push();
                ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(45f));
                float d = 1.4f + grow;
                Glass.rect(ms, -d, -d, d, d, 0f, c);
                ms.pop();
                break;
            case WEAPON:
                for (int side = -1; side <= 1; side += 2) {
                    for (int arm = -1; arm <= 1; arm += 2) {
                        ms.push();
                        ms.translate(side * (gap + size * 0.5f), 0, 0);
                        ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(side * arm * 40f));
                        Glass.rect(ms, -half, arm < 0 ? -size - grow : -grow, half, arm < 0 ? grow : size + grow, half, c);
                        ms.pop();
                    }
                }
                Glass.dot(ms, 0, 0, 0.9f + grow, c);
                break;
            default:
                for (int i = 0; i < 4; i++) {
                    ms.push();
                    ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i * 90f));
                    Glass.rect(ms, -half, -gap - size - grow, half, -gap + grow, Math.min(half, 1f), c);
                    ms.pop();
                }
                break;
        }
    }

    /** A ring of quads, so it has real thickness on drivers where line width is ignored. */
    private static void ring(MatrixStack ms, float radius, float width, int segments, int c) {
        for (int i = 0; i < segments; i++) {
            ms.push();
            ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i * 360f / segments));
            float arc = (float) (Math.PI * 2 * radius / segments) / 2f + 0.3f;
            Glass.rect(ms, -arc, -radius - width / 2f, arc, -radius + width / 2f, 0f, c);
            ms.pop();
        }
    }
}
