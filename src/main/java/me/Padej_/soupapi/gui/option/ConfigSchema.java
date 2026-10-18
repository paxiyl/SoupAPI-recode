package me.Padej_.soupapi.gui.option;

import me.Padej_.soupapi.SoupAPI;
import me.Padej_.soupapi.compat.SoupLog;
import me.Padej_.soupapi.config.ConfigManager;
import me.Padej_.soupapi.config.SoupAPI_Config;
import me.Padej_.soupapi.config.annotation.ConfigEntry;
import me.Padej_.soupapi.modules.Capes;
import me.Padej_.soupapi.screen.CapeSelectScreen;
import me.Padej_.soupapi.screen.ConfigHudPositionsScreen;
import me.Padej_.soupapi.screen.SwingHandScreen;
import me.Padej_.soupapi.utils.Themes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Builds the settings screen's pages by reflecting over the config.
 *
 * <p>This is the piece Cloth Config used to provide and that the port was missing: without it the
 * settings screen could only show the handful of {@code Setting} objects hanging off five
 * {@code SoupModule}s, leaving all 236 annotated config fields — most of the mod — unreachable in
 * game. Those five modules read a second, parallel copy of their options, so the config entries for
 * ambient particles, fog, the aspect ratio, the hotbar and capes did nothing at all; they now read
 * the config itself, and this is the only schema.
 *
 * <p>Labels, category names and group captions come from the same
 * {@code text.autoconfig.soupapi.*} keys Cloth used, which the lang files already carry.
 */
public final class ConfigSchema {

    private static final String OPTION_PREFIX = "text.autoconfig.soupapi.option.";
    private static final String CATEGORY_PREFIX = "text.autoconfig.soupapi.category.";

    /**
     * Options that cannot work on 1.8.9, and why.
     *
     * <p>Each one needs something the version does not have. They stay in the screen, marked and
     * explained in the footer, instead of being hidden or left looking functional.
     */
    private static final Map<String, String> UNAVAILABLE = new LinkedHashMap<String, String>();

    private static final Map<String, String> UNAVAILABLE_CATEGORIES = new LinkedHashMap<String, String>();

    static {
        String combat = "1.9+ only: 1.8.9 has no attack cooldown, so no sweep, knockback, "
                + "strong or weak hit classes to set a volume for";
        UNAVAILABLE.put("hitSoundOverwriteCritVolume", combat);
        UNAVAILABLE.put("hitSoundOverwriteSweepVolume", combat);
        UNAVAILABLE.put("hitSoundOverwriteNoDamageVolume", combat);
        UNAVAILABLE.put("hitSoundOverwriteKnockbackVolume", combat);
        UNAVAILABLE.put("hitSoundOverwriteStrongVolume", combat);
        UNAVAILABLE.put("hitSoundOverwriteWeakVolume", combat);

        String scoreboard = "1.8.9 draws the scoreboard inside the HUD with no hook to replace it, "
                + "and this port uses no bytecode patching";
        UNAVAILABLE.put("hudBetterScoreboardEnabled", scoreboard);
        UNAVAILABLE.put("hudBetterScoreboardGlow", scoreboard);
        UNAVAILABLE.put("hudBetterScoreboardDarker", scoreboard);
        UNAVAILABLE.put("hudBetterScoreboardColor", scoreboard);

        String hurtFlash = "the damage flash colour is set inside RendererLivingEntity, which has "
                + "no hook on 1.8.9";
        UNAVAILABLE.put("hitColorEnabled", hurtFlash);
        UNAVAILABLE.put("hitColorCustomColor", hurtFlash);
        UNAVAILABLE.put("hitColorColor", hurtFlash);
        UNAVAILABLE.put("hitColorAlpha", hurtFlash);

        UNAVAILABLE_CATEGORIES.put("totem",
                "1.11+ only: the Totem of Undying does not exist on 1.8.9, so nothing can pop one");
    }

    private ConfigSchema() {
    }

