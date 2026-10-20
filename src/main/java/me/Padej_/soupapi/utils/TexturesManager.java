package me.Padej_.soupapi.utils;

import me.Padej_.soupapi.config.ConfigurableModule;
import net.minecraft.util.ResourceLocation;

import java.util.Random;

public class TexturesManager extends ConfigurableModule {
    private static final Random random = new Random();

    private static final ResourceLocation CIRCLE = new ResourceLocation("soupapi", "textures/jump_circles/circle.png");
    private static final ResourceLocation CIRCLE_BOLD = new ResourceLocation("soupapi", "textures/jump_circles/circle_bold.png");
    private static final ResourceLocation HEXAGON = new ResourceLocation("soupapi", "textures/jump_circles/hexagon.png");
    public static final ResourceLocation PORTAL = new ResourceLocation("soupapi", "textures/jump_circles/portal.png");
    public static final ResourceLocation SOUP = new ResourceLocation("soupapi", "textures/jump_circles/soup.png");
    public static final ResourceLocation PORTAL_2 = new ResourceLocation("soupapi", "textures/jump_circles/portal_2.png");

    public static final ResourceLocation RUNE_RING = new ResourceLocation("soupapi", "textures/jump_circles/rune_ring.png");
    public static final ResourceLocation SAKURA_RING = new ResourceLocation("soupapi", "textures/jump_circles/sakura_ring.png");
    public static final ResourceLocation SNOWFLAKE_RING = new ResourceLocation("soupapi", "textures/jump_circles/snowflake_ring.png");
    public static final ResourceLocation MAGIC_RING = new ResourceLocation("soupapi", "textures/jump_circles/magic_ring.png");
    public static final ResourceLocation SHATTER_RING = new ResourceLocation("soupapi", "textures/jump_circles/shatter_ring.png");


    public static final ResourceLocation LEGACY = new ResourceLocation("soupapi", "textures/target_render/legacy.png");
    private static final ResourceLocation SCIFI = new ResourceLocation("soupapi", "textures/target_render/scifi.png");
    private static final ResourceLocation SIMPLE = new ResourceLocation("soupapi", "textures/target_render/simple.png");
    private static final ResourceLocation BO = new ResourceLocation("soupapi", "textures/target_render/bo.png");
    private static final ResourceLocation MARKER = new ResourceLocation("soupapi", "textures/target_render/marker.png");
    private static final ResourceLocation SKULL = new ResourceLocation("soupapi", "textures/target_render/skull.png");
    public static final ResourceLocation AMOGUS = new ResourceLocation("soupapi", "textures/target_render/amongus.png");
    private static final ResourceLocation VEGAS = new ResourceLocation("soupapi", "textures/target_render/vegas.png");
    private static final ResourceLocation JEKA = new ResourceLocation("soupapi", "textures/target_render/jeka.png");

    public static final ResourceLocation FIREFLY = new ResourceLocation("soupapi", "textures/particle/firefly.png");
    public static final ResourceLocation FIREFLY_ALT = new ResourceLocation("soupapi", "textures/particle/firefly_alt.png");
    public static final ResourceLocation DOLLAR = new ResourceLocation("soupapi", "textures/particle/dollar.png");
    public static final ResourceLocation SNOWFLAKE = new ResourceLocation("soupapi", "textures/particle/snowflake.png");
    public static final ResourceLocation HEART = new ResourceLocation("soupapi", "textures/particle/heart.png");
    public static final ResourceLocation STAR = new ResourceLocation("soupapi", "textures/particle/star.png");

