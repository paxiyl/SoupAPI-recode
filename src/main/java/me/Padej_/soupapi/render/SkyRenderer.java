package me.Padej_.soupapi.render;

import me.Padej_.soupapi.config.ConfigurableModule;
import me.Padej_.soupapi.config.SoupAPI_Config;
import me.Padej_.soupapi.modules.AnimatedSky;
import me.Padej_.soupapi.utils.FxClock;
import me.Padej_.soupapi.utils.Palette;
import me.Padej_.soupapi.utils.SkyStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.IRenderHandler;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.nio.FloatBuffer;
import java.util.Random;

/**
 * The animated sky, drawn where the sky is drawn.
 *
 * <p>Installed as the world provider's sky renderer — Forge's own hook, which {@code
 * RenderGlobal.renderSky} defers to when one is set — so it runs at the sky's own point in the frame:
 * before terrain, entities and every effect, under the camera's rotation and with nothing to cover
 * up. The previous version was a screen-space quad painted from {@code RenderWorldLastEvent}, after
 * the world, which is why it hid the mod's own effects and why turning the camera slid it around
 * instead of the world turning under it.
 *
 * <p>The sky is a dome of {@value #AZ}×{@value #EL} quads, each corner coloured by the style's
 * function of direction and time — 1.8.9 has no shader to put a pattern into, and gl4es would not
 * run one if it had — then detailed by a tileable texture layer, stars, and geometry the style owns
 * (aurora curtains, a corona, rift cracks, rune rings, falling glyphs, glass, lightning). Near the
 * horizon the colour is blended into the current fog colour, so the terrain fades into the sky
 * instead of meeting it at a seam. Vanilla's sun and moon can be drawn over it.
 *
 * <p>State on exit matches what vanilla's own sky leaves: texturing on, depth writes on, fog on,
 * blending off, alpha test on.
 */
public final class SkyRenderer extends IRenderHandler {

    public static final SkyRenderer INSTANCE = new SkyRenderer();

    private static final int AZ = 48;
    private static final int EL = 24;
    private static final float EL_MIN = (float) Math.toRadians(-30);
    private static final float EL_MAX = (float) Math.toRadians(90);
    /** Well inside the far plane at the shortest render distance (64 blocks). */
    private static final float R = 16f;

    private static final ResourceLocation CLOUD = new ResourceLocation("soupapi", "textures/vfx/sky_cloud.png");
    private static final ResourceLocation GRAIN = new ResourceLocation("soupapi", "textures/vfx/sky_grain.png");
    private static final ResourceLocation STREAK = new ResourceLocation("soupapi", "textures/vfx/sky_streak.png");
    private static final ResourceLocation SUN = new ResourceLocation("textures/environment/sun.png");
    private static final ResourceLocation MOON = new ResourceLocation("textures/environment/moon_phases.png");

    private static final int V = (AZ + 1) * (EL + 1);
    private static final float[] DX = new float[V], DY = new float[V], DZ = new float[V];
    private static final float[] AZI = new float[V], ELE = new float[V];
    private static final float[] CR = new float[V], CG = new float[V], CB = new float[V];

    private static final int STARS = 600;
    private static final float[] SX = new float[STARS], SY = new float[STARS], SZ = new float[STARS];
    private static final float[] SB = new float[STARS], SP = new float[STARS];

    /** Fixed scatter for the style geometry (rifts, shards, figures, glyph columns). */
    private static final float[] RAND = new float[512];

    /**
     * The shattered-glass web: rays out from the impact and rings around it, which is how glass
     * actually breaks - radial cracks first, then circumferential ones between them. The nodes
     * are the crossings, stored as an angular radius from the impact and an angle around it; each
     * cell between four of them is a pane.
     */
    private static final int WEB_RAYS = 18, WEB_RINGS = 9;
    private static final float[] WEB_RADIUS = new float[WEB_RAYS * (WEB_RINGS + 1)];
    private static final float[] WEB_ANGLE = new float[WEB_RAYS * (WEB_RINGS + 1)];
    /** Per-pane shade and the phase of its sheen, so no two panes catch the light together. */
    private static final float[] PANE_SHADE = new float[WEB_RAYS * WEB_RINGS];
    private static final float[] PANE_PHASE = new float[WEB_RAYS * WEB_RINGS];
    /** How heavy each radial crack is: a few run the whole way, most are hairlines. */
    private static final float[] RAY_WEIGHT = new float[WEB_RAYS];
    /** Which stretches of the rings are actually broken. Glass does not crack in full circles. */
    private static final boolean[] RING_SEGMENT = new boolean[WEB_RAYS * (WEB_RINGS + 1)];
    private static final float[] SCRATCH_A = new float[3], SCRATCH_B = new float[3];
    private static final float[] SCRATCH_C = new float[3], SCRATCH_D = new float[3];

    private static final FloatBuffer FOG = BufferUtils.createFloatBuffer(16);

    /** Where the pane is struck: high and a little to one side, so the web is not centred. */
    private static final float[] IMPACT = {-0.34f, 0.86f, -0.38f};
    private static final float[] IMPACT_U = new float[3], IMPACT_V = new float[3];

    static {
        float il = (float) Math.sqrt(IMPACT[0] * IMPACT[0] + IMPACT[1] * IMPACT[1] + IMPACT[2] * IMPACT[2]);
        IMPACT[0] /= il;
        IMPACT[1] /= il;
        IMPACT[2] /= il;
        SmokeFx.basis(IMPACT[0], IMPACT[1], IMPACT[2], IMPACT_U, IMPACT_V);
        Random web = new Random(0x5A77E2DL);
        for (int ray = 0; ray < WEB_RAYS; ray++) {
            // Each ray leaves the impact at its own angle and wanders as it goes out, so the
            // panes are irregular rather than a dartboard.
            float base = (float) (ray / (double) WEB_RAYS * Math.PI * 2) + (web.nextFloat() - 0.5f) * 0.22f;
            float wander = 0f;
            for (int ring = 0; ring <= WEB_RINGS; ring++) {
                float f = ring / (float) WEB_RINGS;
                // Rings crowd near the impact and spread out towards the horizon, and each node
                // sits well off the nominal radius so a ring reads as a jagged line of breaks
                // rather than a circle.
                float radius = (float) (Math.pow(f, 1.4) * 2.6 + 0.05);
                radius *= 0.62f + web.nextFloat() * 0.74f;
                wander += (web.nextFloat() - 0.5f) * 0.16f;
                int k = ray * (WEB_RINGS + 1) + ring;
                WEB_RADIUS[k] = radius;
                WEB_ANGLE[k] = base + wander * f;
                // Three segments in ten are missing, which is what keeps the rings from closing.
                RING_SEGMENT[k] = web.nextFloat() > 0.3f;
            }
            // Two rays in nine are the heavy cracks that carry the break outward.
            RAY_WEIGHT[ray] = web.nextFloat() < 0.22f ? 1f : 0.3f + web.nextFloat() * 0.3f;
        }
        for (int i = 0; i < PANE_SHADE.length; i++) {
            PANE_SHADE[i] = 0.35f + web.nextFloat() * 0.65f;
            PANE_PHASE[i] = web.nextFloat() * 6.283f;
        }
        for (int j = 0; j <= EL; j++) {
            float el = EL_MIN + (EL_MAX - EL_MIN) * j / EL;
            for (int i = 0; i <= AZ; i++) {
                float az = (float) (Math.PI * 2 * i / AZ);
                int k = j * (AZ + 1) + i;
                float c = (float) Math.cos(el);
                DX[k] = (float) Math.cos(az) * c;
                DY[k] = (float) Math.sin(el);
                DZ[k] = (float) Math.sin(az) * c;
                AZI[k] = az;
                ELE[k] = el;
            }
        }
        Random random = new Random(0x5EED);
        for (int s = 0; s < STARS; s++) {
            // Uniform on the upper hemisphere and a little below the horizon.
            float y = -0.15f + 1.15f * random.nextFloat();
            float a = (float) (random.nextFloat() * Math.PI * 2);
            float rr = (float) Math.sqrt(Math.max(0, 1 - y * y));
            SX[s] = (float) Math.cos(a) * rr;
            SY[s] = y;
            SZ[s] = (float) Math.sin(a) * rr;
            SB[s] = 0.35f + 0.65f * random.nextFloat() * random.nextFloat();
            SP[s] = random.nextFloat() * 100f;
        }
        for (int i = 0; i < RAND.length; i++) RAND[i] = random.nextFloat();
    }

    private SkyRenderer() {
    }

    // ---- per-frame inputs ------------------------------------------------------------------------

    private float t;
    private float intensity;
    private float[] main = new float[3], alt = new float[3], dark = new float[3];
    private float[] fog = new float[3], vanilla = new float[3];
    private final float[] out = new float[3];
    private SkyStyle style;

    @Override
    public void render(float partialTicks, WorldClient world, Minecraft mc) {
        SoupAPI_Config c = ConfigurableModule.CONFIG;
        style = AnimatedSky.style();
        boolean tintOnly = style == SkyStyle.PLAIN;
        if (tintOnly && !c.coloredSkyEnabled) return; // uninstall pending; draw nothing extra

        Palette.Element previous = Palette.scope(Palette.Element.SKY);
        try {
            prepare(world, mc, partialTicks, c);
            beginState();
            try {
                Tessellator tessellator = Tessellator.getInstance();
                WorldRenderer buffer = tessellator.getWorldRenderer();
                computeColours(tintOnly, c);
                drawDome(tessellator, buffer);
                if (!tintOnly) {
                    detailLayer(mc, tessellator, buffer, c);
                    if (style.wantsStars() || c.skyStars > 0 && starsFor(style)) stars(mc, tessellator, buffer, c, world, partialTicks);
                    extras(mc, tessellator, buffer, c);
                }
                if (c.skyCelestials) celestials(mc, tessellator, buffer, world, partialTicks, tintOnly ? 1f : 0.85f);
            } finally {
                endState();
            }
        } finally {
            Palette.scope(previous);
        }
    }

    private boolean starsFor(SkyStyle s) {
        switch (s) {
            case AURORA:
            case OCEAN_DEPTH:
            case SILK:
            case BANDS:
                return true;
            default:
                return false;
        }
    }

    private void prepare(WorldClient world, Minecraft mc, float pt, SoupAPI_Config c) {
        t = FxClock.seconds() * (c.skySpeed / 100f) * style.speed();
        intensity = c.skyIntensity / 100f;

        Color m, a, d;
        if (c.coloredSkyUseTheme) {
            m = Palette.getColor(0f);
            a = Palette.getColor(0.5f);
        } else {
            m = new Color(c.coloredSkyColor);
            a = new Color(c.skyColor2);
        }
        d = new Color(c.skyDarkColor);
        set(main, m);
        set(alt, a);
        set(dark, d);

        FOG.clear();
        GL11.glGetFloat(GL11.GL_FOG_COLOR, FOG);
        fog[0] = FOG.get(0);
        fog[1] = FOG.get(1);
        fog[2] = FOG.get(2);

        Vec3 sky = world.getSkyColor(mc.getRenderViewEntity(), pt);
        vanilla[0] = (float) sky.xCoord;
        vanilla[1] = (float) sky.yCoord;
        vanilla[2] = (float) sky.zCoord;
    }

    private static void set(float[] dst, Color c) {
        dst[0] = c.getRed() / 255f;
        dst[1] = c.getGreen() / 255f;
        dst[2] = c.getBlue() / 255f;
    }

