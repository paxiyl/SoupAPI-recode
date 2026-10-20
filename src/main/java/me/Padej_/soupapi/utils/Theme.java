package me.Padej_.soupapi.utils;

import me.Padej_.soupapi.config.SoupAPI_Config;
import me.Padej_.soupapi.modules.Halo;
import me.Padej_.soupapi.modules.HitBubbles;
import me.Padej_.soupapi.modules.JumpCircles;
import me.Padej_.soupapi.modules.PlayerEffects;
import me.Padej_.soupapi.modules.Trails;

/**
 * A complete look for the mod: colours <i>and</i> the shapes that go with them.
 *
 * <p>A theme started as four colours, which turned out not to feel like much — the same rings and
 * the same particles in a different hue. Each theme now also names the ring, the trail, the player
 * effect, the halo and the particle sprite that suit it, so picking "Halloween" changes what you see
 * and not just its colour. The three parts are separately switchable: colours always apply,
 * {@code themeVisuals} applies the shapes, {@code themeWorld} applies the sky and fog.
 *
 * <p>Picking a theme copies its choices into the settings rather than overriding them, so each one
 * stays editable; the first edit to something a theme owns turns the theme into {@link #CUSTOM},
 * which keeps the look and hands every setting back to you.
 */
public enum Theme {

    CUSTOM(0, 0, 0, 0, 0x181a29, 0xffffff, ParticleSprite.NONE, null, Ambience.NONE,
            JumpCircles.JumCircleStyle.CIRCLE, HitBubbles.Style.CIRCLE, Trails.Style.FADED,
            PlayerEffects.Style.GHOST, Halo.Style.RING),

    HALLOWEEN(0xff7518, 0xff3b00, 0x8b00ff, 0x2b0a3d, 0x1a0b22, 0xffd9a0, ParticleSprite.SYMBOL, Symbol.SKULL, Ambience.SKULLS,
            JumpCircles.JumCircleStyle.RUNE, HitBubbles.Style.RUNE, Trails.Style.NEON,
            PlayerEffects.Style.EMBERS, Halo.Style.SPIKES),

    CHRISTMAS(0xff2b2b, 0x18a558, 0xffffff, 0xffd447, 0x10231a, 0xffffff, ParticleSprite.SNOWFLAKE, null, Ambience.SNOW,
            JumpCircles.JumCircleStyle.SNOWFLAKE, HitBubbles.Style.SNOWFLAKE, Trails.Style.GRADIENT,
            PlayerEffects.Style.SNOW, Halo.Style.RING),

    CYBERPUNK(0xff00a0, 0x00f0ff, 0x7b2bff, 0x13002b, 0x0d0320, 0xf0e6ff, ParticleSprite.SYMBOL, Symbol.CIRCUIT, Ambience.NEON_RAIN,
            JumpCircles.JumCircleStyle.MAGIC, HitBubbles.Style.MAGIC, Trails.Style.NEON,
            PlayerEffects.Style.BEAM, Halo.Style.GYROSCOPE),

    OCEAN(0x0b3d91, 0x00a8cc, 0x7ef9ff, 0x003355, 0x061b2b, 0xdff6ff, ParticleSprite.SYMBOL, Symbol.WAVE, Ambience.BUBBLES,
            JumpCircles.JumCircleStyle.CIRCLE, HitBubbles.Style.CIRCLE, Trails.Style.RIBBON,
            PlayerEffects.Style.BUBBLES, Halo.Style.AURA),

    LAVA(0xff2a00, 0xff8c00, 0xffd000, 0x5a0d00, 0x1f0a04, 0xffe3c0, ParticleSprite.FIREFLY, null, Ambience.EMBERS,
            JumpCircles.JumCircleStyle.SHATTER, HitBubbles.Style.SHATTER, Trails.Style.SOLID,
            PlayerEffects.Style.FLAME, Halo.Style.ECLIPSE),

    TOXIC(0x6fff00, 0x00ff85, 0xc6ff00, 0x0f3b00, 0x0a1c07, 0xdcffc0, ParticleSprite.SYMBOL, Symbol.BIOHAZARD, Ambience.SPORES,
            JumpCircles.JumCircleStyle.MAGIC, HitBubbles.Style.MAGIC, Trails.Style.DOTTED,
            PlayerEffects.Style.GHOST, Halo.Style.RUNE_DISC),

