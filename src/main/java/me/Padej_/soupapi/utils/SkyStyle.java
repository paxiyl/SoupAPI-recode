package me.Padej_.soupapi.utils;

/**
 * What the sky does, beyond being a colour.
 *
 * <p>A theme used to paint the sky one flat tint, which is why every theme ended up feeling like
 * the same theme: the biggest surface in the game was a single colour that only changed hue. Each
 * of these is a pattern evaluated across the sky and moving with time — bands, ribbons, plasma,
 * rifts, falling code — built out of a vertex-coloured mesh, because 1.8.9 has no shader to put a
 * noise function in and gl4es would not run one.
 */
public enum SkyStyle {

    /** The flat tint the sky had before. */
    PLAIN,
    /** Curtains of light that drift and fold. */
    AURORA,
    /** Soft clouds of colour, slowly turning over. */
    NEBULA,
    /** Horizontal bands sliding past each other. */
    BANDS,
    /** Interference patterns in two colours. */
    PLASMA,
    /** A glowing rip across the sky, breathing. */
    VOID_RIFT,
    /** A grid in perspective, scrolling away. */
    GRID,
    /** A honeycomb that pulses from the centre out. */
    HEX_GRID,
    /** Hard horizontal lines rolling upward. */
    SCANLINES,
    /** Everything spiralling into one point. */
    VORTEX,
    /** A dark disc with a bright edge. */
    ECLIPSE,
    /** Columns of falling code, the sky version. */
    CODE_FALL,
    /** Heat rising off a horizon. */
    EMBER_SKY,
    /** Light shafts coming down through water. */
    OCEAN_DEPTH,
    /** Cells of colour creeping across each other. */
    LAVA_FLOW,
    /** A wash of colour toward one corner, like late sun. */
    SUNSET,
    /** Rings spreading out from overhead. */
    RIPPLE,
    /** Streaks pulled toward the horizon. */
    SPEED_LINES,
    /** Stacked diamonds, shifting in and out of phase. */
    LATTICE,
    /** Slow, wide waves, like a flag. */
    SILK,
    /** A storm front moving across, flickering. */
    STORM_FRONT,
    /** Circuit traces lighting up and going dark. */
    CIRCUIT_SKY;

    /** True for the styles that also want points of light scattered over them. */
    public boolean wantsStars() {
        switch (this) {
            case NEBULA:
            case VOID_RIFT:
            case VORTEX:
            case ECLIPSE:
            case LATTICE:
                return true;
            default:
                return false;
        }
    }

    /** How fast this one moves by default, as a multiplier. */
    public float speed() {
        switch (this) {
            case SCANLINES:
            case CODE_FALL:
            case SPEED_LINES:
            case STORM_FRONT:
                return 2.2f;
            case PLASMA:
            case CIRCUIT_SKY:
            case RIPPLE:
                return 1.4f;
            case NEBULA:
            case SILK:
            case SUNSET:
            case OCEAN_DEPTH:
                return 0.45f;
            default:
                return 1f;
        }
    }
}
