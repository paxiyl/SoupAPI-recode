package me.Padej_.soupapi.utils;

import me.Padej_.soupapi.config.ConfigurableModule;
import net.minecraft.util.Identifier;

import java.util.Random;

public class TexturesManager extends ConfigurableModule {
    private static final Random random = new Random();

    private static final Identifier CIRCLE = Identifier.of("soupapi", "textures/jump_circles/circle.png");
    private static final Identifier CIRCLE_BOLD = Identifier.of("soupapi", "textures/jump_circles/circle_bold.png");
    private static final Identifier HEXAGON = Identifier.of("soupapi", "textures/jump_circles/hexagon.png");
    public static final Identifier PORTAL = Identifier.of("soupapi", "textures/jump_circles/portal.png");
    public static final Identifier SOUP = Identifier.of("soupapi", "textures/jump_circles/soup.png");
    public static final Identifier PORTAL_2 = Identifier.of("soupapi", "textures/jump_circles/portal_2.png");

    private static final Identifier CIRCLE_UNBLACK = Identifier.of("soupapi", "textures/jump_circles/circle_unblack.png");
    private static final Identifier CIRCLE_BOLD_UNBLACK = Identifier.of("soupapi", "textures/jump_circles/circle_bold_unblack.png");
    private static final Identifier HEXAGON_UNBLACK = Identifier.of("soupapi", "textures/jump_circles/hexagon_unblack.png");
    private static final Identifier PORTAL_UNBLACK = Identifier.of("soupapi", "textures/jump_circles/portal_unblack.png");
    private static final Identifier SOUP_UNBLACK = Identifier.of("soupapi", "textures/jump_circles/soup_unblack.png");

    public static final Identifier LEGACY = Identifier.of("soupapi", "textures/target_render/legacy.png");
    private static final Identifier SCIFI = Identifier.of("soupapi", "textures/target_render/scifi.png");
    private static final Identifier SIMPLE = Identifier.of("soupapi", "textures/target_render/simple.png");
    private static final Identifier BO = Identifier.of("soupapi", "textures/target_render/bo.png");
    private static final Identifier MARKER = Identifier.of("soupapi", "textures/target_render/marker.png");
    private static final Identifier SKULL = Identifier.of("soupapi", "textures/target_render/skull.png");
    public static final Identifier AMOGUS = Identifier.of("soupapi", "textures/target_render/amongus.png");
    private static final Identifier VEGAS = Identifier.of("soupapi", "textures/target_render/vegas.png");
    private static final Identifier JEKA = Identifier.of("soupapi", "textures/target_render/jeka.png");

    public static final Identifier FIREFLY = Identifier.of("soupapi", "textures/particle/firefly.png");
    public static final Identifier FIREFLY_GLOW = Identifier.of("soupapi", "textures/particle/firefly_glow.png");
    public static final Identifier FIREFLY_ALT = Identifier.of("soupapi", "textures/particle/firefly_alt.png");
    public static final Identifier FIREFLY_ALT_GLOW = Identifier.of("soupapi", "textures/particle/firefly_alt_glow.png");
    public static final Identifier DOLLAR = Identifier.of("soupapi", "textures/particle/dollar.png");
    public static final Identifier DOLLAR_UNBLACK = Identifier.of("soupapi", "textures/particle/dollar_unblack.png");
    public static final Identifier SNOWFLAKE = Identifier.of("soupapi", "textures/particle/snowflake.png");
    public static final Identifier SNOWFLAKE_UNBLACK = Identifier.of("soupapi", "textures/particle/snowflake_unblack.png");
    public static final Identifier HEART = Identifier.of("soupapi", "textures/particle/heart.png");
    public static final Identifier HEART_UNBLACK = Identifier.of("soupapi", "textures/particle/heart_unblack.png");
    public static final Identifier STAR = Identifier.of("soupapi", "textures/particle/star.png");
    public static final Identifier STAR_UNBLACK = Identifier.of("soupapi", "textures/particle/star_unblack.png");

    /**
     * GLYPHS
     **/
    public static final Identifier GLYPH_ABS = Identifier.of("soupapi", "textures/particle/glyph/abs.png");
    public static final Identifier GLYPH_ARROW = Identifier.of("soupapi", "textures/particle/glyph/arrow.png");
    public static final Identifier GLYPH_ARROW_LINE = Identifier.of("soupapi", "textures/particle/glyph/arrow_line.png");
    public static final Identifier GLYPH_CIRCLE = Identifier.of("soupapi", "textures/particle/glyph/circle.png");
    public static final Identifier GLYPH_CROSS = Identifier.of("soupapi", "textures/particle/glyph/cross.png");
    public static final Identifier GLYPH_FLOWER = Identifier.of("soupapi", "textures/particle/glyph/flower.png");
    public static final Identifier GLYPH_FOREVER = Identifier.of("soupapi", "textures/particle/glyph/forever.png");
    public static final Identifier GLYPH_LINE = Identifier.of("soupapi", "textures/particle/glyph/line.png");
    public static final Identifier GLYPH_QUAD = Identifier.of("soupapi", "textures/particle/glyph/quad.png");
    public static final Identifier GLYPH_STAR = Identifier.of("soupapi", "textures/particle/glyph/star.png");
    public static final Identifier GLYPH_TRIANGLE = Identifier.of("soupapi", "textures/particle/glyph/triangle.png");
    public static final Identifier GLYPH_ZIGZAG = Identifier.of("soupapi", "textures/particle/glyph/zigzag.png");