    SAKURA(0xffb7d5, 0xff6fa5, 0xffffff, 0xffd9e8, 0x2b1a22, 0xfff0f6, ParticleSprite.SYMBOL, Symbol.BLOSSOM, Ambience.PETALS,
            JumpCircles.JumCircleStyle.SAKURA, HitBubbles.Style.SAKURA, Trails.Style.FADED,
            PlayerEffects.Style.PETALS, Halo.Style.SHARDS),

    MIDNIGHT(0x2b2bff, 0x6a00ff, 0x00d4ff, 0x0a0a2b, 0x07071a, 0xdde4ff, ParticleSprite.SYMBOL, Symbol.MOON, Ambience.STARFIELD,
            JumpCircles.JumCircleStyle.PORTAL, HitBubbles.Style.PORTAL, Trails.Style.PULSE,
            PlayerEffects.Style.STARFALL, Halo.Style.GALAXY),

    RAINBOW(0xff0000, 0xffee00, 0x00ff44, 0x0066ff, 0x141414, 0xffffff, ParticleSprite.STAR, null, Ambience.CONFETTI,
            JumpCircles.JumCircleStyle.CIRCLE_BOLD, HitBubbles.Style.CIRCLE_BOLD, Trails.Style.GRADIENT,
            PlayerEffects.Style.ORBIT, Halo.Style.DOUBLE_RING),

    FROST(0x9fe8ff, 0x3aa0ff, 0xffffff, 0xd7f6ff, 0x0d1b26, 0xeaf9ff, ParticleSprite.SNOWFLAKE, null, Ambience.BLIZZARD,
            JumpCircles.JumCircleStyle.SNOWFLAKE, HitBubbles.Style.SNOWFLAKE, Trails.Style.RIBBON,
            PlayerEffects.Style.SNOW, Halo.Style.SATURN),

    SUNSET(0xff9500, 0xff5f6d, 0xc34bff, 0x3a1850, 0x1d0f22, 0xffe8d6, ParticleSprite.SYMBOL, Symbol.SUN, Ambience.DUST,
            JumpCircles.JumCircleStyle.PORTAL, HitBubbles.Style.PORTAL_2, Trails.Style.FADED_INVERT,
            PlayerEffects.Style.HEARTBEAT, Halo.Style.RING),

    BLOOD(0xb00000, 0xff2d2d, 0x5a0000, 0x2b0000, 0x160303, 0xffd6d6, ParticleSprite.SYMBOL, Symbol.FANG, Ambience.BLOOD_RAIN,
            JumpCircles.JumCircleStyle.SHATTER, HitBubbles.Style.SHATTER, Trails.Style.NEON,
            PlayerEffects.Style.SPARKS, Halo.Style.SPIKES),

    GOLD(0xffd447, 0xffa600, 0xfff3c0, 0x8a6300, 0x241b05, 0xfff6d6, ParticleSprite.SYMBOL, Symbol.CROWN, Ambience.GOLD_DUST,
            JumpCircles.JumCircleStyle.RUNE, HitBubbles.Style.RUNE, Trails.Style.SOLID,
            PlayerEffects.Style.CROWN, Halo.Style.RING),

    VOID(0x8a2be2, 0x1a0030, 0x00ffd0, 0x4b0082, 0x0b0014, 0xe6d6ff, ParticleSprite.SYMBOL, Symbol.EYE, Ambience.VOID_MOTES,
            JumpCircles.JumCircleStyle.MAGIC, HitBubbles.Style.MAGIC, Trails.Style.PULSE,
            PlayerEffects.Style.TORNADO, Halo.Style.ECLIPSE),

    VAPORWAVE(0xff71ce, 0x01cdfe, 0x05ffa1, 0xb967ff, 0x1a0b2b, 0xfff5ff, ParticleSprite.SYMBOL, Symbol.WAVE, Ambience.GLITCH,
            JumpCircles.JumCircleStyle.MAGIC, HitBubbles.Style.MAGIC, Trails.Style.RIBBON,
            PlayerEffects.Style.MAGIC_CIRCLE, Halo.Style.GYROSCOPE),

    EMERALD(0x00c97b, 0x0affa3, 0x007a4d, 0xd7fff0, 0x062016, 0xd9fff0, ParticleSprite.SYMBOL, Symbol.LEAF, Ambience.LEAVES,
            JumpCircles.JumCircleStyle.SAKURA, HitBubbles.Style.SAKURA, Trails.Style.GRADIENT,
            PlayerEffects.Style.PETALS, Halo.Style.AURA),

