package me.Padej_.soupapi.melee;

import me.Padej_.soupapi.bedrock.GeoClip;
import me.Padej_.soupapi.config.ConfigurableModule;
import me.Padej_.soupapi.bedrock.GeoModel;
import me.Padej_.soupapi.bedrock.GeoPose;

import java.util.List;

/**
 * Which animation is playing, and how far through.
 *
 * <p>The packs ship their state machines as Lua, which nothing here can run — but they do not need
 * to be run: the clip names say what the machine does. {@code static_idle} holds, {@code draw} plays
 * once on equip, {@code melee_1..3} cycle one per swing, {@code inspect} plays once on request, and
 * {@code run_start}/{@code run}/{@code run_end} wrap the sprint loop. That is the whole machine, and
 * it is reproduced here in terms the mod can see: a base track for the stance and a main track for
 * whatever is happening over it, each cross-fading so a swing starts from where the hands actually
 * are rather than snapping to the first keyframe.
 */
public final class MeleeState {

    /** How long one clip takes to fade into the next, in seconds. */
    private static final float FADE = 0.12f;

    private static final class Track {
        GeoClip clip;
        float time;
        /** The playhead before the last advance, so the cues in between can be found. */
        float lastTime;
        boolean looping;
        GeoClip previous;
        float previousTime;
        float fade;

        void play(GeoClip next, boolean loop) {
            if (next == null) return;
            if (clip == next && looping && loop) return;
            previous = clip;
            previousTime = time;
            fade = clip == null ? 0f : FADE;
            clip = next;
            time = 0f;
            lastTime = -1f;      // so a cue sitting on frame zero still fires
            looping = loop;
        }

        void clear() {
            previous = clip;
            previousTime = time;
            fade = clip == null ? 0f : FADE;
            clip = null;
            time = 0f;
            looping = false;
        }

        boolean finished() {
            return clip != null && !looping && clip.length > 0f && time >= clip.length;
        }

        void advance(float delta) {
            lastTime = time;
            if (fade > 0f) {
                fade = Math.max(0f, fade - delta);
                previousTime += delta;
            }
            if (clip == null) return;
            time += delta;
            if (looping && clip.length > 0f) {
                time %= clip.length;
            }
        }

        /** 0 while the previous clip still owns the pose, 1 once the fade is done. */
        float weight() {
            return fade <= 0f ? 1f : 1f - (fade / FADE);
        }
    }

    private final Track base = new Track();
    private final Track main = new Track();
    private MeleeWeapon weapon;
    private int attackIndex;
    private boolean running;
    private boolean drawn;

    public MeleeWeapon weapon() {
        return weapon;
    }

    /** True while an attack, a draw or an inspect is playing. */
    public boolean busy() {
        return main.clip != null;
    }

    public boolean attacking() {
        return main.clip != null && main.clip.name.startsWith("melee");
    }

    /** Anything showy enough that the eyes should go wild: attacks, summoning, flourishes, inspects. */
    public boolean intense() {
        return main.clip != null && (main.clip.name.startsWith("melee") || main.clip.name.startsWith("summon")
                || main.clip.name.startsWith("flourish") || main.clip.name.startsWith("inspect")
                || main.clip.name.startsWith("claw"));
    }

    /**
     * Whether another swing can start.
     *
     * <p>These clips run one and a half to two seconds, and the first version would not start a new
     * one until the old had finished — so spam-clicking dropped every click but the first and the
     * weapon kept swinging on its own long after. A swing can be cut into once it is past its own
     * halfway point, which is where the pack's own sound cues sit.
     */
    public boolean canAttack() {
        if (main.clip == null) return true;
        if (!attacking()) return true;
        return progress() >= Math.max(0.05f,
                ConfigurableModule.CONFIG.meleeComboWindow / 100f);
    }

    public boolean inspecting() {
        return main.clip != null && main.clip.name.startsWith("inspect");
    }

    /** How far through the current main clip, 0 to 1; 0 when nothing is playing. */
    public float progress() {
        if (main.clip == null || main.clip.length <= 0f) return 0f;
        return Math.min(1f, main.time / main.clip.length);
    }

    public void setWeapon(MeleeWeapon next) {
        if (weapon == next) return;
        weapon = next;
        base.clip = null;
        base.previous = null;
        main.clip = null;
        main.previous = null;
        attackIndex = 0;
        drawn = false;
    }

    /** Plays the equip animation; called when the weapon is first drawn. */
    public void equip() {
        if (weapon == null) return;
        main.play(weapon.draw(), false);
        drawn = true;
    }

