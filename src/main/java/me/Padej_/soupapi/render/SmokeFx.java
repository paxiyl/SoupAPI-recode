package me.Padej_.soupapi.render;

import me.Padej_.soupapi.compat.Cameras;
import me.Padej_.soupapi.config.ConfigurableModule;
import me.Padej_.soupapi.utils.FxClock;
import me.Padej_.soupapi.utils.Palette;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Layered spectral smoke, and the embers, sparks, runes and glass that travel with it.
 *
 * <p>The mod's other particle pool ({@link FxParticles}) draws every sprite additively, which is right
 * for light and makes dark smoke impossible: black added to the frame is the frame. Smoke here is
 * drawn the way smoke has to be drawn — alpha-blended, back to front, with depth testing so terrain
 * hides it — and then given back its readability with a second, additive pass that lights only the
 * thin band just inside each puff's silhouette. That is the "dark core with a lit edge": without it,
 * black smoke against a night sky vanishes, and with too much of it, it stops being black.
 *
 * <p>Motion is what sells it. Each puff forms quickly, billows outward with an ease-out, curls
 * through a cheap curl-like noise field, drifts, and dissipates slowly, turning as it goes; several
 * sizes and lifetimes overlap so the cloud reads as layers rather than a sheet. Everything is batched
 * through two atlases, so the whole pool is two draw calls for smoke, one for the additive accents,
 * plus one per world-space decal.
 *
 * <p>Six named variants sit on top of the pool and are what the rest of the mod calls: abyssal,
 * soul siphon, phantom drift, rift, infernal ash and spectral. The pool is independently
 * switchable, capped by a quality setting, cleared on world change, and allocates nothing per frame.
 */
public final class SmokeFx extends ConfigurableModule {

    private static final ResourceLocation SMOKE_ATLAS = new ResourceLocation("soupapi", "textures/vfx/smoke_atlas.png");
    private static final ResourceLocation EDGE_ATLAS = new ResourceLocation("soupapi", "textures/vfx/smoke_edge_atlas.png");
    public static final ResourceLocation FX_ATLAS = new ResourceLocation("soupapi", "textures/vfx/fx_atlas.png");
    public static final ResourceLocation RUNE_RING_0 = new ResourceLocation("soupapi", "textures/vfx/rune_ring_0.png");
    public static final ResourceLocation RUNE_RING_1 = new ResourceLocation("soupapi", "textures/vfx/rune_ring_1.png");
    public static final ResourceLocation RUNE_RING_2 = new ResourceLocation("soupapi", "textures/vfx/rune_ring_2.png");
    public static final ResourceLocation FRACTURE = new ResourceLocation("soupapi", "textures/vfx/fracture.png");
    public static final ResourceLocation GLOW = new ResourceLocation("soupapi", "textures/vfx/glow.png");
    public static final ResourceLocation PORTAL_CORE = new ResourceLocation("soupapi", "textures/vfx/portal_core.png");

    /** What a particle is, which decides its atlas, its blend and how it faces the camera. */
    public enum Kind {
        /** A billowing puff: alpha-blended, sorted, rim-lit. */
        SMOKE,
        /** A long strand stretched along its motion: alpha-blended, sorted, rim-lit. */
        WISP,
        /** A hot dot. Additive. */
        EMBER,
        /** A streak stretched along its motion. Additive. */
        SPARK,
        /** A soft disc of light. Additive. */
        GLOW,
        /** A glowing rune. Additive, turning. */
        RUNE,
        /** A glass fragment, tumbling. Additive. */
        SHARD,
        /** A spectral feather, see-sawing as it falls. Additive. */
        FEATHER,
        /** A flat textured quad in the world (rune ring, fracture, rift core). Additive. */
        DECAL
    }

    /** Which pass a particle belongs to. */
    private static boolean sorted(Kind kind) {
        return kind == Kind.SMOKE || kind == Kind.WISP;
    }

    /** Draw quality: how many particles may exist, and whether the edge-light pass runs. */
    public enum Quality {
        LOW(320, false), MEDIUM(800, true), HIGH(1500, true), ULTRA(2600, true);

        final int cap;
        final boolean edges;

        Quality(int cap, boolean edges) {
            this.cap = cap;
            this.edges = edges;
        }
    }

    /** One particle. Built with the fluent setters, spawned with {@link #spawn}. */
    public static final class P {
        Kind kind = Kind.SMOKE;
        double x, y, z, px, py, pz;
        double vx, vy, vz;
        double drag = 0.94;
        double gravity;
        float age;
        float life = 40;
        float size0 = 0.6f, size1 = 1.6f;
        float rot, rotSpeed;
        /** Base colour, 0..1; for smoke this is the darkness of the core. */
        float r = 0.04f, g = 0.03f, b = 0.06f;
        /** Accent: the rim light and any additive colour. */
        float ar = 0.6f, ag = 0.3f, ab = 1f;
        /** When >= 0, the accent follows the live palette at this position. */
        float accentPalette = -1f;
        Palette.Element element = Palette.Element.PARTICLES;
        float alpha = 0.85f;
        float edge = 0.6f;
        float turbulence = 1f;
        float stretch = 1f;
        int variant;
        /** Fraction of life spent forming, and when dissipation starts. */
        float formEnd = 0.12f, fadeStart = 0.55f;
        // Converging (soul siphon).
        boolean attract;
        double tx, ty, tz;
        double pull;
        // Spiralling about a vertical axis (abyssal).
        boolean swirl;
        double sx, sz;
        double swirlRate, inward;
        // Decal.
        ResourceLocation decal;
        /** A decal drawn with normal alpha blending (a dark core), instead of as light. */
        boolean darkDecal;
        /** A decal that keeps facing the camera rather than a fixed normal. */
        boolean facing;
        float nx, ny = 1f, nz;
        float seed;
        /** Height of the surface under the puff, and whether one was found. */
        double groundY;
        boolean overGround;

        /**
         * How much of this puff has settled onto the ground: 0 standing in the air, 1 lying flat.
         *
         * <p>Smoke drawn as a plate facing the camera cuts into the floor at a line, which is what
         * reads as a picture pasted into the world rather than smoke in it. Near the ground the
         * plate is turned to lie on the surface instead, so it spreads over it.
         */
        float settle(float pt) {
            if (!overGround || kind != Kind.SMOKE || !CONFIG.smokeGroundShards) return 0f;
            double at = py + (y - py) * pt;
            // Only a puff that has actually come to rest on the surface, and only once it is
            // dissipating. The first try flattened anything within a block of the ground, which in
            // practice is most smoke there is - it spawns at people's feet - and a flat plate seen
            // from standing height is edge-on and all but invisible. That read as the smoke being
            // gone.
            float above = (float) (at - groundY);
            if (above > 0.45f) return 0f;
            float low = 1f - smooth(Math.min(1f, Math.max(0f, above / 0.45f)));
            float old = smooth(Math.min(1f, Math.max(0f, (progress(pt) - 0.45f) / 0.3f)));
            return low * old;
        }