    CANDY(0xff6ec7, 0xffd166, 0x6ee7ff, 0xfff1f8, 0x2b1526, 0xfff1f8, ParticleSprite.HEART, null, Ambience.HEARTS,
            JumpCircles.JumCircleStyle.SAKURA, HitBubbles.Style.CIRCLE_BOLD, Trails.Style.DOTTED,
            PlayerEffects.Style.HEARTBEAT, Halo.Style.SHARDS),

    ABYSS(0x001f3f, 0x0074d9, 0x7fdbff, 0x000a14, 0x02060d, 0xcfe8ff, ParticleSprite.SYMBOL, Symbol.EYE, Ambience.PLANKTON,
            JumpCircles.JumCircleStyle.PORTAL, HitBubbles.Style.PORTAL, Trails.Style.FADED,
            PlayerEffects.Style.TORNADO, Halo.Style.ECLIPSE),

    SOLAR(0xfff200, 0xff9000, 0xff4d00, 0xfff7cc, 0x2b1d00, 0xfff7cc, ParticleSprite.SYMBOL, Symbol.SUN, Ambience.SOLAR_FLARE,
            JumpCircles.JumCircleStyle.RUNE, HitBubbles.Style.RUNE, Trails.Style.NEON,
            PlayerEffects.Style.FLAME, Halo.Style.ECLIPSE),

    MONOCHROME(0xffffff, 0xb0b0b0, 0x606060, 0x202020, 0x0d0d0d, 0xffffff, ParticleSprite.SYMBOL, Symbol.QUAD, Ambience.DIGITAL_SNOW,
            JumpCircles.JumCircleStyle.HEXAGON, HitBubbles.Style.HEXAGON, Trails.Style.SOLID,
            PlayerEffects.Style.PULSE_SPHERE, Halo.Style.GYROSCOPE),

    STORM(0x4a6fff, 0x9db4ff, 0xe6ecff, 0x1b2340, 0x0b1020, 0xdfe6ff, ParticleSprite.SYMBOL, Symbol.BOLT, Ambience.THUNDERBUGS,
            JumpCircles.JumCircleStyle.SHATTER, HitBubbles.Style.SHATTER, Trails.Style.PULSE,
            PlayerEffects.Style.SPARKS, Halo.Style.GYROSCOPE),

    MATRIX(0x00ff41, 0x008f11, 0x00ff41, 0x003b00, 0x000000, 0x00ff41, ParticleSprite.SYMBOL, Symbol.CIRCUIT, Ambience.MATRIX_RAIN,
            JumpCircles.JumCircleStyle.MAGIC, HitBubbles.Style.MAGIC, Trails.Style.DOTTED,
            PlayerEffects.Style.BEAM, Halo.Style.RUNE_DISC),

    MATRIX_RED(0xff2a2a, 0x8f1111, 0xff6b6b, 0x3b0000, 0x050000, 0xff6b6b, ParticleSprite.SYMBOL, Symbol.CIRCUIT, Ambience.MATRIX_RAIN,
            JumpCircles.JumCircleStyle.MAGIC, HitBubbles.Style.MAGIC, Trails.Style.DOTTED,
            PlayerEffects.Style.BEAM, Halo.Style.RUNE_DISC),

    INFERNO(0xff3c00, 0xff7b00, 0xffc400, 0x3b0a00, 0x140500, 0xffd9b0, ParticleSprite.FIREFLY, null, Ambience.EMBERS,
            JumpCircles.JumCircleStyle.SHATTER, HitBubbles.Style.SHATTER, Trails.Style.NEON,
            PlayerEffects.Style.FLAME, Halo.Style.ECLIPSE),

    ARCTIC(0xd7f6ff, 0x7fd4ff, 0xffffff, 0x9fe8ff, 0x0a1620, 0xeaf9ff, ParticleSprite.SNOWFLAKE, null, Ambience.FROST_SHARDS,
            JumpCircles.JumCircleStyle.SNOWFLAKE, HitBubbles.Style.SNOWFLAKE, Trails.Style.RIBBON,
            PlayerEffects.Style.SNOW, Halo.Style.SATURN),