    private static void beginState() {
        GlStateManager.disableFog();
        GlStateManager.disableAlpha();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.depthMask(false);
        GlStateManager.enableBlend();
        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        GlStateManager.disableTexture2D();
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    /** Leaves the state the way vanilla's own sky does. */
    private static void endState() {
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
        GlStateManager.enableCull();
        GlStateManager.enableFog();
        GlStateManager.depthMask(true);
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    // ---- the dome --------------------------------------------------------------------------------

    private void computeColours(boolean tintOnly, SoupAPI_Config c) {
        float opacity = tintOnly ? c.coloredSkyAlpha / 100f : c.skyOpacity / 100f;
        float horizon = c.skyHorizonBlend / 100f;
        for (int k = 0; k < V; k++) {
            if (tintOnly) {
                // The old flat tint, now under the world instead of over it.
                out[0] = main[0];
                out[1] = main[1];
                out[2] = main[2];
            } else {
                colour(k);
            }
            // Mix with the vanilla sky by opacity, then into the fog at the horizon.
            float r = vanilla[0] + (out[0] - vanilla[0]) * opacity;
            float g = vanilla[1] + (out[1] - vanilla[1]) * opacity;
            float b = vanilla[2] + (out[2] - vanilla[2]) * opacity;
            float h = DY[k];
            float toFog = horizon * (1f - smooth(-0.08f, 0.32f, h));
            CR[k] = clamp(r + (fog[0] - r) * toFog);
            CG[k] = clamp(g + (fog[1] - g) * toFog);
            CB[k] = clamp(b + (fog[2] - b) * toFog);
        }
    }

    private static void drawDome(Tessellator tessellator, WorldRenderer buffer) {
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        for (int j = 0; j < EL; j++) {
            for (int i = 0; i < AZ; i++) {
                int a = j * (AZ + 1) + i, b = a + 1, c = a + AZ + 1, d = c + 1;
                vertex(buffer, a);
                vertex(buffer, b);
                vertex(buffer, d);
                vertex(buffer, c);
            }
        }
        tessellator.draw();
    }

    private static void vertex(WorldRenderer buffer, int k) {
        buffer.pos(DX[k] * R, DY[k] * R, DZ[k] * R).color(CR[k], CG[k], CB[k], 1f).endVertex();
    }

    // ---- style colour functions ------------------------------------------------------------------

    /** Writes the style's colour for dome vertex {@code k} into {@link #out}. */
    private void colour(int k) {
        float x = DX[k], y = DY[k], z = DZ[k];
        float az = AZI[k], el = ELE[k];
        float up = Math.max(0f, y);
        float I = intensity;
        switch (style) {
            case AURORA: {
                gradient(dark, mix3(dark, alt, 0.25f), up);
                float centre = 0.55f + 0.18f * MathHelper.sin(az * 3f + t * 0.25f);
                float band = gauss(el - centre, 0.22f);
                float folds = 0.55f + 0.45f * MathHelper.sin(az * 11f + t * 1.1f + MathHelper.sin(az * 3f - t * 0.4f) * 2.5f);
                add(main, band * folds * 0.9f * I);
                add(alt, gauss(el - centre - 0.25f, 0.18f) * folds * 0.5f * I);
                break;
            }
            case NEBULA: {
                gradient(dark, mix3(dark, main, 0.15f), up);
                float n = fbm(x * 2f + t * 0.05f, y * 2f, z * 2f - t * 0.04f);
                float n2 = fbm(x * 3.3f - t * 0.03f, y * 3.1f + 7f, z * 3.2f);
                add(main, smooth(0.45f, 0.85f, n) * 0.75f * I);
                add(alt, smooth(0.5f, 0.9f, n2) * 0.6f * I);
                break;
            }
            case BANDS: {
                float s = MathHelper.sin(el * 9f - t * 0.6f) * 0.5f + 0.5f;
                float s2 = MathHelper.sin(el * 15f + t * 0.9f + az) * 0.5f + 0.5f;
                lerp(dark, main, s * 0.7f * I);
                add(alt, s2 * s * 0.35f * I);
                break;
            }
            case PLASMA: {
                float v = MathHelper.sin(x * 4f + t) + MathHelper.sin(y * 5f - t * 0.8f)
                        + MathHelper.sin(z * 3f + t * 1.3f) + MathHelper.sin((x + z) * 2.5f - t * 0.6f);
                v = v * 0.125f + 0.5f;
                lerp(main, alt, v);
                scale(0.25f + 0.75f * I * (0.6f + 0.4f * MathHelper.sin(v * 12f + t)));
                break;
            }
            case VOID_RIFT:
            case DIMENSIONAL_RIFT: {
                gradient(mix3(dark, main, 0.05f), dark, up);
                float n = fbm(x * 2.2f + t * 0.03f, y * 2.2f, z * 2.2f);
                add(alt, smooth(0.55f, 0.95f, n) * 0.3f * I);
                // Distance from the rift's great circles, breathing.
                int rifts = style == SkyStyle.VOID_RIFT ? 1 : 3;
                for (int r = 0; r < rifts; r++) {
                    float ax = RAND[r * 3] - 0.5f, ay = 0.6f + RAND[r * 3 + 1] * 0.4f, azz = RAND[r * 3 + 2] - 0.5f;
                    float len = (float) Math.sqrt(ax * ax + ay * ay + azz * azz);
                    float d = Math.abs((x * ax + y * ay + z * azz) / len);
                    float jag = 0.02f * MathHelper.sin(az * 23f + r * 4f) + 0.015f * MathHelper.sin(az * 47f - r);
                    float breathe = 0.75f + 0.25f * MathHelper.sin(t * 1.2f + r * 2f);
                    float glow = gauss(d + jag, 0.05f + 0.03f * breathe) * (1f - smooth(0.2f, 0.6f, Math.abs(y - 0.55f))) ;
                    add(main, glow * 1.4f * breathe * I);
                    add(1f, 1f, 1f, gauss(d + jag, 0.012f) * 0.8f * breathe * I);
                }
                break;
            }
            case GRID: {
                gradient(mix3(dark, main, 0.35f), dark, up);
                float lines = Math.max(line(az * 16f / (float) Math.PI, 0.06f),
                        line((1f - up) * 10f + t * 0.5f, 0.06f));
                add(main, lines * (1f - up) * I);
                add(alt, gauss(y, 0.08f) * 0.8f * I);
                break;
            }
            case HEX_GRID: {
                gradient(dark, mix3(dark, alt, 0.3f), up);
                add(main, gauss(1.57f - el, 0.5f) * 0.25f * I);
                break;
            }
            case SCANLINES: {
                gradient(dark, mix3(dark, main, 0.3f), up);
                float l = line(el * 14f - t * 0.8f, 0.12f);
                add(main, l * 0.8f * I);
                add(alt, line(el * 3f - t * 0.2f, 0.05f) * 0.4f * I);
                break;
            }
            case VORTEX: {
                float spiral = 0.5f + 0.5f * MathHelper.sin(az * 5f + (1.6f - el) * 9f - t * 2f);
                lerp(dark, main, spiral * up * I);
                add(alt, gauss(1.57f - el, 0.25f) * 0.9f * I);
                break;
            }
            case ECLIPSE: {
                gradient(mix3(dark, alt, 0.2f), dark, up);
                // The corona itself is geometry; the sky only darkens toward it.
                float sd = dot(x, y, z, ECX, ECY, ECZ);
                add(main, gauss(1f - sd, 0.08f) * 0.6f * I);
                break;
            }
            case CODE_FALL: {
                gradient(dark, mix3(dark, main, 0.12f), up);
                float col = MathHelper.sin(az * 24f) * 0.5f + 0.5f;
                add(main, col * line(el * 3f + t * 0.7f + RAND[(int) (az * 12f) & 255] * 7f, 0.25f) * 0.25f * I);
                break;
            }
            case EMBER_SKY: {
                gradient(mix3(main, alt, 0.5f), dark, smooth(-0.1f, 0.5f, y));
                float heat = fbm(x * 3f, y * 4f - t * 0.6f, z * 3f) * (1f - smooth(0f, 0.6f, up));
                add(main, heat * 0.6f * I);
                break;
            }
            case OCEAN_DEPTH: {
                gradient(mix3(dark, main, 0.3f), dark, 1f - up);
                float rays = 0.5f + 0.5f * MathHelper.sin(az * 9f + MathHelper.sin(t * 0.4f + az * 2f) * 1.5f);
                add(alt, rays * smooth(0.3f, 1f, up) * 0.45f * I);
                float current = fbm(x * 2.5f + t * 0.12f, y * 1.5f, z * 2.5f - t * 0.1f);
                add(main, line(current * 6f, 0.15f) * 0.25f * I);
                break;
            }
            case LAVA_FLOW: {
                float n = fbm(x * 2.6f + t * 0.04f, y * 2.6f, z * 2.6f - t * 0.05f);
                float cells = smooth(0.48f, 0.52f, n);
                lerp(dark, main, cells * I);
                add(alt, gauss(n - 0.5f, 0.02f) * 0.8f * I);
                break;
            }
            case SUNSET: {
                float toward = 0.5f + 0.5f * x;
                gradient(new float[]{0.95f, 0.45f, 0.2f}, new float[]{0.18f, 0.12f, 0.35f}, smooth(-0.05f, 0.7f, y));
                add(1f, 0.6f, 0.25f, toward * (1f - up) * 0.5f * I);
                float clouds = fbm(x * 3f + t * 0.05f, y * 6f, z * 3f);
                add(1f, 0.5f, 0.6f, smooth(0.55f, 0.8f, clouds) * (1f - up) * 0.4f * I);
                break;
            }
            case RIPPLE: {
                gradient(dark, mix3(dark, alt, 0.3f), up);
                add(main, line((1f - up) * 12f - t * 1.5f, 0.12f) * I);
                break;
            }
            case SPEED_LINES: {
                gradient(dark, mix3(dark, main, 0.15f), up);
                add(alt, gauss(y, 0.12f) * 0.35f * I);
                break;
            }
            case LATTICE: {
                gradient(dark, mix3(dark, alt, 0.25f), up);
                float d1 = MathHelper.sin(az * 8f + el * 8f + t), d2 = MathHelper.sin(az * 8f - el * 8f - t * 0.7f);
                add(main, smooth(0.85f, 1f, Math.max(d1, d2)) * I);
                break;
            }
            case SILK: {
                float wave = 0.5f + 0.5f * MathHelper.sin(az * 2f + el * 3f + MathHelper.sin(t * 0.5f) * 2f + t * 0.3f);
                lerp(main, alt, wave);
                scale(0.25f + 0.55f * I);
                break;
            }
            case STORM_FRONT: {
                float n = fbm(x * 2.4f + t * 0.15f, y * 2.4f, z * 2.4f + t * 0.1f);
                float g = 0.12f + 0.25f * n;
                out[0] = g;
                out[1] = g * 1.02f;
                out[2] = g * 1.12f;
                float flash = flash(t);
                add(0.8f, 0.85f, 1f, flash * (0.4f + 0.6f * n) * I);
                break;
            }
            case CIRCUIT_SKY: {
                gradient(dark, mix3(dark, main, 0.15f), up);
                add(alt, gauss(y - 0.1f, 0.2f) * 0.2f * I);
                break;
            }
            case INFERNAL: {
                float flame = fbm(x * 3f, y * 3.5f - t * 0.9f, z * 3f) * (1f - smooth(-0.1f, 0.75f, y));
                gradient(new float[]{0.95f, 0.32f, 0.04f}, new float[]{0.05f, 0.01f, 0.01f}, smooth(-0.15f, 0.65f, y));
                add(1f, 0.55f, 0.1f, flame * 0.9f * I);
                float smoke = fbm(x * 2f + t * 0.1f, y * 2f + 3f, z * 2f);
                float s = smooth(0.5f, 0.75f, smoke) * smooth(0.1f, 0.6f, up);
                out[0] *= 1f - s * 0.85f;
                out[1] *= 1f - s * 0.85f;
                out[2] *= 1f - s * 0.85f;
                break;
            }
            case CYBERPUNK: {
                gradient(mix3(main, alt, 0.5f), dark, smooth(-0.1f, 0.45f, y));
                float grid = Math.max(line(az * 12f / (float) Math.PI, 0.05f), line(-y * 18f + t * 0.8f, 0.07f));
                add(alt, grid * (1f - smooth(-0.3f, 0.05f, y)) * I);
                add(main, gauss(y - 0.02f, 0.06f) * I);
                break;
            }
            case ARCANE: {
                gradient(dark, mix3(dark, main, 0.25f), up);
                float n = fbm(x * 2f, y * 2f + t * 0.03f, z * 2f);
                add(alt, smooth(0.6f, 0.9f, n) * 0.35f * I);
                add(main, gauss(1.57f - el, 0.35f) * 0.25f * (0.8f + 0.2f * MathHelper.sin(t)) * I);
                break;
            }
            case SHATTERED_GLASS: {
                gradient(dark, mix3(dark, alt, 0.3f), up);
                float n = fbm(x * 2.5f + t * 0.02f, y * 2.5f, z * 2.5f);
                add(main, smooth(0.5f, 0.9f, n) * 0.4f * I);
                break;
            }
            case VOID_ENERGY: {
                out[0] = dark[0] * 0.6f;
                out[1] = dark[1] * 0.6f;
                out[2] = dark[2] * 0.6f;
                float haze = fbm(x * 2f + t * 0.03f, y * 2f, z * 2f - t * 0.02f);
                add(alt, smooth(0.55f, 0.9f, haze) * 0.18f * I);
                break;
            }
            case BLEEDING_CLOUDS: {
                // Clouds pulled outward from a burning point straight overhead.
                float rad = 1.57f - el;
                float n = fbm(az * 1.4f, rad * 3f - t * 0.25f, 3.1f) * 0.6f + fbm(x * 3f, y * 3f, z * 3f + t * 0.05f) * 0.4f;
                float shade = 0.15f + 0.85f * smooth(0.35f, 0.75f, n);
                out[0] = 0.55f * shade + 0.05f;
                out[1] = 0.02f + 0.04f * shade;
                out[2] = 0.02f + 0.03f * shade;
                float core = gauss(rad, 0.22f);
                add(1f, 0.25f, 0.2f, core * 1.2f * I);
                add(1f, 0.85f, 0.8f, gauss(rad, 0.06f) * I);
                out[0] *= 0.4f + 0.6f * smooth(-0.1f, 0.5f, y) + 0.2f;
                break;
            }
            case SUSPENDED: {
                float n = fbm(x * 2.2f + t * 0.06f, y * 3f, z * 2.2f);
                float g = 0.22f + 0.35f * n * smooth(-0.2f, 0.6f, y + 0.2f);
                out[0] = g;
                out[1] = g;
                out[2] = g * 1.04f;
                break;
            }
            case DREAMCORE: {
                // Pink at the horizon to a soft blue overhead, with big soft clouds in front.
                float h = (float) Math.pow(up, 0.7);
                out[0] = 1.0f + (0.62f - 1.0f) * h;
                out[1] = 0.80f + (0.78f - 0.80f) * h;
                out[2] = 0.90f + (1.0f - 0.90f) * h;
                float n = fbm(x * 1.8f + t * 0.03f, y * 2.6f, z * 1.8f - t * 0.02f);
                float puff = smooth(0.45f, 0.75f, n) * smooth(-0.05f, 0.25f, y);
                lerpTo(1f, 0.95f, 0.98f, puff * 0.6f);
                if (y < 0f) lerpTo(0.86f, 0.78f, 0.95f, smooth(0f, -0.3f, y));
                scale(0.55f + 0.45f * I);
                break;
            }
            case DREAM: {
                // Violet overhead to pink at the horizon, billowing clouds shaded from below.
                float h = smooth(-0.05f, 0.9f, y);
                out[0] = 0.96f + (0.36f - 0.96f) * h;
                out[1] = 0.58f + (0.20f - 0.58f) * h;
                out[2] = 0.78f + (0.55f - 0.78f) * h;
                float n = fbm(x * 2.3f + t * 0.04f, y * 2.8f, z * 2.3f);
                float n2 = fbm(x * 4.1f - t * 0.02f, y * 4.4f + 3f, z * 4.1f);
                float billow = smooth(0.4f, 0.8f, n);
                lerpTo(1f, 0.78f, 0.9f, billow * 0.55f);
                scale(1f - 0.25f * smooth(0.5f, 0.85f, n2));
                scale(0.6f + 0.4f * I);
                break;
            }
            case HEAVEN_GATE: {
                // Pale blue at the horizon to white-gold overhead, with a glow pooled at the zenith.
                // Deep blue at the horizon to burning gold overhead, the gate's light pooled there.
                float h = (float) Math.pow(up, 0.7);
                out[0] = 0.18f + 0.78f * h;
                out[1] = 0.32f + 0.48f * h;
                out[2] = 0.66f - 0.30f * h;
                add(1f, 0.85f, 0.55f, gauss(1.57f - el, 0.38f) * 0.5f * I);
                float n = fbm(x * 1.6f + t * 0.02f, y * 2.2f, z * 1.6f);
                lerpTo(1f, 0.98f, 0.94f, smooth(0.5f, 0.8f, n) * smooth(0.05f, 0.4f, y) * 0.45f);
                break;
            }
            case BLOOD_SUN: {
                // Crimson overhead, burning orange low down, the haze thickest toward the sun.
                float h = (float) Math.pow(up, 0.55);
                out[0] = 0.95f - 0.55f * h;
                out[1] = 0.32f - 0.28f * h;
                out[2] = 0.10f - 0.06f * h;
                float toward = Math.max(0f, dot(x, y, z, BSX, BSY, BSZ));
                add(1f, 0.45f, 0.12f, (float) Math.pow(toward, 6) * 0.9f * I);
                float n = fbm(x * 2.4f + t * 0.05f, y * 5f, z * 2.4f);
                add(0.4f, 0.05f, 0.02f, smooth(0.5f, 0.85f, n) * smooth(0.25f, -0.05f, y) * 0.5f * I);
                break;
            }
            case HERESY_CROWN: {
                // Near black, a red glow welling up from the horizon, and the crown's light above.
                out[0] = 0.04f + 0.40f * smooth(0.35f, -0.05f, y);
                out[1] = 0.01f + 0.06f * smooth(0.35f, -0.05f, y);
                out[2] = 0.02f;
                add(1f, 0.35f, 0.08f, gauss(1.57f - el - 0.32f, 0.18f) * 0.35f * I);
                float n = fbm(x * 2.2f + t * 0.04f, y * 2.2f - t * 0.06f, z * 2.2f);
                add(0.35f, 0.06f, 0.02f, smooth(0.55f, 0.9f, n) * 0.4f * I);
                break;
            }
            case CYBER_PULSE: {
                out[0] = 0.0f;
                out[1] = 0.012f;
                out[2] = 0.008f;
                add(0.1f, 1f, 0.35f, gauss(y, 0.05f) * 0.55f * I);
                add(0.05f, 0.4f, 0.15f, gauss(y, 0.22f) * 0.18f * I);
                break;
            }
            case TEMPEST: {
                // Slate churning in long bands, lit from inside now and then.
                float n = fbm(x * 1.8f + t * 0.12f, y * 4.5f, z * 1.8f - t * 0.08f);
                float n2 = fbm(x * 3.6f - t * 0.2f, y * 7f + 3f, z * 3.6f);
                float g = 0.10f + 0.20f * smooth(0.3f, 0.8f, n) + 0.06f * n2;
                out[0] = g * 0.92f;
                out[1] = g;
                out[2] = g * 1.12f;
                float flash = (float) Math.pow(Math.max(0f, MathHelper.sin(t * 0.9f) * MathHelper.sin(t * 2.3f + 1f)), 18);
                add(0.75f, 0.82f, 1f, flash * smooth(0.4f, 0.9f, n) * 1.2f * I);
                break;
            }
            case BLACK_HOLE: {
                gradient(new float[]{0.02f, 0.012f, 0.03f}, new float[]{0.005f, 0.004f, 0.012f}, up);
                float toward = Math.max(0f, dot(x, y, z, BHX, BHY, BHZ));
                // The glow of the disc spilling into the sky round it.
                add(1f, 0.55f, 0.2f, (float) Math.pow(toward, 14) * 0.45f * I);
                float n = fbm(x * 2f + t * 0.01f, y * 2f, z * 2f);
                add(0.25f, 0.12f, 0.35f, smooth(0.55f, 0.9f, n) * 0.25f * I);
                break;
            }
            case GALAXY: {
                gradient(dark, mix3(dark, main, 0.08f), up);
                float n = fbm(x * 2.5f + t * 0.01f, y * 2.5f, z * 2.5f);
                add(alt, smooth(0.55f, 0.95f, n) * 0.22f * I);
                add(main, gauss(1.57f - el, 0.5f) * 0.18f * I);
                break;
            }
            case SHATTERED_MOON: {
                float h = (float) Math.pow(up, 0.7);
                out[0] = 0.07f - 0.04f * h;
                out[1] = 0.09f - 0.05f * h;
                out[2] = 0.20f - 0.10f * h;
                float toward = Math.max(0f, dot(x, y, z, SMX, SMY, SMZ));
                add(0.75f, 0.8f, 0.95f, (float) Math.pow(toward, 10) * 0.35f * I);
                break;
            }
            case NEON_HALO: {
                float h = (float) Math.pow(up, 0.6);
                gradient(mix3(main, dark, 0.55f), mix3(dark, alt, 0.12f), h);
                add(main, gauss(y, 0.08f) * 0.9f * I);
                add(alt, gauss(y - 0.12f, 0.1f) * 0.35f * I);
                break;
            }
            case GOLDEN_HAZE: {
                float h = (float) Math.pow(up, 0.5);
                out[0] = 1.0f - 0.25f * h;
                out[1] = 0.72f - 0.22f * h;
                out[2] = 0.36f - 0.12f * h;
                float toward = Math.max(0f, dot(x, y, z, GSX, GSY, GSZ));
                add(1f, 0.95f, 0.75f, (float) Math.pow(toward, 8) * 0.6f * I);
                float n = fbm(x * 3f + t * 0.15f, y * 6f, z * 3f - t * 0.1f);
                add(0.35f, 0.22f, 0.08f, smooth(0.5f, 0.85f, n) * smooth(0.3f, 0f, y) * 0.5f * I);
                scale(0.75f + 0.25f * I);
                break;
            }
            default:
                out[0] = main[0];
                out[1] = main[1];
                out[2] = main[2];
        }
    }

    private void lerpTo(float r, float g, float b, float f) {
        f = clamp(f);
        out[0] += (r - out[0]) * f;
        out[1] += (g - out[1]) * f;
        out[2] += (b - out[2]) * f;
    }

    /** The eclipse sits high in the south-east, fixed. */
    private static final float ECX = 0.45f, ECY = 0.62f, ECZ = -0.64f;
    /** Where the blood sun sits: low, ahead. */
    private static final float BSX = 0.20f, BSY = 0.16f, BSZ = -0.97f;
    /** The black hole: a third of the way up, off to one side. */
    private static final float BHX = -0.55f, BHY = 0.42f, BHZ = -0.72f;
    /** The shattered moon. */
    private static final float SMX = 0.52f, SMY = 0.55f, SMZ = -0.65f;
    /** The golden haze's sun. */
    private static final float GSX = -0.30f, GSY = 0.26f, GSZ = -0.92f;

    private void gradient(float[] low, float[] high, float f) {
        f = clamp(f);
        out[0] = low[0] + (high[0] - low[0]) * f;
        out[1] = low[1] + (high[1] - low[1]) * f;
        out[2] = low[2] + (high[2] - low[2]) * f;
    }

    private void lerp(float[] a, float[] b, float f) {
        gradient(a, b, f);
    }

    private static float[] mix3(float[] a, float[] b, float f) {
        return new float[]{a[0] + (b[0] - a[0]) * f, a[1] + (b[1] - a[1]) * f, a[2] + (b[2] - a[2]) * f};
    }

    private void add(float[] c, float amount) {
        add(c[0], c[1], c[2], amount);
    }

    private void add(float r, float g, float b, float amount) {
        if (amount <= 0f) return;
        out[0] += r * amount;
        out[1] += g * amount;
        out[2] += b * amount;
    }

    private void scale(float k) {
        out[0] *= k;
        out[1] *= k;
        out[2] *= k;
    }

    // ---- detail, stars and style geometry --------------------------------------------------------

    /** A tileable texture laid over the dome, scrolling, so detail exists between the vertices. */
    private void detailLayer(Minecraft mc, Tessellator tessellator, WorldRenderer buffer, SoupAPI_Config c) {
        ResourceLocation tex;
        float strength;
        float su = 0f, sv = 0f;
        float[] tint = main;
        switch (style) {
            case NEBULA: case DIMENSIONAL_RIFT: case ARCANE: case VOID_RIFT:
                tex = CLOUD; strength = 0.35f; su = t * 0.004f; break;
            case STORM_FRONT: case SUSPENDED:
                tex = CLOUD; strength = 0.25f; su = t * 0.02f; tint = new float[]{0.7f, 0.72f, 0.78f}; break;
            case BLEEDING_CLOUDS:
                tex = CLOUD; strength = 0.4f; sv = -t * 0.02f; tint = new float[]{1f, 0.2f, 0.15f}; break;
            case DREAMCORE:
                tex = CLOUD; strength = 0.3f; su = t * 0.006f; tint = new float[]{1f, 0.95f, 1f}; break;
            case DREAM:
                tex = CLOUD; strength = 0.3f; su = t * 0.008f; tint = new float[]{1f, 0.75f, 0.9f}; break;
            case INFERNAL: case EMBER_SKY: case LAVA_FLOW:
                tex = CLOUD; strength = 0.3f; sv = t * 0.05f; tint = new float[]{1f, 0.45f, 0.1f}; break;
            case SILK:
                tex = CLOUD; strength = 0.2f; su = t * 0.01f; tint = alt; break;
            case AURORA: case OCEAN_DEPTH:
                tex = STREAK; strength = 0.22f; su = t * 0.01f; tint = alt; break;
            case CODE_FALL: case SPEED_LINES: case SCANLINES: case CYBERPUNK:
                tex = STREAK; strength = 0.18f; sv = t * 0.15f; break;
            case VOID_ENERGY: case SHATTERED_GLASS:
                tex = GRAIN; strength = 0.12f; su = t * 0.01f; break;
            default:
                tex = GRAIN; strength = 0.1f; su = t * 0.01f; break;
        }
        strength *= c.skyIntensity / 100f * (c.skyOpacity / 100f);
        if (strength <= 0.01f) return;
        float scale = Math.max(0.25f, c.skyScale / 100f);
        GlStateManager.enableTexture2D();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        mc.getTextureManager().bindTexture(tex);
        repeat();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        for (int j = 0; j < EL; j++) {
            for (int i = 0; i < AZ; i++) {
                int a = j * (AZ + 1) + i, b = a + 1, cc = a + AZ + 1, d = cc + 1;
                tv(buffer, a, i, j, su, sv, scale, tint, strength);
                tv(buffer, b, i + 1, j, su, sv, scale, tint, strength);
                tv(buffer, d, i + 1, j + 1, su, sv, scale, tint, strength);
                tv(buffer, cc, i, j + 1, su, sv, scale, tint, strength);
            }
        }
        tessellator.draw();
        GlStateManager.disableTexture2D();
    }

    private static void tv(WorldRenderer buffer, int k, int i, int j, float su, float sv, float scale,
                           float[] tint, float strength) {
        float u = (float) i / AZ * 4f / scale + su;
        float v = (float) j / EL * 2f / scale + sv;
        // Fades out toward the zenith, where the azimuthal mapping pinches.
        float a = strength * (1f - smooth(0.55f, 0.9f, DY[k])) * (0.4f + 0.6f * smooth(-0.2f, 0.2f, DY[k]));
        buffer.pos(DX[k] * R * 0.99f, DY[k] * R * 0.99f, DZ[k] * R * 0.99f).tex(u, v)
                .color(tint[0], tint[1], tint[2], a).endVertex();
    }

    private static void repeat() {
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
    }

    private void stars(Minecraft mc, Tessellator tessellator, WorldRenderer buffer, SoupAPI_Config c,
                       WorldClient world, float pt) {
        int count = Math.min(STARS, c.skyStars * 3 / 2);
        if (count <= 0) return;
        GlStateManager.enableTexture2D();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        mc.getTextureManager().bindTexture(SmokeFx.FX_ATLAS);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        float day = 1f - world.getStarBrightness(pt) * 2f; // brighter at night
        float vis = 0.55f + 0.45f * clamp(1f - day);
        for (int s = 0; s < count; s++) {
            float tw = 0.55f + 0.45f * MathHelper.sin(t * (1.5f + SP[s] * 0.05f) + SP[s]);
            float a = SB[s] * tw * vis * smooth(-0.05f, 0.15f, SY[s]);
            float size = 0.05f + 0.07f * SB[s];
            boolean warm = (s & 3) == 0;
            float[] tint = warm ? alt : (s % 5 == 0 ? main : WHITE);
            billboard(buffer, SX[s], SY[s], SZ[s], R * 0.97f, size, 0f, 0f, 0f, 0.25f, 0.25f,
                    tint[0], tint[1], tint[2], a);
        }
        tessellator.draw();
        GlStateManager.disableTexture2D();
    }

    private static final float[] WHITE = {1f, 1f, 1f};

    /** The geometry a style owns on top of its colours. */
    private void extras(Minecraft mc, Tessellator tessellator, WorldRenderer buffer, SoupAPI_Config c) {
        float I = intensity;
        switch (style) {
            case AURORA:
                auroraCurtains(tessellator, buffer, I);
                break;
            case ECLIPSE:
                eclipse(mc, tessellator, buffer, I);
                break;
            case VOID_RIFT:
            case DIMENSIONAL_RIFT:
                riftCracks(mc, tessellator, buffer, I, style == SkyStyle.VOID_RIFT ? 1 : 3);
                if (style == SkyStyle.DIMENSIONAL_RIFT) glassShards(mc, tessellator, buffer, I, 26, true);
                break;
            case CODE_FALL:
                glyphRain(mc, tessellator, buffer, I, 34, main);
                glitchTears(tessellator, buffer, I * 0.6f);
                break;
            case CYBERPUNK:
                stripedSun(tessellator, buffer, I);
                glyphRain(mc, tessellator, buffer, I * 0.7f, 22, alt);
                break;
            case ARCANE:
                runeRings(mc, tessellator, buffer, I);
                constellations(tessellator, buffer, I);
                break;
            case SHATTERED_GLASS:
                // Fragments first: the web and the point of impact belong in front of them.
                glassShards(mc, tessellator, buffer, I, 22, false);
                shatteredWeb(mc, tessellator, buffer, I);
                break;
            case STORM_FRONT:
                lightning(tessellator, buffer, I);
                break;
            case EMBER_SKY:
            case INFERNAL:
                risingEmbers(mc, tessellator, buffer, I);
                break;
            case BLEEDING_CLOUDS:
                bleedingCore(mc, tessellator, buffer, I);
                break;
            case SUSPENDED:
                figures(tessellator, buffer, I);
                break;
            case DREAMCORE:
                dreamcore(mc, tessellator, buffer, I);
                break;
            case DREAM:
                dreamFace(mc, tessellator, buffer, I);
                break;
            case VOID_ENERGY:
                veins(mc, tessellator, buffer, I);
                voidPulses(mc, tessellator, buffer, I);
                break;
            case HEX_GRID:
                hexLattice(tessellator, buffer, I);
                break;
            case GRID:
                // A grid on its own is a grid. What makes this sky is what is sitting in it.
                stripedSun(tessellator, buffer, I);
                speedStreaks(tessellator, buffer, I * 0.55f);
                break;
            case BANDS:
            case SILK:
            case SUNSET:
                cloudBanks(tessellator, buffer, I, style == SkyStyle.SILK ? 5 : 4);
                break;
            case NEBULA:
                cloudBanks(tessellator, buffer, I * 0.7f, 3);
                break;
            case OCEAN_DEPTH:
                lightShafts(tessellator, buffer, I);
                break;
            case VORTEX:
                spiralArms(tessellator, buffer, I);
                break;
            case RIPPLE:
                expandingRings(tessellator, buffer, I);
                break;
            case SCANLINES:
                glitchTears(tessellator, buffer, I);
                break;
            case LATTICE:
                glassShards(mc, tessellator, buffer, I, 30, true);
                break;
            case PLASMA:
                voidPulses(mc, tessellator, buffer, I * 0.8f);
                break;
            case LAVA_FLOW:
                risingEmbers(mc, tessellator, buffer, I * 0.8f);
                break;
            case SPEED_LINES:
                speedStreaks(tessellator, buffer, I);
                break;
            case CIRCUIT_SKY:
                circuitTraces(mc, tessellator, buffer, I);
                break;
            case HEAVEN_GATE:
                heavenGate(mc, tessellator, buffer, I);
                break;
            case BLOOD_SUN:
                bloodSun(mc, tessellator, buffer, I);
                break;
            case HERESY_CROWN:
                fireCrown(mc, tessellator, buffer, I);
                risingEmbers(mc, tessellator, buffer, I * 0.8f);
                break;
            case CYBER_PULSE:
                cyberPulse(tessellator, buffer, I);
                break;
            case TEMPEST:
                cloudBanks(tessellator, buffer, I * 0.45f, 4);
                lightning(tessellator, buffer, I);
                rain(tessellator, buffer, I);
                break;
            case BLACK_HOLE:
                blackHole(mc, tessellator, buffer, I);
                break;
            case GALAXY:
                galaxy(mc, tessellator, buffer, I);
                break;
            case SHATTERED_MOON:
                shatteredMoon(mc, tessellator, buffer, I);
                break;
            case NEON_HALO:
                neonHalos(tessellator, buffer, I);
                break;
            case GOLDEN_HAZE:
                goldenSun(mc, tessellator, buffer, I);
                speedStreaks(tessellator, buffer, I * 0.6f);
                break;
            default:
                break;
        }
    }

    private void auroraCurtains(Tessellator tessellator, WorldRenderer buffer, float I) {
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        for (int curtain = 0; curtain < 3; curtain++) {
            float base = curtain * 2.1f + 0.4f;
            float elev = 0.35f + curtain * 0.12f;
            float[] col = curtain == 1 ? alt : main;
            int steps = 60;
            for (int s = 0; s < steps; s++) {
                float a0 = base + s * 0.045f, a1 = a0 + 0.045f;
                float w0 = 0.12f * MathHelper.sin(a0 * 5f + t * 0.7f + curtain) + 0.08f * MathHelper.sin(a0 * 13f - t * 1.3f);
                float w1 = 0.12f * MathHelper.sin(a1 * 5f + t * 0.7f + curtain) + 0.08f * MathHelper.sin(a1 * 13f - t * 1.3f);
                float lit0 = (0.5f + 0.5f * MathHelper.sin(a0 * 17f + t * 2f)) * fadeEnds(s, steps);
                float lit1 = (0.5f + 0.5f * MathHelper.sin(a1 * 17f + t * 2f)) * fadeEnds(s + 1, steps);
                float lo = elev + w0, hi = lo + 0.35f;
                float lo1 = elev + w1, hi1 = lo1 + 0.35f;
                curtainVertex(buffer, a0, lo, col, 0.55f * lit0 * I);
                curtainVertex(buffer, a1, lo1, col, 0.55f * lit1 * I);
                curtainVertex(buffer, a1, hi1, col, 0f);
                curtainVertex(buffer, a0, hi, col, 0f);
            }
        }
        tessellator.draw();
    }

    private static float fadeEnds(int s, int steps) {
        float f = s / (float) steps;
        return smooth(0f, 0.15f, f) * (1f - smooth(0.85f, 1f, f));
    }

    private static void curtainVertex(WorldRenderer buffer, float az, float el, float[] col, float a) {
        float c = MathHelper.cos(el);
        buffer.pos(MathHelper.cos(az) * c * R * 0.96f, MathHelper.sin(el) * R * 0.96f, MathHelper.sin(az) * c * R * 0.96f)
                .color(col[0], col[1], col[2], clamp(a)).endVertex();
    }

    private void eclipse(Minecraft mc, Tessellator tessellator, WorldRenderer buffer, float I) {
        GlStateManager.enableTexture2D();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        mc.getTextureManager().bindTexture(SmokeFx.GLOW);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        float breathe = 0.85f + 0.15f * MathHelper.sin(t * 0.8f);
        billboard(buffer, ECX, ECY, ECZ, R * 0.95f, 14f * breathe, 0f, 0f, 0f, 1f, 1f, main[0], main[1], main[2], 0.7f * I);
        billboard(buffer, ECX, ECY, ECZ, R * 0.95f, 7f, 0f, 0f, 0f, 1f, 1f, alt[0], alt[1], alt[2], 0.6f * I);
        billboard(buffer, ECX, ECY, ECZ, R * 0.95f, 5.2f, t * 3f, 0f, 0f, 1f, 1f, 1f, 0.95f, 0.85f, 1f * I);
        tessellator.draw();
        // Streamers: long thin rays turning slowly.
        mc.getTextureManager().bindTexture(SmokeFx.FX_ATLAS);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        for (int i = 0; i < 14; i++) {
            float rot = i * 360f / 14f + t * 4f + RAND[i] * 20f;
            float len = 9f + RAND[i + 20] * 6f + MathHelper.sin(t + i) * 1.2f;
            billboardStretched(buffer, ECX, ECY, ECZ, R * 0.95f, len, 0.9f, rot, 0.5f, 0f, 0.75f, 0.25f,
                    alt[0], alt[1], alt[2], 0.7f * I);
        }
        // The diamond-ring flare, once every so often.
        float ring = smooth(0.92f, 1f, 0.5f + 0.5f * MathHelper.sin(t * 0.35f));
        if (ring > 0.01f) {
            float ang = (float) Math.toRadians(40);
            billboard(buffer, ECX + 0.02f * MathHelper.cos(ang), ECY + 0.02f * MathHelper.sin(ang), ECZ, R * 0.94f,
                    2.4f, t * 30f, 0.25f, 0f, 0.5f, 0.25f, 1f, 1f, 1f, ring * I);
        }
        tessellator.draw();
        GlStateManager.disableTexture2D();
        // The dark disc itself, drawn normally so it covers the glow behind it.
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        disc(tessellator, buffer, ECX, ECY, ECZ, R * 0.93f, 1.9f, 0.01f, 0.005f, 0.015f, 1f);
    }

    private void riftCracks(Minecraft mc, Tessellator tessellator, WorldRenderer buffer, float I, int rifts) {
        GlStateManager.enableTexture2D();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        mc.getTextureManager().bindTexture(SmokeFx.FRACTURE);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        for (int r = 0; r < rifts; r++) {
            float ax = RAND[r * 3] - 0.5f, ay = 0.6f + RAND[r * 3 + 1] * 0.4f, azz = RAND[r * 3 + 2] - 0.5f;
            // Along the rift's great circle, a chain of fractures that breathe in turn.
            float[] u = new float[3], v = new float[3];
            float len = (float) Math.sqrt(ax * ax + ay * ay + azz * azz);
            SmokeFx.basis(ax / len, ay / len, azz / len, u, v);
            for (int i = 0; i < 9; i++) {
                float ang = (float) (i / 9.0 * Math.PI) + 0.15f + RAND[40 + r * 10 + i] * 0.2f;
                float px = u[0] * MathHelper.cos(ang) + v[0] * MathHelper.sin(ang);
                float py = u[1] * MathHelper.cos(ang) + v[1] * MathHelper.sin(ang);
                float pz = u[2] * MathHelper.cos(ang) + v[2] * MathHelper.sin(ang);
                if (py < -0.05f) { px = -px; py = -py; pz = -pz; }
                float breathe = 0.5f + 0.5f * MathHelper.sin(t * 1.4f + i * 0.9f + r);
                billboard(buffer, px, py, pz, R * 0.95f, 2.2f + breathe * 1.4f, i * 47f + t * 2f, 0f, 0f, 1f, 1f,
                        main[0], main[1], main[2], (0.35f + 0.5f * breathe) * I);
            }
        }
        tessellator.draw();
        GlStateManager.disableTexture2D();
    }

    /**
     * The sky as a struck pane of glass.
     *
     * <p>The style used to be a gradient with a little noise in it and some floating fragments,
     * which was not a shattered anything. This draws the break itself: panes of tinted glass, and
     * the cracks between them as light caught along an edge. The web is geometry rather than a
     * texture because the dome's own vertices are far too coarse for a crack - {@value #AZ} by
     * {@value #EL} across the whole sky - and because 1.8.9 has no shader to put one in.
     *
     * <p>Three passes, each over the same {@value #WEB_RAYS} by {@value #WEB_RINGS} web: the panes,
     * then every crack as a soft glow, then the same cracks again as a thin bright core, so the
     * seams read as light coming through rather than as drawn lines. The whole web turns slowly
     * about the point of impact, panes catch the light one after another, and a pulse runs outward
     * along the cracks from the impact every few seconds.
     */
    private void shatteredWeb(Minecraft mc, Tessellator tessellator, WorldRenderer buffer, float I) {
        float spin = t * 0.012f;
        float pulse = (t * 0.33f) % 1f;

        // ---- panes ----
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        for (int ray = 0; ray < WEB_RAYS; ray++) {
            int next = (ray + 1) % WEB_RAYS;
            for (int ring = 0; ring < WEB_RINGS; ring++) {
                int pane = ray * WEB_RINGS + ring;
                float sheen = 0.5f + 0.5f * MathHelper.sin(t * 0.55f + PANE_PHASE[pane]);
                // Panes thin out towards the edge of the web so it does not end at a hard rim.
                float out = 1f - ring / (float) WEB_RINGS;
                float a = (0.16f + 0.4f * sheen * PANE_SHADE[pane]) * (0.3f + 0.7f * out) * I;
                float[] col = ((ray * 7 + ring * 3) % 5) < 2 ? alt : main;
                float lift = 0.35f + 0.65f * PANE_SHADE[pane];
                webNode(SCRATCH_A, ray, ring, spin);
                webNode(SCRATCH_B, next, ring, spin);
                webNode(SCRATCH_C, next, ring + 1, spin);
                webNode(SCRATCH_D, ray, ring + 1, spin);
                paneVertex(buffer, SCRATCH_A, col, a * lift);
                paneVertex(buffer, SCRATCH_B, col, a * lift);
                paneVertex(buffer, SCRATCH_C, col, a * 0.75f * lift);
                paneVertex(buffer, SCRATCH_D, col, a * 0.75f * lift);
            }
        }
        tessellator.draw();

        // ---- the cracks, twice: a wide glow, then a thin core ----
        for (int pass = 0; pass < 2; pass++) {
            boolean core = pass == 1;
            float width = core ? 0.004f : 0.022f;
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            for (int ray = 0; ray < WEB_RAYS; ray++) {
                int next = (ray + 1) % WEB_RAYS;
                float weight = RAY_WEIGHT[ray];
                for (int ring = 0; ring <= WEB_RINGS; ring++) {
                    float f = ring / (float) WEB_RINGS;
                    // The pulse is a band of brightness travelling out from the impact.
                    float wave = 1f + 1.9f * gauss(f - pulse, 0.1f);
                    // A crack is widest where it was struck and closes to a hairline at its end.
                    float taper = (0.25f + 0.75f * (1f - f)) * (0.45f + 0.55f * weight);
                    float fade = (1f - smooth(0.7f, 1f, f)) * wave * I * (0.4f + 0.6f * weight);
                    if (ring < WEB_RINGS) {
                        webNode(SCRATCH_A, ray, ring, spin);
                        webNode(SCRATCH_B, ray, ring + 1, spin);
                        crack(buffer, SCRATCH_A, SCRATCH_B, width * taper, core, fade);
                    }
                    if (ring > 0 && RING_SEGMENT[ray * (WEB_RINGS + 1) + ring]) {
                        webNode(SCRATCH_A, ray, ring, spin);
                        webNode(SCRATCH_B, next, ring, spin);
                        crack(buffer, SCRATCH_A, SCRATCH_B, width * taper * 0.7f, core, fade * 0.8f);
                    }
                }
            }
            tessellator.draw();
        }

        // ---- the point it was struck ----
        GlStateManager.enableTexture2D();
        mc.getTextureManager().bindTexture(SmokeFx.GLOW);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        float beat = 0.8f + 0.2f * MathHelper.sin(t * 1.7f);
        // Drawn nearer than the web so the crush of cracks at the centre cannot sit on top of it.
        billboard(buffer, IMPACT[0], IMPACT[1], IMPACT[2], R * 0.9f, 4.2f * beat, 0f, 0f, 0f, 1f, 1f,
                0.85f + main[0] * 0.15f, 0.9f + main[1] * 0.1f, 1f, 0.55f * I);
        billboard(buffer, IMPACT[0], IMPACT[1], IMPACT[2], R * 0.89f, 1.8f * beat, 45f, 0f, 0f, 1f, 1f,
                1f, 1f, 1f, 0.9f * I);
        billboardStretched(buffer, IMPACT[0], IMPACT[1], IMPACT[2], R * 0.89f, 7.5f * beat, 0.5f,
                t * 3f, 0f, 0f, 1f, 1f, 1f, 1f, 1f, 0.35f * I);
        billboardStretched(buffer, IMPACT[0], IMPACT[1], IMPACT[2], R * 0.89f, 0.5f, 7.5f * beat,
                t * 3f, 0f, 0f, 1f, 1f, 1f, 1f, 1f, 0.35f * I);
        tessellator.draw();
        GlStateManager.disableTexture2D();
    }

    /** A node of the web as a direction, turned by {@code spin} about the impact. */
    private static void webNode(float[] out, int ray, int ring, float spin) {
        int k = ray * (WEB_RINGS + 1) + ring;
        float r = WEB_RADIUS[k], a = WEB_ANGLE[k] + spin;
        float sr = MathHelper.sin(r), cr = MathHelper.cos(r);
        float ca = MathHelper.cos(a), sa = MathHelper.sin(a);
        out[0] = IMPACT[0] * cr + (IMPACT_U[0] * ca + IMPACT_V[0] * sa) * sr;
        out[1] = IMPACT[1] * cr + (IMPACT_U[1] * ca + IMPACT_V[1] * sa) * sr;
        out[2] = IMPACT[2] * cr + (IMPACT_U[2] * ca + IMPACT_V[2] * sa) * sr;
    }

    private void paneVertex(WorldRenderer buffer, float[] dir, float[] col, float a) {
        buffer.pos(dir[0] * R * 0.97f, dir[1] * R * 0.97f, dir[2] * R * 0.97f)
                .color(col[0], col[1], col[2], clamp(a)).endVertex();
    }

    /**
     * One crack, as a ribbon laid on the dome.
     *
     * <p>Drawn as three quads - the lit core and a fade either side of it - so the light along the
     * edge falls off instead of ending at a line.
     */
    private void crack(WorldRenderer buffer, float[] a, float[] b, float width, boolean core, float alpha) {
        if (alpha <= 0.004f) return;
        // Perpendicular to the crack, in the dome's surface.
        float mx = a[0] + b[0], my = a[1] + b[1], mz = a[2] + b[2];
        float dx = b[0] - a[0], dy = b[1] - a[1], dz = b[2] - a[2];
        float px = my * dz - mz * dy, py = mz * dx - mx * dz, pz = mx * dy - my * dx;
        float len = (float) Math.sqrt(px * px + py * py + pz * pz);
        if (len < 1.0E-5f) return;
        px /= len;
        py /= len;
        pz /= len;
        float[] col = core ? null : main;
        float r = core ? 1f : 0.55f + main[0] * 0.45f;
        float g = core ? 1f : 0.6f + main[1] * 0.4f;
        float bl = core ? 1f : 0.75f + main[2] * 0.25f;
        if (col != null) {
            r = 0.6f + col[0] * 0.4f;
            g = 0.65f + col[1] * 0.35f;
            bl = 0.85f + col[2] * 0.15f;
        }
        float inner = core ? width : width * 0.35f;
        float outer = width;
        float ca = clamp(alpha * (core ? 0.85f : 0.3f));
        if (core) {
            ribbonQuad(buffer, a, b, px, py, pz, -inner, inner, r, g, bl, ca, ca);
        } else {
            ribbonQuad(buffer, a, b, px, py, pz, -outer, -inner, r, g, bl, 0f, ca);
            ribbonQuad(buffer, a, b, px, py, pz, -inner, inner, r, g, bl, ca, ca);
            ribbonQuad(buffer, a, b, px, py, pz, inner, outer, r, g, bl, ca, 0f);
        }
    }

    private void ribbonQuad(WorldRenderer buffer, float[] a, float[] b, float px, float py, float pz,
                            float from, float to, float r, float g, float bl, float a0, float a1) {
        vertexOffset(buffer, a, px, py, pz, from, r, g, bl, a0);
        vertexOffset(buffer, b, px, py, pz, from, r, g, bl, a0);
        vertexOffset(buffer, b, px, py, pz, to, r, g, bl, a1);
        vertexOffset(buffer, a, px, py, pz, to, r, g, bl, a1);
    }

    private void vertexOffset(WorldRenderer buffer, float[] dir, float px, float py, float pz,
                              float amount, float r, float g, float b, float a) {
        float x = dir[0] + px * amount, y = dir[1] + py * amount, z = dir[2] + pz * amount;
        float len = (float) Math.sqrt(x * x + y * y + z * z);
        float d = R * 0.955f / len;
        buffer.pos(x * d, y * d, z * d).color(r, g, b, clamp(a)).endVertex();
    }

    private void glassShards(Minecraft mc, Tessellator tessellator, WorldRenderer buffer, float I, int count, boolean drift) {
        GlStateManager.enableTexture2D();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        mc.getTextureManager().bindTexture(SmokeFx.FX_ATLAS);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        for (int i = 0; i < count; i++) {
            float az = RAND[100 + i] * 6.283f + (drift ? t * 0.01f * (RAND[200 + i] - 0.5f) : 0f);
            float el = 0.05f + RAND[300 + i % 200] * 1.35f;
            float c = MathHelper.cos(el);
            float x = MathHelper.cos(az) * c, y = MathHelper.sin(el), z = MathHelper.sin(az) * c;
            int cell = 4 + (i % 3);
            float u0 = (cell % 4) * 0.25f, v0 = (cell / 4) * 0.25f;
            float spin = RAND[i] * 360f + t * (RAND[i + 7] - 0.5f) * 12f;
            float glint = 0.35f + 0.65f * smooth(0.75f, 1f, 0.5f + 0.5f * MathHelper.sin(t * 0.9f + i * 1.7f));
            float[] col = (i & 1) == 0 ? main : alt;
            billboard(buffer, x, y, z, R * 0.94f, (drift ? 0.9f : 1.6f) + RAND[i + 50] * 1.8f, spin, u0, v0, u0 + 0.25f, v0 + 0.25f,
                    col[0], col[1], col[2], glint * 0.8f * I);
        }
        tessellator.draw();
        GlStateManager.disableTexture2D();
    }

    private void glyphRain(Minecraft mc, Tessellator tessellator, WorldRenderer buffer, float I, int columns, float[] col) {
        GlStateManager.enableTexture2D();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        mc.getTextureManager().bindTexture(SmokeFx.FX_ATLAS);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        int trail = 10;
        for (int ci = 0; ci < columns; ci++) {
            float az = RAND[ci] * 6.283f;
            float speed = 0.12f + RAND[ci + 60] * 0.12f;
            float head = 1.45f - ((t * speed + RAND[ci + 120] * 3f) % 1.9f);
            float spacing = 0.075f;
            for (int g = 0; g < trail; g++) {
                float el = head + g * spacing;
                if (el > 1.45f || el < -0.15f) continue;
                float c = MathHelper.cos(el);
                float fade = 1f - g / (float) trail;
                int cell = 8 + ((ci * 7 + g * 3 + (int) (t * 4f)) & 7);
                float u0 = (cell % 4) * 0.25f, v0 = (cell / 4) * 0.25f;
                float bright = g == 0 ? 1f : fade * 0.7f;
                float r = g == 0 ? 1f : col[0], gg = g == 0 ? 1f : col[1], b = g == 0 ? 1f : col[2];
                billboard(buffer, MathHelper.cos(az) * c, MathHelper.sin(el), MathHelper.sin(az) * c, R * 0.95f,
                        1.0f, 0f, u0, v0, u0 + 0.25f, v0 + 0.25f, r, gg, b, Math.min(1f, bright * 1.3f * I));
            }
        }
        tessellator.draw();
        GlStateManager.disableTexture2D();
    }

    private void stripedSun(Tessellator tessellator, WorldRenderer buffer, float I) {
        // A synthwave sun low on the horizon, cut by bands that scroll down through it.
        float sx = 0f, sy = 0.16f, sz = -1f;
        float len = (float) Math.sqrt(sx * sx + sy * sy + sz * sz);
        sx /= len; sy /= len; sz /= len;
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        float[] u = new float[3], v = new float[3];
        SmokeFx.basis(sx, sy, sz, u, v);
        // Make v point up the sky.
        if (v[1] < 0) { v[0] = -v[0]; v[1] = -v[1]; v[2] = -v[2]; }
        float radius = 3.2f;
        float cx = sx * R * 0.93f, cy = sy * R * 0.93f, cz = sz * R * 0.93f;
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        int rows = 28;
        for (int rIdx = 0; rIdx < rows; rIdx++) {
            float y0 = -1f + 2f * rIdx / rows, y1 = -1f + 2f * (rIdx + 1) / rows;
            // Gaps widen toward the bottom and scroll.
            float band = ((y0 + 1f) * 6f + t * 0.8f) % 1f;
            float gap = 0.15f + 0.55f * (1f - (y0 + 1f) / 2f);
            if (y0 < 0.1f && band < gap) continue;
            float w0 = (float) Math.sqrt(Math.max(0, 1 - y0 * y0)), w1 = (float) Math.sqrt(Math.max(0, 1 - y1 * y1));
            float f0 = (y0 + 1f) / 2f, f1 = (y1 + 1f) / 2f;
            sunV(buffer, cx, cy, cz, u, v, -w0 * radius, y0 * radius, f0, I);
            sunV(buffer, cx, cy, cz, u, v, w0 * radius, y0 * radius, f0, I);
            sunV(buffer, cx, cy, cz, u, v, w1 * radius, y1 * radius, f1, I);
            sunV(buffer, cx, cy, cz, u, v, -w1 * radius, y1 * radius, f1, I);
        }
        tessellator.draw();
    }

    private void sunV(WorldRenderer buffer, float cx, float cy, float cz, float[] u, float[] v, float a, float b, float f, float I) {
        float r = main[0] + (1f - main[0]) * f, g = main[1] * (1 - f) + 0.85f * f, bl = main[2] * (1 - f) + 0.3f * f;
        buffer.pos(cx + u[0] * a + v[0] * b, cy + u[1] * a + v[1] * b, cz + u[2] * a + v[2] * b)
                .color(r, g, bl, 0.9f * I).endVertex();
    }

    private void runeRings(Minecraft mc, Tessellator tessellator, WorldRenderer buffer, float I) {
        GlStateManager.enableTexture2D();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        ResourceLocation[] rings = {SmokeFx.RUNE_RING_0, SmokeFx.RUNE_RING_1, SmokeFx.RUNE_RING_2};
        float[] sizes = {22f, 15f, 9f};
        float[] speeds = {3f, -5f, 9f};
        for (int i = 0; i < 3; i++) {
            mc.getTextureManager().bindTexture(rings[i]);
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
            float pulse = 0.6f + 0.4f * MathHelper.sin(t * 0.9f + i * 1.3f);
            float[] col = i == 1 ? alt : main;
            // Flat overhead, facing down at the player.
            flatOverhead(buffer, R * (0.9f - i * 0.04f), sizes[i], t * speeds[i], col, pulse * 0.75f * I);
            tessellator.draw();
        }
        GlStateManager.disableTexture2D();
    }

    private static void flatOverhead(WorldRenderer buffer, float height, float size, float rotDeg, float[] col, float a) {
        float rad = (float) Math.toRadians(rotDeg);
        float c = MathHelper.cos(rad) * size / 2f, s = MathHelper.sin(rad) * size / 2f;
        buffer.pos(-c + s, height, -s - c).tex(0, 0).color(col[0], col[1], col[2], a).endVertex();
        buffer.pos(c + s, height, s - c).tex(1, 0).color(col[0], col[1], col[2], a).endVertex();
        buffer.pos(c - s, height, s + c).tex(1, 1).color(col[0], col[1], col[2], a).endVertex();
        buffer.pos(-c - s, height, -s + c).tex(0, 1).color(col[0], col[1], col[2], a).endVertex();
    }

    private void constellations(Tessellator tessellator, WorldRenderer buffer, float I) {
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        // Join each of a few bright stars to its nearest bright neighbour.
        for (int s = 0; s < 90; s += 3) {
            if (SB[s] < 0.45f || SY[s] < 0.15f) continue;
            int best = -1;
            float bestD = 0.12f;
            for (int o = s + 3; o < 200; o += 3) {
                if (SB[o] < 0.45f || SY[o] < 0.15f) continue;
                float d = dist2(SX[s], SY[s], SZ[s], SX[o], SY[o], SZ[o]);
                if (d < bestD) {
                    bestD = d;
                    best = o;
                }
            }
            if (best < 0) continue;
            float a = (0.3f + 0.3f * MathHelper.sin(t * 0.7f + s)) * I;
            band(buffer, SX[s], SY[s], SZ[s], SX[best], SY[best], SZ[best], R * 0.96f, 0.04f, main, a);
        }
        tessellator.draw();
    }

    private void lightning(Tessellator tessellator, WorldRenderer buffer, float I) {
        float f = flash(t);
        if (f <= 0.05f) return;
        int seed = (int) (t / 7f);
        Random random = new Random(seed * 31L + 7L);
        float az = random.nextFloat() * 6.283f;
        float el = 1.1f;
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        float px = MathHelper.cos(az) * MathHelper.cos(el), py = MathHelper.sin(el), pz = MathHelper.sin(az) * MathHelper.cos(el);
        for (int s = 0; s < 14 && el > 0f; s++) {
            el -= 0.08f;
            az += (random.nextFloat() - 0.5f) * 0.12f;
            float nx = MathHelper.cos(az) * MathHelper.cos(el), ny = MathHelper.sin(el), nz = MathHelper.sin(az) * MathHelper.cos(el);
            band(buffer, px, py, pz, nx, ny, nz, R * 0.95f, 0.12f, WHITE, f * I);
            band(buffer, px, py, pz, nx, ny, nz, R * 0.95f, 0.45f, new float[]{0.6f, 0.7f, 1f}, f * 0.35f * I);
            px = nx; py = ny; pz = nz;
        }
        tessellator.draw();
    }

    /** A flash every seven seconds or so, a quick double strike that fades. */
    private static float flash(float t) {
        float phase = t % 7f;
        if (phase > 0.6f) return 0f;
        float a = (float) Math.exp(-phase * 9f);
        float b = phase > 0.18f ? (float) Math.exp(-(phase - 0.18f) * 12f) * 0.7f : 0f;
        return Math.min(1f, a + b);
    }

    private void risingEmbers(Minecraft mc, Tessellator tessellator, WorldRenderer buffer, float I) {
        GlStateManager.enableTexture2D();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        mc.getTextureManager().bindTexture(SmokeFx.FX_ATLAS);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        for (int i = 0; i < 90; i++) {
            float az = RAND[i] * 6.283f + MathHelper.sin(t * 0.3f + i) * 0.05f;
            float life = (t * (0.05f + RAND[i + 90] * 0.08f) + RAND[i + 180]) % 1f;
            float el = -0.05f + life * 0.9f;
            float c = MathHelper.cos(el);
            float a = (1f - life) * smooth(0f, 0.1f, life) * I;
            billboard(buffer, MathHelper.cos(az) * c, MathHelper.sin(el), MathHelper.sin(az) * c, R * 0.95f,
                    0.18f + RAND[i + 270] * 0.2f, 0f, 0f, 0f, 0.25f, 0.25f, 1f, 0.5f + RAND[i] * 0.3f, 0.1f, a);
        }
        tessellator.draw();
        GlStateManager.disableTexture2D();
    }

    private void bleedingCore(Minecraft mc, Tessellator tessellator, WorldRenderer buffer, float I) {
        GlStateManager.enableTexture2D();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        mc.getTextureManager().bindTexture(SmokeFx.GLOW);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        float pulse = 0.8f + 0.2f * MathHelper.sin(t * 2.2f);
        flatOverhead(buffer, R * 0.9f, 9f * pulse, 0f, new float[]{1f, 0.3f, 0.25f}, 0.8f * I);
        flatOverhead(buffer, R * 0.89f, 3.2f, 45f, new float[]{1f, 0.85f, 0.8f}, 0.95f * I);
        tessellator.draw();
        GlStateManager.disableTexture2D();
    }

    /** Still, dark figures hanging in the overcast, turning slowly on nothing. */
    private void figures(Tessellator tessellator, WorldRenderer buffer, float I) {
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < 16; i++) {
            float az = RAND[i * 2] * 6.283f;
            float el = 0.18f + RAND[i * 2 + 1] * 0.95f;
            float sway = MathHelper.sin(t * 0.4f + i) * 0.015f;
            float bob = MathHelper.sin(t * 0.25f + i * 1.7f) * 0.02f;
            float c = MathHelper.cos(el + bob);
            float x = MathHelper.cos(az + sway) * c, y = MathHelper.sin(el + bob), z = MathHelper.sin(az + sway) * c;
            float size = 1.4f + RAND[i + 40] * 2.2f; // nearer ones are bigger
            float shade = 0.05f + 0.1f * (1f - RAND[i + 40]);
            float alpha = (0.55f + 0.4f * RAND[i + 40]) * I;
            figure(buffer, x, y, z, size, MathHelper.sin(t * 0.3f + i * 2f) * 0.25f, shade, alpha);
        }
        tessellator.draw();
    }

    private static void figure(WorldRenderer buffer, float x, float y, float z, float size, float turn,
                               float shade, float a) {
        float[] u = new float[3], v = new float[3];
        upright(x, y, z, u, v);
        // Turning on the spot narrows the silhouette, as a body seen edge-on would.
        float w = 0.55f + 0.45f * Math.abs(MathHelper.cos(turn));
        float cx = x * R * 0.94f, cy = y * R * 0.94f, cz = z * R * 0.94f;
        float s = size;
        // Head, torso, arms hanging, legs together: a still body, nothing more.
        rect(buffer, cx, cy, cz, u, v, -0.09f * s * w, 0.62f * s, 0.09f * s * w, 0.8f * s, shade, a);
        rect(buffer, cx, cy, cz, u, v, -0.15f * s * w, 0.18f * s, 0.15f * s * w, 0.6f * s, shade, a);
        rect(buffer, cx, cy, cz, u, v, -0.21f * s * w, 0.22f * s, -0.15f * s * w, 0.58f * s, shade, a);
        rect(buffer, cx, cy, cz, u, v, 0.15f * s * w, 0.22f * s, 0.21f * s * w, 0.58f * s, shade, a);
        rect(buffer, cx, cy, cz, u, v, -0.13f * s * w, -0.42f * s, 0.13f * s * w, 0.18f * s, shade, a);
    }

    // ---- dreamcore and dream ---------------------------------------------------------------------

    private static final ResourceLocation EYE = new ResourceLocation("soupapi", "textures/particle/glyph/eye.png");

    /**
     * Dreamcore: eyes floating in the clouds that look about and blink, white doors standing in the
     * sky with light behind them, and a stairway climbing into the clouds.
     */
    private void dreamcore(Minecraft mc, Tessellator tessellator, WorldRenderer buffer, float I) {
        float[] u = new float[3], v = new float[3];
        GlStateManager.enableTexture2D();
        mc.getTextureManager().bindTexture(SmokeFx.GLOW);
        // Soft shadows that lift the doors and steps off the pale clouds (multiply-darken: the glow's
        // black surround leaves the sky untouched, so no alpha channel is needed).
        GlStateManager.blendFunc(GL11.GL_ZERO, GL11.GL_ONE_MINUS_SRC_COLOR);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        for (int i = 0; i < 5; i++) {
            float az = 0.6f + i * 1.25f + RAND[i + 60] * 0.4f;
            float el = 0.12f + RAND[i + 70] * 0.45f;
            float c = MathHelper.cos(el);
            float x = MathHelper.cos(az) * c, y = MathHelper.sin(el), z = MathHelper.sin(az) * c;
            upright(x, y, z, u, v);
            float s = 1.8f + RAND[i + 80] * 1.2f;
            glow(buffer, x * R * 0.94f, y * R * 0.94f, z * R * 0.94f, u, v, 0.08f * s, -0.1f * s, 1.4f * s, 2.1f * s,
                    0.25f * I, 0.32f * I, 0.2f * I, 1f);
        }
        tessellator.draw();
        GlStateManager.disableTexture2D();

        // Doors and the stair: solid pastel shapes, alpha-blended.
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < 5; i++) {
            float az = 0.6f + i * 1.25f + RAND[i + 60] * 0.4f;
            float el = 0.12f + RAND[i + 70] * 0.45f + MathHelper.sin(t * 0.3f + i) * 0.01f;
            float c = MathHelper.cos(el);
            float x = MathHelper.cos(az) * c, y = MathHelper.sin(el), z = MathHelper.sin(az) * c;
            upright(x, y, z, u, v);
            float px = x * R * 0.93f, py = y * R * 0.93f, pz = z * R * 0.93f;
            float s = 1.8f + RAND[i + 80] * 1.2f;
            // Frame, the door, a light spilling from the gap, a knob.
            quad(buffer, px, py, pz, u, v, -0.36f * s, -0.7f * s, 0.36f * s, 0.7f * s, 0.99f, 0.98f, 1f, I);
            quad(buffer, px, py, pz, u, v, -0.3f * s, -0.66f * s, 0.3f * s, 0.62f * s, 0.82f, 0.72f, 0.9f, I);
            quad(buffer, px, py, pz, u, v, -0.26f * s, 0.08f * s, 0.2f * s, 0.56f * s, 0.88f, 0.8f, 0.94f, I);
            quad(buffer, px, py, pz, u, v, -0.26f * s, -0.6f * s, 0.2f * s, -0.02f * s, 0.88f, 0.8f, 0.94f, I);
            quad(buffer, px, py, pz, u, v, 0.24f * s, -0.66f * s, 0.3f * s, 0.62f * s, 1f, 0.97f, 0.75f, I);
            quad(buffer, px, py, pz, u, v, 0.12f * s, -0.04f * s, 0.18f * s, 0.02f * s, 0.6f, 0.48f, 0.68f, I);
        }
        // The stairway: steps rising toward the zenith on one side of the sky.
        for (int k = 0; k < 14; k++) {
            float el = 0.05f + k * 0.07f;
            float az = 3.9f + k * 0.035f;
            float c = MathHelper.cos(el);
            float x = MathHelper.cos(az) * c, y = MathHelper.sin(el), z = MathHelper.sin(az) * c;
            upright(x, y, z, u, v);
            float px = x * R * 0.92f, py = y * R * 0.92f, pz = z * R * 0.92f;
            float w = 0.9f * (1f - k * 0.04f);
            float fade = 1f - smooth(0.6f, 1f, k / 14f);
            quad(buffer, px, py, pz, u, v, -w, 0f, w, 0.1f, 1f, 1f, 1f, fade * I);
            quad(buffer, px, py, pz, u, v, -w, -0.22f, w, 0f, 0.74f, 0.68f, 0.88f, fade * I);
        }
        tessellator.draw();

        GlStateManager.enableTexture2D();
        // Light behind the doors.
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        mc.getTextureManager().bindTexture(SmokeFx.GLOW);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        for (int i = 0; i < 5; i++) {
            float az = 0.6f + i * 1.25f + RAND[i + 60] * 0.4f;
            float el = 0.12f + RAND[i + 70] * 0.45f;
            float c = MathHelper.cos(el);
            float x = MathHelper.cos(az) * c, y = MathHelper.sin(el), z = MathHelper.sin(az) * c;
            upright(x, y, z, u, v);
            float s = 1.8f + RAND[i + 80] * 1.2f;
            glow(buffer, x * R * 0.925f, y * R * 0.925f, z * R * 0.925f, u, v, 0.27f * s, 0f, 0.5f * s, 1.6f * s,
                    1f, 0.92f, 0.6f, 0.5f * I);
        }
        tessellator.draw();

        // Eyes, drawn as dark line-work (the eye glyph used as a darkening mask), each upright,
        // drifting, and blinking now and then.
        GlStateManager.blendFunc(GL11.GL_ZERO, GL11.GL_ONE_MINUS_SRC_COLOR);
        mc.getTextureManager().bindTexture(EYE);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        for (int i = 0; i < 11; i++) {
            float az = RAND[i * 3] * 6.283f;
            float el = 0.15f + RAND[i * 3 + 1] * 0.9f + MathHelper.sin(t * 0.4f + i * 2f) * 0.015f;
            float c = MathHelper.cos(el);
            float x = MathHelper.cos(az) * c, y = MathHelper.sin(el), z = MathHelper.sin(az) * c;
            upright(x, y, z, u, v);
            float size = 0.8f + RAND[i * 3 + 2] * 1.4f;
            float cycle = (t * 0.35f + RAND[i + 90] * 7f) % 4f;
            float open = cycle < 0.18f ? Math.abs(cycle / 0.09f - 1f) : 1f;
            glow(buffer, x * R * 0.925f, y * R * 0.925f, z * R * 0.925f, u, v, 0f, 0f, size,
                    size * Math.max(0.08f, open), 0.5f * I, 0.7f * I, 0.42f * I, 1f);
        }
        tessellator.draw();
        GlStateManager.disableTexture2D();
    }