    public boolean drawn() {
        return drawn;
    }

    /** Plays the next swing. Returns the clip so the caller can time effects off it. */
    public GeoClip attack() {
        if (weapon == null) return null;
        List<GeoClip> attacks = weapon.attacks();
        if (attacks.isEmpty()) return null;
        GeoClip clip = attacks.get(attackIndex % attacks.size());
        attackIndex++;
        main.play(clip, false);
        return clip;
    }

    public void inspect() {
        if (weapon == null) return;
        main.play(weapon.inspect(), false);
    }

    /** Cancels whatever is playing over the stance — used when the inspect is interrupted. */
    public void cancel() {
        main.clear();
    }

    public void setRunning(boolean sprinting) {
        this.running = sprinting;
    }

    /** Advances both tracks and rebuilds the pose. */
    public void update(float delta) {
        if (weapon == null || !weapon.loaded()) return;
        GeoModel model = weapon.model();
        GeoPose pose = weapon.pose();
        if (model == null || pose == null) return;

        if (delta > 0.25f) delta = 0.25f;   // a lag spike should not fast-forward a swing

        if (main.finished()) {
            main.clear();
        }

        // The sprint clips move the weapon a long way off the idle pose, because they were
        // authored around the view anchor of the mod the pack was built for - which is the one
        // thing about these models this port cannot reuse. Left on, the weapon jumps somewhere odd
        // the moment the player starts running, which is exactly what it looked like.
        boolean sprintClip = running && ConfigurableModule.CONFIG.meleeRunAnimation && weapon.run() != null;
        GeoClip stance = sprintClip ? weapon.run() : weapon.idle();
        if (stance != null && base.clip != stance) {
            base.play(stance, true);
        }

        base.advance(delta);
        main.advance(delta);
        fireSounds(main);

        pose.reset();
        apply(model, pose, base, 1f);
        apply(model, pose, main, 1f);
        // Motion no clip can author: eyes that dart on their own, a spine that breathes, ribbons.
        WeaponFx.procedural(weapon, model, pose, this, delta);
    }

    /** Plays a named clip over the stance; returns false when the weapon has no such clip. */
    public boolean play(GeoClip clip) {
        if (weapon == null || clip == null) return false;
        main.play(clip, false);
        drawn = true;
        return true;
    }

    /** The summoning sequence, where the weapon has one; the ordinary draw otherwise. */
    public boolean summon() {
        if (weapon == null) return false;
        GeoClip clip = weapon.summon();
        if (clip == null) {
            equip();
            return false;
        }
        main.play(clip, false);
        drawn = true;
        return true;
    }

    public void flourish() {
        if (weapon != null) play(weapon.flourish());
    }

    public boolean putAway() {
        return weapon != null && play(weapon.putAway());
    }

    /** Lets the equip play again next time the weapon comes back into the hand. */
    public void sheathe() {
        drawn = false;
        main.clip = null;
        main.previous = null;
    }

    /** The main clip's name, or "" when only the stance is playing. */
    public String mainName() {
        return main.clip == null ? "" : main.clip.name;
    }

    public boolean summoning() {
        return main.clip != null && main.clip.name.startsWith("summon");
    }

    public boolean puttingAway() {
        return main.clip != null && main.clip.name.startsWith("put_away");
    }

    private static void apply(GeoModel model, GeoPose pose, Track track, float weight) {
        if (track.clip == null && track.previous == null) return;
        float blend = track.weight() * weight;
        if (track.previous != null && track.fade > 0f) {
            float previousTime = track.previousTime;
            if (track.previous.length > 0f) {
                previousTime = Math.min(previousTime, track.previous.length);
            }
            // Fades the outgoing clip properly instead of holding it and snapping at the end.
            pose.sampleFading(model, track.previous, previousTime, track.clip, blend);
        }
        if (track.clip != null) {
            pose.sample(model, track.clip, track.time, blend);
        }
    }

    /**
     * Plays whatever cues the track just passed.
     *
     * <p>Only the main track: the stance loops, and a looping clip with a cue in it would retrigger
     * that sound several times a second.
     */
    private static void fireSounds(Track track) {
        if (track.clip == null || track.clip.sounds.isEmpty()) return;
        for (GeoClip.SoundCue cue : track.clip.sounds) {
            if (cue.time > track.lastTime && cue.time <= track.time) {
                MeleeSounds.play(cue.effect);
            }
        }
    }

    /** The clip currently driving the main track, for effects that key off a swing. */
    public GeoClip mainClip() {
        return main.clip;
    }

    public float mainTime() {
        return main.time;
    }
}
