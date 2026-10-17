package me.Padej_.soupapi.utils;

/**
 * The weather a theme brings with it.
 *
 * <p>Themes used to change colours and then shapes; what they could not change was the air. A
 * Matrix theme that only tints the HUD green is not a Matrix theme — the point of it is the rain of
 * glyphs down the screen. Each of these is a volume of particles around the camera with its own
 * motion, sprite and colouring, drawn by {@link me.Padej_.soupapi.modules.WorldAmbience}.
 */
public enum Ambience {

    /** Nothing in the air. */
    NONE,
    /** Columns of glyphs falling in front of the player, brightest at the head of each column. */
    MATRIX_RAIN,
    /** Code fragments drifting upward instead of falling. */
    MATRIX_RISE,
    SNOW,
    BLIZZARD,
    EMBERS,
    ASH,
    FIREFLIES,
    PETALS,
    LEAVES,
    RAIN,
    DRIZZLE,
    BUBBLES,
    SPORES,
    DUST,
    SPARKS,
    STARFIELD,
    METEORS,
    AURORA,
    SOULS,
    HEARTS,
    CONFETTI,
    GLITCH,
    VOID_MOTES,
    PLANKTON,
    SANDSTORM,
    FEATHERS,
    BLOOD_RAIN,
    FROST_SHARDS,
    GOLD_DUST,
    NEON_RAIN,
    CIRCUIT_MOTES,
    PENTAGRAMS,
    SKULLS,
    SNOWFLAKE_SPIN,
    LANTERNS,
    JELLYFISH,
    RUNE_FALL,
    DIGITAL_SNOW,
    SOLAR_FLARE,
    THUNDERBUGS;

    /** Whether the particles fall, rise, or hang about. */
    public float gravity() {
        switch (this) {
            case MATRIX_RAIN:
            case NEON_RAIN:
            case RUNE_FALL:
            case DIGITAL_SNOW:
                return -0.55f;
            case RAIN:
            case BLOOD_RAIN:
                return -1.4f;
            case DRIZZLE:
                return -0.8f;
            case BLIZZARD:
            case SANDSTORM:
                return -0.25f;
            case SNOW:
            case SNOWFLAKE_SPIN:
            case FROST_SHARDS:
            case PETALS:
            case LEAVES:
            case FEATHERS:
            case CONFETTI:
            case ASH:
                return -0.12f;
            case EMBERS:
            case SOULS:
            case LANTERNS:
            case MATRIX_RISE:
            case BUBBLES:
            case PLANKTON:
            case SOLAR_FLARE:
                return 0.14f;
            case HEARTS:
                return 0.08f;
            case METEORS:
                return -0.9f;
            default:
                return 0f;
        }
    }

    /** How far the particles wander sideways each tick. */
    public float drift() {
        switch (this) {
            case BLIZZARD:
            case SANDSTORM:
                return 0.5f;
            case SNOW:
            case PETALS:
            case LEAVES:
            case FEATHERS:
            case CONFETTI:
                return 0.14f;
            case FIREFLIES:
            case SPORES:
            case DUST:
            case PLANKTON:
            case JELLYFISH:
            case THUNDERBUGS:
                return 0.09f;
            case GLITCH:
            case DIGITAL_SNOW:
                return 0.3f;
            case MATRIX_RAIN:
            case NEON_RAIN:
            case RAIN:
            case DRIZZLE:
            case BLOOD_RAIN:
                return 0.01f;
            default:
                return 0.05f;
        }
    }

    /** The sprite each particle draws, as a path under {@code textures/particle/}. */
    public String sprite() {
        switch (this) {
            case MATRIX_RAIN:
            case MATRIX_RISE:
            case DIGITAL_SNOW:
                return "glyph/circuit";
            case CIRCUIT_MOTES:
                return "glyph/gear";
            case SNOW:
            case BLIZZARD:
            case SNOWFLAKE_SPIN:
                return "snowflake";
            case FROST_SHARDS:
                return "glyph/snow_crystal";
            case EMBERS:
            case SPARKS:
            case SOLAR_FLARE:
                return "firefly";
            case ASH:
            case DUST:
            case SANDSTORM:
                return "firefly_alt";
            case FIREFLIES:
            case PLANKTON:
            case THUNDERBUGS:
                return "firefly_alt";
            case PETALS:
                return "glyph/blossom";
            case LEAVES:
                return "glyph/leaf";
            case RAIN:
            case DRIZZLE:
            case NEON_RAIN:
            case BLOOD_RAIN:
                return "glyph/line";
            case BUBBLES:
                return "glyph/circle";
            case SPORES:
                return "glyph/flower";
            case STARFIELD:
            case METEORS:
                return "star";
            case AURORA:
                return "glyph/wave";
            case SOULS:
                return "glyph/eye";
            case HEARTS:
                return "heart";
            case CONFETTI:
                return "glyph/quad";
            case GLITCH:
                return "glyph/zigzag";
            case VOID_MOTES:
                return "glyph/moon";
            case FEATHERS:
                return "glyph/leaf";
            case GOLD_DUST:
                return "glyph/crown";
            case PENTAGRAMS:
                return "glyph/pentagram";
            case SKULLS:
                return "glyph/skull";
            case LANTERNS:
                return "glyph/sun";
            case JELLYFISH:
                return "glyph/blossom";
            case RUNE_FALL:
                return "glyph/rune_algiz";
            default:
                return "firefly";
        }
    }

    /** True for the ones drawn as columns rather than scattered motes. */
    public boolean columnar() {
        return this == MATRIX_RAIN || this == NEON_RAIN || this == RUNE_FALL
                || this == DIGITAL_SNOW || this == MATRIX_RISE;
    }

    /** True for the ones that should spin as they travel. */
    public boolean spins() {
        return this == SNOWFLAKE_SPIN || this == CONFETTI || this == LEAVES
                || this == PENTAGRAMS || this == GLITCH;
    }

    /** A size multiplier, so a glyph column and a dust mote are not the same size. */
    public float size() {
        switch (this) {
            case MATRIX_RAIN:
            case MATRIX_RISE:
            case RUNE_FALL:
            case NEON_RAIN:
                return 0.5f;
            case METEORS:
            case LANTERNS:
            case JELLYFISH:
            case AURORA:
                return 1.6f;
            case DUST:
            case SPORES:
            case PLANKTON:
            case DIGITAL_SNOW:
                return 0.35f;
            case RAIN:
            case DRIZZLE:
            case BLOOD_RAIN:
                return 0.8f;
            default:
                return 0.7f;
        }
    }
}