    /** Builds one page per config category, plus the pages that are not config fields. */
    public static List<OptionPage> build() {
        List<OptionPage> pages = buildConfigPages();
        OptionPage weapons = buildWeaponsPage();
        if (weapons != null) pages.add(weapons);
        pages.add(buildKeysPage());
        return pages;
    }

    /**
     * The weapons in the installed packs, as rows.
     *
     * <p>A weapon list cannot come from the config: the weapons are whatever packs are on disk, so
     * they are only known once those are read. They were reachable through a button that opened a
     * sub-screen; this puts them in the settings proper, one row each, where they can be found by
     * someone who does not already know the button exists.
     */
    private static OptionPage buildWeaponsPage() {
        java.util.List<me.Padej_.soupapi.melee.MeleeWeapon> weapons;
        try {
            weapons = me.Padej_.soupapi.melee.MeleePack.weapons();
        } catch (Throwable t) {
            SoupLog.warn("[SoupAPI] could not list the weapon packs", t);
            return null;
        }

        OptionPage page = new OptionPage("weapons",
                translate(CATEGORY_PREFIX + "weapons", "Weapons"));
        page.add(new Option.Header(translate(OPTION_PREFIX + "weapons.carry", "CARRIED WEAPON")));
        page.add(new Option.Toggle(
                translate(OPTION_PREFIX + "meleeEnabled", "Carry A Weapon"), null,
                () -> SoupAPI.configHolder.get().meleeEnabled,
                value -> {
                    SoupAPI.configHolder.get().meleeEnabled = value;
                    save();
                },
                new SoupAPI_Config().meleeEnabled));

        if (weapons.isEmpty()) {
            String reason = me.Padej_.soupapi.melee.MeleePack.lastError();
            page.add(new Option.Header(reason == null ? "No weapons found" : reason));
            return page;
        }

        page.add(new Option.Header(translate(OPTION_PREFIX + "weapons.list", "PACK CONTENTS")));
        for (final me.Padej_.soupapi.melee.MeleeWeapon weapon : weapons) {
            // On: in the cycle. Off: skipped by the keys. Clicking the name carries it.
            page.add(new Option.Toggle(plain(weapon.displayName), weapon.pack.namespace + ":" + weapon.id,
                    () -> !me.Padej_.soupapi.melee.MeleePack.isDisabled(weapon),
                    value -> {
                        me.Padej_.soupapi.melee.MeleePack.setDisabled(weapon, !value);
                        if (value) {
                            SoupAPI.configHolder.get().meleeWeapon = weapon.id;
                            SoupAPI.configHolder.get().meleeEnabled = true;
                        }
                        save();
                    },
                    true));
        }
        page.add(new Option.Action(
                translate(OPTION_PREFIX + "selectMelee_BUTTON", "Weapon List"), null, openLabel(),
                actionFor("selectMelee_BUTTON")));
        page.add(new Option.Action(
                translate(OPTION_PREFIX + "rescanMeleePacks_BUTTON", "Look For Packs Again"), null,
                openLabel(), actionFor("rescanMeleePacks_BUTTON")));
        return page;
    }

    /** Minecraft colour codes in a pack's own names; the mod's font draws them as glyphs. */
    private static String plain(String text) {
        return text == null ? "" : text.replaceAll("\u00a7[0-9a-fk-or]", "");
    }

    /** The mod's keys, rebindable without leaving the screen. */
    private static OptionPage buildKeysPage() {
        OptionPage page = new OptionPage("keys", translate(CATEGORY_PREFIX + "keys", "Keys"));
        page.add(new Option.Header(translate(OPTION_PREFIX + "keys.hint",
                "TAP A ROW, THEN PRESS A KEY  -  RIGHT-CLICK TO RESET")));
        key(page, me.Padej_.soupapi.screen.SoupSettingsShortcut.OPEN_SETTINGS);
        key(page, me.Padej_.soupapi.melee.Melee.TOGGLE);
        key(page, me.Padej_.soupapi.melee.Melee.NEXT);
        key(page, me.Padej_.soupapi.melee.Melee.PREVIOUS);
        key(page, me.Padej_.soupapi.melee.Melee.INSPECT);
        return page;
    }