    /**
     * SYMBOLS
     *
     * <p>Every symbol under {@code particle/glyph/}. The set went from twelve thin line drawings to
     * 43 heavier ones; see {@link Symbol} for picking a single symbol instead of a random one.
     **/
    public static final ResourceLocation GLYPH_ABS = new ResourceLocation("soupapi", "textures/particle/glyph/abs.png");
    public static final ResourceLocation GLYPH_ANKH = new ResourceLocation("soupapi", "textures/particle/glyph/ankh.png");
    public static final ResourceLocation GLYPH_ARROW = new ResourceLocation("soupapi", "textures/particle/glyph/arrow.png");
    public static final ResourceLocation GLYPH_ARROW_HEAVY = new ResourceLocation("soupapi", "textures/particle/glyph/arrow_heavy.png");
    public static final ResourceLocation GLYPH_ARROW_LINE = new ResourceLocation("soupapi", "textures/particle/glyph/arrow_line.png");
    public static final ResourceLocation GLYPH_ATOM = new ResourceLocation("soupapi", "textures/particle/glyph/atom.png");
    public static final ResourceLocation GLYPH_BIOHAZARD = new ResourceLocation("soupapi", "textures/particle/glyph/biohazard.png");
    public static final ResourceLocation GLYPH_BLOSSOM = new ResourceLocation("soupapi", "textures/particle/glyph/blossom.png");
    public static final ResourceLocation GLYPH_BOLT = new ResourceLocation("soupapi", "textures/particle/glyph/bolt.png");
    public static final ResourceLocation GLYPH_CIRCLE = new ResourceLocation("soupapi", "textures/particle/glyph/circle.png");
    public static final ResourceLocation GLYPH_CIRCUIT = new ResourceLocation("soupapi", "textures/particle/glyph/circuit.png");
    public static final ResourceLocation GLYPH_CLOVER = new ResourceLocation("soupapi", "textures/particle/glyph/clover.png");
    public static final ResourceLocation GLYPH_CROSS = new ResourceLocation("soupapi", "textures/particle/glyph/cross.png");
    public static final ResourceLocation GLYPH_CROSS_SWORDS = new ResourceLocation("soupapi", "textures/particle/glyph/cross_swords.png");
    public static final ResourceLocation GLYPH_CROWN = new ResourceLocation("soupapi", "textures/particle/glyph/crown.png");
    public static final ResourceLocation GLYPH_DIAMOND_SUIT = new ResourceLocation("soupapi", "textures/particle/glyph/diamond_suit.png");
    public static final ResourceLocation GLYPH_EYE = new ResourceLocation("soupapi", "textures/particle/glyph/eye.png");
    public static final ResourceLocation GLYPH_FANG = new ResourceLocation("soupapi", "textures/particle/glyph/fang.png");
    public static final ResourceLocation GLYPH_FLAME = new ResourceLocation("soupapi", "textures/particle/glyph/flame.png");
    public static final ResourceLocation GLYPH_FLOWER = new ResourceLocation("soupapi", "textures/particle/glyph/flower.png");
    public static final ResourceLocation GLYPH_FOREVER = new ResourceLocation("soupapi", "textures/particle/glyph/forever.png");
    public static final ResourceLocation GLYPH_GEAR = new ResourceLocation("soupapi", "textures/particle/glyph/gear.png");
    public static final ResourceLocation GLYPH_HEART_BOLD = new ResourceLocation("soupapi", "textures/particle/glyph/heart_bold.png");
    public static final ResourceLocation GLYPH_HEXAGON_BOLD = new ResourceLocation("soupapi", "textures/particle/glyph/hexagon_bold.png");
    public static final ResourceLocation GLYPH_INFINITY_BOLD = new ResourceLocation("soupapi", "textures/particle/glyph/infinity_bold.png");
    public static final ResourceLocation GLYPH_LEAF = new ResourceLocation("soupapi", "textures/particle/glyph/leaf.png");
    public static final ResourceLocation GLYPH_LINE = new ResourceLocation("soupapi", "textures/particle/glyph/line.png");
    public static final ResourceLocation GLYPH_MOON = new ResourceLocation("soupapi", "textures/particle/glyph/moon.png");
    public static final ResourceLocation GLYPH_PENTAGRAM = new ResourceLocation("soupapi", "textures/particle/glyph/pentagram.png");
    public static final ResourceLocation GLYPH_QUAD = new ResourceLocation("soupapi", "textures/particle/glyph/quad.png");
    public static final ResourceLocation GLYPH_RUNE_ALGIZ = new ResourceLocation("soupapi", "textures/particle/glyph/rune_algiz.png");
    public static final ResourceLocation GLYPH_RUNE_THURISAZ = new ResourceLocation("soupapi", "textures/particle/glyph/rune_thurisaz.png");
    public static final ResourceLocation GLYPH_SHURIKEN = new ResourceLocation("soupapi", "textures/particle/glyph/shuriken.png");
    public static final ResourceLocation GLYPH_SKULL = new ResourceLocation("soupapi", "textures/particle/glyph/skull.png");
    public static final ResourceLocation GLYPH_SNOW_CRYSTAL = new ResourceLocation("soupapi", "textures/particle/glyph/snow_crystal.png");
    public static final ResourceLocation GLYPH_SPADE = new ResourceLocation("soupapi", "textures/particle/glyph/spade.png");
    public static final ResourceLocation GLYPH_STAR = new ResourceLocation("soupapi", "textures/particle/glyph/star.png");
    public static final ResourceLocation GLYPH_SUN = new ResourceLocation("soupapi", "textures/particle/glyph/sun.png");
    public static final ResourceLocation GLYPH_TARGET = new ResourceLocation("soupapi", "textures/particle/glyph/target.png");
    public static final ResourceLocation GLYPH_TRIANGLE = new ResourceLocation("soupapi", "textures/particle/glyph/triangle.png");
    public static final ResourceLocation GLYPH_TRIQUETRA = new ResourceLocation("soupapi", "textures/particle/glyph/triquetra.png");
    public static final ResourceLocation GLYPH_WAVE = new ResourceLocation("soupapi", "textures/particle/glyph/wave.png");
    public static final ResourceLocation GLYPH_ZIGZAG = new ResourceLocation("soupapi", "textures/particle/glyph/zigzag.png");