        public P kind(Kind k) { this.kind = k; return this; }
        public P at(double x, double y, double z) { this.x = this.px = x; this.y = this.py = y; this.z = this.pz = z; return this; }
        public P vel(double x, double y, double z) { this.vx = x; this.vy = y; this.vz = z; return this; }
        public P drag(double d) { this.drag = d; return this; }
        public P gravity(double g) { this.gravity = g; return this; }
        public P life(float ticks) { this.life = Math.max(1f, ticks); return this; }
        public P size(float from, float to) { this.size0 = from; this.size1 = to; return this; }
        public P spin(float start, float perTick) { this.rot = start; this.rotSpeed = perTick; return this; }
        public P colour(float r, float g, float b) { this.r = r; this.g = g; this.b = b; return this; }
        public P colour(Color c) { return colour(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f); }
        public P accent(float r, float g, float b) { this.ar = r; this.ag = g; this.ab = b; this.accentPalette = -1f; return this; }
        public P accent(Color c) { return accent(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f); }
        /** The accent follows the live palette (so RGB modes animate it) at this position. */
        public P accentPalette(float position, Palette.Element element) { this.accentPalette = position; this.element = element; return this; }
        public P alpha(float a) { this.alpha = a; return this; }
        public P edge(float e) { this.edge = e; return this; }
        public P turbulence(float t) { this.turbulence = t; return this; }
        public P stretch(float s) { this.stretch = s; return this; }
        public P variant(int v) { this.variant = v; return this; }
        public P fade(float formEnd, float fadeStart) { this.formEnd = formEnd; this.fadeStart = fadeStart; return this; }
        public P attract(double x, double y, double z, double pull) { this.attract = true; this.tx = x; this.ty = y; this.tz = z; this.pull = pull; return this; }
        public P swirl(double cx, double cz, double rate, double inward) { this.swirl = true; this.sx = cx; this.sz = cz; this.swirlRate = rate; this.inward = inward; return this; }
        /** Draws this decal with normal blending, so it can darken (a void core, an eclipse disc). */
        public P dark() { this.darkDecal = true; return this; }
        /** Turns this decal to face the camera every frame. */
        public P facing() { this.facing = true; return this; }
        public P decal(ResourceLocation texture, float nx, float ny, float nz) {
            this.kind = Kind.DECAL; this.decal = texture;
            float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (len < 1e-5f) { nx = 0; ny = 1; nz = 0; len = 1; }
            this.nx = nx / len; this.ny = ny / len; this.nz = nz / len;
            return this;
        }

        float progress(float pt) {
            return Math.min(1f, Math.max(0f, (age + pt) / life));
        }

        /** Forms quickly, holds, dissipates slowly: the shape of a real puff's opacity. */
        float opacity(float pt) {
            float p = progress(pt);
            float in = formEnd <= 0f ? 1f : smooth(Math.min(1f, p / formEnd));
            float out = p <= fadeStart ? 1f : 1f - smooth((p - fadeStart) / Math.max(1e-3f, 1f - fadeStart));
            return in * out;
        }

        float size(float pt) {
            float p = progress(pt);
            // Billows out fast and keeps spreading slowly, like a puff losing its momentum.
            float grow = 1f - (1f - p) * (1f - p);
            return size0 + (size1 - size0) * grow;
        }
    }

    private static final List<P> POOL = new ArrayList<P>();
    /** Where on the palette the current burst's accents sit; moved on per burst. */
    private static float accentBase;

    /** Starts a new burst: its accents cluster around a fresh point on the palette. */
    public static void burst() {
        accentBase = RANDOM.nextFloat() * 0.8f;
    }
    private static final Random RANDOM = new Random();
    private static long[] sortKeys = new long[256];
    private static Object lastWorld;

    private SmokeFx() {
    }

    // ---- settings ------------------------------------------------------------------------------

    public static boolean enabled() {
        return CONFIG.smokeEnabled;
    }

    private static Quality quality() {
        return CONFIG.smokeQuality == null ? Quality.HIGH : CONFIG.smokeQuality;
    }

    /** Scales an emitter's particle count by the density slider and the quality level. */
    public static int count(int base) {
        float density = CONFIG.smokeDensity / 100f;
        float q;
        switch (quality()) {
            case LOW: q = 0.4f; break;
            case MEDIUM: q = 0.7f; break;
            case ULTRA: q = 1.35f; break;
            default: q = 1f; break;
        }
        return Math.max(1, Math.round(base * density * q));
    }

    private static float sizeScale() {
        return CONFIG.smokeSize / 100f;
    }

    private static float speedScale() {
        return Math.max(0.05f, CONFIG.smokeSpeed / 100f);
    }

    /** The configured smoke colour, the darkness every variant starts from. */
    public static Color smokeColour() {
        return new Color(CONFIG.smokeColor);
    }

    /** The accent for an emitter that does not choose its own: the theme's, or the fixed one. */
    public static Color accentColour(float position) {
        if (CONFIG.smokeAccentUseTheme) return Palette.getColor(position);
        return new Color(CONFIG.smokeAccentColor);
    }

    // ---- pool ----------------------------------------------------------------------------------

    public static P particle() {
        P p = new P();
        p.seed = RANDOM.nextFloat() * 100f;
        p.variant = RANDOM.nextInt(4);
        Color base = smokeColour();
        p.colour(base);
        if (CONFIG.smokeAccentUseTheme) {
            // Restrained: one family of the palette per burst, not every colour at once.
            p.accentPalette(accentBase + RANDOM.nextFloat() * 0.18f, Palette.currentElement() == Palette.Element.GENERAL
                    ? Palette.Element.PARTICLES : Palette.currentElement());
        } else {
            p.accent(new Color(CONFIG.smokeAccentColor));
        }
        return p;
    }

    public static void spawn(P p) {
        if (!enabled() || p == null) return;
        if (POOL.size() >= quality().cap) return;
        float s = sizeScale();
        if (sorted(p.kind)) {
            // Larger, individually fainter puffs that overlap: density comes from the layering,
            // which is what makes it read as volume rather than as a pile of cut-outs. A little
            // variation in opacity and shade per puff gives the layers depth.
            s *= 1.35f;
            p.alpha *= 0.55f + 0.45f * RANDOM.nextFloat();
            float lift = RANDOM.nextFloat() * 0.06f;
            p.r = Math.min(1f, p.r + lift);
            p.g = Math.min(1f, p.g + lift);
            p.b = Math.min(1f, p.b + lift * 1.2f);
        }
        p.size0 *= s;
        p.size1 *= s;
        POOL.add(p);
    }