    public static final Identifier ANON_SKIN = Identifier.of("soupapi", "textures/skin/anon_skin.png");

    public static final Identifier GUI_BUBBLE = Identifier.of("soupapi", "textures/gui/bubble.png");
    public static final Identifier GUI_HAM = Identifier.of("soupapi", "textures/gui/ham.png");
    public static final Identifier GUI_HEART = Identifier.of("soupapi", "textures/gui/heart.png");
    public static final Identifier GUI_SHIELD = Identifier.of("soupapi", "textures/gui/shield.png");
    public static final Identifier GUI_POTION = Identifier.of("soupapi", "textures/gui/potion.png");
    public static final Identifier GUI_HITBOX = Identifier.of("soupapi", "textures/gui/hitbox.png");

    public static final Identifier MC_TIERS_LOGO = Identifier.of("soupapi", "textures/gui/mctiers/mctiers.png");
    public static final Identifier ROOKIE = Identifier.of("soupapi", "textures/gui/mctiers/rookie.png");
    public static final Identifier LTMS = Identifier.of("soupapi", "textures/gui/mctiers/2v2.png");
    public static final Identifier AXE = Identifier.of("soupapi", "textures/gui/mctiers/axe.png");
    public static final Identifier MACE = Identifier.of("soupapi", "textures/gui/mctiers/mace.png");
    public static final Identifier NETHOP = Identifier.of("soupapi", "textures/gui/mctiers/nethop.png");
    public static final Identifier POT = Identifier.of("soupapi", "textures/gui/mctiers/pot.png");
    public static final Identifier SMP = Identifier.of("soupapi", "textures/gui/mctiers/smp.png");
    public static final Identifier SWORD = Identifier.of("soupapi", "textures/gui/mctiers/sword.png");
    public static final Identifier UHC = Identifier.of("soupapi", "textures/gui/mctiers/uhc.png");
    public static final Identifier VANILLA = Identifier.of("soupapi", "textures/gui/mctiers/vanilla.png");

    public static final Identifier[] GLYPH_TEXTURES = new Identifier[] {
            GLYPH_ABS, GLYPH_LINE, GLYPH_ARROW, GLYPH_ARROW_LINE,
            GLYPH_CIRCLE, GLYPH_CROSS, GLYPH_FLOWER, GLYPH_FOREVER,
            GLYPH_QUAD, GLYPH_STAR, GLYPH_TRIANGLE, GLYPH_ZIGZAG,
    };

    public static Identifier getJumpCircle() {
        switch (CONFIG.jumpCirclesStyle) {
            case CIRCLE:
                return CIRCLE;
            case PORTAL:
                return PORTAL;
            case HEXAGON:
                return HEXAGON;
            case CIRCLE_BOLD:
                return CIRCLE_BOLD;
            case SOUP:
                return SOUP;
        }
        throw new IllegalStateException("Unexpected value: " + CONFIG.jumpCirclesStyle);
    }

    public static Identifier getTargetRenderTexture() {
        switch (CONFIG.targetRenderLegacyTexture) {
            case LEGACY:
                return LEGACY;
            case MARKER:
                return MARKER;
            case BO:
                return BO;
            case SIMPLE:
                return SIMPLE;
            case SCIFI:
                return SCIFI;
            case JEKA:
                return JEKA;
            case AMONGUS:
                return AMOGUS;
            case SKULL:
                return SKULL;
            case VEGAS:
                return VEGAS;
        }
        throw new IllegalStateException("Unexpected value: " + CONFIG.targetRenderLegacyTexture);
    }

    public static Identifier getHitBubbleTexture() {
        switch (CONFIG.hitBubblesStyle) {
            case CIRCLE:
                return CIRCLE;
            case CIRCLE_BOLD:
                return CIRCLE_BOLD;
            case HEXAGON:
                return HEXAGON;
            case PORTAL:
                return PORTAL;
            case PORTAL_2:
                return PORTAL_2;
        }
        throw new IllegalStateException("Unexpected value: " + CONFIG.hitBubblesStyle);
    }

    public static Identifier getMC_TiersGameModeTexture() {
        switch (CONFIG.mctiersGameMode) {
            case LTMs:
                return LTMS;
            case AXE:
                return AXE;
            case MACE:
                return MACE;
            case NETHER_OP:
                return NETHOP;
            case POT:
                return POT;
            case SMP:
                return SMP;
            case SWORD:
                return SWORD;
            case UHC:
                return UHC;
            case VANILLA:
                return VANILLA;
        }
        throw new IllegalStateException("Unexpected value: " + CONFIG.mctiersGameMode);
    }

    public static Identifier getRandomGlyphParticle() {
        int index = random.nextInt(GLYPH_TEXTURES.length);
        return GLYPH_TEXTURES[index];
    }

    public static Identifier getSoulTexture() {
        switch (CONFIG.targetRenderSoulTexture) {
            case FIREFLY:
                return FIREFLY;
            case ALT:
                return FIREFLY_ALT;
        }
        throw new IllegalStateException("Unexpected value: " + CONFIG.targetRenderSoulTexture);
    }
}
