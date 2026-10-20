package me.Padej_.soupapi.utils;

/**
 * The one clock every animated colour, sky and effect reads.
 *
 * <p>Before this, each module that wanted motion read {@code System.currentTimeMillis()} on its own
 * and scaled it its own way, so two things meant to pulse together drifted apart and nothing could
 * be synchronised. Everything that animates over time now asks here, which is what makes "global
 * synchronisation" of the RGB cycle a property of the code rather than a coincidence.
 *
 * <p>Seconds since the clock was first read, from {@link System#nanoTime()} so a wall-clock change
 * cannot make it jump, wrapped once an hour so the float keeps its precision.
 */
public final class FxClock {

    private static final long START = System.nanoTime();
    private static final double WRAP = 3600.0;

    private FxClock() {
    }

    /** Seconds since start, wrapped hourly. */
    public static float seconds() {
        double s = (System.nanoTime() - START) / 1.0E9;
        return (float) (s % WRAP);
    }

    /** A 0..1 saw wave with the given period in seconds. */
    public static float cycle(float periodSeconds) {
        if (periodSeconds <= 0f) return 0f;
        float t = seconds() / periodSeconds;
        return t - (float) Math.floor(t);
    }

    /** A smooth 0..1 wave with the given period in seconds. */
    public static float wave(float periodSeconds) {
        return 0.5f + 0.5f * (float) Math.sin(cycle(periodSeconds) * Math.PI * 2.0);
    }
}