    public static final ResourceLocation ANON_SKIN = new ResourceLocation("soupapi", "textures/skin/anon_skin.png");

    public static final ResourceLocation GUI_BUBBLE = new ResourceLocation("soupapi", "textures/gui/bubble.png");
    public static final ResourceLocation GUI_HAM = new ResourceLocation("soupapi", "textures/gui/ham.png");
    public static final ResourceLocation GUI_HEART = new ResourceLocation("soupapi", "textures/gui/heart.png");
    public static final ResourceLocation GUI_SHIELD = new ResourceLocation("soupapi", "textures/gui/shield.png");
    public static final ResourceLocation GUI_POTION = new ResourceLocation("soupapi", "textures/gui/potion.png");
    public static final ResourceLocation GUI_HITBOX = new ResourceLocation("soupapi", "textures/gui/hitbox.png");

    public static final ResourceLocation MC_TIERS_LOGO = new ResourceLocation("soupapi", "textures/gui/mctiers/mctiers.png");
    public static final ResourceLocation ROOKIE = new ResourceLocation("soupapi", "textures/gui/mctiers/rookie.png");
    public static final ResourceLocation LTMS = new ResourceLocation("soupapi", "textures/gui/mctiers/2v2.png");
    public static final ResourceLocation AXE = new ResourceLocation("soupapi", "textures/gui/mctiers/axe.png");
    public static final ResourceLocation MACE = new ResourceLocation("soupapi", "textures/gui/mctiers/mace.png");
    public static final ResourceLocation NETHOP = new ResourceLocation("soupapi", "textures/gui/mctiers/nethop.png");
    public static final ResourceLocation POT = new ResourceLocation("soupapi", "textures/gui/mctiers/pot.png");
    public static final ResourceLocation SMP = new ResourceLocation("soupapi", "textures/gui/mctiers/smp.png");
    public static final ResourceLocation SWORD = new ResourceLocation("soupapi", "textures/gui/mctiers/sword.png");
    public static final ResourceLocation UHC = new ResourceLocation("soupapi", "textures/gui/mctiers/uhc.png");
    public static final ResourceLocation VANILLA = new ResourceLocation("soupapi", "textures/gui/mctiers/vanilla.png");

public static final ResourceLocation[] GLYPH_TEXTURES = new ResourceLocation[] {
            GLYPH_ABS, GLYPH_ANKH, GLYPH_ARROW, GLYPH_ARROW_HEAVY,
            GLYPH_ARROW_LINE, GLYPH_ATOM, GLYPH_BIOHAZARD, GLYPH_BLOSSOM,
            GLYPH_BOLT, GLYPH_CIRCLE, GLYPH_CIRCUIT, GLYPH_CLOVER,
            GLYPH_CROSS, GLYPH_CROSS_SWORDS, GLYPH_CROWN, GLYPH_DIAMOND_SUIT,
            GLYPH_EYE, GLYPH_FANG, GLYPH_FLAME, GLYPH_FLOWER,
            GLYPH_FOREVER, GLYPH_GEAR, GLYPH_HEART_BOLD, GLYPH_HEXAGON_BOLD,
            GLYPH_INFINITY_BOLD, GLYPH_LEAF, GLYPH_LINE, GLYPH_MOON,
            GLYPH_PENTAGRAM, GLYPH_QUAD, GLYPH_RUNE_ALGIZ, GLYPH_RUNE_THURISAZ,
            GLYPH_SHURIKEN, GLYPH_SKULL, GLYPH_SNOW_CRYSTAL, GLYPH_SPADE,
            GLYPH_STAR, GLYPH_SUN, GLYPH_TARGET, GLYPH_TRIANGLE,
            GLYPH_TRIQUETRA, GLYPH_WAVE, GLYPH_ZIGZAG,
    };