    /**
     * Dream: a vast face in the clouds in the southern sky, pale and soft-edged, built from soft
     * glows - a lighter oval, shadowed sockets, eyes that close slowly and open again, a nose and a
     * mouth that are only shading. It breathes.
     */
    private void dreamFace(Minecraft mc, Tessellator tessellator, WorldRenderer buffer, float I) {
        float el = 0.5f, az = 1.5708f;
        float c = MathHelper.cos(el);
        float x = MathHelper.cos(az) * c, y = MathHelper.sin(el), z = MathHelper.sin(az) * c;
        float[] u = new float[3], v = new float[3];
        upright(x, y, z, u, v);
        float dist = R * 0.9f;
        float px = x * dist, py = y * dist, pz = z * dist;
        float s = 15f * (1f + 0.015f * MathHelper.sin(t * 0.9f));
        float cycle = (t * 0.12f) % 3f;
        float open = cycle < 0.5f ? Math.abs(cycle / 0.25f - 1f) : 1f;
        open = 0.15f + 0.85f * smooth(0f, 1f, open);

        GlStateManager.enableTexture2D();
        mc.getTextureManager().bindTexture(SmokeFx.GLOW);
        // The face itself: a pale glow, brighter on the forehead and cheeks.
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        glow(buffer, px, py, pz, u, v, 0f, 0.05f * s, 0.95f * s, 1.25f * s, 1f, 0.85f, 0.93f, 0.4f * I);
        glow(buffer, px, py, pz, u, v, 0f, 0.3f * s, 0.6f * s, 0.45f * s, 1f, 0.92f, 0.96f, 0.25f * I);
        glow(buffer, px, py, pz, u, v, -0.24f * s, -0.12f * s, 0.32f * s, 0.25f * s, 1f, 0.8f, 0.88f, 0.2f * I);
        glow(buffer, px, py, pz, u, v, 0.24f * s, -0.12f * s, 0.32f * s, 0.25f * s, 1f, 0.8f, 0.88f, 0.2f * I);
        tessellator.draw();
        // Shading: sockets, the eyes themselves (narrowing as they close), brows, the nose's shadow,
        // the mouth. Multiply-darken, so the glow's black surround leaves the sky untouched and only
        // the soft centres shade it - violet, because green is taken out more than red and blue.
        GlStateManager.blendFunc(GL11.GL_ZERO, GL11.GL_ONE_MINUS_SRC_COLOR);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        for (int side = -1; side <= 1; side += 2) {
            shade(buffer, px, py, pz, u, v, side * 0.2f * s, 0.12f * s, 0.4f * s, 0.26f * s, 0.35f * I);
            shade(buffer, px, py, pz, u, v, side * 0.2f * s, 0.11f * s, 0.24f * s, 0.1f * s * open + 0.015f * s, 0.8f * I);
            shade(buffer, px, py, pz, u, v, side * 0.21f * s, 0.27f * s, 0.32f * s, 0.06f * s, 0.3f * I);
        }
        shade(buffer, px, py, pz, u, v, 0.04f * s, -0.1f * s, 0.08f * s, 0.32f * s, 0.25f * I);
        shade(buffer, px, py, pz, u, v, 0f, -0.27f * s, 0.22f * s, 0.07f * s, 0.35f * I);
        shade(buffer, px, py, pz, u, v, 0f, -0.43f * s, 0.32f * s, 0.08f * s, 0.55f * I);
        shade(buffer, px, py, pz, u, v, 0f, -0.5f * s, 0.24f * s, 0.07f * s, 0.25f * I);
        shade(buffer, px, py, pz, u, v, 0f, -0.75f * s, 0.6f * s, 0.25f * s, 0.2f * I);
        tessellator.draw();
        // A glint in each eye while it is open.
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        for (int side = -1; side <= 1; side += 2) {
            glow(buffer, px, py, pz, u, v, side * 0.2f * s + 0.02f * s, 0.12f * s, 0.06f * s, 0.05f * s * open,
                    1f, 0.9f, 1f, 0.6f * smooth(0.5f, 1f, open) * I);
        }
        tessellator.draw();
        GlStateManager.disableTexture2D();
    }

