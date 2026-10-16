package me.Padej_.soupapi.modules;

import me.Padej_.soupapi.compat.BlendFactor;
import me.Padej_.soupapi.compat.BufferBuilder;
import me.Padej_.soupapi.compat.BufferRenderer;
import me.Padej_.soupapi.compat.Camera;
import me.Padej_.soupapi.compat.Cameras;
import me.Padej_.soupapi.compat.Mat4;
import me.Padej_.soupapi.compat.MatrixStack;
import me.Padej_.soupapi.compat.RenderSystem;
import me.Padej_.soupapi.compat.RotationAxis;
import me.Padej_.soupapi.compat.ShaderKey;
import me.Padej_.soupapi.compat.Tessellator;
import me.Padej_.soupapi.compat.VertexFormat;
import me.Padej_.soupapi.compat.VertexFormats;
import me.Padej_.soupapi.config.ConfigurableModule;
import me.Padej_.soupapi.event.WorldRenderContext;
import me.Padej_.soupapi.render.Render2D;
import me.Padej_.soupapi.render.Fx;
import me.Padej_.soupapi.utils.MathUtility;
import me.Padej_.soupapi.utils.Palette;
import me.Padej_.soupapi.utils.TexturesManager;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Cosmetic effects attached to a player.
 *
 * <p>Twelve styles, each drawn around the player it is attached to, with one shared set of controls
 * (style, who it applies to, size, speed, opacity, density) rather than a page of options per
 * effect. Everything is client-side: nobody else sees these.
 *
 * <p>Two kinds of effect live here. The geometric ones — orbits, helixes, rings, wings, crowns,
 * spheres — are computed from the world time each frame and keep no state at all, which is both
 * cheaper and impossible to leak. The emitted ones — flames, snow, sparks, footprints, shockwaves —
 * spawn particles on the tick and age them out. {@link Style#GHOST} is its own thing: it re-renders
 * the player model at the positions it occupied a moment ago.
 */
public class PlayerEffects extends ConfigurableModule {

    /** How the effect is attached. */
    public enum Target {
        SELF, OTHERS, BOTH
    }

    public enum Style {
        /** Fading copies of the player left behind at past positions. */
        GHOST,
        /** Sprites circling the player at waist height. */
        ORBIT,
        /** Two strands of sprites spiralling up around the player. */
        HELIX,
        /** Rings rising from the feet and shrinking as they go. */
        RINGS,
        /** A ring that expands across the ground each time the player lands. */
        SHOCKWAVE,
        /** Flames rising from the feet. */
        FLAME,
        /** Snow drifting down around the player. */
        SNOW,
        /** Sparks thrown outward from the feet, pulled down by gravity. */
        SPARKS,
        /** A pair of glowing wings behind the shoulders. */
        WINGS,
        /** Shards orbiting above the head. */
        CROWN,
        /** A wireframe sphere around the player, pulsing with the palette. */
        PULSE_SPHERE,
        /** Glowing marks left on the ground where the player steps. */
        FOOTPRINTS,
        /** Bubbles rising as if underwater. */
        BUBBLES,
        /** Petals drifting down and sideways. */
        PETALS,
        /** Slow embers rising and flickering out. */
        EMBERS,
        /** Stars falling from above and landing at the feet. */
        STARFALL,
        /** A spinning cone of sprites, wide at the top. */
        TORNADO,
        /** A rune disc spinning flat under the player. */
        MAGIC_CIRCLE,
        /** A pillar of light through the player. */
        BEAM,
        /** Rings that pulse outward from the chest in time with a heartbeat. */
        HEARTBEAT
    }

    // ---- state ------------------------------------------------------------------------------

    /** Past positions for {@link Style#GHOST}, per player. */
    private static final Map<Entity, List<Pose>> HISTORY = new WeakHashMap<Entity, List<Pose>>();
    /** Emitted particles, in world space. */
    private static final List<Puff> PUFFS = new ArrayList<Puff>();
    /** Ground contact state, for shockwaves and footprints. */
    private static final Map<Entity, Boolean> GROUNDED = new WeakHashMap<Entity, Boolean>();
    private static final Map<Entity, Double> WALKED = new WeakHashMap<Entity, Double>();

    /** Set while a ghost is being drawn, so the mod's own feature layers do not re-enter. */
    private static boolean drawingGhost;

    public static boolean isDrawingGhost() {
        return drawingGhost;
    }

    // ---- tick -------------------------------------------------------------------------------

    public static void onTick() {
        if (mc.theWorld == null || mc.thePlayer == null) return;
        if (!CONFIG.playerEffectsEnabled) {
            if (!PUFFS.isEmpty()) PUFFS.clear();
            if (!HISTORY.isEmpty()) HISTORY.clear();
            return;
        }

        for (int i = 0; i < PUFFS.size(); i++) {
            PUFFS.get(i).tick();
        }
        for (Iterator<Puff> it = PUFFS.iterator(); it.hasNext(); ) {
            if (it.next().dead()) it.remove();
        }

        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (!isTarget(player)) continue;
            trackHistory(player);
            emit(player);
        }
    }

    /**
     * Records a pose once the player has moved far enough from the last one.
     *
     * <p>Snapshots used to be taken on a tick interval, so at walking speed the copies were a few
     * centimetres apart and the effect read as a smeared running animation rather than a row of
     * clones. Recording by <i>distance</i> instead puts them a fixed gap apart no matter how fast
     * the player is going, which is what makes them look like a teleport trail.
     */
    private static void trackHistory(EntityPlayer player) {
        if (CONFIG.playerEffectsStyle != Style.GHOST) return;
        List<Pose> poses = HISTORY.get(player);
        if (poses == null) {
            poses = new ArrayList<Pose>();
            HISTORY.put(player, poses);
        }

        double spacing = Math.max(0.2, CONFIG.playerEffectsGhostSpacing / 10.0);
        if (!poses.isEmpty()) {
            Pose last = poses.get(poses.size() - 1);
            double dx = player.posX - last.x;
            double dy = player.posY - last.y;
            double dz = player.posZ - last.z;
            if (dx * dx + dy * dy + dz * dz < spacing * spacing) return;
        }

        poses.add(new Pose(player.posX, player.posY, player.posZ, player.renderYawOffset,
                player.limbSwing, player.limbSwingAmount, player.ticksExisted,
                player.isSneaking()));
        while (poses.size() > CONFIG.playerEffectsGhostCount + 1) {
            poses.remove(0);
        }
    }

    private static void emit(EntityPlayer player) {
        Style style = CONFIG.playerEffectsStyle;
        float scale = CONFIG.playerEffectsScale / 100f;
        int count = CONFIG.playerEffectsCount;

        switch (style) {
            case FLAME:
                for (int i = 0; i < Math.max(1, count / 4); i++) {
                    PUFFS.add(new Puff(player.posX + MathUtility.random(-0.35f, 0.35f) * scale,
                            player.posY + 0.1,
                            player.posZ + MathUtility.random(-0.35f, 0.35f) * scale,
                            MathUtility.random(-0.01f, 0.01f), MathUtility.random(0.04f, 0.09f),
                            MathUtility.random(-0.01f, 0.01f),
                            16, 0.45f * scale, TexturesManager.FIREFLY, 0f));
                }
                break;
            case SNOW:
                for (int i = 0; i < Math.max(1, count / 6); i++) {
                    PUFFS.add(new Puff(player.posX + MathUtility.random(-1.2f, 1.2f) * scale,
                            player.posY + 2.6,
                            player.posZ + MathUtility.random(-1.2f, 1.2f) * scale,
                            MathUtility.random(-0.01f, 0.01f), -0.035f,
                            MathUtility.random(-0.01f, 0.01f),
                            48, 0.3f * scale, TexturesManager.SNOWFLAKE, 1.5f));
                }
                break;
            case SPARKS:
                if (player.moveForward != 0f || player.moveStrafing != 0f) {
                    for (int i = 0; i < Math.max(1, count / 4); i++) {
                        PUFFS.add(new Puff(player.posX, player.posY + 0.05, player.posZ,
                                MathUtility.random(-0.12f, 0.12f), MathUtility.random(0.05f, 0.14f),
                                MathUtility.random(-0.12f, 0.12f),
                                20, 0.22f * scale, TexturesManager.STAR, -0.012f));
                    }
                }
                break;
            case FOOTPRINTS:
                trackFootprints(player, scale);
                break;
            case BUBBLES:
                for (int i = 0; i < Math.max(1, count / 6); i++) {
                    PUFFS.add(new Puff(player.posX + MathUtility.random(-0.5f, 0.5f) * scale,
                            player.posY + MathUtility.random(0.1f, 1.4f),
                            player.posZ + MathUtility.random(-0.5f, 0.5f) * scale,
                            MathUtility.random(-0.005f, 0.005f), MathUtility.random(0.02f, 0.05f),
                            MathUtility.random(-0.005f, 0.005f),
                            40, 0.22f * scale, TexturesManager.GUI_BUBBLE, 0f));
                }
                break;
            case PETALS:
                for (int i = 0; i < Math.max(1, count / 6); i++) {
                    PUFFS.add(new Puff(player.posX + MathUtility.random(-1.3f, 1.3f) * scale,
                            player.posY + 2.4,
                            player.posZ + MathUtility.random(-1.3f, 1.3f) * scale,
                            MathUtility.random(-0.03f, 0.03f), -0.025f,
                            MathUtility.random(-0.03f, 0.03f),
                            60, 0.28f * scale, TexturesManager.GLYPH_FLOWER, 1.2f));
                }
                break;
            case EMBERS:
                for (int i = 0; i < Math.max(1, count / 5); i++) {
                    PUFFS.add(new Puff(player.posX + MathUtility.random(-0.6f, 0.6f) * scale,
                            player.posY + MathUtility.random(0.2f, 1.2f),
                            player.posZ + MathUtility.random(-0.6f, 0.6f) * scale,
                            MathUtility.random(-0.015f, 0.015f), MathUtility.random(0.01f, 0.035f),
                            MathUtility.random(-0.015f, 0.015f),
                            44, 0.16f * scale, TexturesManager.FIREFLY, -0.0005f));
                }
                break;
            case STARFALL:
                for (int i = 0; i < Math.max(1, count / 8); i++) {
                    PUFFS.add(new Puff(player.posX + MathUtility.random(-1.5f, 1.5f) * scale,
                            player.posY + 3.2,
                            player.posZ + MathUtility.random(-1.5f, 1.5f) * scale,
                            0, -0.12f, 0,
                            30, 0.3f * scale, TexturesManager.STAR, -0.004f));
                }
                break;
            case HEARTBEAT:
                // Two beats per second, like a pulse.
                if ((mc.theWorld.getTotalWorldTime() % 20L) == 0L
                        || (mc.theWorld.getTotalWorldTime() % 20L) == 6L) {
                    PUFFS.add(new Puff(player.posX, player.posY + player.height * 0.6,
                            player.posZ, 0, 0, 0, 14, 0.3f * scale,
                            TexturesManager.HEART, 0f).expanding(1.6f * scale));
                }
                break;
            case SHOCKWAVE:
                trackLanding(player, scale);
                break;
            default:
                break;
        }
    }

    private static void trackFootprints(EntityPlayer player, float scale) {
        Double last = WALKED.get(player);
        double walked = player.distanceWalkedModified;
        if (last != null && walked - last > 0.9 && player.onGround) {
            PUFFS.add(new Puff(player.posX, player.posY + 0.02, player.posZ,
                    0, 0, 0, 30, 0.7f * scale, TexturesManager.GLYPH_CIRCLE, 0f).flat());
        }
        if (last == null || walked - last > 0.9) {
            WALKED.put(player, walked);
        }
    }

    private static void trackLanding(EntityPlayer player, float scale) {
        Boolean wasGrounded = GROUNDED.get(player);
        boolean grounded = player.onGround;
        if (wasGrounded != null && !wasGrounded && grounded) {
            PUFFS.add(new Puff(player.posX, player.posY + 0.05, player.posZ,
                    0, 0, 0, 18, 0.5f * scale, null, 0f).expanding(3.2f * scale));
        }
        GROUNDED.put(player, grounded);
    }

    public static boolean isTarget(Entity entity) {
        boolean self = entity == mc.thePlayer;
        switch (CONFIG.playerEffectsTarget) {
            case SELF:
                return self;
            case OTHERS:
                return !self;
            case BOTH:
            default:
                return true;
        }
    }

    // ---- rendering --------------------------------------------------------------------------

    public static void render(WorldRenderContext context) {
        if (!CONFIG.playerEffectsEnabled) return;
        if (mc.theWorld == null || mc.thePlayer == null) return;

        float tickDelta = context.tickCounter().getTickDelta(true);
        Style style = CONFIG.playerEffectsStyle;

        if (style == Style.GHOST) {
            renderGhosts(tickDelta);
            return;
        }

        // The emitted styles draw their own particles; the geometric ones are computed here.
        List<Puff> puffs = PUFFS;
        if (!puffs.isEmpty()) {
            renderPuffs(tickDelta);
        }

        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (!isTarget(player)) continue;
            if (player == mc.thePlayer && mc.gameSettings.thirdPersonView == 0
                    && !CONFIG.playerEffectsFirstPerson) {
                continue;
            }
            double x = interp(player.lastTickPosX, player.posX, tickDelta);
            double y = interp(player.lastTickPosY, player.posY, tickDelta);
            double z = interp(player.lastTickPosZ, player.posZ, tickDelta);
            renderGeometric(style, player, x, y, z, tickDelta);
        }
    }

    private static void renderGeometric(Style style, EntityPlayer player,
                                        double x, double y, double z, float tickDelta) {
        float scale = CONFIG.playerEffectsScale / 100f;
        float speed = CONFIG.playerEffectsSpeed / 100f;
        int count = Math.max(3, CONFIG.playerEffectsCount);
        double time = (mc.theWorld.getTotalWorldTime() + tickDelta) * 0.08 * speed;
        float alpha = CONFIG.playerEffectsAlpha / 100f;
        float height = player.height;

        switch (style) {
            case ORBIT: {
                List<Puff> sprites = new ArrayList<Puff>();
                for (int i = 0; i < count; i++) {
                    double angle = time + i * (Math.PI * 2 / count);
                    double radius = 0.9 * scale;
                    sprites.add(Puff.still(x + Math.cos(angle) * radius,
                            y + height * 0.55 + Math.sin(time * 2 + i) * 0.1,
                            z + Math.sin(angle) * radius,
                            0.3f * scale, colour(i / (float) count), alpha));
                }
                drawSprites(sprites, TexturesManager.FIREFLY, tickDelta);
                break;
            }
            case HELIX: {
                List<Puff> sprites = new ArrayList<Puff>();
                int perStrand = Math.max(4, count);
                for (int strand = 0; strand < 2; strand++) {
                    for (int i = 0; i < perStrand; i++) {
                        double t = i / (double) perStrand;
                        double angle = time * 2 + t * Math.PI * 4 + strand * Math.PI;
                        double radius = 0.7 * scale * (1.0 - t * 0.3);
                        sprites.add(Puff.still(x + Math.cos(angle) * radius,
                                y + t * (height + 0.4),
                                z + Math.sin(angle) * radius,
                                0.22f * scale, colour((float) t), alpha));
                    }
                }
                drawSprites(sprites, TexturesManager.FIREFLY, tickDelta);
                break;
            }
            case RINGS: {
                for (int ring = 0; ring < 3; ring++) {
                    double phase = ((time * 0.5) + ring / 3.0) % 1.0;
                    double ringY = y + phase * (height + 0.5);
                    double radius = (0.95 - phase * 0.55) * scale;
                    ring(x, ringY, z, radius, count * 2, colour((float) phase), alpha * (1f - (float) phase));
                }
                break;
            }
            case WINGS: {
                double yaw = Math.toRadians(interp(player.prevRenderYawOffset,
                        player.renderYawOffset, tickDelta));
                double backX = Math.sin(yaw) * 0.18;
                double backZ = -Math.cos(yaw) * 0.18;
                double baseY = y + height * 0.72;
                int feathers = Math.max(4, count);
                for (int side = -1; side <= 1; side += 2) {
                    for (int i = 0; i < feathers; i++) {
                        double spread = (i + 1) / (double) feathers;
                        double flap = Math.sin(time * 3) * 0.25 + 0.6;
                        double tipX = x + backX + Math.cos(yaw) * side * spread * 1.4 * scale * flap;
                        double tipZ = z + backZ + Math.sin(yaw) * side * spread * 1.4 * scale * flap;
                        double tipY = baseY + spread * 0.9 * scale - Math.pow(spread, 2) * 0.4;
                        line(x + backX, baseY, z + backZ, tipX, tipY, tipZ,
                                colour((float) spread), alpha);
                    }
                }
                break;
            }
            case CROWN: {
                List<Puff> sprites = new ArrayList<Puff>();
                int shards = Math.max(3, count / 2);
                for (int i = 0; i < shards; i++) {
                    double angle = -time * 1.5 + i * (Math.PI * 2 / shards);
                    double radius = 0.45 * scale;
                    sprites.add(Puff.still(x + Math.cos(angle) * radius,
                            y + height + 0.35 + Math.sin(time * 2 + i) * 0.05,
                            z + Math.sin(angle) * radius,
                            0.25f * scale, colour(i / (float) shards), alpha));
                }
                drawSprites(sprites, TexturesManager.GLYPH_TRIANGLE, tickDelta);
                break;
            }
            case TORNADO: {
                List<Puff> sprites = new ArrayList<Puff>();
                int rings = Math.max(3, count / 2);
                for (int r = 0; r < rings; r++) {
                    double t = r / (double) rings;
                    double radius = (0.15 + t * 1.1) * scale;
                    int per = 3 + r / 2;
                    for (int i = 0; i < per; i++) {
                        double angle = time * 4 + i * (Math.PI * 2 / per) + t * 3;
                        sprites.add(Puff.still(x + Math.cos(angle) * radius,
                                y + t * (height + 1.0),
                                z + Math.sin(angle) * radius,
                                0.2f * scale, colour((float) t), alpha));
                    }
                }
                drawSprites(sprites, TexturesManager.FIREFLY, tickDelta);
                break;
            }
            case MAGIC_CIRCLE: {
                // Two counter-rotating rune rings on the ground, plus a slow outer ring.
                int runes = Math.max(4, count);
                List<Puff> sprites = new ArrayList<Puff>();
                for (int i = 0; i < runes; i++) {
                    double angle = time * 1.2 + i * (Math.PI * 2 / runes);
                    double radius = 1.1 * scale;
                    sprites.add(Puff.still(x + Math.cos(angle) * radius, y + 0.03,
                            z + Math.sin(angle) * radius, 0.3f * scale,
                            colour(i / (float) runes), alpha).flat());
                }
                for (int i = 0; i < runes; i++) {
                    double angle = -time * 0.8 + i * (Math.PI * 2 / runes) + 0.4;
                    double radius = 0.6 * scale;
                    sprites.add(Puff.still(x + Math.cos(angle) * radius, y + 0.03,
                            z + Math.sin(angle) * radius, 0.22f * scale,
                            colour(0.5f + i / (float) runes), alpha).flat());
                }
                drawSprites(sprites, TexturesManager.getRandomGlyphParticle(), tickDelta);
                ring(x, y + 0.02, z, 1.35 * scale, runes * 3, colour(0f), alpha);
                break;
            }
            case BEAM: {
                // A column of rings, brightest at the feet.
                int slices = Math.max(6, count * 2);
                for (int i = 0; i < slices; i++) {
                    double t = i / (double) slices;
                    double radius = (0.45 - t * 0.2) * scale;
                    double beamY = y + t * 6.0;
                    ring(x, beamY, z, radius, 10, colour((float) t + (float) time * 0.1f),
                            alpha * (1f - (float) t) * 0.8f);
                }
                break;
            }
            case PULSE_SPHERE: {
                double pulse = 0.8 + Math.sin(time * 2) * 0.15;
                double radius = 0.95 * scale * pulse;
                double centreY = y + height * 0.5;
                ring(x, centreY, z, radius, count * 2, colour(0f), alpha);
                verticalRing(x, centreY, z, radius, count * 2, 0.0, colour(0.33f), alpha);
                verticalRing(x, centreY, z, radius, count * 2, Math.PI / 2, colour(0.66f), alpha);
                break;
            }
            default:
                break;
        }
    }

    /**
     * Draws the player's own model at the positions it held a moment ago.
     *
     * <p>Three rules keep this usable in a fight rather than in the way:
     * <ul>
     *   <li>Ghosts of <i>your own</i> player are only drawn <b>behind</b> you — a ghost standing in
     *       front of your camera is a wall between you and whoever you are hitting.</li>
     *   <li>Nothing is drawn within a couple of blocks of the camera. A model that intersects the
     *       near plane fills the screen, and with additive blending that means an entirely white
     *       screen, which is what happened when a ghost caught up with its owner.</li>
     *   <li>Ghosts have to be a real distance apart from one another, so they read as a few
     *       discrete copies that jump behind you rather than a continuous smear.</li>
     * </ul>
     */
    private static void renderGhosts(float tickDelta) {
        if (drawingGhost) return;
        float alpha = CONFIG.playerEffectsAlpha / 100f;

        Vec3 cameraPos = Cameras.get().getPos();
        Vec3 look = mc.thePlayer.getLookVec();

        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (!isTarget(player)) continue;
            List<Pose> poses = HISTORY.get(player);
            if (poses == null || poses.isEmpty()) continue;

            boolean self = player == mc.thePlayer;
            int drawn = 0;

            // The history is already one entry per step, newest last.
            for (int i = poses.size() - 1; i >= 0 && drawn < CONFIG.playerEffectsGhostCount; i--) {
                Pose pose = poses.get(i);

                double toCameraX = pose.x - cameraPos.xCoord;
                double toCameraY = pose.y - cameraPos.yCoord;
                double toCameraZ = pose.z - cameraPos.zCoord;
                double distanceSq = toCameraX * toCameraX + toCameraY * toCameraY
                        + toCameraZ * toCameraZ;

                // Fade in with distance instead of popping in at a cutoff. A model across the
                // near plane fills the screen - which is what whited everything out - but hiding
                // anything close also meant the ghost never appeared to separate from its owner.
                // Fading it means the copy forms as the player walks out of it.
                double distance = Math.sqrt(distanceSq);
                float proximity = (float) Math.max(0.0, Math.min(1.0, (distance - 0.55) / 1.75));
                if (proximity <= 0.02f) continue;

                if (self) {
                    // Behind only - a ghost in front of you is a wall between you and your target.
                    double dot = toCameraX * look.xCoord + toCameraY * look.yCoord
                            + toCameraZ * look.zCoord;
                    if (dot > 0) continue;
                }

                float age = (poses.size() - i) / (float) Math.max(1, poses.size());
                drawGhost(player, pose, colour(age), alpha * (1f - age * 0.6f) * proximity, tickDelta);
                drawn++;
            }
        }
    }

    /**
     * Draws one ghost.
     *
     * <p>This renders the player's <b>model</b> rather than re-running the entity renderer. Going
     * through the renderer meant the ghost came out white (the renderer resets the colour before
     * drawing), carried a held item drawn with the wrong state, and brought a name tag and a drop
     * shadow with it. The model on its own takes the colour it is given and has no held item,
     * armour, name or shadow to begin with — the held item and armour are separate feature layers.
     *
     * <p>The transform mirrors {@code RendererLivingEntity.doRender}: translate, turn to face the
     * body yaw, flip Y (entity models are authored upside down) and drop by the model's own height
     * offset.
     */
    private static void drawGhost(EntityPlayer player, Pose pose, Color colour, float alpha,
                                  float tickDelta) {
        net.minecraft.client.renderer.entity.RenderManager manager = mc.getRenderManager();
        if (manager == null) return;

        net.minecraft.client.renderer.entity.Render<?> render = manager.getEntityRenderObject(player);
        if (!(render instanceof net.minecraft.client.renderer.entity.RenderPlayer)) return;
        net.minecraft.client.model.ModelPlayer model =
                ((net.minecraft.client.renderer.entity.RenderPlayer) render).getMainModel();
        if (model == null) return;

        drawingGhost = true;
        GlStateManager.pushMatrix();
        try {
            GlStateManager.enableBlend();
            // Additive: the ghost glows and never darkens what is behind it.
            GlStateManager.blendFunc(770, 1);
            GlStateManager.depthMask(false);
            GlStateManager.disableLighting();
            GlStateManager.disableTexture2D();
            GlStateManager.color(colour.getRed() / 255f, colour.getGreen() / 255f,
                    colour.getBlue() / 255f, Math.max(0.02f, alpha));

            GlStateManager.translate(pose.x - manager.viewerPosX,
                    pose.y - manager.viewerPosY,
                    pose.z - manager.viewerPosZ);
            GlStateManager.rotate(180.0f - pose.bodyYaw, 0.0f, 1.0f, 0.0f);
            GlStateManager.scale(-1.0f, -1.0f, 1.0f);
            GlStateManager.translate(0.0f, -1.5078125f, 0.0f);

            model.isSneak = pose.sneaking;
            model.isRiding = false;
            model.isChild = false;
            model.swingProgress = 0.0f;
            // A still stance. Feeding the recorded limb swing in made each copy mid-stride, so a
            // row of them read as one running animation instead of a trail of teleports.
            model.render(player, 0.0f, 0.0f, pose.age, 0.0f, 0.0f, 0.0625f);
        } catch (Throwable ignored) {
            // A model that cannot be drawn is not worth a crash.
        } finally {
            model.isSneak = false;
            GlStateManager.enableTexture2D();
            GlStateManager.enableLighting();
            GlStateManager.depthMask(true);
            GlStateManager.color(1f, 1f, 1f, 1f);
            RenderSystem.defaultBlendFunc();
            GlStateManager.disableBlend();
            GlStateManager.popMatrix();
            drawingGhost = false;
        }
    }

    // ---- primitives -------------------------------------------------------------------------

    private static void renderPuffs(float tickDelta) {
        List<Puff> textured = new ArrayList<Puff>();
        for (int i = 0; i < PUFFS.size(); i++) {
            Puff puff = PUFFS.get(i);
            if (puff.texture == null) {
                // An expanding ground ring.
                double progress = puff.progress(tickDelta);
                ring(puff.x, puff.y, puff.z, puff.size + progress * puff.expandTo,
                        CONFIG.playerEffectsCount * 2, colour((float) progress),
                        (CONFIG.playerEffectsAlpha / 100f) * (1f - (float) progress));
            } else {
                textured.add(puff);
            }
        }
        if (!textured.isEmpty()) {
            drawSprites(textured, textured.get(0).texture, tickDelta);
        }
    }

    /** One batched draw of billboarded sprites. */
    private static void drawSprites(List<Puff> sprites, ResourceLocation texture, float tickDelta) {
        if (sprites.isEmpty()) return;
        Camera camera = Cameras.get();
        Vec3 cameraPos = camera.getPositionVector();

        Render2D.setupRender();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.enableDepthTest();
        RenderSystem.blendFunc(BlendFactor.SrcFactor.SRC_ALPHA, BlendFactor.DstFactor.ONE);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShader(ShaderKey.POSITION_TEX_COLOR);

        BufferBuilder buffer = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        for (int i = 0; i < sprites.size(); i++) {
            Puff sprite = sprites.get(i);
            double px = sprite.renderX(tickDelta) - cameraPos.xCoord;
            double py = sprite.renderY(tickDelta) - cameraPos.yCoord;
            double pz = sprite.renderZ(tickDelta) - cameraPos.zCoord;

            MatrixStack matrices = new MatrixStack();
            matrices.translate(px, py, pz);
            if (sprite.flat) {
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90f));
            } else {
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
            }
            Mat4 matrix = matrices.peek().getPositionMatrix();

            float half = sprite.renderSize(tickDelta) / 2f;
            Color colour = sprite.colour != null ? sprite.colour : colour((float) sprite.progress(tickDelta));
            int a = Math.round(255 * Math.max(0f, Math.min(1f, sprite.renderAlpha(tickDelta))));

            buffer.vertex(matrix, -half, -half, 0f).texture(0f, 1f)
                    .color(colour.getRed(), colour.getGreen(), colour.getBlue(), a);
            buffer.vertex(matrix, half, -half, 0f).texture(1f, 1f)
                    .color(colour.getRed(), colour.getGreen(), colour.getBlue(), a);
            buffer.vertex(matrix, half, half, 0f).texture(1f, 0f)
                    .color(colour.getRed(), colour.getGreen(), colour.getBlue(), a);
            buffer.vertex(matrix, -half, half, 0f).texture(0f, 0f)
                    .color(colour.getRed(), colour.getGreen(), colour.getBlue(), a);
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        Render2D.endRender();
    }

    /**
     * A glowing horizontal band.
     *
     * <p>These used to be built from GL lines. gl4es - what PojavLauncher runs - ignores
     * {@code glLineWidth}, so every one of them arrived on a phone as a one-pixel hairline, which
     * is why the ring-shaped effects looked so thin. {@link Fx} builds real geometry instead.
     */
    private static void ring(double x, double y, double z, double radius, int segments,
                             Color colour, float alpha) {
        float scale = CONFIG.playerEffectsScale / 100f;
        Fx.glowRing(x, y, z, radius, Math.max(0.06, radius * 0.18) * scale,
                Math.max(16, segments), 0.0, 0.0, Fx.withAlpha(colour, alpha));
    }

    /** A glowing band standing on its edge. */
    private static void verticalRing(double x, double y, double z, double radius, int segments,
                                     double tilt, Color colour, float alpha) {
        Fx.glowRing(x, y, z, radius, Math.max(0.06, radius * 0.18), Math.max(16, segments),
                Math.PI / 2, tilt, Fx.withAlpha(colour, alpha));
    }

    private static void line(double x1, double y1, double z1, double x2, double y2, double z2,
                             Color colour, float alpha) {
        Fx.glowLine(x1, y1, z1, x2, y2, z2, 0.09 * (CONFIG.playerEffectsScale / 100f),
                Fx.withAlpha(colour, alpha));
    }

    private static Color colour(float position) {
        if (CONFIG.playerEffectsRandomColor) {
            return Palette.getRandomColor();
        }
        float p = position % 1f;
        return Palette.getColor(p < 0 ? p + 1f : p);
    }

    private static double interp(double previous, double current, float tickDelta) {
        return previous + (current - previous) * tickDelta;
    }

    private static float interp(float previous, float current, float tickDelta) {
        return previous + (current - previous) * tickDelta;
    }

    // ---- value types ------------------------------------------------------------------------

    /** Where a player was and how it was standing, for ghosts. */
    private static final class Pose {
        final double x;
        final double y;
        final double z;
        final float bodyYaw;
        final float limbSwing;
        final float limbSwingAmount;
        final float age;
        final boolean sneaking;

        Pose(double x, double y, double z, float bodyYaw, float limbSwing, float limbSwingAmount,
             float age, boolean sneaking) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.bodyYaw = bodyYaw;
            this.limbSwing = limbSwing;
            this.limbSwingAmount = limbSwingAmount;
            this.age = age;
            this.sneaking = sneaking;
        }
    }

    /** An emitted particle, or a one-off sprite for a geometric style. */
    private static final class Puff {
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
        int life;
        int maxLife;
        float size;
        float expandTo;
        boolean flat;
        ResourceLocation texture;
        Color colour;
        float fixedAlpha = -1f;

        Puff(double x, double y, double z, double motionX, double motionY, double motionZ,
             int life, float size, ResourceLocation texture, double gravity) {
            this.x = this.prevX = x;
            this.y = this.prevY = y;
            this.z = this.prevZ = z;
            this.motionX = motionX;
            this.motionY = motionY;
            this.motionZ = motionZ;
            this.life = this.maxLife = life;
            this.size = size;
            this.texture = texture;
            this.gravity = gravity;
        }

        static Puff still(double x, double y, double z, float size, Color colour, float alpha) {
            Puff puff = new Puff(x, y, z, 0, 0, 0, 1, size, null, 0);
            puff.colour = colour;
            puff.fixedAlpha = alpha;
            return puff;
        }

        Puff flat() {
            this.flat = true;
            return this;
        }

        Puff expanding(float to) {
            this.expandTo = to;
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
            motionX *= 0.96;
            motionZ *= 0.96;
            life--;
        }

        boolean dead() {
            return life <= 0;
        }

        double progress(float tickDelta) {
            if (maxLife <= 0) return 0.0;
            double aged = maxLife - (life - tickDelta);
            return Math.max(0.0, Math.min(1.0, aged / maxLife));
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
            return expandTo > 0f ? size + (float) progress(tickDelta) * expandTo : size;
        }

        float renderAlpha(float tickDelta) {
            if (fixedAlpha >= 0f) return fixedAlpha;
            return (CONFIG.playerEffectsAlpha / 100f) * (1f - (float) progress(tickDelta));
        }
    }
}
