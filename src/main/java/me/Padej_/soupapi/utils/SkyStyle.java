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
    CIRCUIT_SKY,
    /** Violet tears across a nebula, glass fragments drifting out of them. */
    DIMENSIONAL_RIFT,
    /** A sky on fire: flame rising off the horizon under rolling black smoke. */
    INFERNAL,
    /** A neon horizon grid, a striped sun and code raining down the dome. */
    CYBERPUNK,
    /** Rings of runes turning overhead, constellations drawn between the stars. */
    ARCANE,
    /** The sky broken into panes of tinted glass, edges catching the light. */
    SHATTERED_GLASS,
    /** Near-black, with veins of energy pulsing slowly through it. */
    VOID_ENERGY,
    /** Red cloud radiating from a burning light directly overhead. */
    BLEEDING_CLOUDS,
    /** A grey overcast with still, dark figures suspended in it. */
    SUSPENDED,
    /** Pastel pink-and-blue cloudscape, with eyes, doors and a stairway floating in it. */
    DREAMCORE,
    /** Violet and pink clouds, and a vast pale face in them that breathes and blinks. */
    DREAM,

    // ---- after the layers ------------------------------------------------------------------
    /** White and gold: great halos turning overhead and shafts of light fanning down from them. */
    HEAVEN_GATE,
    /** A crimson sky and a vast red sun on the horizon, a black disc sliding across it. */
    BLOOD_SUN,
    /** Black sky, a red horizon, and a colossal crown of fire hanging overhead, shedding ash. */
    HERESY_CROWN,
    /** Black and terminal green: a wireframe floor, rings pulsing out, a beam sweeping round. */
    CYBER_PULSE,
    /** A slate sky churning in bands, sheets of rain and forks of lightning. */
    TEMPEST,

    // ---- and further out -------------------------------------------------------------------
    /** A black hole low in the sky: a glowing accretion disc, a photon ring, the dark at the middle. */
    BLACK_HOLE,
    /** A spiral galaxy wheeling overhead, its arms thick with stars. */
    GALAXY,
    /** A great moon broken into pieces, the fragments slowly drifting apart round it. */
    SHATTERED_MOON,
    /** Giant neon rings hung at angles across the sky, turning, over a glowing horizon. */
    NEON_HALO,
    /** Gold haze over everything, a pale sun throwing long rays, sand streaming past. */
    GOLDEN_HAZE;

    /**
     * True for the styles whose identity is their own colours (fire, blood, grey overcast); the
     * rest are painted in the theme's.
     */
    public boolean fixedColours() {
        switch (this) {
            case INFERNAL:
            case BLEEDING_CLOUDS:
            case SUSPENDED:
            case STORM_FRONT:
            case SUNSET:
            case DREAMCORE:
            case DREAM:
            case HEAVEN_GATE:
            case BLOOD_SUN:
            case HERESY_CROWN:
            case CYBER_PULSE:
            case TEMPEST:
            case BLACK_HOLE:
            case SHATTERED_MOON:
            case GOLDEN_HAZE:
                return true;
            default:
                return false;
        }
    }

    /**
     * True for the skies that are a scene of their own - a sun on the horizon, a hole in the sky,
     * a crown of fire - where vanilla's blocky cloud layer drifting across the front of it would
     * just look pasted on.
     */
    public boolean hidesClouds() {
        switch (this) {
            case SHATTERED_GLASS:
            case HEAVEN_GATE:
            case BLOOD_SUN:
            case HERESY_CROWN:
            case CYBER_PULSE:
            case TEMPEST:
            case BLACK_HOLE:
            case GALAXY:
            case SHATTERED_MOON:
            case NEON_HALO:
            case GOLDEN_HAZE:
            case VOID_ENERGY:
            case DIMENSIONAL_RIFT:
            case ECLIPSE:
                return true;
            default:
                return false;
        }
    }

    /** True for the styles that also want points of light scattered over them. */
    public boolean wantsStars() {
        switch (this) {
            case NEBULA:
            case VOID_RIFT:
            case VORTEX:
            case ECLIPSE:
            case LATTICE:
            case DIMENSIONAL_RIFT:
            case ARCANE:
            case SHATTERED_GLASS:
            case VOID_ENERGY:
            case CYBERPUNK:
            case BLACK_HOLE:
            case GALAXY:
            case SHATTERED_MOON:
            case HERESY_CROWN:
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
            case INFERNAL:
            case CYBERPUNK:
                return 1.6f;
            case PLASMA:
            case CIRCUIT_SKY:
            case RIPPLE:
                return 1.4f;
            case NEBULA:
            case SILK:
            case SUNSET:
            case OCEAN_DEPTH:
            case BLEEDING_CLOUDS:
            case SUSPENDED:
            case ARCANE:
            case DREAMCORE:
            case DREAM:
                return 0.45f;
            default:
                return 1f;
        }
    }
}