    /** A glow quad on the dome at an upright offset (ox, oy) from (px, py, pz), w by h. */
    private static void glow(WorldRenderer buffer, float px, float py, float pz, float[] u, float[] v,
                             float ox, float oy, float w, float h, float r, float g, float b, float a) {
        if (a <= 0.003f) return;
        a = clamp(a);
        float x0 = ox - w / 2f, x1 = ox + w / 2f, y0 = oy - h / 2f, y1 = oy + h / 2f;
        buffer.pos(px + u[0] * x0 + v[0] * y0, py + u[1] * x0 + v[1] * y0, pz + u[2] * x0 + v[2] * y0).tex(0, 1).color(r, g, b, a).endVertex();
        buffer.pos(px + u[0] * x1 + v[0] * y0, py + u[1] * x1 + v[1] * y0, pz + u[2] * x1 + v[2] * y0).tex(1, 1).color(r, g, b, a).endVertex();
        buffer.pos(px + u[0] * x1 + v[0] * y1, py + u[1] * x1 + v[1] * y1, pz + u[2] * x1 + v[2] * y1).tex(1, 0).color(r, g, b, a).endVertex();
        buffer.pos(px + u[0] * x0 + v[0] * y1, py + u[1] * x0 + v[1] * y1, pz + u[2] * x0 + v[2] * y1).tex(0, 0).color(r, g, b, a).endVertex();
    }