    public static ResourceLocation getJumpCircle() {
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
            case RUNE:
                return RUNE_RING;
            case SAKURA:
                return SAKURA_RING;
            case SNOWFLAKE:
                return SNOWFLAKE_RING;
            case MAGIC:
                return MAGIC_RING;
            case SHATTER:
                return SHATTER_RING;
        }
        throw new IllegalStateException("Unexpected value: " + CONFIG.jumpCirclesStyle);
    }

    public static ResourceLocation getTargetRenderTexture() {
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

    public static ResourceLocation getHitBubbleTexture() {
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
            case RUNE:
                return RUNE_RING;
            case SAKURA:
                return SAKURA_RING;
            case SNOWFLAKE:
                return SNOWFLAKE_RING;
            case MAGIC:
                return MAGIC_RING;
            case SHATTER:
                return SHATTER_RING;
        }
        throw new IllegalStateException("Unexpected value: " + CONFIG.hitBubblesStyle);
    }

    public static ResourceLocation getMC_TiersGameModeTexture() {
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

    /**
     * The sprite a particle should use.
     *
     * <p>{@code particleSpriteOverride} replaces every effect's own sprite when it is set, which is
     * what makes a theme change how the particles look and not just their colour. A theme writes it
     * when it is picked; it is an ordinary setting the rest of the time, so it can be set — and
     * unset — without one.
     */
    public static ResourceLocation themed(ResourceLocation fallback) {
        ParticleSprite override = CONFIG.particleSpriteOverride;
        ResourceLocation sprite = override == null ? null : override.texture();
        return sprite != null ? sprite : fallback;
    }

    /**
     * The symbol a glyph particle should use.
     *
     * <p>Honours the chosen symbol, falling back to a random one when it is set to Random.
     */
    public static ResourceLocation getRandomGlyphParticle() {
        Symbol symbol = CONFIG.particleSymbol;
        if (symbol != null && symbol != Symbol.RANDOM) {
            return symbol.texture();
        }
        int index = random.nextInt(GLYPH_TEXTURES.length);
        return GLYPH_TEXTURES[index];
    }

    public static ResourceLocation getSoulTexture() {
        switch (CONFIG.targetRenderSoulTexture) {
            case FIREFLY:
                return FIREFLY;
            case ALT:
                return FIREFLY_ALT;
        }
        throw new IllegalStateException("Unexpected value: " + CONFIG.targetRenderSoulTexture);
    }
}
