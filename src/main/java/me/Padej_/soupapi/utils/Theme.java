package me.Padej_.soupapi.utils;

import me.Padej_.soupapi.modules.Halo;
import me.Padej_.soupapi.modules.HitBubbles;
import me.Padej_.soupapi.modules.JumpCircles;
import me.Padej_.soupapi.modules.PlayerEffects;
import me.Padej_.soupapi.modules.Trails;
import net.minecraft.util.ResourceLocation;

/**
 * A complete look for the mod: colours <i>and</i> the shapes that go with them.
 *
 * <p>A theme started as four colours, which turned out not to feel like much — the same rings and
 * the same particles in a different hue. Each theme now also names the ring, the trail, the player
 * effect, the halo and the particle sprite that suit it, so picking "Halloween" changes what you see
 * and not just its colour. The three parts are separately switchable: colours always apply,
 * {@code themeVisuals} applies the shapes, {@code themeWorld} applies the sky and fog.
 *
 * <p>{@link #CUSTOM} hands everything back to the individual settings.
 */
public enum Theme {

    CUSTOM(0, 0, 0, 0, 0x181a29, 0xffffff, null,
            JumpCircles.JumCircleStyle.CIRCLE, HitBubbles.Style.CIRCLE, Trails.Style.FADED,
            PlayerEffects.Style.GHOST, Halo.Style.RING),

    HALLOWEEN(0xff7518, 0xff3b00, 0x8b00ff, 0x2b0a3d, 0x1a0b22, 0xffd9a0, "glyph/skull",
            JumpCircles.JumCircleStyle.RUNE, HitBubbles.Style.RUNE, Trails.Style.NEON,
            PlayerEffects.Style.EMBERS, Halo.Style.SPIKES),

    CHRISTMAS(0xff2b2b, 0x18a558, 0xffffff, 0xffd447, 0x10231a, 0xffffff, "snowflake",
            JumpCircles.JumCircleStyle.SNOWFLAKE, HitBubbles.Style.SNOWFLAKE, Trails.Style.GRADIENT,
            PlayerEffects.Style.SNOW, Halo.Style.RING),

    CYBERPUNK(0xff00a0, 0x00f0ff, 0x7b2bff, 0x13002b, 0x0d0320, 0xf0e6ff, "glyph/circuit",
            JumpCircles.JumCircleStyle.MAGIC, HitBubbles.Style.MAGIC, Trails.Style.NEON,
            PlayerEffects.Style.BEAM, Halo.Style.GYROSCOPE),

    OCEAN(0x0b3d91, 0x00a8cc, 0x7ef9ff, 0x003355, 0x061b2b, 0xdff6ff, "glyph/wave",
            JumpCircles.JumCircleStyle.CIRCLE, HitBubbles.Style.CIRCLE, Trails.Style.RIBBON,
            PlayerEffects.Style.BUBBLES, Halo.Style.AURA),

    LAVA(0xff2a00, 0xff8c00, 0xffd000, 0x5a0d00, 0x1f0a04, 0xffe3c0, "firefly",
            JumpCircles.JumCircleStyle.SHATTER, HitBubbles.Style.SHATTER, Trails.Style.SOLID,
            PlayerEffects.Style.FLAME, Halo.Style.ECLIPSE),

    TOXIC(0x6fff00, 0x00ff85, 0xc6ff00, 0x0f3b00, 0x0a1c07, 0xdcffc0, "glyph/biohazard",
            JumpCircles.JumCircleStyle.MAGIC, HitBubbles.Style.MAGIC, Trails.Style.DOTTED,
            PlayerEffects.Style.GHOST, Halo.Style.RUNE_DISC),

    SAKURA(0xffb7d5, 0xff6fa5, 0xffffff, 0xffd9e8, 0x2b1a22, 0xfff0f6, "glyph/blossom",
            JumpCircles.JumCircleStyle.SAKURA, HitBubbles.Style.SAKURA, Trails.Style.FADED,
            PlayerEffects.Style.PETALS, Halo.Style.SHARDS),

    MIDNIGHT(0x2b2bff, 0x6a00ff, 0x00d4ff, 0x0a0a2b, 0x07071a, 0xdde4ff, "glyph/moon",
            JumpCircles.JumCircleStyle.PORTAL, HitBubbles.Style.PORTAL, Trails.Style.PULSE,
            PlayerEffects.Style.STARFALL, Halo.Style.GALAXY),

    RAINBOW(0xff0000, 0xffee00, 0x00ff44, 0x0066ff, 0x141414, 0xffffff, "star",
            JumpCircles.JumCircleStyle.CIRCLE_BOLD, HitBubbles.Style.CIRCLE_BOLD, Trails.Style.GRADIENT,
            PlayerEffects.Style.ORBIT, Halo.Style.DOUBLE_RING),

    FROST(0x9fe8ff, 0x3aa0ff, 0xffffff, 0xd7f6ff, 0x0d1b26, 0xeaf9ff, "snowflake",
            JumpCircles.JumCircleStyle.SNOWFLAKE, HitBubbles.Style.SNOWFLAKE, Trails.Style.RIBBON,
            PlayerEffects.Style.SNOW, Halo.Style.SATURN),