    /** A soft darkening at an upright offset, tinted toward violet; for the multiply-darken pass. */
    private static void shade(WorldRenderer buffer, float px, float py, float pz, float[] u, float[] v,
                              float ox, float oy, float w, float h, float amount) {
        amount = clamp(amount);
        glow(buffer, px, py, pz, u, v, ox, oy, w, h, 0.55f * amount, 0.8f * amount, 0.45f * amount, 1f);
    }

    /** A flat-coloured quad on the dome at an upright offset. */
    private static void quad(WorldRenderer buffer, float px, float py, float pz, float[] u, float[] v,
                             float x0, float y0, float x1, float y1, float r, float g, float b, float a) {
        if (a <= 0.003f) return;
        a = clamp(a);
        buffer.pos(px + u[0] * x0 + v[0] * y0, py + u[1] * x0 + v[1] * y0, pz + u[2] * x0 + v[2] * y0).color(r, g, b, a).endVertex();
        buffer.pos(px + u[0] * x1 + v[0] * y0, py + u[1] * x1 + v[1] * y0, pz + u[2] * x1 + v[2] * y0).color(r, g, b, a).endVertex();
        buffer.pos(px + u[0] * x1 + v[0] * y1, py + u[1] * x1 + v[1] * y1, pz + u[2] * x1 + v[2] * y1).color(r, g, b, a).endVertex();
        buffer.pos(px + u[0] * x0 + v[0] * y1, py + u[1] * x0 + v[1] * y1, pz + u[2] * x0 + v[2] * y1).color(r, g, b, a).endVertex();
    }

