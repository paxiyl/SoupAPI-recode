package me.Padej_.soupapi.font;
final class Glyph {
    private final int u;
    private final int v;
    private final int width;
    private final int height;
    private final char value;
    private final GlyphMap owner;

    public Glyph(int u, int v, int width, int height, char value, GlyphMap owner) {
        this.u = u;
        this.v = v;
        this.width = width;
        this.height = height;
        this.value = value;
        this.owner = owner;
    }

    public int u() {
        return u;
    }

    public int v() {
        return v;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public char value() {
        return value;
    }

    public GlyphMap owner() {
        return owner;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Glyph)) return false;
        Glyph other = (Glyph) o;
        if (u != other.u) return false;
        if (v != other.v) return false;
        if (width != other.width) return false;
        if (height != other.height) return false;
        if (value != other.value) return false;
        if (owner == null ? other.owner != null : !owner.equals(other.owner)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + (int) u;
        result = 31 * result + (int) v;
        result = 31 * result + (int) width;
        result = 31 * result + (int) height;
        result = 31 * result + (int) value;
        result = 31 * result + (owner == null ? 0 : owner.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "Glyph[" + "u=" + u + ", " + "v=" + v + ", " + "width=" + width + ", " + "height=" + height + ", " + "value=" + value + ", " + "owner=" + owner + "]";
    }
}
