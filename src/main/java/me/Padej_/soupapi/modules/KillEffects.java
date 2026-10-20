package me.Padej_.soupapi.modules;

import me.Padej_.soupapi.config.ConfigurableModule;
import me.Padej_.soupapi.event.AttackEntityCallback;
import me.Padej_.soupapi.render.Fx;
import me.Padej_.soupapi.render.FxParticles;
import me.Padej_.soupapi.render.SmokeFx;
import me.Padej_.soupapi.utils.EntityUtils;
import me.Padej_.soupapi.utils.MathUtility;
import me.Padej_.soupapi.utils.Palette;
import me.Padej_.soupapi.utils.TexturesManager;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;

import java.awt.Color;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * What happens where something dies.
 *
 * <p><b>Finding the death.</b> The earlier version polled the entity list and fired only for an
 * entity still in it with no health left. That misses most kills that matter: the server's destroy
 * packet routinely lands in the same tick as the death, and plenty of servers remove a killed player
 * without ever sending a death status. It also assumed the client gets no death notification at all,
 * which is not so — entity status 3 runs {@code onDeath()} on the client thread, and Forge raises
 * {@code LivingDeathEvent} from inside it while the entity is still in the world. That event is now
 * the primary trigger.
 *
 * <p>Two fallbacks cover servers that never send it, without reading every disappearance as a kill:
 * an entity that vanishes is counted only if it was hurt in the last few ticks and was either low or
 * just hit by the player, and was well inside tracking range (leaving an area unloads entities at
 * the edge, unhurt); and a player whose health refills while they teleport away right after taking
 * damage is a server-side respawn, so the effect fires where they fell. Every trigger goes through
 * one de-duplicating gate, and the whole state belongs to a world and is dropped with it.
 *
 * <p><b>The effects.</b> Each style is its own choreography with its own timing and stages, run by a
 * small sequencer: the void implosion pulls in before it rings out, crystals grow before they
 * detonate, a rift opens, drinks the body and snaps shut. They share the particle pools and the
 * layered smoke rather than each carrying its own renderer.
 */
public class KillEffects extends ConfigurableModule {

    public enum Style {
        /** A column of flame rising from the body. */
        FLAME_PILLAR,
        /** A sphere of sprites thrown outward, then drawn back in. */
        SOUL_BURST,
        /** A fast, wide blast with a ground ring. */
        EXPLOSION,
        /** A flat ring that opens on the ground, like a closing portal. */
        PORTAL,
        /** A spinning funnel that narrows as it rises. */
        VORTEX,
        /** A hard flash with falling sparks. */
        LIGHTNING,
        /** Three rings rising one after another. */
        RINGS,
        /** Slow embers drifting up and out. */
        ASH,
        /** Black smoke boils off the body and is drawn, with the soul's light, into its killer. */
        SOUL_SIPHON,
        /** The body cracks like glass, hangs a moment, and collapses in shards. */
        FRACTURED_GLASS,
        /** Spectral feathers spiral up out of the body and scatter at the top. */
        RAVEN_ASCENSION,
        /** Everything is pulled into a dark point, which then rings out. */
        VOID_IMPLOSION,
        /** Runes turn on the ground and around the body, then a beam falls. */
        ARCANE_EXECUTION,
        /** Ghost copies of the body drift apart and fade, trailing smoke. */
        PHANTOM_AFTERIMAGES,
        /** A dark disc blooms a corona, flares, and collapses. */
        ECLIPSE_BLOOM,
        /** Crystals grow out of the body, glow, and burst. */
        CRYSTAL_DETONATION,
        /** Motes rise into stars that draw a constellation above the body. */
        SOUL_CONSTELLATION,
        /** A fountain of molten sparks and smoke out of a glowing crack. */
        MOLTEN_ERUPTION,
        /** A rift opens beside the body, drinks it, and snaps shut. */
        RIFT_CLOSURE,
        /** A crown of runes turns above the head, rises, and dissolves. */
        RUNIC_CROWN
    }

    /** What was last known about a living entity in range. */
    private static final class Track {
        WeakReference<EntityLivingBase> ref;
        double x, y, z;
        float width, height, health, maxHealth;
        int lastHurt = -10000;
        boolean player;
        int seen;
    }

    private static final Map<Integer, Track> TRACKED = new HashMap<Integer, Track>();
    /** Entity id → tick the effect fired, so no trigger can fire twice for one death. */
    private static final Map<Integer, Integer> FIRED = new HashMap<Integer, Integer>();
    /** Entity id → tick the player last hit it. */
    private static final Map<Integer, Integer> ATTACKED = new HashMap<Integer, Integer>();
    private static final List<Sequence> ACTIVE = new ArrayList<Sequence>();
    private static final Random RANDOM = new Random();
    private static Object lastWorld;
    private static int tick;

    // ---- triggers --------------------------------------------------------------------------------