    public static int count() {
        return POOL.size();
    }

    public static void clear() {
        POOL.clear();
    }

    public static void tick() {
        Minecraft mc = Minecraft.getMinecraft();
        // Entity positions and ids belong to a world; a new world means everything here is stale.
        Object world = mc.theWorld;
        if (world != lastWorld) {
            POOL.clear();
            lastWorld = world;
        }
        if (world == null || !enabled()) {
            POOL.clear();
            return;
        }

        float speed = speedScale();
        float turbulence = CONFIG.smokeTurbulence / 100f;
        float t = FxClock.seconds();
        int write = 0;
        for (int i = 0; i < POOL.size(); i++) {
            P p = POOL.get(i);
            p.age += speed;
            if (p.age >= p.life) continue;
            step(p, speed, turbulence, t);
            POOL.set(write++, p);
        }
        for (int i = POOL.size() - 1; i >= write; i--) POOL.remove(i);
    }

    /** Reused for the block lookups under the smoke, so the pool still allocates nothing. */
    private static final net.minecraft.util.BlockPos.MutableBlockPos GROUND_POS =
            new net.minecraft.util.BlockPos.MutableBlockPos();

    private static void step(P p, float speed, float turbulence, float t) {
        p.px = p.x;
        p.py = p.y;
        p.pz = p.z;
        if (p.kind == Kind.SMOKE && CONFIG.smokeGroundShards) findGround(p);

        if (p.turbulence > 0f && turbulence > 0f && p.kind != Kind.DECAL) {
            // A curl-like field: rotating sines in three planes give swirling, divergence-light flow
            // that curls rather than jitters, which is what makes smoke look like smoke.
            double k = 0.9;
            double ph = p.seed + t * 0.6;
            double cx = Math.sin(p.y * k + ph) - Math.cos(p.z * k * 1.3 - ph * 0.7);
            double cy = Math.sin(p.z * k + ph * 0.8) - Math.cos(p.x * k * 1.1 + ph);
            double cz = Math.sin(p.x * k + ph * 1.2) - Math.cos(p.y * k * 0.9 - ph * 0.5);
            double amount = 0.0045 * p.turbulence * turbulence;
            p.vx += cx * amount;
            p.vy += cy * amount * 0.6;
            p.vz += cz * amount;
        }

        if (p.attract) {
            double dx = p.tx - p.x, dy = p.ty - p.y, dz = p.tz - p.z;
            double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (d > 1e-3) {
                // Pulled harder as it closes in, so it accelerates into the point and vanishes.
                double f = p.pull * (1.0 + 2.0 * p.progress(0f));
                p.vx += dx / d * f;
                p.vy += dy / d * f;
                p.vz += dz / d * f;
            }
            if (d < 0.15) p.age = Math.max(p.age, p.life * 0.92f);
        }

        if (p.swirl) {
            double dx = p.x - p.sx, dz = p.z - p.sz;
            double radius = Math.sqrt(dx * dx + dz * dz);
            if (radius > 1e-3) {
                double tangential = p.swirlRate;
                p.vx += (-dz / radius) * tangential - dx / radius * p.inward;
                p.vz += (dx / radius) * tangential - dz / radius * p.inward;
            }
        }

        p.vy -= p.gravity;
        p.vx *= p.drag;
        p.vy *= p.drag;
        p.vz *= p.drag;
        p.x += p.vx * speed;
        p.y += p.vy * speed;
        p.z += p.vz * speed;
        p.rot += p.rotSpeed * speed;
    }

    // ---- variants ------------------------------------------------------------------------------

    /** Dense black wisps with violet light, spiralling slowly in toward the centre. */
    public static void abyssal(double x, double y, double z, float scale, int base) {
        if (!enabled()) return;
        burst();
        int n = count(base);
        for (int i = 0; i < n; i++) {
            double a = RANDOM.nextDouble() * Math.PI * 2;
            double r = (0.6 + RANDOM.nextDouble() * 1.4) * scale;
            P p = particle().kind(i % 4 == 0 ? Kind.WISP : Kind.SMOKE)
                    .at(x + Math.cos(a) * r, y + RANDOM.nextDouble() * 1.6 * scale, z + Math.sin(a) * r)
                    .vel(0, 0.004 + RANDOM.nextDouble() * 0.01, 0)
                    .swirl(x, z, 0.0035, 0.0022)
                    .life(50 + RANDOM.nextInt(40))
                    .size(0.5f * scale, (1.3f + RANDOM.nextFloat()) * scale)
                    .spin(RANDOM.nextFloat() * 360f, (RANDOM.nextFloat() - 0.5f) * 2.5f)
                    .alpha(0.92f).edge(0.75f).fade(0.15f, 0.5f);
            p.colour(darken(smokeColour(), 0.7f));
            spawn(p);
        }
        accents(x, y + 0.8 * scale, z, scale, Math.max(1, n / 4), Kind.EMBER, 0.01f);
    }

    /** Smoke and motes drawn into a point, accelerating, then gone: a soul pulled out of a body. */
    public static void siphon(double x, double y, double z, double tx, double ty, double tz, float scale, int base) {
        if (!enabled()) return;
        burst();
        int n = count(base);
        for (int i = 0; i < n; i++) {
            double a = RANDOM.nextDouble() * Math.PI * 2;
            double r = (0.4 + RANDOM.nextDouble() * 0.8) * scale;
            boolean mote = i % 3 == 0;
            P p = particle().kind(mote ? Kind.EMBER : (i % 3 == 1 ? Kind.WISP : Kind.SMOKE))
                    .at(x + Math.cos(a) * r, y + RANDOM.nextDouble() * 1.8 * scale, z + Math.sin(a) * r)
                    .vel((RANDOM.nextDouble() - 0.5) * 0.05, 0.02, (RANDOM.nextDouble() - 0.5) * 0.05)
                    .attract(tx, ty, tz, mote ? 0.012 : 0.008)
                    .drag(0.9)
                    .life(30 + RANDOM.nextInt(25))
                    .size(mote ? 0.12f * scale : 0.6f * scale, mote ? 0.05f * scale : 0.25f * scale)
                    .spin(RANDOM.nextFloat() * 360f, 6f)
                    .alpha(mote ? 1f : 0.8f).edge(0.9f).fade(0.1f, 0.7f).turbulence(0.6f);
            spawn(p);
        }
    }