    /** Tangent axes at a dome direction with v pointing up the sky, u to the right. */
    private static void upright(float x, float y, float z, float[] u, float[] v) {
        float vx = -x * y, vy = 1f - y * y, vz = -z * y;
        float len = (float) Math.sqrt(vx * vx + vy * vy + vz * vz);
        if (len < 1e-4f) {
            SmokeFx.basis(x, y, z, u, v);
            return;
        }
        v[0] = vx / len; v[1] = vy / len; v[2] = vz / len;
        // u = v x n
        u[0] = v[1] * z - v[2] * y;
        u[1] = v[2] * x - v[0] * z;
        u[2] = v[0] * y - v[1] * x;
    }

    private static void rect(WorldRenderer buffer, float cx, float cy, float cz, float[] u, float[] v,
                             float x0, float y0, float x1, float y1, float shade, float a) {
        float r = shade, g = shade, b = shade * 1.1f;
        buffer.pos(cx + u[0] * x0 + v[0] * y0, cy + u[1] * x0 + v[1] * y0, cz + u[2] * x0 + v[2] * y0).color(r, g, b, a).endVertex();
        buffer.pos(cx + u[0] * x1 + v[0] * y0, cy + u[1] * x1 + v[1] * y0, cz + u[2] * x1 + v[2] * y0).color(r, g, b, a).endVertex();
        buffer.pos(cx + u[0] * x1 + v[0] * y1, cy + u[1] * x1 + v[1] * y1, cz + u[2] * x1 + v[2] * y1).color(r, g, b, a).endVertex();
        buffer.pos(cx + u[0] * x0 + v[0] * y1, cy + u[1] * x0 + v[1] * y1, cz + u[2] * x0 + v[2] * y1).color(r, g, b, a).endVertex();
    }

    private void voidPulses(Minecraft mc, Tessellator tessellator, WorldRenderer buffer, float I) {
        GlStateManager.enableTexture2D();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        mc.getTextureManager().bindTexture(SmokeFx.RUNE_RING_2);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        for (int i = 0; i < 4; i++) {
            float life = (t * 0.08f + i * 0.25f) % 1f;
            float a = (1f - life) * smooth(0f, 0.1f, life) * 0.35f * I;
            flatOverhead(buffer, R * 0.9f, 4f + life * 30f, i * 30f + t * 2f, main, a);
        }
        tessellator.draw();
        GlStateManager.disableTexture2D();
    }

    /** A honeycomb over the upper sky, pulsing outward from the zenith. */
    private void hexLattice(Tessellator tessellator, WorldRenderer buffer, float I) {
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        int columns = 22;
        float cellAz = (float) (Math.PI * 2 / columns);
        float cellEl = 0.16f;
        for (int row = 0; row < 9; row++) {
            for (int col = 0; col < columns; col++) {
                float caz = col * cellAz + (row & 1) * cellAz * 0.5f;
                float cel = 0.05f + row * cellEl * 0.86f;
                if (cel > 1.35f) continue;
                float shrink = MathHelper.cos(cel); // cells narrow toward the top, as on a globe
                float pulse = 0.25f + 0.75f * smooth(0.6f, 1f, 0.5f + 0.5f * MathHelper.sin(cel * 6f - t * 2f));
                float a = pulse * 0.85f * I;
                float[] col3 = (row + col) % 5 == 0 ? alt : main;
                for (int e = 0; e < 6; e++) {
                    float a0 = (float) (Math.PI / 3 * e + Math.PI / 6), a1 = (float) (Math.PI / 3 * (e + 1) + Math.PI / 6);
                    float az0 = caz + MathHelper.cos(a0) * cellAz * 0.55f, el0 = cel + MathHelper.sin(a0) * cellEl * 0.55f * shrink;
                    float az1 = caz + MathHelper.cos(a1) * cellAz * 0.55f, el1 = cel + MathHelper.sin(a1) * cellEl * 0.55f * shrink;
                    bandAE(buffer, az0, el0, az1, el1, 0.05f, col3, a);
                }
            }
        }
        tessellator.draw();
    }