    SUNSET(0xff9500, 0xff5f6d, 0xc34bff, 0x3a1850, 0x1d0f22, 0xffe8d6, "glyph/sun",
            JumpCircles.JumCircleStyle.PORTAL, HitBubbles.Style.PORTAL_2, Trails.Style.FADED_INVERT,
            PlayerEffects.Style.HEARTBEAT, Halo.Style.RING),

    BLOOD(0xb00000, 0xff2d2d, 0x5a0000, 0x2b0000, 0x160303, 0xffd6d6, "glyph/fang",
            JumpCircles.JumCircleStyle.SHATTER, HitBubbles.Style.SHATTER, Trails.Style.NEON,
            PlayerEffects.Style.SPARKS, Halo.Style.SPIKES),

    GOLD(0xffd447, 0xffa600, 0xfff3c0, 0x8a6300, 0x241b05, 0xfff6d6, "glyph/crown",
            JumpCircles.JumCircleStyle.RUNE, HitBubbles.Style.RUNE, Trails.Style.SOLID,
            PlayerEffects.Style.CROWN, Halo.Style.RING),

    VOID(0x8a2be2, 0x1a0030, 0x00ffd0, 0x4b0082, 0x0b0014, 0xe6d6ff, "glyph/eye",
            JumpCircles.JumCircleStyle.MAGIC, HitBubbles.Style.MAGIC, Trails.Style.PULSE,
            PlayerEffects.Style.TORNADO, Halo.Style.ECLIPSE),

    VAPORWAVE(0xff71ce, 0x01cdfe, 0x05ffa1, 0xb967ff, 0x1a0b2b, 0xfff5ff, "glyph/wave",
            JumpCircles.JumCircleStyle.MAGIC, HitBubbles.Style.MAGIC, Trails.Style.RIBBON,
            PlayerEffects.Style.MAGIC_CIRCLE, Halo.Style.GYROSCOPE),

    EMERALD(0x00c97b, 0x0affa3, 0x007a4d, 0xd7fff0, 0x062016, 0xd9fff0, "glyph/leaf",
            JumpCircles.JumCircleStyle.SAKURA, HitBubbles.Style.SAKURA, Trails.Style.GRADIENT,
            PlayerEffects.Style.PETALS, Halo.Style.AURA),

    CANDY(0xff6ec7, 0xffd166, 0x6ee7ff, 0xfff1f8, 0x2b1526, 0xfff1f8, "heart",
            JumpCircles.JumCircleStyle.SAKURA, HitBubbles.Style.CIRCLE_BOLD, Trails.Style.DOTTED,
            PlayerEffects.Style.HEARTBEAT, Halo.Style.SHARDS),

    ABYSS(0x001f3f, 0x0074d9, 0x7fdbff, 0x000a14, 0x02060d, 0xcfe8ff, "glyph/eye",
            JumpCircles.JumCircleStyle.PORTAL, HitBubbles.Style.PORTAL, Trails.Style.FADED,
            PlayerEffects.Style.TORNADO, Halo.Style.ECLIPSE),

    SOLAR(0xfff200, 0xff9000, 0xff4d00, 0xfff7cc, 0x2b1d00, 0xfff7cc, "glyph/sun",
            JumpCircles.JumCircleStyle.RUNE, HitBubbles.Style.RUNE, Trails.Style.NEON,
            PlayerEffects.Style.FLAME, Halo.Style.ECLIPSE),

    MONOCHROME(0xffffff, 0xb0b0b0, 0x606060, 0x202020, 0x0d0d0d, 0xffffff, "glyph/quad",
            JumpCircles.JumCircleStyle.HEXAGON, HitBubbles.Style.HEXAGON, Trails.Style.SOLID,
            PlayerEffects.Style.PULSE_SPHERE, Halo.Style.GYROSCOPE),

    STORM(0x4a6fff, 0x9db4ff, 0xe6ecff, 0x1b2340, 0x0b1020, 0xdfe6ff, "glyph/bolt",
            JumpCircles.JumCircleStyle.SHATTER, HitBubbles.Style.SHATTER, Trails.Style.PULSE,
            PlayerEffects.Style.SPARKS, Halo.Style.GYROSCOPE);

    private final int c1;
    private final int c2;
    private final int c3;
    private final int c4;
    private final int back;
    private final int text;
    private final String particle;
    private final JumpCircles.JumCircleStyle circleStyle;
    private final HitBubbles.Style bubbleStyle;
    private final Trails.Style trailStyle;
    private final PlayerEffects.Style effectStyle;
    private final Halo.Style haloStyle;

    Theme(int c1, int c2, int c3, int c4, int back, int text, String particle,
          JumpCircles.JumCircleStyle circleStyle, HitBubbles.Style bubbleStyle,
          Trails.Style trailStyle, PlayerEffects.Style effectStyle, Halo.Style haloStyle) {
        this.c1 = c1;
        this.c2 = c2;
        this.c3 = c3;
        this.c4 = c4;
        this.back = back;
        this.text = text;
        this.particle = particle;
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

    /** The sprite this theme prefers for particles, or {@code null} to leave the choice alone. */
    public ResourceLocation particleTexture() {
        if (particle == null) return null;
        return new ResourceLocation("soupapi", "textures/particle/" + particle + ".png");
    }
}