    /** Long dark strands left behind something moving; call every tick with its position and motion. */
    public static void phantomDrift(double x, double y, double z, double mx, double my, double mz, float scale, int base) {
        if (!enabled()) return;
        burst();
        int n = count(base);
        for (int i = 0; i < n; i++) {
            P p = particle().kind(Kind.WISP)
                    .at(x + (RANDOM.nextDouble() - 0.5) * 0.4 * scale, y + RANDOM.nextDouble() * 1.6 * scale,
                            z + (RANDOM.nextDouble() - 0.5) * 0.4 * scale)
                    .vel(-mx * 0.25, -my * 0.25 + 0.006, -mz * 0.25)
                    .life(24 + RANDOM.nextInt(16))
                    .size(0.35f * scale, 0.9f * scale)
                    .stretch(1.9f)
                    .alpha(0.7f).edge(0.5f).fade(0.1f, 0.4f).turbulence(0.5f);
            spawn(p);
        }
    }

    /**
     * Smoke flowing around a ring — a portal, a rift — with coloured fragments and runes caught in
     * it. The ring is in the plane whose normal is (nx, ny, nz).
     */
    public static void rift(double x, double y, double z, float radius, float nx, float ny, float nz, int base) {
        if (!enabled()) return;
        burst();
        int n = count(base);
        float[] u = new float[3], v = new float[3];
        basis(nx, ny, nz, u, v);
        for (int i = 0; i < n; i++) {
            double a = RANDOM.nextDouble() * Math.PI * 2;
            double r = radius * (0.9 + RANDOM.nextDouble() * 0.35);
            double cx = Math.cos(a), sy = Math.sin(a);
            double px = x + (u[0] * cx + v[0] * sy) * r;
            double py = y + (u[1] * cx + v[1] * sy) * r;
            double pz = z + (u[2] * cx + v[2] * sy) * r;
            // Tangent to the ring, so the smoke flows around it.
            double tx = (-u[0] * sy + v[0] * cx), ty = (-u[1] * sy + v[1] * cx), tz = (-u[2] * sy + v[2] * cx);
            Kind kind = i % 7 == 0 ? Kind.RUNE : (i % 5 == 0 ? Kind.SHARD : (i % 3 == 0 ? Kind.WISP : Kind.SMOKE));
            boolean accent = kind == Kind.RUNE || kind == Kind.SHARD;
            P p = particle().kind(kind).at(px, py, pz)
                    .vel(tx * 0.03, ty * 0.03 + 0.002, tz * 0.03)
                    .life(26 + RANDOM.nextInt(26))
                    .size(accent ? 0.22f : 0.45f, accent ? 0.16f : 1.1f)
                    .spin(RANDOM.nextFloat() * 360f, (RANDOM.nextFloat() - 0.5f) * 8f)
                    .alpha(accent ? 1f : 0.85f).edge(0.9f).fade(0.15f, 0.55f).turbulence(0.4f);
            spawn(p);
        }
    }

    /** Dark smoke rising off heat, with red-orange embers and sparks thrown up through it. */
    public static void infernalAsh(double x, double y, double z, float scale, int base) {
        if (!enabled()) return;
        burst();
        int n = count(base);
        for (int i = 0; i < n; i++) {
            double a = RANDOM.nextDouble() * Math.PI * 2;
            double r = RANDOM.nextDouble() * 0.7 * scale;
            P p = particle().kind(Kind.SMOKE)
                    .at(x + Math.cos(a) * r, y + RANDOM.nextDouble() * 0.4, z + Math.sin(a) * r)
                    .vel((RANDOM.nextDouble() - 0.5) * 0.02, 0.03 + RANDOM.nextDouble() * 0.04, (RANDOM.nextDouble() - 0.5) * 0.02)
                    .drag(0.96)
                    .life(40 + RANDOM.nextInt(40))
                    .size(0.45f * scale, (1.5f + RANDOM.nextFloat()) * scale)
                    .spin(RANDOM.nextFloat() * 360f, (RANDOM.nextFloat() - 0.5f) * 3f)
                    .alpha(0.8f).edge(0.7f).fade(0.1f, 0.45f);
            p.colour(0.06f, 0.035f, 0.03f);
            p.accent(1f, 0.38f + RANDOM.nextFloat() * 0.2f, 0.08f);
            spawn(p);
        }
        for (int i = 0; i < Math.max(2, n / 2); i++) {
            P e = particle().kind(i % 3 == 0 ? Kind.SPARK : Kind.EMBER)
                    .at(x + (RANDOM.nextDouble() - 0.5) * scale, y + 0.2, z + (RANDOM.nextDouble() - 0.5) * scale)
                    .vel((RANDOM.nextDouble() - 0.5) * 0.08, 0.08 + RANDOM.nextDouble() * 0.12, (RANDOM.nextDouble() - 0.5) * 0.08)
                    .gravity(0.002).drag(0.97)
                    .life(25 + RANDOM.nextInt(30))
                    .size(0.12f * scale, 0.05f * scale).stretch(2f)
                    .alpha(1f).fade(0.02f, 0.5f).turbulence(1.5f);
            e.accent(1f, 0.45f + RANDOM.nextFloat() * 0.35f, 0.1f);
            spawn(e);
        }
    }

    /** Translucent charcoal wisps with restrained cyan or violet highlights. */
    public static void spectral(double x, double y, double z, float scale, int base) {
        if (!enabled()) return;
        burst();
        int n = count(base);
        for (int i = 0; i < n; i++) {
            double a = RANDOM.nextDouble() * Math.PI * 2;
            double r = RANDOM.nextDouble() * 0.9 * scale;
            P p = particle().kind(i % 2 == 0 ? Kind.WISP : Kind.SMOKE)
                    .at(x + Math.cos(a) * r, y + RANDOM.nextDouble() * 1.2 * scale, z + Math.sin(a) * r)
                    .vel((RANDOM.nextDouble() - 0.5) * 0.02, 0.01 + RANDOM.nextDouble() * 0.015, (RANDOM.nextDouble() - 0.5) * 0.02)
                    .life(45 + RANDOM.nextInt(35))
                    .size(0.4f * scale, (1.2f + RANDOM.nextFloat() * 0.8f) * scale)
                    .spin(RANDOM.nextFloat() * 360f, (RANDOM.nextFloat() - 0.5f) * 2f)
                    .stretch(1.8f)
                    .alpha(0.45f).edge(1f).fade(0.2f, 0.5f).turbulence(1.2f);
            p.colour(0.16f, 0.17f, 0.2f);
            spawn(p);
        }
        accents(x, y + 0.6 * scale, z, scale, Math.max(1, n / 5), Kind.GLOW, 0.006f);
    }

