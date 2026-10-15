package me.Padej_.soupapi.compat;

/**
 * Replacement for {@code net.minecraft.client.render.RenderTickCounter}.
 *
 * <p>1.8.9 passes a bare {@code float tickDelta} through its render methods. 1.21.4 wrapped it in a
 * counter whose {@code getTickDelta(boolean ignoreFreeze)} distinguishes the frozen-world case,
 * which 1.8.9 has no equivalent for — so both answers are the same value here.
 */
public final class RenderTickCounter {

    private float tickDelta;

    public RenderTickCounter(float tickDelta) {
        this.tickDelta = tickDelta;
    }

    public void setTickDelta(float tickDelta) {
        this.tickDelta = tickDelta;
    }

    public float getTickDelta(boolean ignoreFreeze) {
        return tickDelta;
    }

    public float getTickDelta() {
        return tickDelta;
    }

    /** 1.21.4 name for the same value. */
    public float getLastFrameDuration() {
        return tickDelta;
    }
}