    /** Streaks pulled down toward the horizon, as if falling through the sky at speed. */
    private void speedStreaks(Tessellator tessellator, WorldRenderer buffer, float I) {
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < 110; i++) {
            float az = RAND[i] * 6.283f;
            float life = (t * (0.25f + RAND[i + 110] * 0.35f) + RAND[i + 220]) % 1f;
            float head = 1.4f - life * 1.5f;
            float len = 0.12f + RAND[i + 330] * 0.25f;
            float a = smooth(0f, 0.15f, life) * (1f - smooth(0.7f, 1f, life)) * I;
            float[] col = (i % 4 == 0) ? alt : main;
            bandAE(buffer, az, head, az, head + len, 0.035f + RAND[i + 40] * 0.03f, col, a);
        }
        tessellator.draw();
    }

    /** Circuit traces: right-angled paths that light up a segment at a time, with nodes. */
    private void circuitTraces(Minecraft mc, Tessellator tessellator, WorldRenderer buffer, float I) {
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        float step = 0.11f;
        for (int path = 0; path < 44; path++) {
            float az = RAND[(path * 5) & 511] * 6.283f, el = 0.08f + RAND[(path * 5 + 1) & 511] * 0.9f;
            float lit = (t * 0.6f + RAND[(path * 5 + 2) & 511] * 5f) % 6f;
            for (int seg = 0; seg < 8; seg++) {
                boolean horizontal = ((path + seg) & 1) == 0;
                float dir = RAND[(path * 13 + seg) & 511] > 0.5f ? 1f : -1f;
                float az1 = horizontal ? az + dir * step / Math.max(0.3f, MathHelper.cos(el)) : az;
                float el1 = horizontal ? el : el + dir * step;
                if (el1 < 0.02f || el1 > 1.3f) el1 = el;
                float glow = Math.max(0f, 1f - Math.abs(lit - seg) * 0.8f);
                float[] col = glow > 0.6f ? WHITE : main;
                bandAE(buffer, az, el, az1, el1, 0.075f, col, (0.3f + glow * 0.9f) * I);
                az = az1;
                el = el1;
            }
        }
        tessellator.draw();
    }

    /** Veins of energy in the dark: fractures that brighten and fade in turn. */
    private void veins(Minecraft mc, Tessellator tessellator, WorldRenderer buffer, float I) {
        GlStateManager.enableTexture2D();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        mc.getTextureManager().bindTexture(SmokeFx.FRACTURE);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        for (int i = 0; i < 14; i++) {
            float az = RAND[i + 400] * 6.283f, el = 0.1f + RAND[i + 420] * 1.1f;
            float c = MathHelper.cos(el);
            float pulse = smooth(0.3f, 1f, 0.5f + 0.5f * MathHelper.sin(t * 0.7f + i * 2.1f));
            float[] col = (i & 1) == 0 ? main : alt;
            billboard(buffer, MathHelper.cos(az) * c, MathHelper.sin(el), MathHelper.sin(az) * c, R * 0.95f,
                    4f + RAND[i + 440] * 4f, RAND[i + 460] * 360f, 0f, 0f, 1f, 1f, col[0], col[1], col[2], pulse * 0.55f * I);
        }
        tessellator.draw();
        GlStateManager.disableTexture2D();
    }

    /**
     * Strata of cloud, drifting.
     *
     * <p>What a banded sky was missing is that bands are flat: the same stripe all the way round,
     * moving as one, which reads as wallpaper. These are rings of cloud at different heights, each
     * drifting at its own speed, each with its own lumpy edge from the noise table, and each
     * fading out top and bottom — so they overlap, part, and let the sky through between them.
     */
    private void cloudBanks(Tessellator tessellator, WorldRenderer buffer, float I, int layers) {
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        int steps = 64;
        for (int layer = 0; layer < layers; layer++) {
            float seed = layer * 97f;
            float height = 0.12f + layer * (1.05f / layers);
            float drift = t * (0.05f + 0.05f * layer) * (layer % 2 == 0 ? 1f : -1f);
            float thickness = 0.14f + 0.07f * RAND[(layer * 31) & 511];
            float[] col = layer % 3 == 1 ? alt : main;
            for (int s = 0; s < steps; s++) {
                float a0 = s / (float) steps * 6.283f + drift;
                float a1 = (s + 1) / (float) steps * 6.283f + drift;
                // A lumpy underside and a lumpier top: three octaves off the shared noise table.
                float lo0 = height + ridge(a0, seed) * thickness;
                float lo1 = height + ridge(a1, seed) * thickness;
                float hi0 = lo0 + thickness * (0.7f + 0.6f * ridge(a0 * 1.7f, seed + 11f));
                float hi1 = lo1 + thickness * (0.7f + 0.6f * ridge(a1 * 1.7f, seed + 11f));
                float lit0 = 0.35f + 0.65f * ridge(a0 * 0.8f, seed + 23f);
                float lit1 = 0.35f + 0.65f * ridge(a1 * 0.8f, seed + 23f);
                // Dense at the bottom where it is in its own shadow, open at the top.
                curtainVertex(buffer, a0, lo0, col, 0.0f);
                curtainVertex(buffer, a1, lo1, col, 0.0f);
                curtainVertex(buffer, a1, (lo1 + hi1) * 0.5f, col, 0.42f * lit1 * I);
                curtainVertex(buffer, a0, (lo0 + hi0) * 0.5f, col, 0.42f * lit0 * I);

                curtainVertex(buffer, a0, (lo0 + hi0) * 0.5f, col, 0.42f * lit0 * I);
                curtainVertex(buffer, a1, (lo1 + hi1) * 0.5f, col, 0.42f * lit1 * I);
                curtainVertex(buffer, a1, hi1, col, 0f);
                curtainVertex(buffer, a0, hi0, col, 0f);
            }
        }
        tessellator.draw();
    }

    /** A smooth, repeatable 0..1 wobble around the sky, from the shared noise table. */
    private static float ridge(float angle, float seed) {
        float a = angle + seed;
        return 0.5f + 0.5f * (MathHelper.sin(a * 1.3f) * 0.5f + MathHelper.sin(a * 2.7f + 1.7f) * 0.3f
                + MathHelper.sin(a * 5.1f - 0.9f) * 0.2f);
    }

    /** Shafts of light coming down through water: long, narrow, swaying, brightest overhead. */
    private void lightShafts(Tessellator tessellator, WorldRenderer buffer, float I) {
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < 26; i++) {
            float base = RAND[(i * 7) & 511] * 6.283f;
            float sway = MathHelper.sin(t * 0.4f + i) * 0.14f;
            float az = base + sway;
            float top = 1.45f;
            float bottom = 0.08f + RAND[(i * 7 + 1) & 511] * 0.25f;
            float width = 0.05f + RAND[(i * 7 + 2) & 511] * 0.10f;
            float shimmer = 0.45f + 0.55f * (0.5f + 0.5f * MathHelper.sin(t * 1.1f + i * 2.3f));
            float[] col = i % 4 == 0 ? alt : main;
            // Drawn in pieces so it can fade along its length rather than ending at a line.
            int pieces = 7;
            for (int k = 0; k < pieces; k++) {
                float e0 = bottom + (top - bottom) * k / pieces;
                float e1 = bottom + (top - bottom) * (k + 1) / pieces;
                float f0 = (float) k / pieces, f1 = (k + 1f) / pieces;
                bandAE(buffer, az + sway * f0, e0, az + sway * f1, e1,
                        width * (0.5f + f0), col, 0.30f * shimmer * f0 * I);
            }
        }
        tessellator.draw();
    }

    /** The vortex's arms: logarithmic spirals wound out from the zenith, turning. */
    private void spiralArms(Tessellator tessellator, WorldRenderer buffer, float I) {
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        int arms = 5;
        for (int arm = 0; arm < arms; arm++) {
            float phase = arm / (float) arms * 6.283f - t * 0.55f;
            float[] col = arm % 2 == 0 ? main : alt;
            int steps = 40;
            float paz = phase, pel = 1.55f;
            for (int k = 1; k <= steps; k++) {
                float f = k / (float) steps;
                // Elevation falls to the horizon while the angle runs on: a spiral on the dome.
                float el = 1.55f * (1f - f);
                float az = phase + f * 7.5f;
                float fade = smooth(0f, 0.12f, f) * (1f - smooth(0.78f, 1f, f));
                float pulse = 0.55f + 0.45f * MathHelper.sin(f * 14f - t * 3f + arm);
                bandAE(buffer, paz, pel, az, el, 0.05f + 0.10f * f, col, fade * pulse * 0.9f * I);
                paz = az;
                pel = el;
            }
        }
        // The eye of it.
        for (int ring = 0; ring < 3; ring++) {
            float el = 1.56f - ring * 0.035f;
            float a = (0.8f - ring * 0.2f) * (0.6f + 0.4f * MathHelper.sin(t * 2.4f - ring)) * I;
            int steps = 24;
            for (int s = 0; s < steps; s++) {
                float a0 = s / (float) steps * 6.283f, a1 = (s + 1) / (float) steps * 6.283f;
                bandAE(buffer, a0, el, a1, el, 0.03f, WHITE, a);
            }
        }
        tessellator.draw();
    }

    /** Rings spreading from directly overhead, each one born, widening and fading in turn. */
    private void expandingRings(Tessellator tessellator, WorldRenderer buffer, float I) {
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        int rings = 7;
        int steps = 48;
        for (int ring = 0; ring < rings; ring++) {
            float life = ((t * 0.22f) + ring / (float) rings) % 1f;
            float el = 1.57f * (1f - life);
            float a = smooth(0f, 0.1f, life) * (1f - smooth(0.7f, 1f, life)) * I;
            float[] col = ring % 2 == 0 ? main : alt;
            float wobble = 0.03f * (1f - life);
            for (int s = 0; s < steps; s++) {
                float a0 = s / (float) steps * 6.283f, a1 = (s + 1) / (float) steps * 6.283f;
                float e0 = el + MathHelper.sin(a0 * 6f + t) * wobble;
                float e1 = el + MathHelper.sin(a1 * 6f + t) * wobble;
                bandAE(buffer, a0, e0, a1, e1, 0.035f + 0.05f * life, col, a * 0.9f);
            }
        }
        tessellator.draw();
    }

    /**
     * Glitch tears: slabs of the sky knocked sideways for a moment.
     *
     * <p>Scanlines on their own are stripes. What makes a picture look broken rather than striped
     * is a band of it jumping out of place and snapping back, so this draws short-lived horizontal
     * slabs displaced in azimuth, in a colour that does not quite match the sky.
     */
    private void glitchTears(Tessellator tessellator, WorldRenderer buffer, float I) {
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < 18; i++) {
            // Each tear lives for a slice of its own cycle, so they fire at different moments.
            float cycle = (t * (0.35f + RAND[(i * 11) & 511] * 0.5f) + RAND[(i * 11 + 1) & 511] * 5f) % 3f;
            if (cycle > 0.4f) continue;
            float a = (1f - cycle / 0.4f) * I;
            float el = 0.05f + RAND[(i * 11 + 2) & 511] * 1.3f;
            float height = 0.02f + RAND[(i * 11 + 3) & 511] * 0.05f;
            float shift = (RAND[(i * 11 + 4) & 511] - 0.5f) * 0.9f;
            float[] col = i % 3 == 0 ? alt : (i % 3 == 1 ? main : WHITE);
            int steps = 20;
            for (int s = 0; s < steps; s++) {
                float a0 = s / (float) steps * 6.283f + shift;
                float a1 = (s + 1) / (float) steps * 6.283f + shift;
                float cut = RAND[(i * 31 + s) & 511];
                if (cut < 0.35f) continue;              // the tear is broken up, not continuous
                bandAE(buffer, a0, el, a1, el, height, col, a * 0.8f);
            }
        }
        tessellator.draw();
    }

    // ---- the newer skies --------------------------------------------------------------------------

    /**
     * A ring hung in the sky with its own tilt, rather than facing the viewer: a halo, an
     * accretion disc, a neon hoop. Centred `dist` out along (dx, dy, dz), lying in the plane of
     * (ux, uy, uz) and (vx, vy, vz), drawn as quads between an inner and an outer radius. Each
     * segment's alpha goes through `shade`, which is how a ring flickers, dashes or fades round.
     */
    private interface RingShade {
        float at(float angle);
    }

    private static void ring3d(WorldRenderer buffer, float cx, float cy, float cz,
                               float ux, float uy, float uz, float vx, float vy, float vz,
                               float radius, float width, float[] col, float a, int segments, RingShade shade) {
        float r0 = radius - width * 0.5f, r1 = radius + width * 0.5f;
        for (int i = 0; i < segments; i++) {
            float a0 = (float) (Math.PI * 2 * i / segments), a1 = (float) (Math.PI * 2 * (i + 1) / segments);
            float k = shade == null ? 1f : shade.at((a0 + a1) * 0.5f);
            if (k <= 0.003f) continue;
            float al = clamp(a * k);
            float c0 = MathHelper.cos(a0), s0 = MathHelper.sin(a0), c1 = MathHelper.cos(a1), s1 = MathHelper.sin(a1);
            buffer.pos(cx + (ux * c0 + vx * s0) * r0, cy + (uy * c0 + vy * s0) * r0, cz + (uz * c0 + vz * s0) * r0).color(col[0], col[1], col[2], al).endVertex();
            buffer.pos(cx + (ux * c1 + vx * s1) * r0, cy + (uy * c1 + vy * s1) * r0, cz + (uz * c1 + vz * s1) * r0).color(col[0], col[1], col[2], al).endVertex();
            buffer.pos(cx + (ux * c1 + vx * s1) * r1, cy + (uy * c1 + vy * s1) * r1, cz + (uz * c1 + vz * s1) * r1).color(col[0], col[1], col[2], al).endVertex();
            buffer.pos(cx + (ux * c0 + vx * s0) * r1, cy + (uy * c0 + vy * s0) * r1, cz + (uz * c0 + vz * s0) * r1).color(col[0], col[1], col[2], al).endVertex();
        }
    }

    /** A unit direction's position on the dome, `dist` out. */
    private static float[] onDome(float x, float y, float z, float dist) {
        float len = (float) Math.sqrt(x * x + y * y + z * z);
        return new float[]{x / len * dist, y / len * dist, z / len * dist};
    }

    private static final float[] GOLD = {1f, 0.84f, 0.45f};
    private static final float[] PALE_GOLD = {1f, 0.95f, 0.8f};
    private static final float[] EMBER = {1f, 0.42f, 0.08f};
    private static final float[] BLOOD = {0.85f, 0.04f, 0.04f};
    private static final float[] TERM_GREEN = {0.15f, 1f, 0.4f};
    private static final float[] RAIN = {0.7f, 0.78f, 0.9f};

    /** Halos turning overhead, one inside another, and shafts of light fanning down off them. */
    private void heavenGate(Minecraft mc, Tessellator tessellator, WorldRenderer buffer, float I) {
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        // The shafts: long tapering bands from the zenith toward the horizon, slowly wheeling.
        int shafts = 28;
        for (int i = 0; i < shafts; i++) {
            float az = i * 6.283f / shafts + t * 0.02f + RAND[i] * 0.08f;
            float flick = 0.45f + 0.55f * (0.5f + 0.5f * MathHelper.sin(t * 0.6f + i * 1.7f));
            float low = 0.15f + RAND[i + 40] * 0.25f;
            for (int k = 0; k < 6; k++) {
                float e0 = 1.5f - (1.5f - low) * k / 6f, e1 = 1.5f - (1.5f - low) * (k + 1) / 6f;
                float fade = 1f - k / 6f;
                bandAE(buffer, az, e0, az, e1, 0.03f + 0.09f * k / 6f, PALE_GOLD, 0.16f * flick * fade * I);
            }
        }
        // The halos: four rings at the top of the sky, each turning, each broken into arcs.
        for (int ring = 0; ring < 4; ring++) {
            final int rr = ring;
            float el = 1.45f - ring * 0.085f;
            float spin = t * (ring % 2 == 0 ? 0.12f : -0.09f);
            int steps = 64;
            for (int sIdx = 0; sIdx < steps; sIdx++) {
                float a0 = sIdx * 6.283f / steps + spin, a1 = (sIdx + 1) * 6.283f / steps + spin;
                float dash = (sIdx % (6 + rr)) < 4 + rr ? 1f : 0.15f;
                bandAE(buffer, a0, el, a1, el, 0.035f + 0.012f * rr, rr == 1 ? PALE_GOLD : GOLD,
                        (0.75f - rr * 0.12f) * dash * I);
            }
        }
        tessellator.draw();
        // The gate itself: a white burn at the very top.
        GlStateManager.enableTexture2D();
        mc.getTextureManager().bindTexture(SmokeFx.GLOW);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        float pulse = 0.85f + 0.15f * MathHelper.sin(t * 0.7f);
        billboard(buffer, 0.001f, 1f, 0.0f, R * 0.95f, 9f * pulse, t * 2f, 0f, 0f, 1f, 1f, 1f, 0.92f, 0.7f, 0.8f * I);
        billboard(buffer, 0.001f, 1f, 0.0f, R * 0.95f, 3.4f, 0f, 0f, 0f, 1f, 1f, 1f, 1f, 1f, 1f * I);
        tessellator.draw();
        GlStateManager.disableTexture2D();
    }

    /** A vast red sun on the horizon with a black disc crossing it, and its corona flaring. */
    private void bloodSun(Minecraft mc, Tessellator tessellator, WorldRenderer buffer, float I) {
        GlStateManager.enableTexture2D();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        mc.getTextureManager().bindTexture(SmokeFx.GLOW);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        float breathe = 0.9f + 0.1f * MathHelper.sin(t * 0.5f);
        billboard(buffer, BSX, BSY, BSZ, R * 0.95f, 24f * breathe, 0f, 0f, 0f, 1f, 1f, 1f, 0.25f, 0.05f, 0.75f * I);
        billboard(buffer, BSX, BSY, BSZ, R * 0.95f, 12f, 0f, 0f, 0f, 1f, 1f, 1f, 0.45f, 0.12f, 0.9f * I);
        billboard(buffer, BSX, BSY, BSZ, R * 0.95f, 8.2f, 0f, 0f, 0f, 1f, 1f, 1f, 0.75f, 0.45f, 1f * I);
        tessellator.draw();
        // Corona streamers off the rim.
        mc.getTextureManager().bindTexture(SmokeFx.FX_ATLAS);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        for (int i = 0; i < 18; i++) {
            float rot = i * 20f + t * 3f + RAND[i] * 15f;
            float len = 11f + RAND[i + 30] * 6f + 1.5f * MathHelper.sin(t * 1.3f + i);
            billboardStretched(buffer, BSX, BSY, BSZ, R * 0.95f, len, 0.8f, rot, 0.5f, 0f, 0.75f, 0.25f,
                    1f, 0.2f, 0.05f, 0.55f * I);
        }
        tessellator.draw();
        GlStateManager.disableTexture2D();
        // The black disc, drifting across the face of the sun on a long cycle.
        float slide = MathHelper.sin(t * 0.05f) * 0.06f;
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        disc(tessellator, buffer, BSX + slide, BSY + 0.01f, BSZ, R * 0.94f, 3.2f, 0.03f, 0.0f, 0.0f, 0.97f);
        // A thin bleeding ring where the disc meets the light.
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        float[] c = onDome(BSX + slide, BSY + 0.01f, BSZ, R * 0.935f);
        float[] u = new float[3], v = new float[3];
        SmokeFx.basis(BSX, BSY, BSZ, u, v);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        ring3d(buffer, c[0], c[1], c[2], u[0], u[1], u[2], v[0], v[1], v[2], 3.3f, 0.22f, BLOOD, 0.9f * I, 48,
                ang -> 0.6f + 0.4f * MathHelper.sin(ang * 7f + t * 2f));
        tessellator.draw();
    }

    /** A crown of fire hanging overhead: a ring of flames, a ring of embers, points of fire. */
    private void fireCrown(Minecraft mc, Tessellator tessellator, WorldRenderer buffer, float I) {
        GlStateManager.enableTexture2D();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        mc.getTextureManager().bindTexture(SmokeFx.FX_ATLAS);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        float el = 1.18f;
        int flames = 72;
        float turn = t * 0.05f;
        for (int i = 0; i < flames; i++) {
            float az = i * 6.283f / flames + turn;
            float flick = 0.6f + 0.4f * MathHelper.sin(t * 6f + i * 2.3f) * MathHelper.sin(t * 3.7f + i);
            float x = MathHelper.cos(az) * MathHelper.cos(el), y = MathHelper.sin(el), z = MathHelper.sin(az) * MathHelper.cos(el);
            // Each flame is a glow sprite stretched upward and tipped out from the crown.
            billboardStretched(buffer, x, y, z, R * 0.94f, 1.1f, 2.6f * flick, (float) Math.toDegrees(-az) + 90f,
                    0.25f, 0f, 0.5f, 0.25f, 1f, 0.38f + 0.2f * flick, 0.06f, 0.75f * I);
            billboard(buffer, x, y, z, R * 0.94f, 0.7f, t * 50f + i * 30f, 0f, 0f, 0.25f, 0.25f, 1f, 0.8f, 0.4f, 0.65f * flick * I);
        }
        // The crown's points: eight spires of fire rising off the ring.
        for (int i = 0; i < 8; i++) {
            float az = i * 6.283f / 8 + turn;
            float lift = 0.85f + 0.15f * MathHelper.sin(t * 2f + i);
            for (int k = 0; k < 5; k++) {
                float e = el + 0.04f + k * 0.035f * lift;
                float x = MathHelper.cos(az) * MathHelper.cos(e), y = MathHelper.sin(e), z = MathHelper.sin(az) * MathHelper.cos(e);
                billboard(buffer, x, y, z, R * 0.94f, 1.6f - k * 0.25f, t * 40f + k * 50f, 0.25f, 0f, 0.5f, 0.25f,
                        1f, 0.55f + k * 0.08f, 0.15f, (0.8f - k * 0.12f) * I);
            }
        }
        tessellator.draw();
        GlStateManager.disableTexture2D();
    }

    /** The terminal: a wireframe floor, rings pulsing out from overhead, and a sweeping beam. */
    private void cyberPulse(Tessellator tessellator, WorldRenderer buffer, float I) {
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        // The floor: meridians converging and rings receding toward the horizon, scrolling in.
        for (int i = 0; i < 48; i++) {
            float az = i * 6.283f / 48;
            bandAE(buffer, az, 0.0f, az, 0.24f, 0.012f, TERM_GREEN, 0.35f * I);
        }
        float scroll = (t * 0.25f) % 1f;
        for (int k = 0; k < 8; k++) {
            float el = 0.24f * (float) Math.pow((k + 1 - scroll) / 8f, 1.8f);
            int steps = 64;
            for (int sIdx = 0; sIdx < steps; sIdx++) {
                float a0 = sIdx * 6.283f / steps, a1 = (sIdx + 1) * 6.283f / steps;
                bandAE(buffer, a0, el, a1, el, 0.012f, TERM_GREEN, 0.45f * (1f - el * 2f) * I);
            }
        }
        // Pulses from the zenith, white then green.
        for (int ring = 0; ring < 4; ring++) {
            float life = ((t * 0.3f) + ring / 4f) % 1f;
            float el = 1.57f - life * 1.3f;
            float a = (1f - life) * smooth(0f, 0.05f, life) * I;
            int steps = 72;
            for (int sIdx = 0; sIdx < steps; sIdx++) {
                float a0 = sIdx * 6.283f / steps, a1 = (sIdx + 1) * 6.283f / steps;
                bandAE(buffer, a0, el, a1, el, 0.02f, ring % 2 == 0 ? WHITE : TERM_GREEN, 0.85f * a);
            }
        }
        // The beam: a vertical bar of light sweeping round the horizon.
        float sweep = t * 0.45f;
        for (int k = 0; k < 10; k++) {
            float e0 = k * 0.12f, e1 = (k + 1) * 0.12f;
            bandAE(buffer, sweep, e0, sweep, e1, 0.10f, TERM_GREEN, 0.5f * (1f - k / 10f) * I);
            bandAE(buffer, sweep - 0.05f, e0, sweep - 0.05f, e1, 0.03f, WHITE, 0.55f * (1f - k / 10f) * I);
        }
        tessellator.draw();
    }

    /** Sheets of rain: long pale streaks falling slantwise across the whole sky. */
    private void rain(Tessellator tessellator, WorldRenderer buffer, float I) {
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < 160; i++) {
            float az = RAND[i & 511] * 6.283f;
            float life = (t * (0.9f + RAND[(i + 160) & 511] * 0.6f) + RAND[(i + 320) & 511]) % 1f;
            float head = 1.4f - life * 1.45f;
            float len = 0.10f + RAND[(i + 80) & 511] * 0.12f;
            bandAE(buffer, az, head, az + 0.05f, head + len, 0.012f, RAIN, 0.28f * I);
        }
        tessellator.draw();
    }

    /** A black hole: the accretion disc tilted across it, the photon ring, the dark in the middle. */
    private void blackHole(Minecraft mc, Tessellator tessellator, WorldRenderer buffer, float I) {
        float[] c = onDome(BHX, BHY, BHZ, R * 0.92f);
        float[] u = new float[3], v = new float[3];
        SmokeFx.basis(BHX, BHY, BHZ, u, v);
        // The disc lies nearly edge-on: its plane is tipped most of the way toward the viewer.
        float tilt = 0.28f;
        float[] w = {u[0], u[1], u[2]};
        float[] dir = onDome(BHX, BHY, BHZ, 1f);
        float[] dv = {v[0] * tilt + dir[0] * (1 - tilt), v[1] * tilt + dir[1] * (1 - tilt), v[2] * tilt + dir[2] * (1 - tilt)};
        float dl = (float) Math.sqrt(dv[0] * dv[0] + dv[1] * dv[1] + dv[2] * dv[2]);
        dv[0] /= dl; dv[1] /= dl; dv[2] /= dl;
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GlStateManager.enableTexture2D();
        mc.getTextureManager().bindTexture(SmokeFx.GLOW);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        billboard(buffer, BHX, BHY, BHZ, R * 0.93f, 16f, 0f, 0f, 0f, 1f, 1f, 1f, 0.5f, 0.2f, 0.45f * I);
        tessellator.draw();
        GlStateManager.disableTexture2D();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        // Hot white at the inner edge, orange, then a dull red outer rim; each band streams round,
        // brighter on the side turning toward you.
        float spin = t * 0.6f;
        float[][] cols = {{1f, 0.95f, 0.85f}, {1f, 0.72f, 0.35f}, {1f, 0.45f, 0.12f}, {0.75f, 0.18f, 0.06f}};
        for (int b = 0; b < 4; b++) {
            final int bb = b;
            ring3d(buffer, c[0], c[1], c[2], w[0], w[1], w[2], dv[0], dv[1], dv[2], 2.6f + b * 0.7f, 0.75f,
                    cols[b], (0.85f - b * 0.15f) * I, 96,
                    ang -> (0.55f + 0.45f * MathHelper.cos(ang)) * (0.7f + 0.3f * MathHelper.sin(ang * (9 + bb * 3) - spin * (4 - bb))));
        }
        tessellator.draw();
        // The dark, over the middle of the disc.
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        disc(tessellator, buffer, BHX, BHY, BHZ, R * 0.91f, 2.0f, 0f, 0f, 0f, 1f);
        // The photon ring, face on, and the back of the disc lensed up over the top.
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        float[] c2 = onDome(BHX, BHY, BHZ, R * 0.905f);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        ring3d(buffer, c2[0], c2[1], c2[2], u[0], u[1], u[2], v[0], v[1], v[2], 2.05f, 0.12f, PALE_GOLD, 1f * I, 72, null);
        ring3d(buffer, c2[0], c2[1], c2[2], u[0], u[1], u[2], v[0], v[1], v[2], 2.5f, 0.55f, EMBER, 0.55f * I, 72,
                ang -> Math.max(0f, MathHelper.sin(ang)));
        tessellator.draw();
    }

    /** A spiral galaxy overhead: two arms of stars wound out from a bright core. */
    private void galaxy(Minecraft mc, Tessellator tessellator, WorldRenderer buffer, float I) {
        GlStateManager.enableTexture2D();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        mc.getTextureManager().bindTexture(SmokeFx.FX_ATLAS);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        float spin = t * 0.03f;
        for (int arm = 0; arm < 2; arm++) {
            for (int i = 0; i < 160; i++) {
                float f = i / 160f;
                float jitterA = (RAND[(i * 3 + arm * 7) & 511] - 0.5f) * 0.5f * (0.3f + f);
                float jitterR = (RAND[(i * 5 + arm * 11) & 511] - 0.5f) * 0.12f * (0.3f + f);
                float theta = arm * 3.1416f + f * 7.0f + spin + jitterA;
                float rad = 0.05f + f * 0.85f + jitterR;           // angular distance from the zenith
                float el = 1.5708f - rad;
                float x = MathHelper.cos(theta) * MathHelper.cos(el), y = MathHelper.sin(el), z = MathHelper.sin(theta) * MathHelper.cos(el);
                float[] col = RAND[(i + arm * 40) & 511] > 0.7f ? alt : main;
                float size = (0.5f + RAND[(i * 7) & 511] * 0.9f) * (1.4f - f);
                float tw = 0.6f + 0.4f * MathHelper.sin(t * 2f + i);
                billboard(buffer, x, y, z, R * 0.94f, size, 0f, 0.25f, 0f, 0.5f, 0.25f, col[0], col[1], col[2],
                        (0.75f - f * 0.45f) * tw * I);
            }
        }
        // The core.
        billboard(buffer, 0.001f, 1f, 0f, R * 0.94f, 5.5f, 0f, 0.25f, 0f, 0.5f, 0.25f, 1f, 0.95f, 0.85f, 0.9f * I);
        tessellator.draw();
        GlStateManager.disableTexture2D();
    }

    /** A great moon broken apart, the pieces drifting round where it was. */
    private void shatteredMoon(Minecraft mc, Tessellator tessellator, WorldRenderer buffer, float I) {
        GlStateManager.enableTexture2D();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        mc.getTextureManager().bindTexture(SmokeFx.GLOW);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        billboard(buffer, SMX, SMY, SMZ, R * 0.95f, 14f, 0f, 0f, 0f, 1f, 1f, 0.6f, 0.7f, 1f, 0.35f * I);
        tessellator.draw();
        GlStateManager.disableTexture2D();
        // The pieces: wedges of the disc, each pushed out from the centre and slowly turning round it.
        float[] u = new float[3], v = new float[3];
        SmokeFx.basis(SMX, SMY, SMZ, u, v);
        float[] c = onDome(SMX, SMY, SMZ, R * 0.93f);
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
        int pieces = 9;
        float radius = 3.4f;
        for (int pIdx = 0; pIdx < pieces; pIdx++) {
            float a0 = pIdx * 6.283f / pieces + RAND[pIdx] * 0.2f, a1 = (pIdx + 1) * 6.283f / pieces + RAND[pIdx + 1] * 0.2f;
            float mid = (a0 + a1) * 0.5f;
            float drift = 0.45f + 0.75f * RAND[pIdx + 20] + 0.25f * MathHelper.sin(t * 0.2f + pIdx);
            float turn = t * 0.01f * (pIdx % 2 == 0 ? 1 : -1);
            float ox = MathHelper.cos(mid + turn) * drift, oy = MathHelper.sin(mid + turn) * drift;
            float shade = 0.75f + 0.25f * RAND[pIdx + 30];
            float[] p0 = {ox, oy};
            int seg = 6;
            for (int k = 0; k < seg; k++) {
                float b0 = a0 + (a1 - a0) * k / seg + turn, b1 = a0 + (a1 - a0) * (k + 1) / seg + turn;
                float rim0 = radius * (0.92f + 0.08f * RAND[(pIdx * 7 + k) & 511]);
                float rim1 = radius * (0.92f + 0.08f * RAND[(pIdx * 7 + k + 1) & 511]);
                // Lit from one side, so each piece has a bright face and a shadowed edge.
                float lit0 = 0.75f + 0.25f * MathHelper.cos(b0 - 0.8f), lit1 = 0.75f + 0.25f * MathHelper.cos(b1 - 0.8f);
                moonVertex(buffer, c, u, v, p0[0], p0[1], 0.95f * shade, 0.95f * shade, 0.98f * shade);
                moonVertex(buffer, c, u, v, p0[0] + MathHelper.cos(b0) * rim0, p0[1] + MathHelper.sin(b0) * rim0,
                        0.82f * shade * lit0, 0.84f * shade * lit0, 0.92f * shade * lit0);
                moonVertex(buffer, c, u, v, p0[0] + MathHelper.cos(b1) * rim1, p0[1] + MathHelper.sin(b1) * rim1,
                        0.82f * shade * lit1, 0.84f * shade * lit1, 0.92f * shade * lit1);
            }
        }
        tessellator.draw();
        // The debris: small chips strung round the break, in a tilted ring.
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        float[] tv = {v[0] * 0.35f + u[0] * 0.1f, v[1] * 0.35f + u[1] * 0.1f, v[2] * 0.35f + u[2] * 0.1f};
        ring3d(buffer, c[0], c[1], c[2], u[0], u[1], u[2], tv[0], tv[1], tv[2], radius * 1.9f, 0.18f,
                PALE_GOLD, 0.6f * I, 120, ang -> RAND[((int) (ang * 40f + t * 3f)) & 511] > 0.55f ? 1f : 0f);
        tessellator.draw();
    }

    private static void moonVertex(WorldRenderer buffer, float[] c, float[] u, float[] v, float a, float b,
                                   float r, float g, float bl) {
        buffer.pos(c[0] + u[0] * a + v[0] * b, c[1] + u[1] * a + v[1] * b, c[2] + u[2] * a + v[2] * b)
                .color(r, g, bl, 1f).endVertex();
    }

    /** Giant neon hoops hung across the sky at angles, each turning slowly on its own axis. */
    private void neonHalos(Tessellator tessellator, WorldRenderer buffer, float I) {
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        float[][] centres = {{0.1f, 0.55f, -0.83f}, {-0.75f, 0.35f, -0.55f}, {0.7f, 0.42f, 0.55f}};
        float[] sizes = {6.5f, 4.2f, 5.0f};
        for (int i = 0; i < 3; i++) {
            float[] c = onDome(centres[i][0], centres[i][1], centres[i][2], R * 0.9f);
            float[] u = new float[3], v = new float[3];
            SmokeFx.basis(centres[i][0], centres[i][1], centres[i][2], u, v);
            // Turn the hoop about its own vertical by a slowly changing angle.
            float ang = t * (0.15f + 0.05f * i) + i;
            float cs = MathHelper.cos(ang), sn = MathHelper.sin(ang);
            float[] d = onDome(centres[i][0], centres[i][1], centres[i][2], 1f);
            float[] uu = {u[0] * cs + d[0] * sn, u[1] * cs + d[1] * sn, u[2] * cs + d[2] * sn};
            float[] col = i == 1 ? alt : main;
            ring3d(buffer, c[0], c[1], c[2], uu[0], uu[1], uu[2], v[0], v[1], v[2], sizes[i], 0.9f, col, 0.25f * I, 96, null);
            ring3d(buffer, c[0], c[1], c[2], uu[0], uu[1], uu[2], v[0], v[1], v[2], sizes[i], 0.22f, col, 0.95f * I, 96,
                    a -> 0.75f + 0.25f * MathHelper.sin(a * 3f + t * 2f));
            ring3d(buffer, c[0], c[1], c[2], uu[0], uu[1], uu[2], v[0], v[1], v[2], sizes[i], 0.07f, WHITE, 0.9f * I, 96, null);
        }
        tessellator.draw();
    }

    /** A pale sun in the gold haze, throwing long rays that turn slowly. */
    private void goldenSun(Minecraft mc, Tessellator tessellator, WorldRenderer buffer, float I) {
        GlStateManager.enableTexture2D();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        mc.getTextureManager().bindTexture(SmokeFx.GLOW);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        billboard(buffer, GSX, GSY, GSZ, R * 0.95f, 18f, 0f, 0f, 0f, 1f, 1f, 1f, 0.8f, 0.45f, 0.55f * I);
        billboard(buffer, GSX, GSY, GSZ, R * 0.95f, 5.5f, 0f, 0f, 0f, 1f, 1f, 1f, 0.98f, 0.9f, 1f * I);
        tessellator.draw();
        mc.getTextureManager().bindTexture(SmokeFx.FX_ATLAS);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        for (int i = 0; i < 22; i++) {
            float rot = i * 360f / 22f + t * 1.5f;
            float len = 18f + RAND[i + 5] * 10f + 2f * MathHelper.sin(t * 0.7f + i);
            billboardStretched(buffer, GSX, GSY, GSZ, R * 0.95f, len, 1.1f, rot, 0.25f, 0f, 0.5f, 0.25f,
                    1f, 0.9f, 0.6f, 0.22f * I);
        }
        tessellator.draw();
        GlStateManager.disableTexture2D();
    }


    private static void bandAE(WorldRenderer buffer, float az0, float el0, float az1, float el1, float width,
                               float[] col, float a) {
        float c0 = MathHelper.cos(el0), c1 = MathHelper.cos(el1);
        band(buffer, MathHelper.cos(az0) * c0, MathHelper.sin(el0), MathHelper.sin(az0) * c0,
                MathHelper.cos(az1) * c1, MathHelper.sin(el1), MathHelper.sin(az1) * c1, R * 0.96f, width, col, a);
    }

    // ---- sun and moon ----------------------------------------------------------------------------

    /** Vanilla's sun and moon, at vanilla's angles, so the day still reads over a themed sky. */
    private static void celestials(Minecraft mc, Tessellator tessellator, WorldRenderer buffer, WorldClient world,
                                   float pt, float strength) {
        if (!world.provider.isSurfaceWorld()) return;
        float rain = 1f - world.getRainStrength(pt);
        GlStateManager.enableTexture2D();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GlStateManager.color(1f, 1f, 1f, rain * strength);
        GlStateManager.pushMatrix();
        GlStateManager.rotate(-90f, 0f, 1f, 0f);
        GlStateManager.rotate(world.getCelestialAngle(pt) * 360f, 1f, 0f, 0f);
        float d = R * 0.9f, s = 30f * R / 100f;
        mc.getTextureManager().bindTexture(SUN);
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        buffer.pos(-s, d, -s).tex(0, 0).endVertex();
        buffer.pos(s, d, -s).tex(1, 0).endVertex();
        buffer.pos(s, d, s).tex(1, 1).endVertex();
        buffer.pos(-s, d, s).tex(0, 1).endVertex();
        tessellator.draw();
        s = 20f * R / 100f;
        mc.getTextureManager().bindTexture(MOON);
        int phase = world.getMoonPhase();
        int k = phase % 4, l = phase / 4 % 2;
        float u0 = k / 4f, v0 = l / 2f, u1 = (k + 1) / 4f, v1 = (l + 1) / 2f;
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        buffer.pos(-s, -d, s).tex(u1, v1).endVertex();
        buffer.pos(s, -d, s).tex(u0, v1).endVertex();
        buffer.pos(s, -d, -s).tex(u0, v0).endVertex();
        buffer.pos(-s, -d, -s).tex(u1, v0).endVertex();
        tessellator.draw();
        GlStateManager.popMatrix();
        GlStateManager.color(1f, 1f, 1f, 1f);
        GlStateManager.disableTexture2D();
    }

    // ---- geometry helpers ------------------------------------------------------------------------

    /** A quad on the dome at direction (x, y, z), facing the centre, turned by {@code rotDeg}. */
    private static void billboard(WorldRenderer buffer, float x, float y, float z, float dist, float size, float rotDeg,
                                  float u0, float v0, float u1, float v1, float r, float g, float b, float a) {
        billboardStretched(buffer, x, y, z, dist, size, size, rotDeg, u0, v0, u1, v1, r, g, b, a);
    }

    private static void billboardStretched(WorldRenderer buffer, float x, float y, float z, float dist, float w, float h,
                                           float rotDeg, float u0, float v0, float u1, float v1,
                                           float r, float g, float b, float a) {
        if (a <= 0.003f) return;
        float[] u = SCRATCH_U, v = SCRATCH_V;
        float len = (float) Math.sqrt(x * x + y * y + z * z);
        SmokeFx.basis(x / len, y / len, z / len, u, v);
        float rad = (float) Math.toRadians(rotDeg);
        float c = MathHelper.cos(rad), s = MathHelper.sin(rad);
        float hw = w / 2f, hh = h / 2f;
        float ax = (u[0] * c + v[0] * s) * hw, ay = (u[1] * c + v[1] * s) * hw, az = (u[2] * c + v[2] * s) * hw;
        float bx = (-u[0] * s + v[0] * c) * hh, by = (-u[1] * s + v[1] * c) * hh, bz = (-u[2] * s + v[2] * c) * hh;
        float cx = x / len * dist, cy = y / len * dist, cz = z / len * dist;
        a = clamp(a);
        buffer.pos(cx - ax - bx, cy - ay - by, cz - az - bz).tex(u0, v1).color(r, g, b, a).endVertex();
        buffer.pos(cx + ax - bx, cy + ay - by, cz + az - bz).tex(u1, v1).color(r, g, b, a).endVertex();
        buffer.pos(cx + ax + bx, cy + ay + by, cz + az + bz).tex(u1, v0).color(r, g, b, a).endVertex();
        buffer.pos(cx - ax + bx, cy - ay + by, cz - az + bz).tex(u0, v0).color(r, g, b, a).endVertex();
    }

    private static final float[] SCRATCH_U = new float[3], SCRATCH_V = new float[3];

    /** A filled disc on the dome, as a fan of quads. */
    private static void disc(Tessellator tessellator, WorldRenderer buffer, float x, float y, float z, float dist,
                             float radius, float r, float g, float b, float a) {
        float[] u = new float[3], v = new float[3];
        float len = (float) Math.sqrt(x * x + y * y + z * z);
        SmokeFx.basis(x / len, y / len, z / len, u, v);
        float cx = x / len * dist, cy = y / len * dist, cz = z / len * dist;
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        int seg = 40;
        for (int i = 0; i < seg; i++) {
            float a0 = (float) (i * Math.PI * 2 / seg), a1 = (float) ((i + 1) * Math.PI * 2 / seg);
            buffer.pos(cx, cy, cz).color(r, g, b, a).endVertex();
            buffer.pos(cx, cy, cz).color(r, g, b, a).endVertex();
            buffer.pos(cx + (u[0] * MathHelper.cos(a0) + v[0] * MathHelper.sin(a0)) * radius,
                    cy + (u[1] * MathHelper.cos(a0) + v[1] * MathHelper.sin(a0)) * radius,
                    cz + (u[2] * MathHelper.cos(a0) + v[2] * MathHelper.sin(a0)) * radius).color(r, g, b, a).endVertex();
            buffer.pos(cx + (u[0] * MathHelper.cos(a1) + v[0] * MathHelper.sin(a1)) * radius,
                    cy + (u[1] * MathHelper.cos(a1) + v[1] * MathHelper.sin(a1)) * radius,
                    cz + (u[2] * MathHelper.cos(a1) + v[2] * MathHelper.sin(a1)) * radius).color(r, g, b, a).endVertex();
        }
        tessellator.draw();
    }

    /** A thin band between two dome directions, for constellation lines and lightning. */
    private static void band(WorldRenderer buffer, float x0, float y0, float z0, float x1, float y1, float z1,
                             float dist, float width, float[] col, float a) {
        float ax = x0 * dist, ay = y0 * dist, az = z0 * dist;
        float bx = x1 * dist, by = y1 * dist, bz = z1 * dist;
        float dx = bx - ax, dy = by - ay, dz = bz - az;
        // Across = segment x midpoint direction (which is the view ray from the centre).
        float mx = (ax + bx) / 2, my = (ay + by) / 2, mz = (az + bz) / 2;
        float sx = dy * mz - dz * my, sy = dz * mx - dx * mz, sz = dx * my - dy * mx;
        float len = (float) Math.sqrt(sx * sx + sy * sy + sz * sz);
        if (len < 1e-5f) return;
        sx = sx / len * width / 2;
        sy = sy / len * width / 2;
        sz = sz / len * width / 2;
        a = clamp(a);
        buffer.pos(ax - sx, ay - sy, az - sz).color(col[0], col[1], col[2], a).endVertex();
        buffer.pos(bx - sx, by - sy, bz - sz).color(col[0], col[1], col[2], a).endVertex();
        buffer.pos(bx + sx, by + sy, bz + sz).color(col[0], col[1], col[2], a).endVertex();
        buffer.pos(ax + sx, ay + sy, az + sz).color(col[0], col[1], col[2], a).endVertex();
    }

    // ---- maths -----------------------------------------------------------------------------------

    private static float n3(float x, float y, float z) {
        return MathHelper.sin(x * 1.7f + MathHelper.sin(y * 1.3f + z * 0.7f))
                * MathHelper.sin(y * 1.9f + MathHelper.sin(z * 1.1f - x * 0.6f))
                * MathHelper.sin(z * 1.5f + MathHelper.sin(x * 0.9f + y * 0.8f));
    }

    /** Smooth, cheap fractal noise in roughly [0, 1]. */
    private static float fbm(float x, float y, float z) {
        float v = 0.55f * n3(x, y, z) + 0.3f * n3(x * 2.1f + 5.2f, y * 2.1f + 1.3f, z * 2.1f + 7.1f)
                + 0.15f * n3(x * 4.3f + 1.7f, y * 4.3f + 9.2f, z * 4.3f + 3.3f);
        return 0.5f + 0.5f * v * 1.6f;
    }



    private static float line(float v, float width) {
        float f = v - (float) Math.floor(v);
        float d = Math.min(f, 1f - f);
        return 1f - smooth(0f, width, d);
    }


    private static float gauss(float d, float width) {
        float k = d / width;
        return (float) Math.exp(-k * k);
    }

    private static float dot(float ax, float ay, float az, float bx, float by, float bz) {
        float lb = (float) Math.sqrt(bx * bx + by * by + bz * bz);
        return (ax * bx + ay * by + az * bz) / lb;
    }

    private static float dist2(float ax, float ay, float az, float bx, float by, float bz) {
        float dx = ax - bx, dy = ay - by, dz = az - bz;
        return dx * dx + dy * dy + dz * dz;
    }

    private static float smooth(float e0, float e1, float x) {
        float t = clamp((x - e0) / (e1 - e0));
        return t * t * (3f - 2f * t);
    }

    private static float clamp(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }
}