    NEON_TOKYO(0xff2fb3, 0x2ff0ff, 0xffe600, 0x2a003b, 0x0c0216, 0xffe9ff, ParticleSprite.SYMBOL, Symbol.CIRCUIT, Ambience.NEON_RAIN,
            JumpCircles.JumCircleStyle.MAGIC, HitBubbles.Style.MAGIC, Trails.Style.NEON,
            PlayerEffects.Style.MAGIC_CIRCLE, Halo.Style.GYROSCOPE),

    BLOODMOON(0x8b0000, 0xff1a1a, 0x4a0000, 0x1a0000, 0x0d0000, 0xffc9c9, ParticleSprite.SYMBOL, Symbol.FANG, Ambience.BLOOD_RAIN,
            JumpCircles.JumCircleStyle.RUNE, HitBubbles.Style.RUNE, Trails.Style.NEON,
            PlayerEffects.Style.EMBERS, Halo.Style.ECLIPSE),

    SPIRIT(0x7ffff0, 0xc9fff8, 0x2fb3a0, 0x0a2b28, 0x051614, 0xdffffb, ParticleSprite.SYMBOL, Symbol.EYE, Ambience.SOULS,
            JumpCircles.JumCircleStyle.PORTAL, HitBubbles.Style.PORTAL, Trails.Style.FADED,
            PlayerEffects.Style.GHOST, Halo.Style.SOULS),

    PHANTOM(0x9b5cff, 0x2a0040, 0x00ffd0, 0x4b0082, 0x0a0014, 0xe8d9ff, ParticleSprite.SYMBOL, Symbol.MOON, Ambience.VOID_MOTES,
            JumpCircles.JumCircleStyle.PORTAL, HitBubbles.Style.PORTAL_2, Trails.Style.PULSE,
            PlayerEffects.Style.GHOST, Halo.Style.GALAXY),

    GLACIER(0x8fd8ff, 0x2f9bd8, 0xffffff, 0xcfeeff, 0x08141c, 0xe6f6ff, ParticleSprite.SYMBOL, Symbol.SNOW_CRYSTAL, Ambience.SNOWFLAKE_SPIN,
            JumpCircles.JumCircleStyle.SNOWFLAKE, HitBubbles.Style.SNOWFLAKE, Trails.Style.GRADIENT,
            PlayerEffects.Style.SNOW, Halo.Style.SHARDS),

    DESERT(0xe8b96b, 0xc98b3c, 0xfff0c9, 0x6b4a1f, 0x1c1408, 0xfff2d6, ParticleSprite.FIREFLY_SOFT, null, Ambience.SANDSTORM,
            JumpCircles.JumCircleStyle.RUNE, HitBubbles.Style.RUNE, Trails.Style.SOLID,
            PlayerEffects.Style.TORNADO, Halo.Style.AURA),

    JUNGLE(0x4ccf5a, 0x1f8f3c, 0xc9ff8f, 0x0a3b14, 0x05170a, 0xdcffd0, ParticleSprite.SYMBOL, Symbol.LEAF, Ambience.SPORES,
            JumpCircles.JumCircleStyle.SAKURA, HitBubbles.Style.SAKURA, Trails.Style.GRADIENT,
            PlayerEffects.Style.PETALS, Halo.Style.AURA),

    NEBULA(0x7a2fff, 0x2f6bff, 0xff6bd8, 0x120033, 0x070018, 0xe8dbff, ParticleSprite.STAR, null, Ambience.STARFIELD,
            JumpCircles.JumCircleStyle.PORTAL, HitBubbles.Style.PORTAL, Trails.Style.GRADIENT,
            PlayerEffects.Style.STARFALL, Halo.Style.GALAXY),

    SUPERNOVA(0xffffff, 0xffc44a, 0xff5a1f, 0x2b1200, 0x120700, 0xfff1d6, ParticleSprite.STAR, null, Ambience.METEORS,
            JumpCircles.JumCircleStyle.CIRCLE_BOLD, HitBubbles.Style.CIRCLE_BOLD, Trails.Style.NEON,
            PlayerEffects.Style.SHOCKWAVE, Halo.Style.ECLIPSE),

    AURORA(0x4cffb3, 0x7a5cff, 0x2fd8ff, 0x061a2b, 0x030d16, 0xd9fff1, ParticleSprite.SYMBOL, Symbol.WAVE, Ambience.AURORA,
            JumpCircles.JumCircleStyle.MAGIC, HitBubbles.Style.MAGIC, Trails.Style.RIBBON,
            PlayerEffects.Style.HELIX, Halo.Style.AURA),

