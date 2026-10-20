package me.Padej_.soupapi.melee;

import me.Padej_.soupapi.compat.SoupLog;
import me.Padej_.soupapi.config.ConfigurableModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import org.lwjgl.input.Keyboard;

import java.util.List;

/**
 * The melee weapon the player is carrying, and the keys that drive it.
 *
 * <p>Everything here is cosmetic and client-side: the weapon replaces what the first-person hand
 * draws, nothing is sent to the server and nothing about combat changes. That is the only way a
 * 1.8.9 client can wear a weapon from a 1.20 gun pack on a server that has never heard of either.
 */
public final class Melee extends ConfigurableModule {

    public static final KeyBinding NEXT = new KeyBinding(
            "key.soupapi.melee.next", Keyboard.KEY_M, "key.categories.soupapi");
    public static final KeyBinding PREVIOUS = new KeyBinding(
            "key.soupapi.melee.previous", Keyboard.KEY_N, "key.categories.soupapi");
    public static final KeyBinding INSPECT = new KeyBinding(
            "key.soupapi.melee.inspect", Keyboard.KEY_H, "key.categories.soupapi");
    public static final KeyBinding TOGGLE = new KeyBinding(
            "key.soupapi.melee.toggle", Keyboard.KEY_J, "key.categories.soupapi");
    /** Replays the weapon's summoning (for the katana, the portal) on demand. */
    public static final KeyBinding SUMMON = new KeyBinding(
            "key.soupapi.melee.summon", Keyboard.KEY_K, "key.categories.soupapi");

    private static final MeleeState STATE = new MeleeState();
    private static Thread loader;
    private static boolean swinging;
    private static boolean attackHeld;
    /** A click that arrived mid-swing, waiting for the swing to be far enough through. */
    private static boolean queuedAttack;
    private static long lastFrame;
    /** Whether the weapon was in the hand last tick, so the equip plays on the way in, once. */
    private static boolean wasHolding;
    /** The toggle key was pressed while carrying: dismiss, then switch off. */
    private static boolean disableAfterPutAway;
    /** Seconds the current weapon has been held, for the effects that build up while idle. */
    private static float held;

    private Melee() {
    }

    public static void register() {
        ClientRegistry.registerKeyBinding(NEXT);
        ClientRegistry.registerKeyBinding(PREVIOUS);
        ClientRegistry.registerKeyBinding(INSPECT);
        ClientRegistry.registerKeyBinding(TOGGLE);
        ClientRegistry.registerKeyBinding(SUMMON);
    }

    public static MeleeState state() {
        return STATE;
    }

    public static float heldSeconds() {
        return held;
    }

    /**
     * The weapon that should be drawn, or {@code null} when none should.
     *
     * <p>Gated on what is in the hand. Standing in for a sword is the point of it — the weapon
     * replaces the hand, so showing it while a block or a bow is held would hide the thing actually
     * being used.
     */
    public static MeleeWeapon active() {
        if (!CONFIG.meleeEnabled) return null;
        MeleeWeapon weapon = STATE.weapon();
        if (weapon == null || !weapon.loaded()) return null;
        if (!holding()) return null;
        return weapon;
    }

    /** True when what the player is holding matches the Show When setting. */
    public static boolean holding() {
        MeleeTrigger trigger = CONFIG.meleeShowWhen;
        if (trigger == null || trigger == MeleeTrigger.ALWAYS) return true;
        if (mc.thePlayer == null) return false;
        return trigger.matches(mc.thePlayer.getHeldItem());
    }

    // ---- ticking ---------------------------------------------------------------------------

    public static void onTick() {
        keys();
        if (!CONFIG.meleeEnabled) {
            STATE.setWeapon(null);
            return;
        }
        if (mc.thePlayer == null) return;

        select();
        swing();
        STATE.setRunning(mc.thePlayer.isSprinting());
        if (disableAfterPutAway && !STATE.puttingAway()) {
            disableAfterPutAway = false;
            CONFIG.meleeEnabled = false;
            STATE.setWeapon(null);
            message("Melee off");
        }
    }

    private static void keys() {
        try {
            if (TOGGLE.isPressed()) {
                // Carrying it: put it away properly first, where the weapon has a dismissal.
                if (CONFIG.meleeEnabled && active() != null && !disableAfterPutAway && STATE.putAway()) {
                    disableAfterPutAway = true;
                } else {
                    CONFIG.meleeEnabled = !CONFIG.meleeEnabled;
                    message(CONFIG.meleeEnabled ? "Melee on" : "Melee off");
                }
            }
            if (!CONFIG.meleeEnabled) return;
            if (NEXT.isPressed()) cycle(1);
            if (PREVIOUS.isPressed()) cycle(-1);
            if (INSPECT.isPressed() && !STATE.summoning()) {
                // A second press mid-inspect plays the weapon's flourish instead.
                if (STATE.inspecting() && STATE.weapon() != null && STATE.weapon().flourish() != null) {
                    STATE.flourish();
                } else {
                    STATE.inspect();
                }
            }
            if (SUMMON.isPressed() && active() != null) summonNow();
        } catch (Throwable ignored) {
            // PojavLauncher's input stub does not implement every key query.
        }
    }

    /** Steps to the next or previous weapon the player has left switched on. */
    public static void cycle(int direction) {
        List<MeleeWeapon> weapons = MeleePack.enabled();
        if (weapons.isEmpty()) {
            message("No weapon packs found - see Melee in the settings");
            return;
        }
        int index = 0;
        MeleeWeapon current = STATE.weapon();
        for (int i = 0; i < weapons.size(); i++) {
            if (weapons.get(i) == current) {
                index = i;
                break;
            }
        }
        index = (index + direction) % weapons.size();
        if (index < 0) index += weapons.size();
        MeleeWeapon next = weapons.get(index);
        CONFIG.meleeWeapon = next.id;
        message(next.displayName);
    }