    private static void key(OptionPage page, final net.minecraft.client.settings.KeyBinding binding) {
        final int fallback = binding.getKeyCodeDefault();
        page.add(new Option.Key(translate(binding.getKeyDescription(),
                prettify(binding.getKeyDescription())), null,
                binding::getKeyCode,
                value -> {
                    binding.setKeyCode(value);
                    // Minecraft caches a key-code lookup table; a binding changed without rebuilding
                    // it keeps firing on the old key and not the new one.
                    net.minecraft.client.settings.KeyBinding.resetKeyBindingArrayAndHash();
                    try {
                        net.minecraft.client.Minecraft.getMinecraft().gameSettings.saveOptions();
                    } catch (Throwable ignored) {
                        // Not being able to write options.txt is not worth losing the binding over.
                    }
                },
                fallback));
    }

    // ---- the annotated config --------------------------------------------------------------

    private static List<OptionPage> buildConfigPages() {
        SoupAPI_Config config = SoupAPI.configHolder.get();
        SoupAPI_Config defaults = new SoupAPI_Config();
        Map<String, OptionPage> byCategory = new LinkedHashMap<String, OptionPage>();

        // getFields() follows declaration order on every JVM this runs on, which is what keeps
        // related options (and their @PrefixText group captions) together.
        for (Field field : SoupAPI_Config.class.getFields()) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) continue;
            ConfigEntry.Category category = field.getAnnotation(ConfigEntry.Category.class);
            String categoryId = category == null ? "main" : category.value();

            OptionPage page = byCategory.get(categoryId);
            if (page == null) {
                page = new OptionPage(categoryId, translate(CATEGORY_PREFIX + categoryId, prettify(categoryId)));
                byCategory.put(categoryId, page);
            }

            if (field.isAnnotationPresent(ConfigEntry.Gui.PrefixText.class)) {
                page.add(new Option.Header(
                        translate(OPTION_PREFIX + field.getName() + ".@PrefixText", "")));
            }