    CHERRY_NIGHT(0xff8fc9, 0xff4a8f, 0xfff0f6, 0x3b0a22, 0x16050d, 0xffe6f1, ParticleSprite.SYMBOL, Symbol.BLOSSOM, Ambience.PETALS,
            JumpCircles.JumCircleStyle.SAKURA, HitBubbles.Style.SAKURA, Trails.Style.FADED,
            PlayerEffects.Style.PETALS, Halo.Style.SHARDS),

    ARCADE(0xff2a2a, 0x2aff2a, 0x2a2aff, 0xffe600, 0x0a0a0a, 0xffffff, ParticleSprite.SYMBOL, Symbol.QUAD, Ambience.GLITCH,
            JumpCircles.JumCircleStyle.HEXAGON, HitBubbles.Style.HEXAGON, Trails.Style.DOTTED,
            PlayerEffects.Style.PULSE_SPHERE, Halo.Style.GYROSCOPE),

    OBSIDIAN(0x2a0a3b, 0x6b2fff, 0x120016, 0x000000, 0x030005, 0xd6c9ff, ParticleSprite.SYMBOL, Symbol.PENTAGRAM, Ambience.ASH,
            JumpCircles.JumCircleStyle.RUNE, HitBubbles.Style.RUNE, Trails.Style.SOLID,
            PlayerEffects.Style.EMBERS, Halo.Style.RUNE_DISC),

    SEAFOAM(0x7affd4, 0x2fd8a0, 0xd9fff1, 0x0a3b2b, 0x051a14, 0xd9fff1, ParticleSprite.SYMBOL, Symbol.WAVE, Ambience.PLANKTON,
            JumpCircles.JumCircleStyle.CIRCLE, HitBubbles.Style.CIRCLE, Trails.Style.RIBBON,
            PlayerEffects.Style.BUBBLES, Halo.Style.AURA),

    FESTIVAL(0xffc44a, 0xff3c3c, 0xff8f2f, 0x3b1400, 0x160700, 0xfff0d6, ParticleSprite.SYMBOL, Symbol.SUN, Ambience.LANTERNS,
            JumpCircles.JumCircleStyle.SOUP, HitBubbles.Style.CIRCLE_BOLD, Trails.Style.GRADIENT,
            PlayerEffects.Style.ORBIT, Halo.Style.DOUBLE_RING),

    WITCHCRAFT(0x6bff2f, 0x8b2fff, 0x2a3b00, 0x14002b, 0x070014, 0xdcffc9, ParticleSprite.SYMBOL, Symbol.PENTAGRAM, Ambience.PENTAGRAMS,
            JumpCircles.JumCircleStyle.RUNE, HitBubbles.Style.RUNE, Trails.Style.NEON,
            PlayerEffects.Style.MAGIC_CIRCLE, Halo.Style.RUNE_DISC),

    THUNDER(0xffe600, 0x4a8fff, 0xffffff, 0x14203b, 0x070b16, 0xeef4ff, ParticleSprite.SYMBOL, Symbol.BOLT, Ambience.THUNDERBUGS,
            JumpCircles.JumCircleStyle.SHATTER, HitBubbles.Style.SHATTER, Trails.Style.PULSE,
            PlayerEffects.Style.SPARKS, Halo.Style.GYROSCOPE);

    private final int c1;
    private final int c2;
    private final int c3;
    private final int c4;
    private final int back;
    private final int text;
    private final ParticleSprite sprite;
    private final Symbol symbol;
    private final Ambience ambience;
    private final JumpCircles.JumCircleStyle circleStyle;
    private final HitBubbles.Style bubbleStyle;
    private final Trails.Style trailStyle;
    private final PlayerEffects.Style effectStyle;
    private final Halo.Style haloStyle;

    Theme(int c1, int c2, int c3, int c4, int back, int text, ParticleSprite sprite, Symbol symbol,
          Ambience ambience, JumpCircles.JumCircleStyle circleStyle, HitBubbles.Style bubbleStyle,
          Trails.Style trailStyle, PlayerEffects.Style effectStyle, Halo.Style haloStyle) {
        this.c1 = c1;
        this.c2 = c2;
        this.c3 = c3;
        this.c4 = c4;
        this.back = back;
        this.text = text;
        this.sprite = sprite;
        this.symbol = symbol;
        this.ambience = ambience;
        this.circleStyle = circleStyle;
        this.bubbleStyle = bubbleStyle;
        this.trailStyle = trailStyle;
        this.effectStyle = effectStyle;
        this.haloStyle = haloStyle;
    }

