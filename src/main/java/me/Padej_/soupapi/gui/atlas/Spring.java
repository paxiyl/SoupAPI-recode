package me.Padej_.soupapi.gui.atlas;

/**
 * A damped spring, which is what makes the menu feel like jelly rather than like a timer.
 *
 * <p>Ported from the Atlas click GUI of Myau Atlas (github.com/Marco-hacker666/Myau_Atlas, GPL-3.0).
 * A spring can overshoot and come back, and it carries velocity, so a pill that is moving fast can be
 * stretched along its path by exactly how fast it is moving, and one sent somewhere new while still
 * travelling bends toward it instead of restarting. Integrated in fixed four-millisecond sub-steps,
 * so the motion is the same at 30 fps and at 260 and a long frame cannot make it explode.
 */
final class Spring {

    private static final float STEP = 0.004F;

    float value;
    float velocity;
    float target;
    private final float stiffness;
    private final float damping;

    /** Stiffness 380 with damping 26 overshoots by about six percent and settles in under half a second. */
    Spring(float value, float stiffness, float damping) {
        this.value = value;
        this.target = value;
        this.stiffness = stiffness;
        this.damping = damping;
    }

    float step(float delta) {
        int steps = Math.max(1, (int) Math.ceil(delta / STEP));
        float h = delta / steps;
        for (int i = 0; i < steps; i++) {
            float accel = -stiffness * (value - target) - damping * velocity;
            velocity += accel * h;
            value += velocity * h;
        }
        if (Math.abs(value - target) < 0.001F && Math.abs(velocity) < 0.01F) {
            value = target;
            velocity = 0.0F;
        }
        return value;
    }

    void snap(float to) {
        value = to;
        target = to;
        velocity = 0.0F;
    }
}
