package me.Padej_.soupapi.modules;

import me.Padej_.soupapi.compat.DrawContext;
import me.Padej_.soupapi.compat.MatrixStack;
import me.Padej_.soupapi.compat.RenderSystem;
import me.Padej_.soupapi.compat.RotationAxis;
import me.Padej_.soupapi.config.ConfigurableModule;
import me.Padej_.soupapi.event.AttackEntityCallback;
import me.Padej_.soupapi.gui.atlas.Glass;
import me.Padej_.soupapi.render.Render2D;
import me.Padej_.soupapi.render.Reticle;
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
        WEAPON,

        // ---- targeting reticles ------------------------------------------------------------
        // Animated, two-colour, in the style of a targeting display. Every one has parts that
        // move on their own: rings that turn against each other, dashes that march, brackets
        // that open with your movement and snap shut on a hit.

        /** A sight with a dashed orbit, tick rails out to either side and bracket arcs. */
        HUD_ORBIT,
        /** Rings inside rings, each turning its own way: dots, dashes, segments. */
        HUD_RADAR,
        /** A hexagon with a chevron and a stem, a second hexagon turning inside it. */
        HUD_HEX,
        /** Four corner brackets that breathe with your movement, a ring and side bars. */
        HUD_BRACKETS,
        /** A pentagon round a dotted orbit, a bar through the middle and a locking square. */
        HUD_PENTAGON,
        /** A triangle with a chevron, a dotted base and a pulsing bar under it. */
        HUD_TRIANGLE,
        /** Two heavy side arcs with chevrons sliding in toward a dotted ring. */
        HUD_ARCS,
        /** Nested diamonds with rows of arrows marching in on the centre. */
        HUD_DIAMOND,
        /** A two-tone tick ring with four arrows closing in. */
        HUD_QUAD,
        /** A split ring, a cross, and four corner wedges turning round a solid core. */
        HUD_TARGET,
        /** A hexagon round a dotted ring, with a bracketed inner hexagon. */
        HUD_SIGIL;

        /** True for the animated targeting reticles, which are drawn as one batch. */
        public boolean reticle() {
            return ordinal() >= HUD_ORBIT.ordinal();
        }
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

    /** Whether vanilla's crosshair was on before this module turned it off, so it can be restored. */
    private static boolean suppressing;

    /**
     * Keeps vanilla's crosshair off for as long as this one is on.
     *
     * <p>Cancelling the {@code CROSSHAIRS} overlay event is the polite way, and it is still done,
     * but it only works when Forge's own in-game GUI is the one drawing: another mod that replaces
     * the in-game GUI, or draws its own crosshair pass, never raises that event and the vanilla
     * cross comes back underneath. Forge also exposes the switch its GUI checks before drawing the
     * crosshair at all, so this flips that too, and flips it back the moment the custom one is off.
     */
    public static void syncVanilla() {
        boolean want = CONFIG.crosshairEnabled;
        if (want && !suppressing) {
            net.minecraftforge.client.GuiIngameForge.renderCrosshairs = false;
            suppressing = true;
        } else if (!want && suppressing) {
            net.minecraftforge.client.GuiIngameForge.renderCrosshairs = true;
            suppressing = false;
        }
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

        if (CONFIG.crosshairStyle != null && CONFIG.crosshairStyle.reticle()) {
            ms.push();
            ms.translate(cx, cy, 0);
            ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(CONFIG.crosshairRotation
                    + FxClock.seconds() * CONFIG.crosshairSpinSpeed));
            if (CONFIG.crosshairGlow) {
                float r = 16f * CONFIG.crosshairSize / 5f;
                Render2D.drawBlurredShadow(ms, -r, -r, r * 2, r * 2, 8, Glass.c(Glass.alpha(main, 0.35f)));
            }
            int accent = accent();
            if (hit > 0f) accent = Glass.blend(accent, hitColour, hit);
            accent = Glass.alpha(accent, CONFIG.crosshairOpacity / 100f);
            Reticle.begin(ms);
            try {
                // The outline first: the same reticle in translucent black with every stroke a
                // little heavier, so a thin two-colour reticle still reads against a bright sky.
                if (CONFIG.crosshairOutline) {
                    int ink = Glass.alpha(0xFF000000, 0.5f * CONFIG.crosshairOpacity / 100f);
                    inkPass = true;
                    reticle(CONFIG.crosshairStyle, ink, ink, 1.1f);
                    inkPass = false;
                }
                reticle(CONFIG.crosshairStyle, main, accent, 0f);
            } finally {
                inkPass = false;
                Reticle.end();
            }
            ms.pop();
            RenderSystem.restoreDefaults();
            return;
        }

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

    /** The second colour of a two-tone reticle: half way round the theme, or a fixed magenta. */
    private static int accent() {
        if (CONFIG.crosshairUseTheme) return Palette.getColor(0.5f).getRGB() | 0xFF000000;
        return 0xFFE0329B;
    }

    // ---- the reticles ----------------------------------------------------------------------

    /**
     * Draws one targeting reticle at the origin.
     *
     * <p>Sizes are in units of {@code u}, which the size slider scales (the default size is one
     * unit to a GUI pixel). {@code open} is how far the frame has been pushed out by movement and
     * pulled back in by a hit, so every style reads the player's state the same way.
     */
    /** True while the outline is being drawn, so styles that fade a part keep its outline solid. */
    private static boolean inkPass;

    private static void reticle(Style style, int main, int accent, float grow) {
        float u = CONFIG.crosshairSize / 5f;
        float w = Math.max(0.6f, CONFIG.crosshairThickness * 0.7f) + grow;
        float t = FxClock.seconds();
        float open = 1f + spread * 0.35f - hit * 0.18f;
        float pulse = 0.5f + 0.5f * (float) Math.sin(t * 3.2f);
        Reticle.colour(main);
        switch (style) {
            case HUD_ORBIT: {
                Reticle.ring(0, 0, 3f * u, w);
                Reticle.disc(0, 0, 0.9f * u);
                Reticle.colour(accent);
                Reticle.dashes(0, 0, 8f * u * open, w, 3, 0.78f, t * 40f);
                Reticle.colour(main);
                for (int side = -1; side <= 1; side += 2) {
                    float x0 = side * 9.5f * u * open, x1 = side * 16f * u * open;
                    Reticle.line(x0, 0, x1, 0, w);
                    for (int k = 0; k <= 6; k++) {
                        float x = x0 + (x1 - x0) * k / 6f;
                        float tall = (k % 3 == 0 ? 1.6f : 0.9f) * u;
                        Reticle.line(x, -tall, x, tall, w * 0.8f);
                    }
                    Reticle.colour(accent);
                    Reticle.arc(0, 0, 19f * u * open, w * 1.8f, side < 0 ? 235f : 55f, side < 0 ? 305f : 125f, 10);
                    Reticle.colour(main);
                }
                Reticle.line(0, 3f * u, 0, 9.5f * u * open, w);
                break;
            }
            case HUD_RADAR: {
                Reticle.ring(0, 0, 3f * u, w);
                Reticle.line(-1.6f * u, 0, 1.6f * u, 0, w * 0.8f);
                Reticle.line(0, -1.6f * u, 0, 1.6f * u, w * 0.8f);
                Reticle.colour(accent);
                Reticle.dots(0, 0, 6f * u * open, 24, 1.1f * u * 0.8f, t * 30f);
                Reticle.colour(main);
                Reticle.dashes(0, 0, 9f * u * open, w * 1.2f, 12, 0.6f, -t * 45f);
                Reticle.colour(accent);
                Reticle.dashes(0, 0, 12.5f * u * open, w * 2f, 4, 0.17f, t * 18f);
                Reticle.colour(Glass.alpha(main, 0.55f));
                Reticle.dashes(0, 0, 14.5f * u * open, w * 0.8f, 36, 0.5f, -t * 8f);
                break;
            }
            case HUD_HEX: {
                Reticle.polygon(0, 0, 6, 13f * u * open, w * 1.3f, 30f);
                Reticle.colour(Glass.alpha(accent, 0.75f));
                Reticle.polygon(0, 0, 6, 7.5f * u, w, 30f + t * 25f);
                Reticle.colour(accent);
                Reticle.chevron(0, -4f * u, 4f * u, w * 1.4f, 0f);
                Reticle.colour(main);
                Reticle.line(-2.4f * u, 4f * u, 2.4f * u, 4f * u, w);
                Reticle.line(0, 4f * u, 0, 7.5f * u, w);
                for (int side = -1; side <= 1; side += 2) {
                    float x = side * 13.8f * u * open;
                    Reticle.rect(x - w, -4f * u, x + w, 4f * u);
                    Reticle.rect(x + side * 1.4f * u - w * 0.5f, -2.4f * u, x + side * 1.4f * u + w * 0.5f, 2.4f * u);
                }
                Reticle.disc(0, 0, 0.7f * u);
                break;
            }
            case HUD_BRACKETS: {
                float d = 10f * u * open;
                float arm = 3.6f * u;
                Reticle.bracket(-d, -d, arm, w * 1.4f, 1, 1);
                Reticle.bracket(d, -d, arm, w * 1.4f, -1, 1);
                Reticle.bracket(-d, d, arm, w * 1.4f, 1, -1);
                Reticle.bracket(d, d, arm, w * 1.4f, -1, -1);
                Reticle.ring(0, 0, 4f * u, w);
                for (int i = 0; i < 4; i++) {
                    double a = Math.toRadians(i * 90);
                    float s = (float) Math.sin(a), c = (float) -Math.cos(a);
                    Reticle.line(s * 2.4f * u, c * 2.4f * u, s * 5.6f * u, c * 5.6f * u, w);
                }
                Reticle.colour(accent);
                Reticle.dots(0, 0, 7f * u, 20, 0.8f * u, t * 20f);
                for (int side = -1; side <= 1; side += 2) {
                    float x = side * (d + 2.6f * u);
                    Reticle.rect(x - w, -d * 0.55f, x + w, d * 0.55f);
                    Reticle.rect(x + side * 1.6f * u - w * 0.5f, -d * 0.3f, x + side * 1.6f * u + w * 0.5f, d * 0.3f);
                }
                break;
            }
            case HUD_PENTAGON: {
                Reticle.polygon(0, 0, 5, 13.5f * u * open, w * 1.3f, 0f);
                Reticle.colour(accent);
                Reticle.dots(0, 0, 9f * u, 30, 0.7f * u, t * 24f);
                Reticle.colour(main);
                Reticle.ring(0, 0, 3f * u, w);
                Reticle.line(-9f * u, 0, -3f * u, 0, w);
                Reticle.line(3f * u, 0, 9f * u, 0, w);
                Reticle.arrow(0, -6f * u - pulse * u, 1.3f * u, 180f);
                Reticle.arrow(0, 6f * u + pulse * u, 1.3f * u, 0f);
                // The lock: a square that turns and tightens on the centre.
                Reticle.colour(accent);
                float q = 5f * u * (1.25f - 0.25f * pulse) * open;
                float ang = t * 60f;
                for (int i = 0; i < 4; i++) {
                    double a0 = Math.toRadians(ang + i * 90 + 45);
                    float x = (float) Math.sin(a0) * q, y = (float) -Math.cos(a0) * q;
                    Reticle.arrow(x, y, 0.9f * u, ang + i * 90 + 225f);
                }
                break;
            }
            case HUD_TRIANGLE: {
                Reticle.polygon(0, 0, 3, 14f * u * open, w * 1.3f, 0f);
                Reticle.colour(accent);
                Reticle.chevron(0, -3.5f * u - pulse * 1.2f * u, 3.6f * u, w * 1.4f, 0f);
                Reticle.colour(main);
                Reticle.line(-1.8f * u, 0, 1.8f * u, 0, w);
                Reticle.line(0, -1.8f * u, 0, 1.8f * u, w);
                Reticle.dotted(-7f * u * open, 5f * u * open, 7f * u * open, 5f * u * open, 9, 0.6f * u);
                Reticle.colour(accent);
                float bar = (5f + 3f * pulse) * u;
                Reticle.rect(-bar, 9.2f * u * open, bar, 9.2f * u * open + w * 1.6f);
                Reticle.arrow(0, 2.8f * u, 0.9f * u, 0f);
                break;
            }
            case HUD_ARCS: {
                Reticle.colour(accent);
                Reticle.arc(0, 0, 13f * u * open, w * 2.6f, 228f, 312f, 14);
                Reticle.arc(0, 0, 13f * u * open, w * 2.6f, 48f, 132f, 14);
                // Chevrons sliding in from either side, looping.
                Reticle.colour(main);
                float slide = (t * 1.4f) % 1f;
                for (int k = 0; k < 2; k++) {
                    float x = (9.5f - 2.2f * k - 2.2f * slide) * u * open;
                    float fade = k == 0 ? slide : 1f;
                    Reticle.colour(Glass.alpha(main, 0.35f + 0.65f * fade));
                    Reticle.chevron(-x, 0, 1.6f * u, w, 90f);
                    Reticle.chevron(x, 0, 1.6f * u, w, 270f);
                }
                Reticle.colour(main);
                Reticle.dots(0, 0, 7.5f * u, 28, 0.6f * u, -t * 20f);
                Reticle.ring(0, 0, 3.6f * u, w);
                Reticle.colour(accent);
                Reticle.arrow(0, -5.2f * u, 0.9f * u, 180f);
                break;
            }
            case HUD_DIAMOND: {
                Reticle.colour(accent);
                Reticle.polygon(0, 0, 4, 14f * u * open, w * 2f, 0f);
                Reticle.colour(main);
                Reticle.polygon(0, 0, 4, 11.5f * u * open, w, 0f);
                // Rows of arrows marching in along each axis.
                float march = (t * 1.6f) % 1f;
                for (int axis = 0; axis < 4; axis++) {
                    float rot = axis * 90f;
                    double a = Math.toRadians(rot);
                    float s = (float) Math.sin(a), c = (float) -Math.cos(a);
                    for (int k = 0; k < 3; k++) {
                        float d = (9.5f - (k + march) * 2.2f) * u;
                        if (d < 3.4f * u) continue;
                        Reticle.colour(Glass.alpha(main, k == 0 ? 1f - march * 0.5f : 1f));
                        Reticle.arrow(s * d, c * d, 0.8f * u, rot + 180f);
                    }
                }
                Reticle.colour(accent);
                Reticle.polygon(0, 0, 4, 2.6f * u, w, 45f);
                for (int i = 0; i < 4; i++) {
                    double a = Math.toRadians(45 + i * 90);
                    float d = 6.5f * u * open;
                    Reticle.arrow((float) Math.sin(a) * d, (float) -Math.cos(a) * d, 1f * u, 45 + i * 90 + 180f);
                }
                break;
            }
            case HUD_QUAD: {
                // Two-tone tick ring: the colours chase round it.
                int n = 56;
                float chase = t * 70f;
                for (int i = 0; i < n; i++) {
                    float ang = i * 360f / n;
                    float mix = 0.5f + 0.5f * (float) Math.sin(Math.toRadians(ang - chase));
                    Reticle.colour(Glass.blend(main, accent, mix));
                    double a = Math.toRadians(ang);
                    float s = (float) Math.sin(a), c = (float) -Math.cos(a);
                    float r0 = 13f * u * open, r1 = r0 + 2.2f * u;
                    Reticle.line(s * r0, c * r0, s * r1, c * r1, w * 1.2f);
                }
                Reticle.colour(main);
                Reticle.ring(0, 0, 11f * u * open, w * 0.8f);
                Reticle.ring(0, 0, 2.6f * u, w);
                for (int i = 0; i < 4; i++) {
                    double a = Math.toRadians(45 + i * 90);
                    float s = (float) Math.sin(a), c = (float) -Math.cos(a);
                    Reticle.line(s * 3.4f * u, c * 3.4f * u, s * 10.4f * u, c * 10.4f * u, w * 0.7f);
                }
                Reticle.colour(accent);
                float d = (6f + 2.2f * pulse) * u * open;
                for (int i = 0; i < 4; i++) {
                    double a = Math.toRadians(i * 90);
                    Reticle.arrow((float) Math.sin(a) * d, (float) -Math.cos(a) * d, 1.5f * u, i * 90 + 180f);
                }
                break;
            }
            case HUD_TARGET: {
                Reticle.arc(0, 0, 12.5f * u * open, w * 1.6f, 0f, 250f, 40);
                Reticle.colour(accent);
                Reticle.arc(0, 0, 12.5f * u * open, w * 1.6f, 270f, 345f, 12);
                Reticle.colour(main);
                Reticle.line(-10.5f * u, 0, 10.5f * u, 0, w * 0.8f);
                Reticle.line(0, -10.5f * u, 0, 10.5f * u, w * 0.8f);
                Reticle.rect(-10.5f * u - w, -w, -10.5f * u + w, w);
                Reticle.rect(10.5f * u - w, -w, 10.5f * u + w, w);
                Reticle.rect(-w, -10.5f * u - w, w, -10.5f * u + w);
                Reticle.rect(-w, 10.5f * u - w, w, 10.5f * u + w);
                Reticle.colour(accent);
                Reticle.disc(0, 0, 2.8f * u * (0.85f + 0.15f * pulse));
                float spin = t * 35f;
                for (int i = 0; i < 4; i++) {
                    float rot = spin + 45f + i * 90f;
                    double a = Math.toRadians(rot);
                    float d = 7.2f * u * open;
                    Reticle.colour(i % 2 == 0 ? accent : main);
                    Reticle.arrow((float) Math.sin(a) * d, (float) -Math.cos(a) * d, 1.8f * u, rot + 180f);
                }
                break;
            }
            case HUD_SIGIL:
            default: {
                Reticle.polygon(0, 0, 6, 14f * u * open, w * 1.2f, 0f);
                Reticle.colour(Glass.alpha(main, 0.85f));
                Reticle.dots(0, 0, 10.5f * u, 40, 0.55f * u, t * 15f);
                Reticle.colour(accent);
                // A hexagon of brackets: each vertex carries two short strokes along its edges.
                float hr = 6.6f * u * open;
                float turn = -t * 20f;
                for (int i = 0; i < 6; i++) {
                    double a = Math.toRadians(turn + i * 60);
                    double b0 = Math.toRadians(turn + (i - 1) * 60), b1 = Math.toRadians(turn + (i + 1) * 60);
                    float vx = (float) Math.sin(a) * hr, vy = (float) -Math.cos(a) * hr;
                    float px = (float) Math.sin(b0) * hr, py = (float) -Math.cos(b0) * hr;
                    float nx = (float) Math.sin(b1) * hr, ny = (float) -Math.cos(b1) * hr;
                    Reticle.line(vx, vy, vx + (px - vx) * 0.32f, vy + (py - vy) * 0.32f, w * 1.4f);
                    Reticle.line(vx, vy, vx + (nx - vx) * 0.32f, vy + (ny - vy) * 0.32f, w * 1.4f);
                }
                Reticle.colour(main);
                Reticle.ring(0, 0, 4.4f * u, w);
                Reticle.line(-2.4f * u, 0, 2.4f * u, 0, w);
                Reticle.line(0, -2.4f * u, 0, 2.4f * u, w);
                break;
            }
        }
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
