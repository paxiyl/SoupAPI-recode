package me.Padej_.soupapi.utils;

import net.minecraft.util.ResourceLocation;

/**
 * A whole-mod colour scheme.
 *
 * <p>Every visual in the mod takes its colours from {@link Palette}, which reads four configurable
 * colours. A theme simply supplies those four (plus the background and text colours) from a preset,
 * so one setting restyles the HUD, the trails, the particles, the jump circles and everything else
 * at once. {@link #CUSTOM} hands control back to the four colour pickers.
 *
 * <p>Each theme also names a particle sprite that suits it, used when themed particles are on.
 */
public enum Theme {

    /** The four colours from the config, unchanged. */
    CUSTOM(0, 0, 0, 0, 0x181a29, 0xffffff, null),

    HALLOWEEN(0xff7518, 0xff3b00, 0x8b00ff, 0x2b0a3d, 0x1a0b22, 0xffd9a0, "glyph/star"),
    CHRISTMAS(0xff2b2b, 0x18a558, 0xffffff, 0xffd447, 0x10231a, 0xffffff, "snowflake"),
    CYBERPUNK(0xff00a0, 0x00f0ff, 0x7b2bff, 0x13002b, 0x0d0320, 0xf0e6ff, "glyph/quad"),
    OCEAN(0x0b3d91, 0x00a8cc, 0x7ef9ff, 0x003355, 0x061b2b, 0xdff6ff, "glyph/circle"),
    LAVA(0xff2a00, 0xff8c00, 0xffd000, 0x5a0d00, 0x1f0a04, 0xffe3c0, "firefly"),
    TOXIC(0x6fff00, 0x00ff85, 0xc6ff00, 0x0f3b00, 0x0a1c07, 0xdcffc0, "glyph/abs"),
    SAKURA(0xffb7d5, 0xff6fa5, 0xffffff, 0xffd9e8, 0x2b1a22, 0xfff0f6, "heart"),
    MIDNIGHT(0x2b2bff, 0x6a00ff, 0x00d4ff, 0x0a0a2b, 0x07071a, 0xdde4ff, "glyph/forever"),
    RAINBOW(0xff0000, 0xffee00, 0x00ff44, 0x0066ff, 0x141414, 0xffffff, "star"),
    FROST(0x9fe8ff, 0x3aa0ff, 0xffffff, 0xd7f6ff, 0x0d1b26, 0xeaf9ff, "snowflake"),
    SUNSET(0xff9500, 0xff5f6d, 0xc34bff, 0x3a1850, 0x1d0f22, 0xffe8d6, "glyph/triangle"),
    BLOOD(0xb00000, 0xff2d2d, 0x5a0000, 0x2b0000, 0x160303, 0xffd6d6, "glyph/cross"),
    GOLD(0xffd447, 0xffa600, 0xfff3c0, 0x8a6300, 0x241b05, 0xfff6d6, "dollar"),
    VOID(0x8a2be2, 0x1a0030, 0x00ffd0, 0x4b0082, 0x0b0014, 0xe6d6ff, "glyph/zigzag");

    private final int c1;
    private final int c2;
    private final int c3;
    private final int c4;
    private final int back;
    private final int text;
    private final String particle;

    Theme(int c1, int c2, int c3, int c4, int back, int text, String particle) {
        this.c1 = c1;
        this.c2 = c2;
        this.c3 = c3;
        this.c4 = c4;
        this.back = back;
        this.text = text;
        this.particle = particle;
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

    /** The sprite this theme prefers for particles, or {@code null} to leave the choice alone. */
    public ResourceLocation particleTexture() {
        if (particle == null) return null;
        return new ResourceLocation("soupapi", "textures/particle/" + particle + ".png");
    }
}
