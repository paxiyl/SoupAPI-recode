package me.Padej_.soupapi.config;

import me.Padej_.soupapi.modules.HitBubbles;
import me.Padej_.soupapi.modules.HitEffects;
import me.Padej_.soupapi.modules.JumpCircles;
import me.Padej_.soupapi.modules.KillEffects;
import me.Padej_.soupapi.modules.LandingImpact;
import me.Padej_.soupapi.modules.PlayerEffects;
import me.Padej_.soupapi.modules.Trails;
import me.Padej_.soupapi.melee.MeleeFinish;
import me.Padej_.soupapi.modules.BetterHudStyles;
import me.Padej_.soupapi.modules.Crosshair;
import me.Padej_.soupapi.render.SmokeFx;
import me.Padej_.soupapi.utils.Ambience;
import me.Padej_.soupapi.utils.Palette;
import me.Padej_.soupapi.utils.SkyStyle;
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
    EVERYTHING,
    /** A rift-torn sky, abyssal smoke, the Fractured-Eye katana in obsidian, rift kills. */
    RIFTBLADE,
    /** Dark violet night: void sky, violet fireflies, raven claw, raven kills. */
    NIGHTFALL,
    /** Blood-red glass: shattered sky, glass fragments, crimson flow, fractured-glass kills. */
    BLOODGLASS,
    /** Stars and constellations: arcane sky, spectral blade, soul-constellation kills. */
    ASTRAL,
    /** The Inferno preset, deeper: infernal sky and smoke, molten katana, eruptions. */
    INFERNO_ENHANCED,
    /** The Abyss preset, deeper: ocean sky, abyssal smoke, void implosions. */
    ABYSS_ENHANCED,
    /** The Vaporwave preset, deeper: cyberpunk sky, rainbow RGB everywhere, glass hotbar. */
    VAPORWAVE_ENHANCED,
    /** The Occult preset, deeper: bleeding-cloud sky, occult symbols, arcane executions. */
    OCCULT_ENHANCED,
    /** Crystal and storm: storm-front sky, glass fragments, crystal detonations. */
    CRYSTAL_STORM,
    /** Pale and cold: aurora sky, spectral smoke, spectral blade, phantom afterimages. */
    FROST_SPECTER,
    /** For fighting: a clean crosshair and hit feedback, nothing else drawing. */
    MINIMAL_PVP,
    /** For recording: everything that reads on camera, smoothly, with the weapon summoned. */
    CINEMATIC_SHOWCASE,
    /** For weak devices and phones: the cheapest settings that still look like the mod. */
    LOW_END_PERFORMANCE,
    /** Pastel dreamcore sky, magic dust, soft pink, the spectral blade. */
    DREAMSCAPE,

    // ---- the layers ------------------------------------------------------------------------
    // One preset a floor, after the game that names them. Each is its theme plus the sky,
    // ambience, kill effect, crosshair and hotbar that go with it.

    /** White marble and a blue machine: cold, clean, almost nothing on the screen. */
    LAYER_PRELUDE,
    /** Still water over white stone, light coming up through it. */
    LAYER_LIMBO,
    /** Pink neon standing in black water. */
    LAYER_LUST,
    /** Flesh and bone, dull and close. */
    LAYER_GLUTTONY,
    /** A grey sea under a grey storm. */
    LAYER_WRATH,
    /** Gold on sand, the heat coming off it. */
    LAYER_GREED,
    /** Black stone and gold fire. */
    LAYER_HERESY,
    /** Blood on sand under a burnt sky. */
    LAYER_VIOLENCE,
    /** Black, green and a grid: the terminal. */
    LAYER_CYBER_GRIND,
    /** White and gold, lit like a chapel. */
    LAYER_PRIME_SANCTUM;

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
        if (theme() != null) config.themeStyle = theme();

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
        // The newer presets name their own sky, ambience and effects over the theme's choices.
        signature(config);
    }

    /**
     * The newer presets. Each picks a theme first (in {@link #theme}), then names exactly which sky,
     * ambience, smoke, kill effect, finish, crosshair and hotbar it wants, so it does not depend on
     * whatever a theme happens to map to.
     */
    private Theme theme() {
        switch (this) {
            case RIFTBLADE: return Theme.PHANTOM;
            case NIGHTFALL: return Theme.VOID;
            case BLOODGLASS: return Theme.BLOODMOON;
            case ASTRAL: return Theme.NEBULA;
            case INFERNO_ENHANCED: return Theme.INFERNO;
            case ABYSS_ENHANCED: return Theme.ABYSS;
            case VAPORWAVE_ENHANCED: return Theme.VAPORWAVE;
            case OCCULT_ENHANCED: return Theme.WITCHCRAFT;
            case CRYSTAL_STORM: return Theme.STORM;
            case FROST_SPECTER: return Theme.ARCTIC;
            case MINIMAL_PVP: return Theme.MONOCHROME;
            case CINEMATIC_SHOWCASE: return Theme.NEON_TOKYO;
            case LOW_END_PERFORMANCE: return Theme.MIDNIGHT;
            case DREAMSCAPE: return Theme.CANDY;
            case LAYER_PRELUDE: return Theme.PRELUDE;
            case LAYER_LIMBO: return Theme.LIMBO;
            case LAYER_LUST: return Theme.LUST;
            case LAYER_GLUTTONY: return Theme.GLUTTONY;
            case LAYER_WRATH: return Theme.WRATH;
            case LAYER_GREED: return Theme.GREED;
            case LAYER_HERESY: return Theme.HERESY;
            case LAYER_VIOLENCE: return Theme.VIOLENCE;
            case LAYER_CYBER_GRIND: return Theme.CYBER_GRIND;
            case LAYER_PRIME_SANCTUM: return Theme.PRIME_SANCTUM;
            default: return null;
        }
    }

    private void signature(SoupAPI_Config c) {
        switch (this) {
            case RIFTBLADE:
                look(c, SkyStyle.DIMENSIONAL_RIFT, Ambience.ABYSSAL_SMOKE, KillEffects.Style.RIFT_CLOSURE);
                katana(c, MeleeFinish.OBSIDIAN);
                c.rgbMode = Palette.RgbMode.FLOW;
                c.crosshairStyle = Crosshair.Style.WEAPON;
                c.hudBetterHotbarStyle = BetterHudStyles.HotbarStyle.GLASS;
                break;
            case NIGHTFALL:
                look(c, SkyStyle.VOID_ENERGY, Ambience.VIOLET_FIREFLIES, KillEffects.Style.RAVEN_ASCENSION);
                weapon(c);
                c.crosshairStyle = Crosshair.Style.CIRCULAR;
                c.hudBetterHotbarStyle = BetterHudStyles.HotbarStyle.FLOATING;
                break;
            case BLOODGLASS:
                look(c, SkyStyle.SHATTERED_GLASS, Ambience.GLASS_FRAGMENTS, KillEffects.Style.FRACTURED_GLASS);
                katana(c, MeleeFinish.DARK_METAL);
                c.rgbMode = Palette.RgbMode.PULSE;
                c.crosshairStyle = Crosshair.Style.FOUR_SEGMENT;
                c.hudBetterHotbarStyle = BetterHudStyles.HotbarStyle.NEON;
                break;
            case ASTRAL:
                look(c, SkyStyle.ARCANE, Ambience.CONSTELLATIONS, KillEffects.Style.SOUL_CONSTELLATION);
                weapon(c);
                c.meleeRunes = true;
                c.rgbMode = Palette.RgbMode.SHIFT;
                c.crosshairStyle = Crosshair.Style.RUNE;
                c.hudBetterHotbarStyle = BetterHudStyles.HotbarStyle.GLASS;
                break;
            case INFERNO_ENHANCED:
                look(c, SkyStyle.INFERNAL, Ambience.INFERNAL_SMOKE, KillEffects.Style.MOLTEN_ERUPTION);
                katana(c, MeleeFinish.MOLTEN);
                c.landingImpactEnabled = true;
                c.landingImpactStyle = LandingImpact.Style.CRACK;
                c.crosshairStyle = Crosshair.Style.WEAPON;
                c.hudBetterHotbarStyle = BetterHudStyles.HotbarStyle.GLOW;
                break;
            case ABYSS_ENHANCED:
                look(c, SkyStyle.OCEAN_DEPTH, Ambience.ABYSSAL_SMOKE, KillEffects.Style.VOID_IMPLOSION);
                weapon(c);
                c.customFogEnabled = true;
                c.crosshairStyle = Crosshair.Style.CIRCULAR;
                c.hudBetterHotbarStyle = BetterHudStyles.HotbarStyle.GLASS;
                break;
            case VAPORWAVE_ENHANCED:
                look(c, SkyStyle.CYBERPUNK, Ambience.SPECTRAL_CURRENTS, KillEffects.Style.PHANTOM_AFTERIMAGES);
                weapon(c);
                flourish(c);
                c.killEffectsStyle = KillEffects.Style.PHANTOM_AFTERIMAGES;
                c.rgbMode = Palette.RgbMode.RAINBOW;
                c.hudBetterHotbarRgbBorder = true;
                c.crosshairStyle = Crosshair.Style.FOUR_SEGMENT;
                c.hudBetterHotbarStyle = BetterHudStyles.HotbarStyle.NEON;
                break;
            case OCCULT_ENHANCED:
                look(c, SkyStyle.BLEEDING_CLOUDS, Ambience.OCCULT_SYMBOLS, KillEffects.Style.ARCANE_EXECUTION);
                katana(c, MeleeFinish.OBSIDIAN);
                c.haloEnabled = true;
                c.crosshairStyle = Crosshair.Style.RUNE;
                c.hudBetterHotbarStyle = BetterHudStyles.HotbarStyle.GLOW;
                break;
            case CRYSTAL_STORM:
                look(c, SkyStyle.STORM_FRONT, Ambience.GLASS_FRAGMENTS, KillEffects.Style.CRYSTAL_DETONATION);
                weapon(c);
                c.meleeShards = true;
                c.crosshairStyle = Crosshair.Style.FOUR_SEGMENT;
                c.hudBetterHotbarStyle = BetterHudStyles.HotbarStyle.FLOATING;
                break;
            case FROST_SPECTER:
                look(c, SkyStyle.AURORA, Ambience.SPECTRAL_SMOKE, KillEffects.Style.PHANTOM_AFTERIMAGES);
                weapon(c);
                c.meleePhantom = true;
                c.crosshairStyle = Crosshair.Style.CIRCULAR;
                c.hudBetterHotbarStyle = BetterHudStyles.HotbarStyle.GLASS;
                break;
            case MINIMAL_PVP:
                combat(c);
                c.hitBubblesEnabled = false;
                c.hitEffectsEnabled = false;
                c.crosshairEnabled = true;
                c.crosshairStyle = Crosshair.Style.MINIMAL;
                c.crosshairDynamic = false;
                c.crosshairHitConfirm = true;
                c.ambienceEnabled = false;
                c.coloredSkyEnabled = false;
                c.smokeEnabled = false;
                c.hudBetterHotbarEnabled = true;
                c.hudBetterHotbarStyle = BetterHudStyles.HotbarStyle.MINIMAL;
                break;
            case CINEMATIC_SHOWCASE:
                look(c, SkyStyle.NEBULA, Ambience.MAGIC_DUST, KillEffects.Style.ECLIPSE_BLOOM);
                katana(c, MeleeFinish.SPECTRAL);
                flourish(c);
                c.killEffectsStyle = KillEffects.Style.ECLIPSE_BLOOM;
                c.meleePhantom = true;
                c.meleeRunes = true;
                c.meleeSummonOnEquip = true;
                c.rgbMode = Palette.RgbMode.FLOW;
                c.smokeQuality = SmokeFx.Quality.HIGH;
                c.ambienceQuality = Ambience.Quality.HIGH;
                c.crosshairStyle = Crosshair.Style.WEAPON;
                c.hudBetterHotbarStyle = BetterHudStyles.HotbarStyle.GLASS;
                break;
            case LOW_END_PERFORMANCE:
                combat(c);
                c.hitParticlesEnabled = false;
                c.ambienceEnabled = false;
                c.coloredSkyEnabled = false;
                c.customFogEnabled = false;
                c.blurShadowEnabled = false;
                c.smokeEnabled = true;
                c.smokeQuality = SmokeFx.Quality.LOW;
                c.killEffectsEnabled = true;
                c.killEffectsStyle = KillEffects.Style.RINGS;
                c.killEffectsSmoke = false;
                c.meleeGlow = false;
                c.meleeTrail = false;
                c.meleeSparks = false;
                c.meleeSummonEnabled = false;
                c.rgbMode = Palette.RgbMode.STATIC;
                c.crosshairEnabled = true;
                c.crosshairStyle = Crosshair.Style.DOT;
                c.crosshairGlow = false;
                break;
            case DREAMSCAPE:
                look(c, SkyStyle.DREAMCORE, Ambience.MAGIC_DUST, KillEffects.Style.SOUL_BURST);
                weapon(c);
                c.skyCelestials = false;
                c.crosshairStyle = Crosshair.Style.CIRCULAR;
                c.hudBetterHotbarStyle = BetterHudStyles.HotbarStyle.FLOATING;
                break;
            case LAYER_PRELUDE:
                look(c, SkyStyle.SILK, Ambience.DUST, KillEffects.Style.RINGS);
                blade(c, MeleeFinish.SPECTRAL);
                c.crosshairStyle = Crosshair.Style.FOUR_SEGMENT;
                c.hudBetterHotbarStyle = BetterHudStyles.HotbarStyle.GLASS;
                c.skySpeed = 40;
                c.ambienceEnabled = false;          // the opening is meant to be empty
                break;
            case LAYER_LIMBO:
                look(c, SkyStyle.OCEAN_DEPTH, Ambience.BUBBLES, KillEffects.Style.SOUL_BURST);
                blade(c, MeleeFinish.SPECTRAL);
                c.crosshairStyle = Crosshair.Style.CIRCULAR;
                c.hudBetterHotbarStyle = BetterHudStyles.HotbarStyle.GLASS;
                break;
            case LAYER_LUST:
                look(c, SkyStyle.NEON_HALO, Ambience.NEON_RAIN, KillEffects.Style.PORTAL);
                weapon(c);
                c.rgbMode = Palette.RgbMode.FLOW;
                c.crosshairStyle = Crosshair.Style.CIRCULAR;
                c.hudBetterHotbarStyle = BetterHudStyles.HotbarStyle.NEON;
                break;
            case LAYER_GLUTTONY:
                look(c, SkyStyle.BLEEDING_CLOUDS, Ambience.ASH, KillEffects.Style.SOUL_SIPHON);
                claw(c, MeleeFinish.DARK_METAL);
                c.crosshairStyle = Crosshair.Style.WEAPON;
                c.hudBetterHotbarStyle = BetterHudStyles.HotbarStyle.GLOW;
                break;
            case LAYER_WRATH:
                look(c, SkyStyle.TEMPEST, Ambience.RAIN, KillEffects.Style.LIGHTNING);
                weapon(c);
                c.landingImpactEnabled = true;
                c.landingImpactStyle = LandingImpact.Style.SHOCKWAVE;
                c.crosshairStyle = Crosshair.Style.FOUR_SEGMENT;
                c.hudBetterHotbarStyle = BetterHudStyles.HotbarStyle.FLOATING;
                break;
            case LAYER_GREED:
                look(c, SkyStyle.GOLDEN_HAZE, Ambience.SANDSTORM, KillEffects.Style.RUNIC_CROWN);
                katana(c, MeleeFinish.MOLTEN);
                c.crosshairStyle = Crosshair.Style.RUNE;
                c.hudBetterHotbarStyle = BetterHudStyles.HotbarStyle.GLOW;
                break;
            case LAYER_HERESY:
                look(c, SkyStyle.HERESY_CROWN, Ambience.EMBERS, KillEffects.Style.FLAME_PILLAR);
                katana(c, MeleeFinish.MOLTEN);
                c.landingImpactEnabled = true;
                c.landingImpactStyle = LandingImpact.Style.CRACK;
                c.crosshairStyle = Crosshair.Style.WEAPON;
                c.hudBetterHotbarStyle = BetterHudStyles.HotbarStyle.GLOW;
                break;
            case LAYER_VIOLENCE:
                look(c, SkyStyle.BLOOD_SUN, Ambience.BLOOD_RAIN, KillEffects.Style.MOLTEN_ERUPTION);
                claw(c, MeleeFinish.DARK_METAL);
                c.rgbMode = Palette.RgbMode.PULSE;
                c.crosshairStyle = Crosshair.Style.WEAPON;
                c.hudBetterHotbarStyle = BetterHudStyles.HotbarStyle.NEON;
                break;
            case LAYER_CYBER_GRIND:
                look(c, SkyStyle.CYBER_PULSE, Ambience.DIGITAL_SNOW, KillEffects.Style.EXPLOSION);
                weapon(c);
                c.meleeOutline = true;
                c.crosshairStyle = Crosshair.Style.FOUR_SEGMENT;
                c.hudBetterHotbarStyle = BetterHudStyles.HotbarStyle.NEON;
                c.skySpeed = 180;
                break;
            case LAYER_PRIME_SANCTUM:
                look(c, SkyStyle.HEAVEN_GATE, Ambience.GOLD_DUST, KillEffects.Style.SOUL_CONSTELLATION);
                blade(c, MeleeFinish.DEFAULT);
                c.meleeRunes = true;
                c.haloEnabled = true;
                c.crosshairStyle = Crosshair.Style.RUNE;
                c.hudBetterHotbarStyle = BetterHudStyles.HotbarStyle.GLASS;
                break;
            default:
                break;
        }
    }

    /** The shared base of the newer presets: combat feedback, a sky, an ambience, a kill effect, a HUD. */
    private static void look(SoupAPI_Config c, SkyStyle sky, Ambience ambience, KillEffects.Style kill) {
        combat(c);
        c.skyEnabled = true;
        c.coloredSkyEnabled = true;
        c.skyStyle = sky;
        c.ambienceEnabled = true;
        c.ambienceStyle = ambience;
        c.smokeEnabled = true;
        c.killEffectsEnabled = true;
        c.killEffectsStyle = kill;
        c.crosshairEnabled = true;
        c.hudBetterHotbarEnabled = true;
        c.trailsEnabled = true;
    }

    /** The carried weapon, the katana's eyes and portal, in a finish. Which weapon is carried stays the player's. */
    private static void katana(SoupAPI_Config c, MeleeFinish finish) {
        weapon(c);
        c.meleeFinish = finish;
        c.meleeSummonEnabled = true;
    }

    /** The claw, its feathers and its summon, in a finish. */
    private static void claw(SoupAPI_Config c, MeleeFinish finish) {
        weapon(c);
        c.meleeFinish = finish;
        c.meleeSummonEnabled = true;
    }

    /** The spectral blade, its ribbons and its summon, in a finish. */
    private static void blade(SoupAPI_Config c, MeleeFinish finish) {
        weapon(c);
        c.meleeFinish = finish;
        c.meleeSummonEnabled = true;
        c.meleeGlow = true;
    }

    /** The things worth having in a fight, and nothing that covers the screen. */
    private static void combat(SoupAPI_Config config) {
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
        if (name.startsWith("rgb") || name.startsWith("smoke") || name.startsWith("crosshair")
                || name.startsWith("killEffects") || name.startsWith("hudBetterHotbar")) return true;
        return name.equals("particleSymbol") || name.equals("particleSpriteOverride")
                || name.equals("coloredSkyColor") || name.equals("coloredSkyAlpha")
                || name.equals("coloredSkyUseTheme");
    }

    /** The player's own settings, which no preset touches. */
    private static final String[] KEPT = {
            "friends", "capes", "customCapes", "waterMarkX", "waterMarkY", "configPos",
            "meleeWeapon", "meleeDisabled", "meleePlacements", "meleePackPath", "presetStyle",
            // Where the weapon and hands sit, and how loud things are, belong to the player too.
            "meleeSize", "meleeDistance", "meleeOffset", "meleeYaw", "meleePitch", "meleeRoll",
            "meleeHandsScale", "meleeHandsPitch", "meleeHandsYaw", "meleeHandsOffset", "meleeLeftHand",
            "meleeSoundVolume", "meleeSummonVolume", "meleeShowWhen", "meleeThirdPerson",
            "gui",
    };
}
