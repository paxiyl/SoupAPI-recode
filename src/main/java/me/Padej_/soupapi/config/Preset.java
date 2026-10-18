package me.Padej_.soupapi.config;

import me.Padej_.soupapi.modules.HitBubbles;
import me.Padej_.soupapi.modules.HitEffects;
import me.Padej_.soupapi.modules.JumpCircles;
import me.Padej_.soupapi.modules.KillEffects;
import me.Padej_.soupapi.modules.LandingImpact;
import me.Padej_.soupapi.modules.PlayerEffects;
import me.Padej_.soupapi.modules.Trails;
import me.Padej_.soupapi.utils.Theme;
import me.Padej_.soupapi.utils.Themes;

/**
 * Whole-config presets: one choice that leaves the mod in a state worth looking at.
 *
 * <p>Every option defaults to off, which is the right default for a mod that replaces the hand and
 * paints the sky, and the wrong first impression entirely: a new player turns it on and nothing
 * happens, then turns on six things at once and it looks like a mess. A preset switches on a set
 * that was chosen to work together — and switches the rest <em>off</em>, which is the part that
 * matters, because the usual way a config ends up looking bad is three half-configured effects
 * fighting each other.
 *
 * <p>Applying one never touches the HUD layout, the friends list, the capes or the keys. Those are
 * the player's, not a preset's.
 */
public enum Preset {

    /** What the mod ships with: everything off. */
    NONE,
    /** Readable in a fight: a target HUD, hit markers, nothing in the way of the screen. */
    PVP,
    /** The same, plus the carried weapon and its effects. */
    PVP_WEAPON,
    /** Everything that looks good on camera, turned up. */
    SHOWCASE,
    /** Green code, black sky, and as little else as possible. */
    MATRIX,
    /** Snow, pale blue, a quiet winter. */
    WINTER,
    /** Fire, embers, a glowing horizon. */
    INFERNO,
    /** Deep water: bubbles, light shafts, slow drift. */
    ABYSS,
    /** Pink and cyan, a grid sky, as loud as it gets. */
    VAPORWAVE,
    /** Pentagrams, skulls, an eclipse. */
    OCCULT,
    /** Visuals only where they cost nothing: no ambience, no sky, no extra passes. */
    PERFORMANCE,
    /** Every visual the mod has, at once. Not advice. */
    EVERYTHING;

    /**
     * Writes this preset over the config.
     *
     * <p>Starts from the shipped defaults so a preset is the same whatever was set before it, which
     * is what makes two people comparing the same preset see the same thing.
     */
    public void apply(SoupAPI_Config config) {
        if (this == NONE || config == null) return;

        SoupAPI_Config defaults = new SoupAPI_Config();
        copyVisuals(defaults, config);

        switch (this) {
            case PVP:
                combat(config);
                break;
            case PVP_WEAPON:
                combat(config);
                weapon(config);
                break;
            case SHOWCASE:
                combat(config);
                weapon(config);
                flourish(config);
                config.themeStyle = Theme.NEON_TOKYO;
                config.ambienceEnabled = true;
                config.coloredSkyEnabled = true;
                break;
            case MATRIX:
                config.themeStyle = Theme.MATRIX;
                config.ambienceEnabled = true;
                config.ambienceDensity = 90;
                config.coloredSkyEnabled = true;
                config.trailsEnabled = true;
                config.playerEffectsEnabled = true;
                config.playerEffectsStyle = PlayerEffects.Style.BEAM;
                combat(config);
                break;
            case WINTER:
                config.themeStyle = Theme.FROST;
                config.ambienceEnabled = true;
                config.coloredSkyEnabled = true;
                config.jumpCirclesEnabled = true;
                config.playerEffectsEnabled = true;
                combat(config);
                break;
            case INFERNO:
                config.themeStyle = Theme.INFERNO;
                config.ambienceEnabled = true;
                config.coloredSkyEnabled = true;
                config.playerEffectsEnabled = true;
                config.landingImpactEnabled = true;
                combat(config);
                break;
            case ABYSS:
                config.themeStyle = Theme.ABYSS;
                config.ambienceEnabled = true;
                config.coloredSkyEnabled = true;
                config.customFogEnabled = true;
                config.playerEffectsEnabled = true;
                combat(config);
                break;
            case VAPORWAVE:
                config.themeStyle = Theme.VAPORWAVE;
                flourish(config);
                combat(config);
                weapon(config);
                config.ambienceEnabled = true;
                config.coloredSkyEnabled = true;
                break;
            case OCCULT:
                config.themeStyle = Theme.WITCHCRAFT;
                config.ambienceEnabled = true;
                config.coloredSkyEnabled = true;
                config.haloEnabled = true;
                config.playerEffectsEnabled = true;
                config.killEffectsEnabled = true;
                combat(config);
                break;
            case PERFORMANCE:
                combat(config);
                config.ambienceEnabled = false;
                config.coloredSkyEnabled = false;
                config.meleeGlow = false;
                config.meleeTrail = false;
                config.meleeSparks = false;
                config.hitParticlesEnabled = false;
                config.blurShadowEnabled = false;
                break;
            case EVERYTHING:
                combat(config);
                weapon(config);
                flourish(config);
                config.ambienceEnabled = true;
                config.coloredSkyEnabled = true;
                config.customFogEnabled = true;
                config.haloEnabled = true;
                config.killEffectsEnabled = true;
                config.landingImpactEnabled = true;
                config.jumpParticlesEnabled = true;
                config.ambientParticlesEnabled = true;
                config.meleePhantom = true;
                config.meleeOutline = true;
                config.meleePulse = true;
                config.meleeRunes = true;
                break;
            default:
                break;
        }

        // The theme owns the look, so let it fill the settings in as it would if picked by hand.
        Themes.apply(config);
    }

