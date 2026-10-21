package me.Padej_.soupapi.melee;

import me.Padej_.soupapi.config.ConfigurableModule;
import me.Padej_.soupapi.config.SoupAPI_Config;

import java.util.ArrayList;
import java.util.List;

/**
 * Placement saved per weapon.
 *
 * <p>One set of sliders cannot suit eleven weapons: they are different shapes, authored by different
 * hands, and a position that puts a dagger where you want it puts a greatsword through your face.
 * The sliders stay global — they are what a weapon starts from — but a weapon can be given its own
 * copy, which from then on is what it is drawn with. Everything else keeps following the sliders.
 *
 * <p>Stored in the config as one line per weapon, {@code id=size,distance,x,y,z,yaw,pitch,roll}, so
 * it survives a restart and can be read and edited by hand.
 */
public final class MeleePlacement extends ConfigurableModule {

    /** The eight numbers that place a weapon. */
    public static final class Values {
        public int size;
        public int distance;
        public int offsetX;
        public int offsetY;
        public int offsetZ;
        public int yaw;
        public int pitch;
        public int roll;

        Values(int size, int distance, int offsetX, int offsetY, int offsetZ,
               int yaw, int pitch, int roll) {
            this.size = size;
            this.distance = distance;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.offsetZ = offsetZ;
            this.yaw = yaw;
            this.pitch = pitch;
            this.roll = roll;
        }
    }

    private MeleePlacement() {
    }

    /** True while every placement slider is still where it ships. */
    private static boolean slidersAreDefault() {
        SoupAPI_Config defaults = DEFAULTS;
        return CONFIG.meleeSize == defaults.meleeSize
                && CONFIG.meleeDistance == defaults.meleeDistance
                && CONFIG.meleeOffsetX == defaults.meleeOffsetX
                && CONFIG.meleeOffsetY == defaults.meleeOffsetY
                && CONFIG.meleeOffsetZ == defaults.meleeOffsetZ
                && CONFIG.meleeYaw == defaults.meleeYaw
                && CONFIG.meleePitch == defaults.meleePitch
                && CONFIG.meleeRoll == defaults.meleeRoll;
    }

    private static final SoupAPI_Config DEFAULTS = new SoupAPI_Config();

    /** The sliders as they stand. */
    public static Values current() {
        return new Values(CONFIG.meleeSize, CONFIG.meleeDistance, CONFIG.meleeOffsetX,
                CONFIG.meleeOffsetY, CONFIG.meleeOffsetZ, CONFIG.meleeYaw, CONFIG.meleePitch,
                CONFIG.meleeRoll);
    }

    /** What a weapon should be drawn with: its own saved placement, its pack's, or the sliders. */
    public static Values of(MeleeWeapon weapon) {
        if (weapon == null) return current();
        String stored = find(weapon.id);
        if (stored == null) {
            // Nothing saved for this weapon: the pack's own placement, if it ships one. Only while
            // the sliders are untouched - the moment the player moves one, that is what they mean.
            int[] packed = weapon.defaultPlacement();
            if (packed != null && slidersAreDefault()) {
                return new Values(packed[0], packed[1], packed[2], packed[3], packed[4],
                        packed[5], packed[6], packed[7]);
            }
            return current();
        }
        String[] parts = stored.split(",");
        if (parts.length < 8) return current();
        try {
            return new Values(
                    Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()),
                    Integer.parseInt(parts[2].trim()), Integer.parseInt(parts[3].trim()),
                    Integer.parseInt(parts[4].trim()), Integer.parseInt(parts[5].trim()),
                    Integer.parseInt(parts[6].trim()), Integer.parseInt(parts[7].trim()));
        } catch (NumberFormatException e) {
            return current();
        }
    }

    public static boolean isSaved(MeleeWeapon weapon) {
        return weapon != null && find(weapon.id) != null;
    }

    /** Gives the weapon its own copy of the sliders as they stand. */
    public static void save(MeleeWeapon weapon) {
        if (weapon == null) return;
        Values values = current();
        String line = weapon.id + "=" + values.size + "," + values.distance + ","
                + values.offsetX + "," + values.offsetY + "," + values.offsetZ + ","
                + values.yaw + "," + values.pitch + "," + values.roll;
        List<String> out = new ArrayList<String>();
        for (String entry : entries()) {
            if (!entry.startsWith(weapon.id + "=")) out.add(entry);
        }
        out.add(line);
        CONFIG.meleePlacements = out.toArray(new String[0]);
    }

    /** Drops a weapon's saved placement, so it follows the sliders again. */
    public static void forget(MeleeWeapon weapon) {
        if (weapon == null) return;
        List<String> out = new ArrayList<String>();
        for (String entry : entries()) {
            if (!entry.startsWith(weapon.id + "=")) out.add(entry);
        }
        CONFIG.meleePlacements = out.toArray(new String[0]);
    }

    /** Puts the sliders back to the weapon's saved placement, for editing it further. */
    public static void load(MeleeWeapon weapon) {
        Values values = of(weapon);
        CONFIG.meleeSize = values.size;
        CONFIG.meleeDistance = values.distance;
        CONFIG.meleeOffsetX = values.offsetX;
        CONFIG.meleeOffsetY = values.offsetY;
        CONFIG.meleeOffsetZ = values.offsetZ;
        CONFIG.meleeYaw = values.yaw;
        CONFIG.meleePitch = values.pitch;
        CONFIG.meleeRoll = values.roll;
    }

    /** The sliders back to what the mod ships with. */
    public static void reset() {
        SoupAPI_Config defaults = new SoupAPI_Config();
        CONFIG.meleeSize = defaults.meleeSize;
        CONFIG.meleeDistance = defaults.meleeDistance;
        CONFIG.meleeOffsetX = defaults.meleeOffsetX;
        CONFIG.meleeOffsetY = defaults.meleeOffsetY;
        CONFIG.meleeOffsetZ = defaults.meleeOffsetZ;
        CONFIG.meleeYaw = defaults.meleeYaw;
        CONFIG.meleePitch = defaults.meleePitch;
        CONFIG.meleeRoll = defaults.meleeRoll;
        CONFIG.meleeCameraShake = defaults.meleeCameraShake;
    }

    private static String find(String id) {
        for (String entry : entries()) {
            if (entry.startsWith(id + "=")) return entry.substring(id.length() + 1);
        }
        return null;
    }

    private static String[] entries() {
        String[] list = CONFIG.meleePlacements;
        return list == null ? new String[0] : list;
    }
}