    /** Small additive accents thrown loosely around a point. */
    public static void accents(double x, double y, double z, float scale, int n, Kind kind, float lift) {
        for (int i = 0; i < n; i++) {
            P p = particle().kind(kind)
                    .at(x + (RANDOM.nextDouble() - 0.5) * scale, y + (RANDOM.nextDouble() - 0.5) * scale,
                            z + (RANDOM.nextDouble() - 0.5) * scale)
                    .vel((RANDOM.nextDouble() - 0.5) * 0.03, lift + RANDOM.nextDouble() * 0.02, (RANDOM.nextDouble() - 0.5) * 0.03)
                    .life(30 + RANDOM.nextInt(30))
                    .size(0.14f * scale, 0.06f * scale)
                    .spin(RANDOM.nextFloat() * 360f, (RANDOM.nextFloat() - 0.5f) * 10f)
                    .alpha(1f).fade(0.15f, 0.5f).turbulence(1.2f);
            spawn(p);
        }
    }

    /** One world-space decal: a ring of runes, a fracture, a rift core. */
    public static P decal(ResourceLocation texture, double x, double y, double z, float nx, float ny, float nz,
                          float size0, float size1, float life, float spinPerTick) {
        P p = particle().decal(texture, nx, ny, nz).at(x, y, z).life(life).size(size0, size1)
                .spin(RANDOM.nextFloat() * 360f, spinPerTick).alpha(1f).fade(0.12f, 0.6f).turbulence(0f);
        p.drag = 1.0;
        return p;
    }

    private static Color darken(Color c, float k) {
        return new Color(Math.round(c.getRed() * k), Math.round(c.getGreen() * k), Math.round(c.getBlue() * k));
    }

    // ---- drawing -------------------------------------------------------------------------------

    /** Draws the pool. Called from the world pass, where the GL matrix is the camera's rotation. */
    public static void render(float pt) {
        if (POOL.isEmpty() || !enabled()) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null) return;

        Vec3 cam = Cameras.get().getPos();
        double cx = cam.xCoord, cy = cam.yCoord, cz = cam.zCoord;
        // Vanilla's own particle billboard basis, which is right in every camera mode, F5 included.
        float rX = ActiveRenderInfo.getRotationX();
        float rZ = ActiveRenderInfo.getRotationZ();
        float rYZ = ActiveRenderInfo.getRotationYZ();
        float rXY = ActiveRenderInfo.getRotationXY();
        float rXZ = ActiveRenderInfo.getRotationXZ();
        float[] right = {rX, 0f, rYZ};
        float[] up = {rXY, rZ, rXZ};
        // The view direction is perpendicular to both.
        float[] look = cross(up, right);

        float opacity = CONFIG.smokeOpacity / 100f;
        float glow = CONFIG.smokeGlow / 100f;
        boolean edges = quality().edges && glow > 0f;