    public int color1() {
        return c1;
    }

    public int color2() {
        return c2;
    }

    public int color3() {
        return c3;
    }

    public int color4() {
        return c4;
    }

    public int backColor() {
        return back;
    }

    public int textColor() {
        return text;
    }

    public JumpCircles.JumCircleStyle circleStyle() {
        return circleStyle;
    }

    public HitBubbles.Style bubbleStyle() {
        return bubbleStyle;
    }

    public Trails.Style trailStyle() {
        return trailStyle;
    }

    public PlayerEffects.Style effectStyle() {
        return effectStyle;
    }

    public Halo.Style haloStyle() {
        return haloStyle;
    }

    /** The sprite this theme wants every particle to use. */
    public ParticleSprite sprite() {
        return sprite;
    }

    /** The symbol that goes with {@link ParticleSprite#SYMBOL}, or {@code null}. */
    public Symbol symbol() {
        return symbol;
    }

    /** What this theme puts in the air. */
    public Ambience ambience() {
        return ambience;
    }

    /**
     * What this theme's sky does.
     *
     * <p>Derived from the ambience rather than stored, because the two belong together: a theme
     * raining code wants a sky of falling code behind it, and one full of embers wants a sky that
     * glows from the horizon.
     */
    public SkyStyle skyStyle() {
        switch (ambience) {
            case MATRIX_RAIN:
            case MATRIX_RISE:
            case DIGITAL_SNOW:
                return SkyStyle.CODE_FALL;
            case NEON_RAIN:
            case CIRCUIT_MOTES:
                return SkyStyle.CIRCUIT_SKY;
            case SNOW:
            case SNOWFLAKE_SPIN:
            case FROST_SHARDS:
                return SkyStyle.SILK;
            case BLIZZARD:
                return SkyStyle.STORM_FRONT;
            case EMBERS:
            case SOLAR_FLARE:
            case ASH:
                return SkyStyle.EMBER_SKY;
            case FIREFLIES:
            case SPORES:
                return SkyStyle.NEBULA;
            case PETALS:
            case LEAVES:
            case FEATHERS:
                return SkyStyle.SUNSET;
            case RAIN:
            case DRIZZLE:
                return SkyStyle.SCANLINES;
            case BLOOD_RAIN:
                return SkyStyle.VOID_RIFT;
            case BUBBLES:
            case PLANKTON:
            case JELLYFISH:
                return SkyStyle.OCEAN_DEPTH;
            case STARFIELD:
            case METEORS:
                return SkyStyle.VORTEX;
            case AURORA:
                return SkyStyle.AURORA;
            case SOULS:
            case PENTAGRAMS:
            case SKULLS:
                return SkyStyle.ECLIPSE;
            case HEARTS:
            case CONFETTI:
                return SkyStyle.BANDS;
            case GLITCH:
                return SkyStyle.PLASMA;
            case VOID_MOTES:
                return SkyStyle.VOID_RIFT;
            case SANDSTORM:
            case DUST:
                return SkyStyle.SPEED_LINES;
            case GOLD_DUST:
            case LANTERNS:
                return SkyStyle.RIPPLE;
            case THUNDERBUGS:
                return SkyStyle.STORM_FRONT;
            case RUNE_FALL:
                return SkyStyle.LATTICE;
            case SPARKS:
                return SkyStyle.HEX_GRID;
            default:
                return SkyStyle.PLAIN;
        }
    }

    /** The colour this theme paints the sky, when it is allowed to paint the sky at all. */
    public int skyColor() {
        return back;
    }

    /**
     * How solidly the sky is painted, as a percentage.
     *
     * <p>Most themes only tint, so a texture pack's own sky still shows through. The ones built
     * around a black sky need it opaque or they do not read at all — a Matrix theme over a blue
     * sky is just green text.
     */
    public int skyAlpha() {
        switch (this) {
            case MATRIX:
            case MATRIX_RED:
            case OBSIDIAN:
            case ABYSS:
            case VOID:
                return 96;
            case PHANTOM:
            case BLOODMOON:
            case NEBULA:
            case MIDNIGHT:
                return 85;
            default:
                return 60;
        }
    }