    /** The things worth having in a fight, and nothing that covers the screen. */
    private static void combat(SoupAPI_Config config) {
        config.targetHudEnabled = true;
        config.hitBubblesEnabled = true;
        config.hitBubblesStyle = HitBubbles.Style.CIRCLE_BOLD;
        config.hitEffectsEnabled = true;
        config.hitEffectsStyle = HitEffects.Style.SHARDS;
        config.hitSoundEnabled = true;
        config.waterMarkEnabled = true;
    }

    /** The carried weapon, with the effects that do not get in the way. */
    private static void weapon(SoupAPI_Config config) {
        config.meleeEnabled = true;
        config.meleeHands = true;
        config.meleeGlow = true;
        config.meleeTrail = true;
        config.meleeSparks = true;
        config.meleeSounds = true;
    }

    /** The showy ones. */
    private static void flourish(SoupAPI_Config config) {
        config.trailsEnabled = true;
        config.trailsStyle = Trails.Style.NEON;
        config.jumpCirclesEnabled = true;
        config.jumpCirclesStyle = JumpCircles.JumCircleStyle.MAGIC;
        config.playerEffectsEnabled = true;
        config.killEffectsEnabled = true;
        config.killEffectsStyle = KillEffects.Style.SOUL_BURST;
        config.landingImpactEnabled = true;
        config.landingImpactStyle = LandingImpact.Style.SHOCKWAVE;
        config.haloEnabled = true;
    }

    /**
     * Resets everything a preset is allowed to touch, from the shipped defaults.
     *
     * <p>Done by reflection against a rule rather than by naming fields, because naming them is how
     * this went wrong the first time: the list missed a dozen style enums and every melee slider,
     * so the same preset applied over two different configs gave two different results — which is
     * exactly the complaint that a preset leaves the settings looking a mess.
     *
     * <p>A preset owns the visual state: what is switched on, which style each module draws, and
     * where the carried weapon sits. It does not own the player's own things — their friends, their
     * capes, their HUD layout, which weapon they carry, which weapons they switched off, their
     * saved per-weapon placements, or where their packs live.
     */
    private static void copyVisuals(SoupAPI_Config from, SoupAPI_Config to) {
        for (java.lang.reflect.Field field : SoupAPI_Config.class.getFields()) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) continue;
            if (!owns(field)) continue;
            try {
                field.set(to, field.get(from));
            } catch (IllegalAccessException ignored) {
                // A field that cannot be written is a field a preset cannot own.
            }
        }
    }

    /** Whether a preset is allowed to write this field. */
    private static boolean owns(java.lang.reflect.Field field) {
        String name = field.getName();
        if (name.endsWith("_BUTTON")) return false;
        for (String kept : KEPT) {
            if (name.equals(kept) || name.startsWith(kept)) return false;
        }
        if (name.endsWith("Enabled") && field.getType() == boolean.class) return true;
        if (name.endsWith("Style") && field.getType().isEnum()) return true;
        if (name.startsWith("ambience") || name.startsWith("sky")) return true;
        if (name.startsWith("melee")) return true;
        if (name.startsWith("theme")) return true;
        return name.equals("particleSymbol") || name.equals("particleSpriteOverride")
                || name.equals("coloredSkyColor") || name.equals("coloredSkyAlpha")
                || name.equals("coloredSkyUseTheme");
    }

    /** The player's own settings, which no preset touches. */
    private static final String[] KEPT = {
            "friends", "capes", "customCapes", "waterMarkX", "waterMarkY", "configPos",
            "meleeWeapon", "meleeDisabled", "meleePlacements", "meleePackPath", "presetStyle",
    };
}
