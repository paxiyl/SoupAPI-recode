package me.Padej_.soupapi.utils;

import net.minecraft.util.ResourceLocation;

import java.util.Random;

/**
 * The symbol set the particle effects can draw.
 *
 * <p>There are 43 of them, and a particle can be pinned to any single one instead of taking
 * whatever the random pick hands out — which is what the glyph particles did before, so a specific
 * symbol could not be chosen at all.
 */
public enum Symbol {

    /** Any symbol, chosen fresh for each particle. */
    RANDOM(null),
    /** Abs */
    ABS("abs"),
    /** Ankh */
    ANKH("ankh"),
    /** Arrow */
    ARROW("arrow"),
    /** Arrow Heavy */
    ARROW_HEAVY("arrow_heavy"),
    /** Arrow Line */
    ARROW_LINE("arrow_line"),
    /** Atom */
    ATOM("atom"),
    /** Biohazard */
    BIOHAZARD("biohazard"),
    /** Blossom */
    BLOSSOM("blossom"),
    /** Bolt */
    BOLT("bolt"),
    /** Circle */
    CIRCLE("circle"),
    /** Circuit */
    CIRCUIT("circuit"),
    /** Clover */
    CLOVER("clover"),
    /** Cross */
    CROSS("cross"),
    /** Cross Swords */
    CROSS_SWORDS("cross_swords"),
    /** Crown */
    CROWN("crown"),
    /** Diamond Suit */
    DIAMOND_SUIT("diamond_suit"),
    /** Eye */
    EYE("eye"),
    /** Fang */
    FANG("fang"),
    /** Flame */
    FLAME("flame"),
    /** Flower */
    FLOWER("flower"),
    /** Forever */
    FOREVER("forever"),
    /** Gear */
    GEAR("gear"),
    /** Heart Bold */
    HEART_BOLD("heart_bold"),
    /** Hexagon Bold */
    HEXAGON_BOLD("hexagon_bold"),
    /** Infinity Bold */
    INFINITY_BOLD("infinity_bold"),
    /** Leaf */
    LEAF("leaf"),
    /** Line */
    LINE("line"),
    /** Moon */
    MOON("moon"),
    /** Pentagram */
    PENTAGRAM("pentagram"),
    /** Quad */
    QUAD("quad"),
    /** Rune Algiz */
    RUNE_ALGIZ("rune_algiz"),
    /** Rune Thurisaz */
    RUNE_THURISAZ("rune_thurisaz"),
    /** Shuriken */
    SHURIKEN("shuriken"),
    /** Skull */
    SKULL("skull"),
    /** Snow Crystal */
    SNOW_CRYSTAL("snow_crystal"),
    /** Spade */
    SPADE("spade"),
    /** Star */
    STAR("star"),
    /** Sun */
    SUN("sun"),
    /** Target */
    TARGET("target"),
    /** Triangle */
    TRIANGLE("triangle"),
    /** Triquetra */
    TRIQUETRA("triquetra"),
    /** Wave */
    WAVE("wave"),
    /** Zigzag */
    ZIGZAG("zigzag");

    private static final Random RANDOM_SOURCE = new Random();

    private final String file;

    Symbol(String file) {
        this.file = file;
    }

    /** The texture for this symbol, or a random one for {@link #RANDOM}. */
    public ResourceLocation texture() {
        if (file == null) {
            Symbol[] all = values();
            // index 0 is RANDOM itself
            Symbol pick = all[1 + RANDOM_SOURCE.nextInt(all.length - 1)];
            return pick.texture();
        }
        return new ResourceLocation("soupapi", "textures/particle/glyph/" + file + ".png");
    }
}