            Option option = toOption(field, config, defaults);
            if (option != null) {
                String reason = UNAVAILABLE.get(field.getName());
                if (reason == null) {
                    reason = UNAVAILABLE_CATEGORIES.get(categoryId);
                }
                if (reason != null) {
                    option.markUnavailable(reason);
                }
                page.add(option);
            }
        }
        return new ArrayList<OptionPage>(byCategory.values());
    }

    private static Option toOption(final Field field, final SoupAPI_Config config,
                                   SoupAPI_Config defaults) {
        field.setAccessible(true);
        String label = translate(OPTION_PREFIX + field.getName(), prettify(field.getName()));
        String tooltip = tooltipOf(field);
        Class<?> type = field.getType();

        try {
            // Fields whose name ends in _BUTTON are single-constant enums the original used as
            // action buttons; they carry no value worth cycling.
            if (field.getName().endsWith("_BUTTON")) {
                Runnable action = actionFor(field.getName());
                if (action == null) return null;
                return new Option.Action(label, tooltip, openLabel(), action);
            }

            if (type == boolean.class) {
                final boolean defaultValue = field.getBoolean(defaults);
                return new Option.Toggle(label, tooltip,
                        () -> {
                            try {
                                return field.getBoolean(config);
                            } catch (IllegalAccessException e) {
                                return false;
                            }
                        },
                        value -> set(field, config, value),
                        defaultValue);
            }

            if (type == int.class) {
                ConfigEntry.ColorPicker colour = field.getAnnotation(ConfigEntry.ColorPicker.class);
                final int defaultValue = field.getInt(defaults);
                if (colour != null) {
                    return new Option.Colour(label, tooltip,
                            () -> getInt(field, config),
                            value -> set(field, config, value),
                            colour.allowAlpha(), defaultValue);
                }
                ConfigEntry.BoundedDiscrete bounds = field.getAnnotation(ConfigEntry.BoundedDiscrete.class);
                double min = bounds == null ? Integer.MIN_VALUE : bounds.min();
                double max = bounds == null ? Integer.MAX_VALUE : bounds.max();
                return new Option.Num(label, tooltip,
                        () -> getInt(field, config),
                        value -> set(field, config, (int) Math.round(value)),
                        min, max, bounds != null, true, defaultValue, 1.0);
            }

            if (type == float.class || type == double.class) {
                final double defaultValue = field.getFloat(defaults);
                ConfigEntry.BoundedDiscrete bounds = field.getAnnotation(ConfigEntry.BoundedDiscrete.class);
                double min = bounds == null ? -Float.MAX_VALUE : bounds.min();
                double max = bounds == null ? Float.MAX_VALUE : bounds.max();
                return new Option.Num(label, tooltip,
                        () -> getFloat(field, config),
                        value -> set(field, config, (float) value),
                        min, max, bounds != null, false, defaultValue, 0.01);
            }

            if (type.isEnum()) {
                final Enum<?>[] values = (Enum<?>[]) type.getEnumConstants();
                final Enum<?> defaultValue = (Enum<?>) field.get(defaults);
                return new Option.Choice(label, tooltip,
                        () -> {
                            try {
                                return (Enum<?>) field.get(config);
                            } catch (IllegalAccessException e) {
                                return defaultValue;
                            }
                        },
                        value -> set(field, config, value),
                        values, defaultValue);
            }

            if (type == String.class) {
                final String defaultValue = (String) field.get(defaults);
                return new Option.Text(label, tooltip,
                        () -> {
                            try {
                                return (String) field.get(config);
                            } catch (IllegalAccessException e) {
                                return "";
                            }
                        },
                        value -> set(field, config, value),
                        defaultValue);
            }

            if (type == String[].class) {
                return new Option.TextList(label, tooltip,
                        () -> {
                            try {
                                return (String[]) field.get(config);
                            } catch (IllegalAccessException e) {
                                return new String[0];
                            }
                        },
                        value -> set(field, config, value));
            }
        } catch (Throwable t) {
            SoupLog.warn("[SoupAPI] could not expose config field " + field.getName(), t);
        }
        return null;
    }

    /** The screens the original's {@code *_BUTTON} entries were meant to open. */
    private static Runnable actionFor(String fieldName) {
        final Minecraft mc = Minecraft.getMinecraft();
        if ("configPos_BUTTON".equals(fieldName)) {
            return () -> mc.displayGuiScreen(new ConfigHudPositionsScreen());
        }
        if ("swingHandConfig_BUTTON".equals(fieldName)) {
            return () -> mc.displayGuiScreen(new SwingHandScreen());
        }
        if ("selectCape_BUTTON".equals(fieldName)) {
            return () -> mc.displayGuiScreen(new CapeSelectScreen());
        }
        if ("updateCape_BUTTON".equals(fieldName)) {
            return Capes::reloadCustomCape;
        }
        if ("selectMelee_BUTTON".equals(fieldName)) {
            return () -> {
                net.minecraft.client.gui.GuiScreen current = mc.currentScreen;
                mc.displayGuiScreen(new me.Padej_.soupapi.melee.MeleeSelectScreen(
                        current instanceof me.Padej_.soupapi.compat.SoupScreen
                                ? (me.Padej_.soupapi.compat.SoupScreen) current : null));
            };
        }
        if ("rescanMeleePacks_BUTTON".equals(fieldName)) {
            return me.Padej_.soupapi.melee.MeleePack::rescan;
        }
        if ("applyPreset_BUTTON".equals(fieldName)) {
            // Switches a whole set on and everything else off, so a preset is the same for
            // everyone who picks it rather than whatever was already set plus a few more things.
            return () -> {
                SoupAPI_Config config = SoupAPI.configHolder.get();
                config.presetStyle.apply(config);
                save();
            };
        }
        if ("resetMeleePlacement_BUTTON".equals(fieldName)) {
            return () -> {
                me.Padej_.soupapi.melee.MeleePlacement.reset();
                save();
            };
        }
        if ("saveMeleePlacement_BUTTON".equals(fieldName)) {
            // Gives the weapon in hand its own copy of the sliders, so tuning one does not move
            // the other ten.
            return () -> {
                me.Padej_.soupapi.melee.MeleePlacement.save(
                        me.Padej_.soupapi.melee.Melee.state().weapon());
                save();
            };
        }
        if ("forgetMeleePlacement_BUTTON".equals(fieldName)) {
            return () -> {
                me.Padej_.soupapi.melee.MeleePlacement.forget(
                        me.Padej_.soupapi.melee.Melee.state().weapon());
                save();
            };
        }
        return null;
    }

    // ---- persistence -----------------------------------------------------------------------

    /** Writes both config systems to disk. */
    public static void save() {
        try {
            SoupAPI.configHolder.save();
        } catch (Throwable t) {
            SoupLog.error("[SoupAPI] could not save the config", t);
        }
        try {
            ConfigManager.saveConfig();
        } catch (Throwable t) {
            SoupLog.error("[SoupAPI] could not save the module settings", t);
        }
    }

    // ---- helpers ---------------------------------------------------------------------------

    /**
     * Writes one config field, then lets the theme logic see the edit.
     *
     * <p>Every widget in the screen ends up here, which is why the theme hook lives here too:
     * picking a theme copies its look into the settings, and editing one of the settings a theme
     * filled in drops the theme to Custom. Doing it at the single write point means no widget has
     * to know that themes exist, and nothing can be edited behind a theme's back.
     */
    private static void set(Field field, Object target, Object value) {
        try {
            field.set(target, value);
        } catch (IllegalAccessException e) {
            SoupLog.warn("[SoupAPI] could not write config field " + field.getName(), e);
            return;
        }
        if (target instanceof SoupAPI_Config) {
            Themes.onEdited(field.getName(), (SoupAPI_Config) target);
        }
    }

    private static int getInt(Field field, Object target) {
        try {
            return field.getInt(target);
        } catch (IllegalAccessException e) {
            return 0;
        }
    }

    private static double getFloat(Field field, Object target) {
        try {
            return field.getFloat(target);
        } catch (IllegalAccessException e) {
            return 0.0;
        }
    }

    private static String tooltipOf(Field field) {
        if (!field.isAnnotationPresent(ConfigEntry.Gui.Tooltip.class)) return null;
        String key = OPTION_PREFIX + field.getName() + ".@Tooltip";
        String value = translate(key, null);
        if (value != null) return value;
        return translate(key + "[0]", null);
    }

    /**
     * Looks a key up, falling back when it is missing.
     *
     * <p>{@link I18n#format} returns the key itself for a missing entry, so that is the test. It
     * also throws if the locale has not been built yet, which is why this is guarded: a schema
     * built too early should fall back to readable field names, not take the client down.
     */
    private static String translate(String key, String fallback) {
        String value;
        try {
            value = I18n.format(key);
        } catch (Throwable t) {
            return fallback;
        }
        return value == null || value.equals(key) ? fallback : value;
    }

    /** {@code hitParticleIncludeStar} reads as "Hit particle include star". */
    static String prettify(String identifier) {
        String spaced = identifier.replace('_', ' ').replaceAll("([a-z0-9])([A-Z])", "$1 $2");
        spaced = spaced.trim().toLowerCase(Locale.ROOT);
        if (spaced.isEmpty()) return identifier;
        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }

    /** The caption on an action button. */
    private static String openLabel() {
        return translate("soupapi.gui.open", "Open");
    }
}
