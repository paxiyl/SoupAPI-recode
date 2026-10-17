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

    private static final MeleeState STATE = new MeleeState();
    private static Thread loader;
    private static boolean swinging;
    private static long lastFrame;
    /** Seconds the current weapon has been held, for the effects that build up while idle. */
    private static float held;

    private Melee() {
    }

    public static void register() {
        ClientRegistry.registerKeyBinding(NEXT);
        ClientRegistry.registerKeyBinding(PREVIOUS);
        ClientRegistry.registerKeyBinding(INSPECT);
        ClientRegistry.registerKeyBinding(TOGGLE);
    }

    public static MeleeState state() {
        return STATE;
    }

    public static float heldSeconds() {
        return held;
    }

    /** The weapon that should be drawn, or {@code null} when none should. */
    public static MeleeWeapon active() {
        if (!CONFIG.meleeEnabled) return null;
        MeleeWeapon weapon = STATE.weapon();
        return weapon != null && weapon.loaded() ? weapon : null;
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
    }

    private static void keys() {
        try {
            if (TOGGLE.isPressed()) {
                CONFIG.meleeEnabled = !CONFIG.meleeEnabled;
                message(CONFIG.meleeEnabled ? "Melee on" : "Melee off");
            }
            if (!CONFIG.meleeEnabled) return;
            if (NEXT.isPressed()) cycle(1);
            if (PREVIOUS.isPressed()) cycle(-1);
            if (INSPECT.isPressed()) STATE.inspect();
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
        if (!STATE.drawn()) {
            STATE.equip();
        }
    }

    /**
     * Parses a weapon on a worker thread.
     *
     * <p>Half a megabyte of keyframes and a thousand cubes take long enough to parse that doing it
     * on the client thread drops frames at exactly the moment the player pressed the key. The GL
     * side — display lists and textures — still happens on the render thread, on first draw.
     */
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

    /** Fires an attack animation on the rising edge of the player's swing. */
    private static void swing() {
        boolean now = mc.thePlayer.isSwingInProgress;
        if (now && !swinging && !STATE.attacking()) {
            if (STATE.inspecting()) STATE.cancel();
            STATE.attack();
            MeleeEffects.onSwing();
        }
        swinging = now;
    }

    /** Advances the animation by real time, so a swing reads the same at any frame rate. */
    public static float advance() {
        long now = System.nanoTime();
        if (lastFrame == 0L) lastFrame = now;
        float delta = (now - lastFrame) / 1.0E9f;
        lastFrame = now;
        if (mc.isGamePaused()) delta = 0f;
        delta *= Math.max(0.1f, CONFIG.meleeAnimationSpeed / 100f);
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