        boolean lighting = GL11.glIsEnabled(GL11.GL_LIGHTING);
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.disableCull();
        GlStateManager.enableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.003f);
        GlStateManager.color(1f, 1f, 1f, 1f);
        try {
            int sortedCount = sortSmoke(cx, cy, cz);
            Tessellator tessellator = Tessellator.getInstance();
            WorldRenderer buffer = tessellator.getWorldRenderer();

            if (sortedCount > 0) {
                // Pass 1: the smoke itself, back to front, normal alpha blending.
                GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                mc.getTextureManager().bindTexture(SMOKE_ATLAS);
                buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
                for (int i = 0; i < sortedCount; i++) {
                    P p = POOL.get((int) (sortKeys[i] & 0xFFFFFFFFL));
                    float a = p.opacity(pt) * p.alpha * opacity;
                    if (a <= 0.004f) continue;
                    // Fully settled smoke is left to the fragment pass, unless that is switched
                    // off - in which case it still draws here, lying flat.
                    float settled = p.settle(pt);
                    if (settled > 0.985f && CONFIG.smokeGroundShards) continue;
                    smokeQuad(buffer, p, pt, cx, cy, cz, right, up, look, p.r, p.g, p.b,
                            a * (1f - settled * 0.35f), settled);
                }
                tessellator.draw();

                // Pass 1b: what has settled, as flat fragments lying on the surface.
                if (CONFIG.smokeGroundShards) {
                    GlStateManager.disableTexture2D();
                    buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
                    boolean anyShard = false;
                    for (int i = 0; i < sortedCount; i++) {
                        P p = POOL.get((int) (sortKeys[i] & 0xFFFFFFFFL));
                        float settled = p.settle(pt);
                        if (settled <= 0.02f) continue;
                        float a = p.opacity(pt) * p.alpha * opacity * settled;
                        if (a <= 0.004f) continue;
                        float[] ac = accent(p);
                        groundShard(buffer, p, pt, cx, cy, cz, ac, a);
                        anyShard = true;
                    }
                    if (anyShard) tessellator.draw();
                    else buffer.finishDrawing();
                    GlStateManager.enableTexture2D();
                }

                // Pass 2: the rim light, additive, in the accent colour.
                if (edges) {
                    GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
                    mc.getTextureManager().bindTexture(EDGE_ATLAS);
                    buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
                    for (int i = 0; i < sortedCount; i++) {
                        P p = POOL.get((int) (sortKeys[i] & 0xFFFFFFFFL));
                        float a = p.opacity(pt) * p.edge * glow * opacity * 0.6f;
                        if (a <= 0.004f) continue;
                        float settled = p.settle(pt);
                        if (settled > 0.985f && CONFIG.smokeGroundShards) continue;
                        float[] ac = accent(p);
                        smokeQuad(buffer, p, pt, cx, cy, cz, right, up, look, ac[0], ac[1], ac[2],
                                Math.min(1f, a) * (1f - settled), settled);
                    }
                    tessellator.draw();
                }
            }

            // Pass 3: additive accents from the FX atlas.
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            mc.getTextureManager().bindTexture(FX_ATLAS);
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
            boolean any = false;
            for (int i = 0; i < POOL.size(); i++) {
                P p = POOL.get(i);
                if (sorted(p.kind) || p.kind == Kind.DECAL) continue;
                float a = p.opacity(pt) * p.alpha * Math.max(0.35f, glow);
                if (a <= 0.004f) continue;
                float[] ac = accent(p);
                accentQuad(buffer, p, pt, cx, cy, cz, right, up, look, ac[0], ac[1], ac[2], Math.min(1f, a));
                any = true;
            }
            if (any) tessellator.draw();
            else buffer.finishDrawing();

            // Pass 4: decals, one texture each, few of them.
            for (int i = 0; i < POOL.size(); i++) {
                P p = POOL.get(i);
                if (p.kind != Kind.DECAL || p.decal == null) continue;
                float a = p.opacity(pt) * p.alpha * Math.max(0.35f, glow);
                if (a <= 0.004f) continue;
                float[] ac = accent(p);
                if (p.darkDecal) {
                    GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                    ac = new float[]{p.r, p.g, p.b};
                } else {
                    GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
                }
                if (p.facing) {
                    p.nx = -look[0];
                    p.ny = -look[1];
                    p.nz = -look[2];
                }
                mc.getTextureManager().bindTexture(p.decal);
                buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
                decalQuad(buffer, p, pt, cx, cy, cz, ac[0], ac[1], ac[2], Math.min(1f, a));
                tessellator.draw();
            }
        } finally {
            GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1f);
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GlStateManager.depthMask(true);
            GlStateManager.enableCull();
            GlStateManager.disableBlend();
            GlStateManager.color(1f, 1f, 1f, 1f);
            if (lighting) GlStateManager.enableLighting();
        }
    }

    /**
     * The surface under a puff, if there is one within a couple of blocks.
     *
     * <p>Looked up on the tick rather than the frame, and only for smoke, so this costs one block
     * read per puff per tick. A puff that finds nothing keeps standing up to the camera.
     */
    private static void findGround(P p) {
        net.minecraft.client.multiplayer.WorldClient world = mc.theWorld;
        if (world == null) {
            p.overGround = false;
            return;
        }
        int x = net.minecraft.util.MathHelper.floor_double(p.x);
        int z = net.minecraft.util.MathHelper.floor_double(p.z);
        int top = net.minecraft.util.MathHelper.floor_double(p.y);
        for (int y = top; y >= top - 2 && y > 0; y--) {
            GROUND_POS.set(x, y, z);
            net.minecraft.block.state.IBlockState state = world.getBlockState(GROUND_POS);
            if (state.getBlock().getMaterial().isSolid()) {
                p.groundY = y + 1.0;
                p.overGround = true;
                return;
            }
        }
        p.overGround = false;
    }

    /** Fills {@link #sortKeys} with the alpha-blended particles, farthest first; returns how many. */
    private static int sortSmoke(double cx, double cy, double cz) {
        int n = 0;
        if (sortKeys.length < POOL.size()) sortKeys = new long[POOL.size() * 2];
        for (int i = 0; i < POOL.size(); i++) {
            P p = POOL.get(i);
            if (!sorted(p.kind)) continue;
            double dx = p.x - cx, dy = p.y - cy, dz = p.z - cz;
            float d = (float) (dx * dx + dy * dy + dz * dz);
            // Positive floats order the same as their bits; inverted so ascending = farthest first.
            int bits = Float.floatToIntBits(d);
            sortKeys[n++] = ((long) (Integer.MAX_VALUE - bits) << 32) | (i & 0xFFFFFFFFL);
        }
        Arrays.sort(sortKeys, 0, n);
        return n;
    }

    private static float[] accent(P p) {
        float[] out = SCRATCH_ACCENT;
        if (p.accentPalette >= 0f) {
            Color c = Palette.getColor(p.element, p.accentPalette);
            out[0] = c.getRed() / 255f;
            out[1] = c.getGreen() / 255f;
            out[2] = c.getBlue() / 255f;
        } else {
            out[0] = p.ar;
            out[1] = p.ag;
            out[2] = p.ab;
        }
        return out;
    }

    private static final float[] SCRATCH_ACCENT = new float[3];

    /**
     * One puff.
     *
     * <p>A single plate facing the camera is what makes smoke read as a picture: it has no
     * thickness, so walking around it changes nothing, and every puff in the cloud is exactly
     * parallel to every other. Two things are done about that here. The puff is drawn as a few
     * slices spread along its own axis, which do move against each other as the camera goes round
     * and overlap into something with depth; and each slice is shaded across its face from a fixed
     * light, so it has a lit side and a side in shadow instead of one flat tone.
     *
     * <p>{@code settled} turns the slices down flat onto the ground - see {@link P#settle}.
     */
    private static void smokeQuad(WorldRenderer buffer, P p, float pt, double cx, double cy, double cz,
                                  float[] right, float[] up, float[] look,
                                  float r, float g, float b, float a, float settled) {
        double x = p.px + (p.x - p.px) * pt - cx;
        double y = p.py + (p.y - p.py) * pt - cy;
        double z = p.pz + (p.z - p.pz) * pt - cz;
        float half = p.size(pt) * 0.5f;
        float u0, v0, u1, v1;
        float[] ax = SCRATCH_A, ay = SCRATCH_B;
        boolean wisp = p.kind == Kind.WISP;
        if (wisp) {
            // Wisps lie along their own motion, projected onto the view plane.
            int w = p.variant & 1;
            u0 = w * 0.25f; u1 = u0 + 0.25f; v0 = 0.5f; v1 = 0.75f;
            if (!motionAxes(p, look, right, up, half * p.stretch, half * 0.45f, ax, ay)) {
                rotated(right, up, p.rot, half * p.stretch, half * 0.45f, ax, ay);
            }
        } else {
            int v = p.variant & 3;
            u0 = v * 0.25f; u1 = u0 + 0.25f; v0 = 0f; v1 = 0.5f;
            rotated(right, up, p.rot + (float) Math.sin(p.seed + p.age * 0.02f) * 6f, half, half, ax, ay);
        }
        if (settled > 0.002f) flatten(ax, ay, settled, half);

        int slices = wisp ? 1 : volumeSlices();
        if (slices <= 1) {
            shadedQuad(buffer, x, y, z, ax, ay, u0, v0, u1, v1, r, g, b, a, p);
            return;
        }
        // The slices are spread along an axis of the puff's own, not the camera's, so the shape
        // they make together is the same from every side.
        float sx = MathHelper.cos(p.seed * 1.7f), sz = MathHelper.sin(p.seed * 1.7f);
        float sy = MathHelper.cos(p.seed * 2.9f) * 0.6f;
        float spread = half * 0.5f;
        float each = a / (float) Math.sqrt(slices);
        for (int i = 0; i < slices; i++) {
            float f = slices == 1 ? 0f : (i / (float) (slices - 1) - 0.5f) * 2f;
            float scale = 1f - Math.abs(f) * 0.22f;
            SCRATCH_C[0] = ax[0] * scale; SCRATCH_C[1] = ax[1] * scale; SCRATCH_C[2] = ax[2] * scale;
            SCRATCH_D[0] = ay[0] * scale; SCRATCH_D[1] = ay[1] * scale; SCRATCH_D[2] = ay[2] * scale;
            shadedQuad(buffer, x + sx * spread * f, y + sy * spread * f, z + sz * spread * f,
                    SCRATCH_C, SCRATCH_D, u0, v0, u1, v1, r, g, b, each * (1f - Math.abs(f) * 0.25f), p);
        }
    }

    /** How many slices a puff is drawn as; one on the lowest setting, so nothing is slower. */
    private static int volumeSlices() {
        if (!CONFIG.smokeVolumetric) return 1;
        switch (quality()) {
            case LOW: return 1;
            case MEDIUM: return 2;
            default: return 3;
        }
    }

    /** Turns a pair of billboard axes down towards lying flat on the ground. */
    private static void flatten(float[] ax, float[] ay, float settled, float half) {
        // The flat target is the world's own X and Z, so settled smoke spreads over the surface.
        float fx = half * 1.35f, fz = half * 1.35f;
        ax[0] += (fx - ax[0]) * settled;
        ax[1] += (0f - ax[1]) * settled;
        ax[2] += (0f - ax[2]) * settled;
        ay[0] += (0f - ay[0]) * settled;
        ay[1] += (0f - ay[1]) * settled;
        ay[2] += (fz - ay[2]) * settled;
    }

    /** The light the smoke is shaded from: high, and a little to one side. */
    private static final float[] LIGHT = {0.35f, 0.86f, 0.38f};

    /**
     * A quad whose corners are lit by {@link #LIGHT}.
     *
     * <p>The corner facing the light is brightened and the opposite one darkened, which is what
     * gives a flat plate the look of a rounded mass. The texture is unchanged; only the vertex
     * colours differ, so this costs nothing beyond the colours already being written.
     */
    private static void shadedQuad(WorldRenderer buffer, double x, double y, double z,
                                   float[] ax, float[] ay, float u0, float v0, float u1, float v1,
                                   float r, float g, float b, float a, P p) {
        float lx = dot(ax, LIGHT), ly = dot(ay, LIGHT);
        float len = (float) Math.sqrt(lx * lx + ly * ly);
        if (len > 1e-4f) {
            lx /= len;
            ly /= len;
        }
        float strength = p.kind == Kind.SMOKE ? 0.45f : 0.25f;
        float c00 = 1f + (-lx - ly) * strength;
        float c10 = 1f + (lx - ly) * strength;
        float c11 = 1f + (lx + ly) * strength;
        float c01 = 1f + (-lx + ly) * strength;
        buffer.pos(x - ax[0] - ay[0], y - ax[1] - ay[1], z - ax[2] - ay[2]).tex(u0, v1)
                .color(r * c00, g * c00, b * c00, a).endVertex();
        buffer.pos(x + ax[0] - ay[0], y + ax[1] - ay[1], z + ax[2] - ay[2]).tex(u1, v1)
                .color(r * c10, g * c10, b * c10, a).endVertex();
        buffer.pos(x + ax[0] + ay[0], y + ax[1] + ay[1], z + ax[2] + ay[2]).tex(u1, v0)
                .color(r * c11, g * c11, b * c11, a).endVertex();
        buffer.pos(x - ax[0] + ay[0], y - ax[1] + ay[1], z - ax[2] + ay[2]).tex(u0, v0)
                .color(r * c01, g * c01, b * c01, a).endVertex();
    }

    private static float dot(float[] a, float[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    /**
     * A settled puff, as a fragment of glass lying on the ground.
     *
     * <p>Smoke that has come to rest on a surface is drawn as an irregular flat sliver with a lit
     * edge rather than as another soft blob: angular, slightly raised off the surface so it does
     * not fight with it, and catching the light along one side as it turns. The shape is fixed per
     * particle, so a fragment does not shimmer as it fades.
     */
    private static void groundShard(WorldRenderer buffer, P p, float pt, double cx, double cy, double cz,
                                    float[] accent, float a) {
        double x = p.px + (p.x - p.px) * pt - cx;
        double z = p.pz + (p.z - p.pz) * pt - cz;
        double y = p.groundY + 0.02 - cy;
        float half = p.size(pt) * 0.62f;
        int sides = 5 + (p.variant & 1);
        float spin = (float) Math.toRadians(p.rot * 0.35f);
        // A sliver is brightest where it is edge-on to the light, which sweeps as it turns.
        float glint = 0.35f + 0.65f * Math.abs(MathHelper.sin(spin * 2f + p.seed));
        float r = p.r * 0.5f + accent[0] * 0.5f;
        float g = p.g * 0.5f + accent[1] * 0.5f;
        float b = p.b * 0.5f + accent[2] * 0.5f;
        float edgeR = Math.min(1f, r * 0.4f + accent[0] * 0.9f + glint * 0.35f);
        float edgeG = Math.min(1f, g * 0.4f + accent[1] * 0.9f + glint * 0.35f);
        float edgeB = Math.min(1f, b * 0.4f + accent[2] * 0.9f + glint * 0.35f);
        float px = 0f, pz = 0f;
        for (int i = 0; i <= sides; i++) {
            float ang = spin + (float) (i / (double) sides * Math.PI * 2);
            // A radius that varies per corner is what makes it a shard and not a coin.
            float radius = half * (0.45f + 0.55f * Math.abs(MathHelper.sin(p.seed * 3.1f + i * 2.3f)));
            float vx = MathHelper.cos(ang) * radius, vz = MathHelper.sin(ang) * radius;
            if (i > 0) {
                // Centre, then the edge pair, with the last vertex doubled: a fan drawn as quads.
                buffer.pos(x, y, z).color(r, g, b, a * 0.85f).endVertex();
                buffer.pos(x + px, y, z + pz).color(edgeR, edgeG, edgeB, a * 0.5f).endVertex();
                buffer.pos(x + vx, y, z + vz).color(edgeR, edgeG, edgeB, a * 0.5f).endVertex();
                buffer.pos(x, y, z).color(r, g, b, a * 0.85f).endVertex();
            }
            px = vx;
            pz = vz;
        }
    }

    private static void accentQuad(WorldRenderer buffer, P p, float pt, double cx, double cy, double cz,
                                   float[] right, float[] up, float[] look,
                                   float r, float g, float b, float a) {
        double x = p.px + (p.x - p.px) * pt - cx;
        double y = p.py + (p.y - p.py) * pt - cy;
        double z = p.pz + (p.z - p.pz) * pt - cz;
        float half = p.size(pt) * 0.5f;
        int cell;
        switch (p.kind) {
            case EMBER: cell = 0; break;
            case GLOW: cell = 1; break;
            case SPARK: cell = 2; break;
            case FEATHER: cell = 3; break;
            case SHARD: cell = 4 + (p.variant % 3); break;
            case RUNE: cell = 8 + (p.variant & 7); break;
            default: cell = 1; break;
        }
        float u0 = (cell % 4) * 0.25f, v0 = (cell / 4) * 0.25f;
        float[] ax = SCRATCH_A, ay = SCRATCH_B;
        if (p.kind == Kind.SPARK) {
            if (!motionAxes(p, look, right, up, half * p.stretch, half * 0.6f, ax, ay)) {
                rotated(right, up, p.rot, half, half, ax, ay);
            }
        } else if (p.kind == Kind.FEATHER) {
            float sway = (float) Math.sin(p.seed + p.age * 0.18f) * 35f;
            rotated(right, up, p.rot + sway, half, half, ax, ay);
        } else if (p.kind == Kind.SHARD) {
            // Tumbling: the quad's width breathes as though turning edge-on and back.
            float turn = (float) Math.cos(p.seed + p.age * 0.25f);
            rotated(right, up, p.rot, half, half, ax, ay);
            ax[0] *= turn; ax[1] *= turn; ax[2] *= turn;
            a *= 0.55f + 0.45f * Math.abs(turn);
        } else {
            rotated(right, up, p.rot, half, half, ax, ay);
        }
        quad(buffer, x, y, z, ax, ay, u0, v0, u0 + 0.25f, v0 + 0.25f, r, g, b, a);
    }

    private static void decalQuad(WorldRenderer buffer, P p, float pt, double cx, double cy, double cz,
                                  float r, float g, float b, float a) {
        double x = p.px + (p.x - p.px) * pt - cx;
        double y = p.py + (p.y - p.py) * pt - cy;
        double z = p.pz + (p.z - p.pz) * pt - cz;
        float half = p.size(pt) * 0.5f;
        float[] u = SCRATCH_A, v = SCRATCH_B;
        basis(p.nx, p.ny, p.nz, u, v);
        float rot = (float) Math.toRadians(p.rot + p.rotSpeed * pt);
        float c = (float) Math.cos(rot), s = (float) Math.sin(rot);
        float ux = (u[0] * c + v[0] * s) * half, uy = (u[1] * c + v[1] * s) * half, uz = (u[2] * c + v[2] * s) * half;
        float vx = (-u[0] * s + v[0] * c) * half, vy = (-u[1] * s + v[1] * c) * half, vz = (-u[2] * s + v[2] * c) * half;
        u[0] = ux; u[1] = uy; u[2] = uz;
        v[0] = vx; v[1] = vy; v[2] = vz;
        quad(buffer, x, y, z, u, v, 0f, 0f, 1f, 1f, r, g, b, a);
    }

    private static final float[] SCRATCH_A = new float[3];
    private static final float[] SCRATCH_B = new float[3];
    /** The slice axes, so drawing a puff as several plates still allocates nothing. */
    private static final float[] SCRATCH_C = new float[3];
    private static final float[] SCRATCH_D = new float[3];

    private static void quad(WorldRenderer buffer, double x, double y, double z, float[] ax, float[] ay,
                             float u0, float v0, float u1, float v1, float r, float g, float b, float a) {
        buffer.pos(x - ax[0] - ay[0], y - ax[1] - ay[1], z - ax[2] - ay[2]).tex(u0, v1).color(r, g, b, a).endVertex();
        buffer.pos(x + ax[0] - ay[0], y + ax[1] - ay[1], z + ax[2] - ay[2]).tex(u1, v1).color(r, g, b, a).endVertex();
        buffer.pos(x + ax[0] + ay[0], y + ax[1] + ay[1], z + ax[2] + ay[2]).tex(u1, v0).color(r, g, b, a).endVertex();
        buffer.pos(x - ax[0] + ay[0], y - ax[1] + ay[1], z - ax[2] + ay[2]).tex(u0, v0).color(r, g, b, a).endVertex();
    }

    /** Camera-facing axes turned by {@code degrees} about the view direction. */
    private static void rotated(float[] right, float[] up, float degrees, float w, float h, float[] ax, float[] ay) {
        double rad = Math.toRadians(degrees);
        float c = (float) Math.cos(rad), s = (float) Math.sin(rad);
        for (int k = 0; k < 3; k++) {
            ax[k] = (right[k] * c + up[k] * s) * w;
            ay[k] = (-right[k] * s + up[k] * c) * h;
        }
    }

    /** Axes along the particle's motion as seen from the camera; false when it is barely moving. */
    private static boolean motionAxes(P p, float[] look, float[] right, float[] up, float along, float across,
                                      float[] ax, float[] ay) {
        float mx = (float) (p.x - p.px), my = (float) (p.y - p.py), mz = (float) (p.z - p.pz);
        float dot = mx * look[0] + my * look[1] + mz * look[2];
        mx -= look[0] * dot;
        my -= look[1] * dot;
        mz -= look[2] * dot;
        float len = (float) Math.sqrt(mx * mx + my * my + mz * mz);
        if (len < 1e-4f) return false;
        mx /= len;
        my /= len;
        mz /= len;
        // Across = look x along, which lies in the view plane.
        float sx = look[1] * mz - look[2] * my;
        float sy = look[2] * mx - look[0] * mz;
        float sz = look[0] * my - look[1] * mx;
        ax[0] = mx * along; ax[1] = my * along; ax[2] = mz * along;
        ay[0] = sx * across; ay[1] = sy * across; ay[2] = sz * across;
        return true;
    }

    /** Two unit vectors spanning the plane with this normal. */
    static void basis(float nx, float ny, float nz, float[] u, float[] v) {
        float ax = Math.abs(nx) < 0.9f ? 1f : 0f, ay = ax == 1f ? 0f : 1f;
        // u = normalize(a x n)
        float ux = ay * nz, uy = -ax * nz, uz = ax * ny - ay * nx;
        float len = (float) Math.sqrt(ux * ux + uy * uy + uz * uz);
        u[0] = ux / len; u[1] = uy / len; u[2] = uz / len;
        // v = n x u
        v[0] = ny * u[2] - nz * u[1];
        v[1] = nz * u[0] - nx * u[2];
        v[2] = nx * u[1] - ny * u[0];
    }

    private static float[] cross(float[] a, float[] b) {
        return new float[]{a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    static float smooth(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return t * t * (3f - 2f * t);
    }
}