    /**
     * Writes this theme's look into the settings themselves.
     *
     * <p>A theme used to answer for the options rather than fill them in, which read well in the
     * code and badly in the hand: with a theme picked, changing the particle or a colour did
     * nothing, because the theme kept winning. Picking a theme copies its choices into the real
     * settings now, so every one of them stays editable afterwards — and editing one is what turns
     * the theme into {@link #CUSTOM} (see {@link Themes#onEdited}).
     *
     * <p>The three switches decide how much gets copied: colours always, the shapes with
     * {@code themeVisuals}, the particle sprite with {@code themeParticles}, the sky and fog with
     * {@code themeWorld}.
     */
    public void apply(SoupAPI_Config config) {
        if (this == CUSTOM || config == null) return;

        config.c1 = c1;
        config.c2 = c2;
        config.c3 = c3;
        config.c4 = c4;
        config.textColor = text;
        config.backColor = back;

        if (config.themeVisuals) {
            config.jumpCirclesStyle = circleStyle;
            config.hitBubblesStyle = bubbleStyle;
            config.trailsStyle = trailStyle;
            config.playerEffectsStyle = effectStyle;
            config.haloStyle = haloStyle;
        }

        if (config.themeParticles) {
            config.particleSpriteOverride = sprite;
            if (sprite == ParticleSprite.SYMBOL && symbol != null) {
                config.particleSymbol = symbol;
            }
        }

        if (config.themeAmbience) {
            config.ambienceEnabled = ambience != Ambience.NONE;
            config.ambienceStyle = Ambience.NONE;   // NONE here means "follow the theme"
        }

        if (config.themeWorld) {
            config.coloredSkyEnabled = true;
            config.coloredSkyColor = skyColor();
            config.coloredSkyAlpha = skyAlpha();
            config.skyStyle = SkyStyle.PLAIN;   // PLAIN here means "follow the theme"
            config.skyColor2 = c2;
            config.skyDarkColor = back;
            config.customFogEnabled = true;
        }
    }

    /**
     * Whether a setting still holds what this theme put there.
     *
     * <p>Answers one field at a time because that is how an edit arrives. A field this theme does
     * not own — because the switch that would have copied it is off — always agrees, so toggling a
     * shape with {@code themeVisuals} off does not pretend the theme was changed.
     */
    public boolean agreesWith(String field, SoupAPI_Config config) {
        if (config == null || field == null) return true;
        switch (field) {
            case "c1":
                return config.c1 == c1;
            case "c2":
                return config.c2 == c2;
            case "c3":
                return config.c3 == c3;
            case "c4":
                return config.c4 == c4;
            case "textColor":
                return config.textColor == text;
            case "backColor":
                return config.backColor == back;
            case "jumpCirclesStyle":
                return !config.themeVisuals || config.jumpCirclesStyle == circleStyle;
            case "hitBubblesStyle":
                return !config.themeVisuals || config.hitBubblesStyle == bubbleStyle;
            case "trailsStyle":
                return !config.themeVisuals || config.trailsStyle == trailStyle;
            case "playerEffectsStyle":
                return !config.themeVisuals || config.playerEffectsStyle == effectStyle;
            case "haloStyle":
                return !config.themeVisuals || config.haloStyle == haloStyle;
            case "particleSpriteOverride":
                return !config.themeParticles || config.particleSpriteOverride == sprite;
            case "particleSymbol":
                return !config.themeParticles || sprite != ParticleSprite.SYMBOL
                        || config.particleSymbol == symbol;
            case "ambienceStyle":
                return !config.themeAmbience || config.ambienceStyle == Ambience.NONE;
            case "coloredSkyColor":
                return !config.themeWorld || config.coloredSkyColor == skyColor();
            case "coloredSkyAlpha":
                return !config.themeWorld || config.coloredSkyAlpha == skyAlpha();
            case "skyStyle":
                return !config.themeWorld || config.skyStyle == SkyStyle.PLAIN;
            case "skyColor2":
                return !config.themeWorld || config.skyColor2 == c2;
            case "skyDarkColor":
                return !config.themeWorld || config.skyDarkColor == back;
            default:
                return true;
        }
    }
}
