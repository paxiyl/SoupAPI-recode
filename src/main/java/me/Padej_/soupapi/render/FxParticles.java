package me.Padej_.soupapi.render;

import me.Padej_.soupapi.compat.BufferBuilder;
import net.minecraft.util.ResourceLocation;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A small shared particle pool for the one-shot effects.
 *
 * <p>Hit effects, kill effects and landing impacts all want the same thing: throw a handful of
 * sprites into the world, let them move and fade, and draw them batched by texture. Rather than
 * three copies of that, they spawn into this and the event bridge ticks and draws it once a frame.
 *
 * <p>Positions are world-space; {@link Fx} handles the camera.
 */
public final class FxParticles {

    private static final List<Particle> PARTICLES = new ArrayList<Particle>();
    /** Keeps a bad effect from eating the frame budget. */
    private static final int LIMIT = 2000;

    private FxParticles() {
    }

    public static void spawn(Particle particle) {
        if (PARTICLES.size() >= LIMIT) return;
        PARTICLES.add(particle);
    }

    public static int count() {
        return PARTICLES.size();
    }

    public static void clear() {
        PARTICLES.clear();
    }

    public static void tick() {
        for (int i = 0; i < PARTICLES.size(); i++) {
            PARTICLES.get(i).tick();
        }
        for (Iterator<Particle> it = PARTICLES.iterator(); it.hasNext(); ) {
            if (it.next().dead()) it.remove();
        }
    }

    /** Draws everything, one batch per texture. */
    public static void render(float tickDelta) {
        if (PARTICLES.isEmpty()) return;

        // Rings first: they are geometry rather than sprites.
        for (int i = 0; i < PARTICLES.size(); i++) {
            Particle p = PARTICLES.get(i);
            if (p.texture != null) continue;
            double progress = p.progress(tickDelta);
            double radius = p.size + progress * p.expand;
            Fx.glowRing(p.renderX(tickDelta), p.renderY(tickDelta), p.renderZ(tickDelta),
                    radius, Math.max(0.05, radius * 0.22), 40, p.tilt, p.spin,
                    Fx.withAlpha(p.colour, p.alpha(tickDelta)));
        }

        ResourceLocation batchTexture = null;
        BufferBuilder buffer = null;
        for (int i = 0; i < PARTICLES.size(); i++) {
            Particle p = PARTICLES.get(i);
            if (p.texture == null) continue;
            if (buffer == null || p.texture != batchTexture) {
                if (buffer != null) Fx.endSprites(buffer);
                batchTexture = p.texture;
                buffer = Fx.beginSprites(batchTexture);
            }
            Fx.appendSprite(buffer, p.renderX(tickDelta), p.renderY(tickDelta), p.renderZ(tickDelta),
                    p.renderSize(tickDelta), Fx.withAlpha(p.colour, p.alpha(tickDelta)), p.flat);
        }
        if (buffer != null) Fx.endSprites(buffer);
    }

    /** One particle. Build with the fluent setters; {@code texture == null} means "a ring". */
    public static final class Particle {
        double x;
        double y;
        double z;
        double prevX;
        double prevY;
        double prevZ;
        double motionX;
        double motionY;
        double motionZ;
        double gravity;
        double drag = 0.96;
        int life;
        final int maxLife;
        float size;
        float grow;
        float expand;
        double tilt;
        double spin;
        boolean flat;
        boolean fadeIn;
        ResourceLocation texture;
        Color colour;

        public Particle(double x, double y, double z, int life, float size, Color colour) {
            this.x = this.prevX = x;
            this.y = this.prevY = y;
            this.z = this.prevZ = z;
            this.life = this.maxLife = Math.max(1, life);
            this.size = size;
            this.colour = colour == null ? Color.WHITE : colour;
        }

        public Particle motion(double mx, double my, double mz) {
            this.motionX = mx;
            this.motionY = my;
            this.motionZ = mz;
            return this;
        }

        public Particle gravity(double g) {
            this.gravity = g;
            return this;
        }

        public Particle drag(double d) {
            this.drag = d;
            return this;
        }

        public Particle texture(ResourceLocation t) {
            this.texture = t;
            return this;
        }

        public Particle flat() {
            this.flat = true;
            return this;
        }

        /** Grows by this much over its life, in blocks. */
        public Particle grow(float amount) {
            this.grow = amount;
            return this;
        }

        /** For rings: how far the radius expands over the particle's life. */
        public Particle expand(float amount) {
            this.expand = amount;
            return this;
        }

        public Particle tilt(double radians) {
            this.tilt = radians;
            return this;
        }

        public Particle spin(double radians) {
            this.spin = radians;
            return this;
        }

        /** Fades up over the first fifth of its life instead of starting at full brightness. */
        public Particle fadeIn() {
            this.fadeIn = true;
            return this;
        }

        void tick() {
            prevX = x;
            prevY = y;
            prevZ = z;
            x += motionX;
            y += motionY;
            z += motionZ;
            motionY -= gravity;
            motionX *= drag;
            motionZ *= drag;
            life--;
        }

        boolean dead() {
            return life <= 0;
        }

        double progress(float tickDelta) {
            double aged = maxLife - (life - tickDelta);
            return Math.max(0.0, Math.min(1.0, aged / maxLife));
        }

        float alpha(float tickDelta) {
            float p = (float) progress(tickDelta);
            float out = 1f - p;
            if (fadeIn && p < 0.2f) {
                out *= p / 0.2f;
            }
            return out;
        }

        double renderX(float tickDelta) {
            return prevX + (x - prevX) * tickDelta;
        }

        double renderY(float tickDelta) {
            return prevY + (y - prevY) * tickDelta;
        }

        double renderZ(float tickDelta) {
            return prevZ + (z - prevZ) * tickDelta;
        }

        float renderSize(float tickDelta) {
            return size + grow * (float) progress(tickDelta);
        }
    }
}
