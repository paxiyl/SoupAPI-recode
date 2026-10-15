package me.Padej_.soupapi.utils;

import me.Padej_.soupapi.config.ConfigurableModule;
import net.minecraft.util.Identifier;

import java.util.Random;

public class TexturesManager extends ConfigurableModule {
    private static final Random random = new Random();

    private static final Identifier CIRCLE = new Identifier("soupapi", "textures/jump_circles/circle.png");
    private static final Identifier CIRCLE_BOLD = new Identifier("soupapi", "textures/jump_circles/circle_bold.png");
    private static final Identifier HEXAGON = new Identifier("soupapi", "textures/jump_circles/hexagon.png");
    public static final Identifier PORTAL = new Identifier("soupapi", "textures/jump_circles/portal.png");
    public static final Identifier SOUP = new Identifier("soupapi", "textures/jump_circles/soup.png");
    public static final Identifier PORTAL_2 = new Identifier("soupapi", "textures/jump_circles/portal_2.png");

    private static final Identifier CIRCLE_UNBLACK = new Identifier("soupapi", "textures/jump_circles/circle_unblack.png");
    private static final Identifier CIRCLE_BOLD_UNBLACK = new Identifier("soupapi", "textures/jump_circles/circle_bold_unblack.png");
    private static final Identifier HEXAGON_UNBLACK = new Identifier("soupapi", "textures/jump_circles/hexagon_unblack.png");
    private static final Identifier PORTAL_UNBLACK = new Identifier("soupapi", "textures/jump_circles/portal_unblack.png");
    private static final Identifier SOUP_UNBLACK = new Identifier("soupapi", "textures/jump_circles/soup_unblack.png");

    public static final Identifier LEGACY = new Identifier("soupapi", "textures/target_render/legacy.png");
    private static final Identifier SCIFI = new Identifier("soupapi", "textures/target_render/scifi.png");
    private static final Identifier SIMPLE = new Identifier("soupapi", "textures/target_render/simple.png");
    private static final Identifier BO = new Identifier("soupapi", "textures/target_render/bo.png");
    private static final Identifier MARKER = new Identifier("soupapi", "textures/target_render/marker.png");
    private static final Identifier SKULL = new Identifier("soupapi", "textures/target_render/skull.png");
    public static final Identifier AMOGUS = new Identifier("soupapi", "textures/target_render/amongus.png");
    private static final Identifier VEGAS = new Identifier("soupapi", "textures/target_render/vegas.png");
    private static final Identifier JEKA = new Identifier("soupapi", "textures/target_render/jeka.png");

    public static final Identifier FIREFLY = new Identifier("soupapi", "textures/particle/firefly.png");
    public static final Identifier FIREFLY_GLOW = new Identifier("soupapi", "textures/particle/firefly_glow.png");
    public static final Identifier FIREFLY_ALT = new Identifier("soupapi", "textures/particle/firefly_alt.png");
    public static final Identifier FIREFLY_ALT_GLOW = new Identifier("soupapi", "textures/particle/firefly_alt_glow.png");
    public static final Identifier DOLLAR = new Identifier("soupapi", "textures/particle/dollar.png");
    public static final Identifier DOLLAR_UNBLACK = new Identifier("soupapi", "textures/particle/dollar_unblack.png");
    public static final Identifier SNOWFLAKE = new Identifier("soupapi", "textures/particle/snowflake.png");
    public static final Identifier SNOWFLAKE_UNBLACK = new Identifier("soupapi", "textures/particle/snowflake_unblack.png");
    public static final Identifier HEART = new Identifier("soupapi", "textures/particle/heart.png");
    public static final Identifier HEART_UNBLACK = new Identifier("soupapi", "textures/particle/heart_unblack.png");
    public static final Identifier STAR = new Identifier("soupapi", "textures/particle/star.png");
    public static final Identifier STAR_UNBLACK = new Identifier("soupapi", "textures/particle/star_unblack.png");

    /**
     * GLYPHS
     **/
    public static final Identifier GLYPH_ABS = new Identifier("soupapi", "textures/particle/glyph/abs.png");
    public static final Identifier GLYPH_ARROW = new Identifier("soupapi", "textures/particle/glyph/arrow.png");
    public static final Identifier GLYPH_ARROW_LINE = new Identifier("soupapi", "textures/particle/glyph/arrow_line.png");
    public static final Identifier GLYPH_CIRCLE = new Identifier("soupapi", "textures/particle/glyph/circle.png");
    public static final Identifier GLYPH_CROSS = new Identifier("soupapi", "textures/particle/glyph/cross.png");
    public static final Identifier GLYPH_FLOWER = new Identifier("soupapi", "textures/particle/glyph/flower.png");
    public static final Identifier GLYPH_FOREVER = new Identifier("soupapi", "textures/particle/glyph/forever.png");
    public static final Identifier GLYPH_LINE = new Identifier("soupapi", "textures/particle/glyph/line.png");
    public static final Identifier GLYPH_QUAD = new Identifier("soupapi", "textures/particle/glyph/quad.png");
    public static final Identifier GLYPH_STAR = new Identifier("soupapi", "textures/particle/glyph/star.png");
    public static final Identifier GLYPH_TRIANGLE = new Identifier("soupapi", "textures/particle/glyph/triangle.png");
    public static final Identifier GLYPH_ZIGZAG = new Identifier("soupapi", "textures/particle/glyph/zigzag.png");

    public static final Identifier ANON_SKIN = new Identifier("soupapi", "textures/skin/anon_skin.png");

    public static final Identifier GUI_BUBBLE = new Identifier("soupapi", "textures/gui/bubble.png");
    public static final Identifier GUI_HAM = new Identifier("soupapi", "textures/gui/ham.png");
    public static final Identifier GUI_HEART = new Identifier("soupapi", "textures/gui/heart.png");
    public static final Identifier GUI_SHIELD = new Identifier("soupapi", "textures/gui/shield.png");
    public static final Identifier GUI_POTION = new Identifier("soupapi", "textures/gui/potion.png");
    public static final Identifier GUI_HITBOX = new Identifier("soupapi", "textures/gui/hitbox.png");

    public static final Identifier MC_TIERS_LOGO = new Identifier("soupapi", "textures/gui/mctiers/mctiers.png");
    public static final Identifier ROOKIE = new Identifier("soupapi", "textures/gui/mctiers/rookie.png");
    public static final Identifier LTMS = new Identifier("soupapi", "textures/gui/mctiers/2v2.png");
    public static final Identifier AXE = new Identifier("soupapi", "textures/gui/mctiers/axe.png");
    public static final Identifier MACE = new Identifier("soupapi", "textures/gui/mctiers/mace.png");
    public static final Identifier NETHOP = new Identifier("soupapi", "textures/gui/mctiers/nethop.png");
    public static final Identifier POT = new Identifier("soupapi", "textures/gui/mctiers/pot.png");
    public static final Identifier SMP = new Identifier("soupapi", "textures/gui/mctiers/smp.png");
    public static final Identifier SWORD = new Identifier("soupapi", "textures/gui/mctiers/sword.png");
    public static final Identifier UHC = new Identifier("soupapi", "textures/gui/mctiers/uhc.png");
    public static final Identifier VANILLA = new Identifier("soupapi", "textures/gui/mctiers/vanilla.png");

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