    /** Keeps the state's weapon in step with the setting, loading it off-thread. */
    private static void select() {
        MeleeWeapon wanted = MeleePack.find(CONFIG.meleeWeapon);
        if (wanted == null) {
            List<MeleeWeapon> weapons = MeleePack.enabled();
            if (!weapons.isEmpty()) {
                wanted = weapons.get(0);
                CONFIG.meleeWeapon = wanted.id;
            }
        }
        if (wanted == null) {
            STATE.setWeapon(null);
            return;
        }
        if (MeleePack.isDisabled(wanted)) {
            cycle(1);
            return;
        }
        if (wanted.failed()) {
            STATE.setWeapon(null);
            return;
        }
        if (!wanted.loaded()) {
            STATE.setWeapon(null);
            beginLoad(wanted);
            return;
        }
        if (STATE.weapon() != wanted) {
            unloadOthers(wanted);
            STATE.setWeapon(wanted);
            held = 0f;
        }
        // The equip plays when the weapon comes into the hand - not on every held-item update, which
        // in 1.8.9 happens whenever the stack changes, and which would restart the portal mid-way.
        boolean holdingNow = holding();
        if (!holdingNow && wasHolding) STATE.sheathe();
        if (holdingNow && !STATE.drawn()) {
            if (CONFIG.meleeSummonEnabled && CONFIG.meleeSummonOnEquip && wanted.summon() != null) {
                summonNow();
            } else {
                STATE.equip();
            }
        }
        wasHolding = holdingNow;
    }

    /**
     * Parses a weapon on a worker thread.
     *
     * <p>Half a megabyte of keyframes and a thousand cubes take long enough to parse that doing it
     * on the client thread drops frames at exactly the moment the player pressed the key. The GL
     * side — display lists and textures — still happens on the render thread, on first draw.
     */
    /** Plays the weapon's summoning sequence (for the katana, the portal). */
    public static void summonNow() {
        if (STATE.weapon() == null) return;
        if (!CONFIG.meleeSummonEnabled || STATE.weapon().summon() == null) {
            STATE.equip();
            return;
        }
        MeleePortal.begin();
        STATE.summon();
    }

    private static void beginLoad(final MeleeWeapon weapon) {
        if (loader != null && loader.isAlive()) return;
        loader = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    weapon.load();
                } catch (Throwable t) {
                    SoupLog.warn("[SoupAPI] loading " + weapon.id + " failed", t);
                }
            }
        }, "SoupAPI melee loader");
        loader.setDaemon(true);
        loader.start();
    }

    /** Only one weapon is held at a time, so only one needs its model and textures in memory. */
    private static void unloadOthers(MeleeWeapon keep) {
        for (MeleeWeapon weapon : MeleePack.weapons()) {
            if (weapon != keep && weapon.loaded()) {
                weapon.unload();
            }
        }
    }

    /**
     * Keeps the attack animation in step with the clicks.
     *
     * <p>Two sources, because neither alone is enough: the attack key's own edge, which catches a
     * click even while an animation is running, and the vanilla swing flag, which catches a hit
     * made some other way. A click that lands mid-swing is held rather than dropped and spends
     * itself the moment the current swing is far enough through, so holding or spamming the button
     * keeps the weapon swinging instead of playing one animation and ignoring the rest.
     */
    private static void swing() {
        boolean key = false;
        try {
            key = mc.gameSettings.keyBindAttack.isKeyDown() && mc.currentScreen == null;
        } catch (Throwable ignored) {
            // PojavLauncher's input stub does not implement every query.
        }
        boolean vanilla = mc.thePlayer.isSwingInProgress;

        boolean clicked = (key && !attackHeld) || (vanilla && !swinging);
        attackHeld = key;
        swinging = vanilla;

        if (clicked) queuedAttack = true;
        // Holding the button keeps attacking, the way the vanilla swing does.
        if (key && !STATE.attacking()) queuedAttack = true;

        if (queuedAttack && STATE.summoning() && clicked) {
            // Attacking skips the summon: the portal is a show, never a delay before a fight.
            STATE.cancel();
        }
        if (queuedAttack && STATE.canAttack() && !STATE.summoning()) {
            queuedAttack = false;
            if (STATE.inspecting()) STATE.cancel();
            STATE.attack();
            MeleeEffects.onSwing();
        }
    }

    /** Advances the animation by real time, so a swing reads the same at any frame rate. */
    public static float advance() {
        long now = System.nanoTime();
        if (lastFrame == 0L) lastFrame = now;
        float delta = (now - lastFrame) / 1.0E9f;
        lastFrame = now;
        if (mc.isGamePaused()) delta = 0f;
        delta *= Math.max(0.1f, CONFIG.meleeAnimationSpeed / 100f);
        if (STATE.summoning()) delta *= Math.max(0.25f, CONFIG.meleeSummonSpeed / 100f);
        held += delta;
        STATE.update(delta);
        return delta;
    }

    private static void message(String text) {
        if (!CONFIG.meleeAnnounce) return;
        try {
            Minecraft client = Minecraft.getMinecraft();
            if (client.thePlayer == null) return;
            client.thePlayer.addChatMessage(
                    new net.minecraft.util.ChatComponentText("§8[§bSoup§8] §r" + text));
        } catch (Throwable ignored) {
            // A missing chat line is not worth a crash.
        }
    }
}