    /** Records the player's own hits, so "recently hit by me" can vouch for a disappearance. */
    public static void register() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (entity != null && player == mc.thePlayer) ATTACKED.put(entity.getEntityId(), tick);
            return AttackEntityCallback.Result.PASS;
        });
    }

    /** Forge's death event, raised on the client from the death-status packet. The primary trigger. */
    public static void onDeath(EntityLivingBase entity) {
        if (!CONFIG.killEffectsEnabled || entity == null || mc.theWorld == null) return;
        if (!eligible(entity)) return;
        fire(entity.getEntityId(), entity, entity.posX, entity.posY, entity.posZ, entity.width, entity.height);
    }

    /** The world went away: everything here belonged to it. */
    public static void onWorldUnload() {
        clear();
        lastWorld = null;
    }

    private static void clear() {
        TRACKED.clear();
        FIRED.clear();
        ATTACKED.clear();
        ACTIVE.clear();
    }

    public static void onTick() {
        Object world = mc.theWorld;
        if (world != lastWorld) {
            clear();
            lastWorld = world;
        }
        if (mc.theWorld == null || mc.thePlayer == null) return;
        tick++;
        if (!CONFIG.killEffectsEnabled) {
            if (!TRACKED.isEmpty() || !ACTIVE.isEmpty()) clear();
            return;
        }

        for (Object o : mc.theWorld.loadedEntityList) {
            if (!(o instanceof EntityLivingBase)) continue;
            EntityLivingBase living = (EntityLivingBase) o;
            if (!eligible(living)) continue;
            observe(living);
        }

        // Whatever was tracked and was not seen this tick has left the world.
        for (Iterator<Map.Entry<Integer, Track>> it = TRACKED.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, Track> entry = it.next();
            Track track = entry.getValue();
            if (track.seen == tick) continue;
            it.remove();
            if (removalWasKill(entry.getKey(), track)) {
                fire(entry.getKey(), track.ref.get(), track.x, track.y, track.z, track.width, track.height);
            }
        }

        // Forget old bookkeeping so the maps cannot grow over a long session.
        for (Iterator<Map.Entry<Integer, Integer>> it = FIRED.entrySet().iterator(); it.hasNext(); ) {
            if (tick - it.next().getValue() > 200) it.remove();
        }
        for (Iterator<Map.Entry<Integer, Integer>> it = ATTACKED.entrySet().iterator(); it.hasNext(); ) {
            if (tick - it.next().getValue() > 200) it.remove();
        }

        for (Iterator<Sequence> it = ACTIVE.iterator(); it.hasNext(); ) {
            Sequence s = it.next();
            s.step();
            if (s.age >= s.life) it.remove();
        }
    }

    private static void observe(EntityLivingBase living) {
        int id = living.getEntityId();
        Track track = TRACKED.get(id);
        float health = living.getHealth();
        float max = Math.max(1f, living.getMaxHealth());
        if (track == null) {
            track = new Track();
            track.ref = new WeakReference<EntityLivingBase>(living);
            TRACKED.put(id, track);
        } else {
            if (health < track.health - 0.01f || living.hurtTime > 0) track.lastHurt = tick;
            // A server-side respawn: hurt a moment ago, then full health somewhere else entirely.
            if (CONFIG.killEffectsDetectRespawn && track.player && health > 0f
                    && tick - track.lastHurt <= 40 && track.health <= max * 0.35f && health >= max * 0.9f) {
                double dx = living.posX - track.x, dy = living.posY - track.y, dz = living.posZ - track.z;
                if (dx * dx + dy * dy + dz * dz > 36.0) {
                    fire(id, living, track.x, track.y, track.z, track.width, track.height);
                    FIRED.put(id, tick - 140); // the respawned player can die again soon
                    track.lastHurt = -10000;
                }
            }
        }
        if (health <= 0f || living.isDead) {
            // Dead but still here: the status arrived without Forge's event reaching us.
            if (track.health > 0f) fire(id, living, living.posX, living.posY, living.posZ, living.width, living.height);
        }
        track.x = living.posX;
        track.y = living.posY;
        track.z = living.posZ;
        track.width = living.width;
        track.height = living.height;
        track.health = health;
        track.maxHealth = max;
        track.player = living instanceof EntityPlayer;
        track.seen = tick;
    }

    /** Whether an entity leaving the world was a death, rather than walking out of range. */
    private static boolean removalWasKill(int id, Track track) {
        if (FIRED.containsKey(id)) return false;
        if (track.health <= 0f) return true;
        // The object outlives its removal from the world, and it may know more than the last
        // snapshot did: a death and a removal arriving in the same tick never reach observe().
        EntityLivingBase live = track.ref.get();
        if (live != null) {
            if (live.getHealth() <= 0f) return true;
            if (live.hurtTime > 0) track.lastHurt = tick;
            track.health = Math.min(track.health, live.getHealth());
        }
        if (!CONFIG.killEffectsDetectRemoval) return false;
        boolean hurtJustNow = tick - track.lastHurt <= 12;
        if (!hurtJustNow) return false;
        Integer hitByMe = ATTACKED.get(id);
        boolean mine = hitByMe != null && tick - hitByMe <= 40;
        boolean low = track.health <= track.maxHealth * 0.35f;
        if (!mine && !low) return false;
        double dx = track.x - mc.thePlayer.posX, dy = track.y - mc.thePlayer.posY, dz = track.z - mc.thePlayer.posZ;
        return dx * dx + dy * dy + dz * dz <= 40.0 * 40.0;
    }

    private static boolean eligible(EntityLivingBase living) {
        if (living == mc.thePlayer && !CONFIG.killEffectsIncludeSelf) return false;
        if (CONFIG.killEffectsOnlyPlayers && !(living instanceof EntityPlayer)) return false;
        if (EntityUtils.isSpectator(living)) return false;
        if (CONFIG.killEffectsOnlyMine && living != mc.thePlayer) {
            Integer hit = ATTACKED.get(living.getEntityId());
            if (hit == null || tick - hit > 100) return false;
        }
        return living.getDistanceSqToEntity(mc.thePlayer) <= 64.0 * 64.0;
    }

    private static void fire(int id, Entity entity, double x, double y, double z, float width, float height) {
        if (FIRED.containsKey(id)) return;
        FIRED.put(id, tick);
        Palette.Element previous = Palette.scope(Palette.Element.PARTICLES);
        try {
            spawnAt(entity, x, y, z, width, height);
        } finally {
            Palette.scope(previous);
        }
    }

    /** Plays the configured effect on an entity where it stands. Kept for callers outside the tracker. */
    public static void spawn(Entity target) {
        if (target == null) return;
        spawnAt(target, target.posX, target.posY, target.posZ, target.width, target.height);
    }

    // ---- the effects -----------------------------------------------------------------------------

    private static void spawnAt(Entity target, double x, double y, double z, float width, float height) {
        float scale = CONFIG.killEffectsScale / 100f;
        int count = CONFIG.killEffectsCount;
        int life = Math.max(8, CONFIG.killEffectsDuration);
        Style style = CONFIG.killEffectsStyle == null ? Style.SOUL_BURST : CONFIG.killEffectsStyle;
        switch (style) {
            case FLAME_PILLAR:
            case SOUL_BURST:
            case EXPLOSION:
            case PORTAL:
            case VORTEX:
            case LIGHTNING:
            case RINGS:
            case ASH:
                classic(style, x, y, z, height, scale, count, life);
                break;
            default:
                ACTIVE.add(new Sequence(style, target, x, y, z, width, height, scale, count, life));
                break;
        }
    }

    /** The eight original effects, unchanged. */
    private static void classic(Style style, double x, double y, double z, float height,
                                float scale, int count, int life) {
        switch (style) {
            case FLAME_PILLAR:
                for (int i = 0; i < count * 3; i++) {
                    double angle = MathUtility.random(0f, (float) (Math.PI * 2));
                    double radius = MathUtility.random(0f, 0.45f) * scale;
                    FxParticles.spawn(new FxParticles.Particle(
                            x + Math.cos(angle) * radius, y + 0.1, z + Math.sin(angle) * radius,
                            life + (int) MathUtility.random(0f, 10f), 0.5f * scale, colour(i / (float) (count * 3)))
                            .motion(0, MathUtility.random(0.08f, 0.2f), 0)
                            .texture(TexturesManager.themed(TexturesManager.GLYPH_FLAME))
                            .grow(0.2f * scale));
                }
                ring(x, y + 0.05, z, 0.3f * scale, 2.2f * scale, life, 0);
                break;
            case SOUL_BURST:
                for (int i = 0; i < count * 4; i++) {
                    double theta = MathUtility.random(0f, (float) (Math.PI * 2));
                    double phi = Math.acos(MathUtility.random(-1f, 1f));
                    double speed = MathUtility.random(0.12f, 0.3f) * scale;
                    FxParticles.spawn(new FxParticles.Particle(x, y + height * 0.5, z,
                            life, 0.3f * scale, colour(i / (float) (count * 4)))
                            .motion(Math.sin(phi) * Math.cos(theta) * speed, Math.cos(phi) * speed,
                                    Math.sin(phi) * Math.sin(theta) * speed)
                            .drag(0.9)
                            .texture(TexturesManager.themed(TexturesManager.FIREFLY)));
                }
                break;
            case EXPLOSION:
                for (int i = 0; i < count * 5; i++) {
                    double angle = MathUtility.random(0f, (float) (Math.PI * 2));
                    double speed = MathUtility.random(0.2f, 0.5f) * scale;
                    FxParticles.spawn(new FxParticles.Particle(x, y + 0.6, z, life, 0.35f * scale,
                            colour(i / (float) (count * 5)))
                            .motion(Math.cos(angle) * speed, MathUtility.random(0.05f, 0.35f), Math.sin(angle) * speed)
                            .gravity(0.03)
                            .texture(TexturesManager.themed(TexturesManager.FIREFLY)));
                }
                ring(x, y + 0.05, z, 0.2f * scale, 4.0f * scale, (int) (life * 0.7), 0);
                break;
            case PORTAL:
                ring(x, y + 0.04, z, 2.4f * scale, -2.2f * scale, life * 2, 0);
                for (int i = 0; i < count * 2; i++) {
                    double angle = i * (Math.PI * 2 / (count * 2));
                    FxParticles.spawn(new FxParticles.Particle(
                            x + Math.cos(angle) * 2.0 * scale, y + 0.1, z + Math.sin(angle) * 2.0 * scale,
                            life * 2, 0.3f * scale, colour(i / (float) (count * 2)))
                            .motion(-Math.cos(angle) * 0.06, 0.02, -Math.sin(angle) * 0.06)
                            .texture(TexturesManager.getRandomGlyphParticle()));
                }
                break;
            case VORTEX:
                for (int i = 0; i < count * 4; i++) {
                    double t = i / (double) (count * 4);
                    double angle = t * Math.PI * 6;
                    double radius = (1.2 - t) * scale;
                    FxParticles.spawn(new FxParticles.Particle(
                            x + Math.cos(angle) * radius, y + t * 3.0, z + Math.sin(angle) * radius,
                            life, 0.26f * scale, colour((float) t))
                            .motion(0, 0.06, 0)
                            .texture(TexturesManager.themed(TexturesManager.FIREFLY))
                            .fadeIn());
                }
                break;
            case LIGHTNING:
                for (int i = 0; i < count; i++) {
                    FxParticles.spawn(new FxParticles.Particle(
                            x + MathUtility.random(-0.4f, 0.4f), y + 3.0, z + MathUtility.random(-0.4f, 0.4f),
                            life, 0.7f * scale, colour(i / (float) count))
                            .motion(0, -0.45, 0)
                            .texture(TexturesManager.GLYPH_BOLT));
                }
                ring(x, y + 0.05, z, 0.2f * scale, 3.0f * scale, (int) (life * 0.6), 0);
                break;
            case RINGS:
                for (int i = 0; i < 3; i++) {
                    FxParticles.spawn(new FxParticles.Particle(x, y + 0.05 + i * 0.5, z,
                            life + i * 4, 0.3f * scale, colour(i / 3f))
                            .expand(2.0f * scale)
                            .motion(0, 0.04, 0));
                }
                break;
            case ASH:
                for (int i = 0; i < count * 3; i++) {
                    FxParticles.spawn(new FxParticles.Particle(
                            x + MathUtility.random(-0.6f, 0.6f), y + 0.2,
                            z + MathUtility.random(-0.6f, 0.6f),
                            life * 3, 0.18f * scale, colour(i / (float) (count * 3)))
                            .motion(MathUtility.random(-0.02f, 0.02f), MathUtility.random(0.01f, 0.05f),
                                    MathUtility.random(-0.02f, 0.02f))
                            .texture(TexturesManager.themed(TexturesManager.FIREFLY))
                            .fadeIn());
                }
                break;
            default:
                break;
        }
    }

    /**
     * One running effect: where it is, how far in, and what it does on each tick of its life.
     *
     * <p>Positions are fixed at the death; the entity reference is kept only so the phantom copies can
     * borrow its model, and is weak so a removed entity is not held on to.
     */
    private static final class Sequence {
        final Style style;
        final WeakReference<Entity> entity;
        final double x, y, z;
        final float width, height, scale, bodyYaw;
        final int count, life;
        final long seed;
        int age;
        /** Points the effect computed once and reuses (constellation stars, phantom headings). */
        final double[] points;

        Sequence(Style style, Entity entity, double x, double y, double z, float width, float height,
                 float scale, int count, int baseLife) {
            this.style = style;
            this.entity = new WeakReference<Entity>(entity);
            this.x = x;
            this.y = y;
            this.z = z;
            this.width = Math.max(0.4f, width);
            this.height = Math.max(0.6f, height);
            this.scale = scale;
            this.count = Math.max(2, count);
            this.seed = RANDOM.nextLong();
            this.bodyYaw = entity instanceof EntityLivingBase ? ((EntityLivingBase) entity).renderYawOffset
                    : (entity == null ? 0f : entity.rotationYaw);
            float k;
            switch (style) {
                case SOUL_CONSTELLATION: k = 2.4f; break;
                case SOUL_SIPHON: case RAVEN_ASCENSION: case ARCANE_EXECUTION: case RUNIC_CROWN: k = 1.7f; break;
                case PHANTOM_AFTERIMAGES: case VOID_IMPLOSION: case ECLIPSE_BLOOM: case RIFT_CLOSURE: k = 1.5f; break;
                default: k = 1.3f; break;
            }
            this.life = Math.max(16, Math.round(baseLife * k));
            Random r = new Random(seed);
            if (style == Style.SOUL_CONSTELLATION) {
                points = new double[7 * 3];
                for (int i = 0; i < 7; i++) {
                    double a = i / 7.0 * Math.PI * 2 + r.nextDouble() * 0.6;
                    double rad = (0.6 + r.nextDouble() * 0.9) * scale;
                    points[i * 3] = x + Math.cos(a) * rad;
                    points[i * 3 + 1] = y + height + (0.8 + r.nextDouble() * 1.4) * scale;
                    points[i * 3 + 2] = z + Math.sin(a) * rad;
                }
            } else if (style == Style.PHANTOM_AFTERIMAGES) {
                points = new double[4 * 2];
                for (int i = 0; i < 4; i++) {
                    double a = i / 4.0 * Math.PI * 2 + r.nextDouble() * 0.8;
                    points[i * 2] = Math.cos(a);
                    points[i * 2 + 1] = Math.sin(a);
                }
            } else {
                points = null;
            }
        }

        float progress() {
            return age / (float) life;
        }

        double cy() {
            return y + height * 0.55;
        }

        void step() {
            int a = age++;
            boolean smoke = CONFIG.killEffectsSmoke;
            switch (style) {
                case SOUL_SIPHON: soulSiphon(a, smoke); break;
                case FRACTURED_GLASS: fracturedGlass(a, smoke); break;
                case RAVEN_ASCENSION: ravenAscension(a, smoke); break;
                case VOID_IMPLOSION: voidImplosion(a, smoke); break;
                case ARCANE_EXECUTION: arcaneExecution(a); break;
                case PHANTOM_AFTERIMAGES: phantoms(a, smoke); break;
                case ECLIPSE_BLOOM: eclipseBloom(a); break;
                case CRYSTAL_DETONATION: crystalDetonation(a, smoke); break;
                case SOUL_CONSTELLATION: constellation(a); break;
                case MOLTEN_ERUPTION: moltenEruption(a, smoke); break;
                case RIFT_CLOSURE: riftClosure(a, smoke); break;
                case RUNIC_CROWN: runicCrown(a); break;
                default: break;
            }
        }

        // -- black-smoke soul siphon: boil, drink, flash --------------------------------------------
        private void soulSiphon(int a, boolean smoke) {
            double tx, ty, tz;
            EntityLivingBase killer = mc.thePlayer;
            boolean toKiller = killer != null && entity.get() != killer
                    && killer.getDistanceSq(x, y, z) < 18 * 18;
            if (toKiller) {
                tx = killer.posX;
                ty = killer.posY + killer.getEyeHeight() * 0.8;
                tz = killer.posZ;
            } else {
                tx = x;
                ty = y + height + 2.4 * scale;
                tz = z;
            }
            int drink = (int) (life * 0.65f);
            if (a == 0) {
                if (smoke) SmokeFx.abyssal(x, y, z, scale * Math.max(0.7f, width), count + 4);
                glow(x, cy(), z, 1.4f * scale, 10, 0f);
            }
            if (a < drink && a % 2 == 0) {
                if (smoke) SmokeFx.siphon(x, y + 0.2, z, tx, ty, tz, scale, Math.max(3, count / 2));
                for (int i = 0; i < 3; i++) {
                    SmokeFx.spawn(SmokeFx.particle().kind(SmokeFx.Kind.EMBER)
                            .at(x + rnd(width), y + RANDOM.nextDouble() * height, z + rnd(width))
                            .vel(0, 0.03, 0).attract(tx, ty, tz, 0.016).drag(0.9)
                            .life(28).size(0.18f * scale, 0.06f * scale).alpha(1f).fade(0.1f, 0.75f));
                }
            }
            if (a == drink) {
                glow(tx, ty, tz, 1.8f * scale, 8, 0.5f);
                ring(tx, ty - 0.2, tz, 0.15f * scale, 1.6f * scale, 12, Math.PI / 2);
            }
        }

        // -- fractured glass collapse ---------------------------------------------------------------
        private void fracturedGlass(int a, boolean smoke) {
            if (a == 0) {
                SmokeFx.spawn(SmokeFx.decal(SmokeFx.FRACTURE, x, cy(), z, 0, 0, 1, height * 1.5f * scale,
                        height * 1.7f * scale, 16, 0f).facing().accentPalette(0f, Palette.Element.PARTICLES));
                glow(x, cy(), z, 2.2f * scale, 5, 0.6f);
                // The body's shards hang in place for a moment...
                for (int i = 0; i < count * 3; i++) {
                    SmokeFx.spawn(SmokeFx.particle().kind(SmokeFx.Kind.SHARD)
                            .at(x + rnd(width * 0.9f), y + RANDOM.nextDouble() * height, z + rnd(width * 0.9f))
                            .vel(rnd(0.01f), 0.0, rnd(0.01f)).gravity(0.0).drag(1.0).turbulence(0f)
                            .life(6 + RANDOM.nextInt(4)).size(0.32f * scale, 0.3f * scale)
                            .spin(RANDOM.nextFloat() * 360f, 0f).alpha(1f).fade(0.05f, 0.95f)
                            .accentPalette(RANDOM.nextFloat(), Palette.Element.PARTICLES));
                }
            }
            if (a == 6) {
                // ...then fall, tumbling, and break on the ground.
                for (int i = 0; i < count * 4; i++) {
                    SmokeFx.spawn(SmokeFx.particle().kind(SmokeFx.Kind.SHARD)
                            .at(x + rnd(width), y + RANDOM.nextDouble() * height, z + rnd(width))
                            .vel(rnd(0.12f), RANDOM.nextDouble() * 0.08, rnd(0.12f)).gravity(0.035).drag(0.98)
                            .turbulence(0f).life(24 + RANDOM.nextInt(16)).size(0.3f * scale, 0.22f * scale)
                            .spin(RANDOM.nextFloat() * 360f, rndf(30f)).alpha(1f).fade(0.02f, 0.6f)
                            .accentPalette(RANDOM.nextFloat(), Palette.Element.PARTICLES));
                }
                ring(x, y + 0.05, z, 0.3f * scale, 2.4f * scale, 14, 0);
                if (smoke) SmokeFx.spectral(x, y, z, scale * 0.8f, count / 2 + 2);
            }
            if (a > 6 && a < 14 && a % 2 == 0) {
                SmokeFx.accents(x, y + 0.1, z, scale * 1.4f, 3, SmokeFx.Kind.SPARK, 0.02f);
            }
        }

        // -- raven ascension ------------------------------------------------------------------------
        private void ravenAscension(int a, boolean smoke) {
            int climb = (int) (life * 0.6f);
            if (a == 0) {
                for (int i = 0; i < count * 2; i++) {
                    double ang = RANDOM.nextDouble() * Math.PI * 2;
                    SmokeFx.spawn(SmokeFx.particle().kind(SmokeFx.Kind.FEATHER)
                            .at(x, cy(), z).vel(Math.cos(ang) * 0.16 * scale, 0.05 + RANDOM.nextDouble() * 0.12, Math.sin(ang) * 0.16 * scale)
                            .drag(0.9).gravity(-0.002).life(30 + RANDOM.nextInt(20))
                            .size(0.5f * scale, 0.4f * scale).spin(RANDOM.nextFloat() * 360f, rndf(12f))
                            .alpha(1f).fade(0.05f, 0.6f).turbulence(1.4f)
                            .accentPalette(RANDOM.nextFloat() * 0.5f, Palette.Element.PARTICLES));
                }
                if (smoke) SmokeFx.spectral(x, y, z, scale, count / 2 + 3);
            }
            if (a < climb) {
                double t = a / (double) climb;
                double cyy = y + height * 0.4 + t * 3.2 * scale;
                for (int k = 0; k < 2; k++) {
                    double ang = t * Math.PI * 6 + k * Math.PI;
                    double rad = (0.9 - t * 0.5) * scale;
                    SmokeFx.spawn(SmokeFx.particle().kind(SmokeFx.Kind.FEATHER)
                            .at(x + Math.cos(ang) * rad, cyy, z + Math.sin(ang) * rad)
                            .vel(-Math.sin(ang) * 0.04, 0.03, Math.cos(ang) * 0.04).drag(0.95)
                            .life(22).size(0.42f * scale, 0.3f * scale).spin(RANDOM.nextFloat() * 360f, rndf(8f))
                            .alpha(0.95f).fade(0.1f, 0.5f).turbulence(0.8f)
                            .accentPalette((float) t, Palette.Element.PARTICLES));
                }
                if (smoke && a % 3 == 0) {
                    SmokeFx.phantomDrift(x, cyy - 0.6, z, 0, 0.12, 0, scale * 0.7f, 2);
                }
            }
            if (a == climb) {
                double top = y + height * 0.4 + 3.2 * scale;
                for (int i = 0; i < count * 2; i++) {
                    double ang = RANDOM.nextDouble() * Math.PI * 2;
                    SmokeFx.spawn(SmokeFx.particle().kind(SmokeFx.Kind.FEATHER)
                            .at(x, top, z).vel(Math.cos(ang) * 0.2, rnd(0.08f), Math.sin(ang) * 0.2)
                            .drag(0.9).gravity(0.004).life(30 + RANDOM.nextInt(15))
                            .size(0.45f * scale, 0.35f * scale).spin(RANDOM.nextFloat() * 360f, rndf(18f))
                            .alpha(1f).fade(0.02f, 0.5f).turbulence(1.5f)
                            .accentPalette(RANDOM.nextFloat(), Palette.Element.PARTICLES));
                }
                glow(x, top, z, 2.5f * scale, 8, 0.3f);
            }
        }

        // -- void implosion, then the ring --------------------------------------------------------
        private void voidImplosion(int a, boolean smoke) {
            int collapse = Math.max(8, (int) (life * 0.45f));
            double cy = cy();
            if (a == 0) {
                SmokeFx.spawn(SmokeFx.decal(SmokeFx.PORTAL_CORE, x, cy, z, 0, 0, 1, 2.6f * scale, 0.2f * scale,
                        collapse + 1, 14f).facing().dark().fade(0.15f, 0.85f).colour(0.02f, 0.0f, 0.04f));
            }
            if (a < collapse) {
                double r = 2.4 * scale;
                for (int i = 0; i < 4; i++) {
                    double th = RANDOM.nextDouble() * Math.PI * 2, ph = Math.acos(RANDOM.nextDouble() * 2 - 1);
                    double px = x + Math.sin(ph) * Math.cos(th) * r, py = cy + Math.cos(ph) * r, pz = z + Math.sin(ph) * Math.sin(th) * r;
                    SmokeFx.spawn(SmokeFx.particle().kind(i == 0 ? SmokeFx.Kind.SPARK : SmokeFx.Kind.EMBER)
                            .at(px, py, pz).attract(x, cy, z, 0.03).drag(0.85).stretch(3f)
                            .life(collapse - a + 2).size(0.15f * scale, 0.05f * scale).alpha(1f).fade(0.1f, 0.85f)
                            .turbulence(0f).accentPalette(RANDOM.nextFloat(), Palette.Element.PARTICLES));
                }
                if (smoke && a % 2 == 0) SmokeFx.siphon(x, y, z, x, cy, z, scale * 1.4f, 3);
            }
            if (a == collapse) {
                glow(x, cy, z, 3.4f * scale, 6, 0.7f);
                ring(x, cy, z, 0.2f * scale, 5.0f * scale, 16, 0);
                ring(x, cy, z, 0.2f * scale, 4.2f * scale, 14, Math.PI / 2);
                ring(x, y + 0.05, z, 0.3f * scale, 3.4f * scale, 18, 0);
                if (smoke) {
                    for (int i = 0; i < SmokeFx.count(count * 2); i++) {
                        double th = RANDOM.nextDouble() * Math.PI * 2;
                        SmokeFx.spawn(SmokeFx.particle().kind(SmokeFx.Kind.SMOKE).at(x, cy, z)
                                .vel(Math.cos(th) * 0.22 * scale, rnd(0.05f), Math.sin(th) * 0.22 * scale).drag(0.88)
                                .life(26 + RANDOM.nextInt(16)).size(0.5f * scale, 1.6f * scale)
                                .spin(RANDOM.nextFloat() * 360f, rndf(4f)).alpha(0.85f).edge(1f).fade(0.05f, 0.4f));
                    }
                }
                SmokeFx.accents(x, cy, z, scale * 2f, count, SmokeFx.Kind.SPARK, 0.0f);
            }
        }

        // -- arcane execution ----------------------------------------------------------------------
        private void arcaneExecution(int a) {
            int strike = (int) (life * 0.65f);
            if (a == 0) {
                SmokeFx.spawn(SmokeFx.decal(SmokeFx.RUNE_RING_0, x, y + 0.06, z, 0, 1, 0, 0.4f * scale, 3.6f * scale,
                        life, 6f).accentPalette(0f, Palette.Element.PARTICLES).fade(0.1f, 0.75f));
                SmokeFx.spawn(SmokeFx.decal(SmokeFx.RUNE_RING_1, x, y + height + 0.3, z, 0, 1, 0, 0.3f * scale, 2.2f * scale,
                        strike + 4, -9f).accentPalette(0.5f, Palette.Element.PARTICLES).fade(0.1f, 0.7f));
            }
            if (a < strike) {
                double t = a / (double) strike;
                for (int k = 0; k < 3; k++) {
                    double ang = t * Math.PI * 4 + k * Math.PI * 2 / 3;
                    double rad = 0.9 * scale;
                    SmokeFx.spawn(SmokeFx.particle().kind(SmokeFx.Kind.RUNE)
                            .at(x + Math.cos(ang) * rad, y + 0.2 + t * height * 1.2, z + Math.sin(ang) * rad)
                            .vel(0, 0.02, 0).life(14).size(0.32f * scale, 0.22f * scale).spin(0f, 0f).turbulence(0f)
                            .alpha(1f).fade(0.15f, 0.5f).accentPalette((float) t, Palette.Element.PARTICLES));
                }
            }
            if (a == strike) {
                for (int i = 0; i < 12; i++) {
                    SmokeFx.spawn(SmokeFx.particle().kind(SmokeFx.Kind.GLOW).at(x, y + i * 0.4, z)
                            .life(12).size(1.1f * scale, 0.4f * scale).alpha(1f).fade(0.02f, 0.3f).turbulence(0f)
                            .accentPalette(i / 12f, Palette.Element.PARTICLES));
                }
                glow(x, cy(), z, 3f * scale, 8, 0.4f);
                ring(x, y + 0.05, z, 0.4f * scale, 3.2f * scale, 14, 0);
                SmokeFx.accents(x, y + height, z, scale * 1.6f, count, SmokeFx.Kind.SPARK, -0.04f);
            }
        }

        // -- phantom afterimages: the copies themselves are drawn in render() --------------------------
        private void phantoms(int a, boolean smoke) {
            if (!smoke || a % 3 != 0) return;
            float p = progress();
            for (int i = 0; i < 4; i++) {
                double d = p * 2.4 * scale;
                SmokeFx.phantomDrift(x + points[i * 2] * d, y, z + points[i * 2 + 1] * d,
                        points[i * 2] * 0.08, 0.02, points[i * 2 + 1] * 0.08, scale * 0.8f, 1);
            }
        }

        // -- eclipse bloom -------------------------------------------------------------------------
        private void eclipseBloom(int a) {
            double cy = cy() + 0.4 * scale;
            int flare = (int) (life * 0.35f);
            if (a == 0) {
                SmokeFx.spawn(SmokeFx.decal(SmokeFx.GLOW, x, cy, z, 0, 0, 1, 0.5f * scale, 4.5f * scale, life, 0f)
                        .facing().accentPalette(0f, Palette.Element.PARTICLES).fade(0.2f, 0.6f));
                SmokeFx.spawn(SmokeFx.decal(SmokeFx.PORTAL_CORE, x, cy, z, 0, 0, 1, 0.3f * scale, 1.5f * scale, life, 3f)
                        .facing().dark().colour(0.01f, 0.0f, 0.02f).fade(0.2f, 0.7f));
            }
            if (a == flare) {
                for (int i = 0; i < count * 3; i++) {
                    double ang = i / (double) (count * 3) * Math.PI * 2;
                    double vx = Math.cos(ang), vy = Math.sin(ang);
                    // Radial in the plane facing the player, so the petals open toward them.
                    double[] f = facingPlane(x, z);
                    SmokeFx.spawn(SmokeFx.particle().kind(SmokeFx.Kind.SPARK).at(x, cy, z)
                            .vel((f[0] * vx) * 0.25 * scale, vy * 0.25 * scale, (f[1] * vx) * 0.25 * scale)
                            .drag(0.86).life(16 + RANDOM.nextInt(6)).size(0.35f * scale, 0.15f * scale).stretch(3f)
                            .alpha(1f).fade(0.02f, 0.5f).turbulence(0f)
                            .accentPalette(i / (float) (count * 3), Palette.Element.PARTICLES));
                }
                glow(x, cy, z, 3.8f * scale, 6, 1f);
            }
        }

        // -- crystal detonation --------------------------------------------------------------------
        private void crystalDetonation(int a, boolean smoke) {
            int grow = Math.max(6, (int) (life * 0.35f));
            if (a == 0) {
                for (int i = 0; i < count * 2; i++) {
                    SmokeFx.spawn(SmokeFx.particle().kind(SmokeFx.Kind.SHARD)
                            .at(x + rnd(width * 0.8f), y + 0.2 + RANDOM.nextDouble() * height, z + rnd(width * 0.8f))
                            .vel(0, 0, 0).drag(1.0).turbulence(0f).life(grow + 1)
                            .size(0.05f * scale, (0.45f + RANDOM.nextFloat() * 0.35f) * scale)
                            .spin(RANDOM.nextFloat() * 360f, 0f).alpha(1f).fade(0.3f, 0.95f)
                            .accent(0.75f, 0.95f, 1f));
                }
            }
            if (a == grow) {
                glow(x, cy(), z, 3f * scale, 6, 1f);
                for (int i = 0; i < count * 5; i++) {
                    double th = RANDOM.nextDouble() * Math.PI * 2, ph = Math.acos(RANDOM.nextDouble() * 2 - 1);
                    double sp = (0.18 + RANDOM.nextDouble() * 0.25) * scale;
                    SmokeFx.spawn(SmokeFx.particle().kind(i % 3 == 0 ? SmokeFx.Kind.SPARK : SmokeFx.Kind.SHARD)
                            .at(x, cy(), z).vel(Math.sin(ph) * Math.cos(th) * sp, Math.cos(ph) * sp + 0.05, Math.sin(ph) * Math.sin(th) * sp)
                            .gravity(0.02).drag(0.95).turbulence(0f).life(20 + RANDOM.nextInt(14))
                            .size(0.3f * scale, 0.18f * scale).spin(RANDOM.nextFloat() * 360f, rndf(25f)).stretch(2.5f)
                            .alpha(1f).fade(0.02f, 0.55f).accentPalette(RANDOM.nextFloat(), Palette.Element.PARTICLES));
                }
                ring(x, cy(), z, 0.2f * scale, 3.6f * scale, 12, Math.PI / 2);
                if (smoke) SmokeFx.spectral(x, y, z, scale, count / 2 + 2);
            }
        }

        // -- soul constellation: motes rise to the stars; the lines are drawn in render() -------------
        private void constellation(int a) {
            if (a == 0) {
                for (int i = 0; i < 7; i++) {
                    for (int k = 0; k < 3; k++) {
                        SmokeFx.spawn(SmokeFx.particle().kind(SmokeFx.Kind.EMBER)
                                .at(x + rnd(width), y + RANDOM.nextDouble() * height, z + rnd(width))
                                .attract(points[i * 3], points[i * 3 + 1], points[i * 3 + 2], 0.012).drag(0.88)
                                .life(18 + k * 3).size(0.14f * scale, 0.08f * scale).alpha(1f).fade(0.1f, 0.8f).turbulence(0f)
                                .accentPalette(i / 7f, Palette.Element.PARTICLES));
                    }
                }
            }
            if (a > 10 && a % 4 == 0 && a < life - 8) {
                int i = RANDOM.nextInt(7);
                SmokeFx.accents(points[i * 3], points[i * 3 + 1], points[i * 3 + 2], 0.3f * scale, 1, SmokeFx.Kind.GLOW, 0.005f);
            }
        }

        // -- molten eruption -----------------------------------------------------------------------
        private void moltenEruption(int a, boolean smoke) {
            if (a == 0) {
                SmokeFx.spawn(SmokeFx.decal(SmokeFx.FRACTURE, x, y + 0.06, z, 0, 1, 0, 1.4f * scale, 3.2f * scale,
                        life, 1f).accent(1f, 0.45f, 0.08f).fade(0.05f, 0.6f));
                glow(x, y + 0.5, z, 2.6f * scale, 8, 0.15f);
                if (smoke) SmokeFx.infernalAsh(x, y, z, scale, count + 4);
            }
            if (a < life / 2) {
                int n = a < 4 ? 6 : 3;
                for (int i = 0; i < n; i++) {
                    SmokeFx.spawn(SmokeFx.particle().kind(i % 2 == 0 ? SmokeFx.Kind.EMBER : SmokeFx.Kind.SPARK)
                            .at(x + rnd(0.3f), y + 0.3, z + rnd(0.3f))
                            .vel(rnd(0.12f) * scale, (0.22 + RANDOM.nextDouble() * 0.2) * scale, rnd(0.12f) * scale)
                            .gravity(0.025).drag(0.98).turbulence(0.3f).life(24 + RANDOM.nextInt(16))
                            .size(0.22f * scale, 0.1f * scale).stretch(2.2f).alpha(1f).fade(0.02f, 0.6f)
                            .accent(1f, 0.35f + RANDOM.nextFloat() * 0.45f, 0.05f));
                }
            }
            if (a == 5 && smoke) SmokeFx.infernalAsh(x, y + 0.5, z, scale * 1.2f, count / 2 + 2);
        }

        // -- rift closure --------------------------------------------------------------------------
        private void riftClosure(int a, boolean smoke) {
            int snap = (int) (life * 0.6f);
            double[] f = facingPlane(x, z);
            double rx = x + f[0] * 0.0, rz = z + f[1] * 0.0, ry = cy();
            if (a == 0) {
                SmokeFx.spawn(SmokeFx.decal(SmokeFx.FRACTURE, rx, ry, rz, 0, 0, 1, 0.4f * scale, height * 1.8f * scale,
                        snap, 0f).facing().accentPalette(0f, Palette.Element.PARTICLES).fade(0.25f, 0.85f));
                SmokeFx.spawn(SmokeFx.decal(SmokeFx.PORTAL_CORE, rx, ry, rz, 0, 0, 1, 0.2f * scale, 1.4f * scale,
                        snap, 10f).facing().dark().colour(0.02f, 0.0f, 0.05f).fade(0.25f, 0.85f));
            }
            if (a < snap) {
                if (smoke && a % 2 == 0) {
                    double toCamX = -f[1], toCamZ = f[0];
                    SmokeFx.rift(rx, ry, rz, 1.0f * scale, (float) toCamX, 0f, (float) toCamZ, 4);
                    SmokeFx.siphon(x, y, z, rx, ry, rz, scale, 2);
                }
            }
            if (a == snap) {
                glow(rx, ry, rz, 2.6f * scale, 5, 1f);
                SmokeFx.accents(rx, ry, rz, scale * 1.4f, count, SmokeFx.Kind.SHARD, 0.03f);
                ring(rx, ry, rz, 0.1f * scale, 2.6f * scale, 10, Math.PI / 2);
            }
        }

        // -- runic crown ---------------------------------------------------------------------------
        private void runicCrown(int a) {
            int dissolve = (int) (life * 0.7f);
            double head = y + height + 0.35 + (a / (double) life) * 1.4 * scale;
            if (a == 0) {
                SmokeFx.spawn(SmokeFx.decal(SmokeFx.RUNE_RING_2, x, y + height + 0.35, z, 0, 1, 0, 0.6f * scale, 1.6f * scale,
                        dissolve, -7f).accentPalette(0.3f, Palette.Element.PARTICLES).fade(0.15f, 0.8f));
            }
            if (a < dissolve) {
                int runes = 8;
                for (int i = 0; i < runes; i++) {
                    double ang = i / (double) runes * Math.PI * 2 + a * 0.18;
                    double rad = 0.62 * scale;
                    SmokeFx.spawn(SmokeFx.particle().kind(SmokeFx.Kind.RUNE).variant(i)
                            .at(x + Math.cos(ang) * rad, head + Math.sin(a * 0.3 + i) * 0.05, z + Math.sin(ang) * rad)
                            .life(4).size(0.34f * scale, 0.3f * scale).turbulence(0f).alpha(0.9f).fade(0.1f, 0.4f)
                            .accentPalette(i / (float) runes, Palette.Element.PARTICLES));
                }
            }
            if (a == dissolve) {
                for (int i = 0; i < 8; i++) {
                    double ang = i / 8.0 * Math.PI * 2 + a * 0.18;
                    SmokeFx.accents(x + Math.cos(ang) * 0.62 * scale, head, z + Math.sin(ang) * 0.62 * scale,
                            0.4f * scale, 3, SmokeFx.Kind.SPARK, 0.04f);
                }
                glow(x, head, z, 2f * scale, 8, 0.3f);
            }
        }

        /** The horizontal axis across the player's view at this point, as (x, z). */
        private double[] facingPlane(double px, double pz) {
            double dx = px - mc.thePlayer.posX, dz = pz - mc.thePlayer.posZ;
            double len = Math.sqrt(dx * dx + dz * dz);
            if (len < 1e-3) return new double[]{1, 0};
            return new double[]{-dz / len, dx / len};
        }
    }

    // ---- per-frame drawing for the effects that need it -------------------------------------------

    /** Phantom copies and constellation lines; called from the world pass. */
    public static void render(float pt) {
        if (ACTIVE.isEmpty()) return;
        for (Sequence s : ACTIVE) {
            float p = Math.min(1f, (s.age + pt) / s.life);
            if (s.style == Style.PHANTOM_AFTERIMAGES) {
                Entity e = s.entity.get();
                if (!(e instanceof EntityLivingBase)) continue;
                for (int i = 0; i < 4; i++) {
                    double d = p * 2.4 * s.scale;
                    float alpha = (1f - p) * 0.55f;
                    Color c = colour(i / 4f);
                    PlayerEffects.drawAfterimage((EntityLivingBase) e,
                            s.x + s.points[i * 2] * d, s.y + p * 0.6, s.z + s.points[i * 2 + 1] * d,
                            s.bodyYaw + (float) Math.toDegrees(Math.atan2(s.points[i * 2 + 1], s.points[i * 2])) * 0.15f,
                            c, alpha, s.age + pt);
                }
            } else if (s.style == Style.SOUL_CONSTELLATION) {
                float fade = 1f - smooth((p - 0.75f) / 0.25f);
                for (int i = 0; i < 6; i++) {
                    float appear = smooth(((s.age + pt) - (10 + i * 3)) / 4f);
                    if (appear <= 0f) continue;
                    int j = i + 1;
                    double rise = p * 0.4;
                    double x0 = s.points[i * 3], y0 = s.points[i * 3 + 1] + rise, z0 = s.points[i * 3 + 2];
                    double x1 = s.points[j * 3], y1 = s.points[j * 3 + 1] + rise, z1 = s.points[j * 3 + 2];
                    // The line draws itself from one star toward the next.
                    double ex = x0 + (x1 - x0) * appear, ey = y0 + (y1 - y0) * appear, ez = z0 + (z1 - z0) * appear;
                    Fx.glowLine(x0, y0, z0, ex, ey, ez, 0.07 * s.scale, Fx.withAlpha(colour(i / 6f), 0.85f * fade));
                }
                for (int i = 0; i < 7; i++) {
                    float tw = 0.75f + 0.25f * (float) Math.sin((s.age + pt) * 0.4 + i);
                    Fx.sprite(TexturesManager.themed(TexturesManager.FIREFLY), s.points[i * 3], s.points[i * 3 + 1] + p * 0.4,
                            s.points[i * 3 + 2], 0.45f * s.scale * tw, Fx.withAlpha(colour(i / 7f), fade), false);
                }
            }
        }
        GlStateManager.color(1f, 1f, 1f, 1f);
    }

    // ---- helpers ---------------------------------------------------------------------------------

    private static void ring(double x, double y, double z, float radius, float expand, int life, double tilt) {
        FxParticles.spawn(new FxParticles.Particle(x, y, z, life, radius, colour(0f))
                .expand(expand).tilt(tilt));
    }

    /** A soft flash of light. */
    private static void glow(double x, double y, double z, float size, int life, float position) {
        SmokeFx.spawn(SmokeFx.particle().kind(SmokeFx.Kind.GLOW).at(x, y, z).life(life).size(size * 0.6f, size)
                .alpha(1f).fade(0.05f, 0.25f).turbulence(0f).accentPalette(position, Palette.Element.PARTICLES));
    }

    private static double rnd(float half) {
        return (RANDOM.nextDouble() - 0.5) * 2.0 * half;
    }

    private static float rndf(float half) {
        return (float) rnd(half);
    }

    private static float smooth(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return t * t * (3f - 2f * t);
    }

    private static Color colour(float position) {
        if (CONFIG.killEffectsRandomColor) return Palette.getRandomColor();
        return Palette.getColor(position);
    }
}
