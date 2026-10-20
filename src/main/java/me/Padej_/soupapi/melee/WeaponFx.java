package me.Padej_.soupapi.melee;

import me.Padej_.soupapi.bedrock.GeoBone;
import me.Padej_.soupapi.bedrock.GeoModel;
import me.Padej_.soupapi.bedrock.GeoPose;
import me.Padej_.soupapi.config.ConfigurableModule;
import me.Padej_.soupapi.utils.FxClock;
import me.Padej_.soupapi.utils.Palette;

import java.awt.Color;
import java.util.Random;

/**
 * The life a weapon has beyond its clips: what moves on its own, and what glows.
 *
 * <p>Clips are authored once and play the same every time; a weapon that is meant to be alive needs
 * motion that is not. The Fractured-Eye Katana's eyes glance about in idle and go wild — darting
 * independently, dilating, flaring blood red — whenever it swings, is summoned, flourished or
 * inspected. The Raven Claw's spine breathes in a slow travelling flex. The Spectral Blade's ribbons
 * flicker. All of it is layered on top of whatever the clips wrote, and is purely visual.
 *
 * <p>It also decides the colour of the emissive pass ({@code _illuminated} bones), which is how the
 * eyes, runes, gems and ribbons light themselves.
 */
public final class WeaponFx extends ConfigurableModule {

    private static final Random RANDOM = new Random();
    /** Per-eye target offsets and when each eye next looks somewhere else. */
    private static final float[] EYE_X = new float[64], EYE_Y = new float[64], EYE_NEXT = new float[64];
    private static final float[] EYE_CX = new float[64], EYE_CY = new float[64];
    /** 0 calm .. 1 frenzied, eased so the eyes do not snap between moods. */
    private static float frenzy;
    private static float time;

    private WeaponFx() {
    }

    public static float frenzy() {
        return frenzy;
    }

    static void procedural(MeleeWeapon weapon, GeoModel model, GeoPose pose, MeleeState state, float delta) {
        if (weapon == null || model == null) return;
        time += delta;
        float target = state.intense() ? 1f : 0f;
        float rate = target > frenzy ? 6f : 1.2f;
        frenzy += (target - frenzy) * Math.min(1f, delta * rate);

        String profile = weapon.profileName();
        if ("fractured_eye".equals(profile)) eyes(model, pose, delta);
        else if ("raven_claw".equals(profile)) spine(weapon, model, pose);
        else if ("spectral_blade".equals(profile)) ribbons(model, pose);
    }

    /** Each eye picks a new place to look every so often - far more often, and further, when frenzied. */
    private static void eyes(GeoModel model, GeoPose pose, float delta) {
        float amount = CONFIG.meleeEyeAnimation / 100f;
        if (amount <= 0f) return;
        float f = frenzy;
        for (int k = 0; k < 64; k++) {
            int index = model.indexOf("eye_" + k + "_iris_illuminated");
            if (index < 0) {
                if (k > 0) break;
                continue;
            }
            if (time >= EYE_NEXT[k]) {
                float reach = (0.08f + 0.3f * f) * amount;
                if (f < 0.3f && RANDOM.nextFloat() < 0.6f && k > 0) {
                    // Calm: the eyes mostly glance together, as a creature would.
                    EYE_X[k] = EYE_X[0];
                    EYE_Y[k] = EYE_Y[0];
                } else {
                    EYE_X[k] = (RANDOM.nextFloat() * 2f - 1f) * reach;
                    EYE_Y[k] = (RANDOM.nextFloat() * 2f - 1f) * reach * 0.55f;
                }
                EYE_NEXT[k] = time + (f > 0.5f ? 0.06f + RANDOM.nextFloat() * 0.14f : 0.8f + RANDOM.nextFloat() * 2.2f);
            }
            // Saccades: fast to the new spot, then still.
            float speed = Math.min(1f, delta * (f > 0.5f ? 40f : 18f));
            EYE_CX[k] += (EYE_X[k] - EYE_CX[k]) * speed;
            EYE_CY[k] += (EYE_Y[k] - EYE_CY[k]) * speed;
            pose.add(index, EYE_CX[k], EYE_CY[k], 0f, 0f, 0f, 0f);
            // Pupils dilate in the frenzy and twitch with it.
            float pulse = 1f + f * amount * (0.2f + 0.15f * (float) Math.sin(time * 23f + k * 1.7f));
            pose.scaleBy(index, pulse, pulse, 1f);
        }
    }

    /** A breathing flex travelling down the spine, and feathers that never quite settle. */
    private static void spine(MeleeWeapon weapon, GeoModel model, GeoPose pose) {
        float amount = CONFIG.meleeClawFlex / 100f;
        if (amount <= 0f) return;
        int segments = Math.max(1, weapon.count("spine"));
        for (int k = 0; k < segments; k++) {
            int index = model.indexOf("spine_" + k);
            if (index < 0) break;
            float wave = (float) Math.sin(time * 1.6f - k * 0.5f) * 0.7f * amount;
            pose.add(index, 0f, 0f, 0f, 0f, 0f, wave);
            int feather = model.indexOf("feather_" + k);
            if (feather >= 0) {
                pose.add(feather, 0f, 0f, 0f, 0f, 0f, (float) Math.sin(time * 5.3f + k * 2.1f) * 2.2f * amount);
            }
        }
    }

    private static void ribbons(GeoModel model, GeoPose pose) {
        for (GeoBone bone : model.bones()) {
            if (!bone.name.startsWith("ribbon_")) continue;
            float flicker = 1f + 0.06f * (float) Math.sin(time * 17f + bone.index() * 2.3f);
            pose.scaleBy(bone.index(), 1f, flicker, 1f);
        }
    }

    // ---- glow ------------------------------------------------------------------------------------

    /**
     * The colour the emissive pass draws in, with its strength as alpha. The katana's eyes run from a
     * banked ember in idle to full blood red when frenzied; the rest follow their profile colour, or
     * the theme when that is preferred.
     */
    static float[] emissive(MeleeWeapon weapon) {
        Color base = weapon.colour("emissive");
        if (base == null) base = MeleeRenderer.glowColour();
        float strength = CONFIG.meleeEmissiveStrength / 100f;
        float r = base.getRed() / 255f, g = base.getGreen() / 255f, b = base.getBlue() / 255f;
        if ("fractured_eye".equals(weapon.profileName())) {
            Color eye = CONFIG.meleeEyeUseTheme ? Palette.getColor(0f) : new Color(CONFIG.meleeEyeColor);
            float er = eye.getRed() / 255f, eg = eye.getGreen() / 255f, eb = eye.getBlue() / 255f;
            float f = frenzy;
            // Idle: a slow pulse, dim. Frenzied: bright, and a fast flicker, as if the blade were angry.
            float pulse = 0.55f + 0.2f * (float) Math.sin(FxClock.seconds() * 2.2f);
            float rage = 0.85f + 0.15f * (float) Math.sin(FxClock.seconds() * 31f);
            float k = (1f - f) * pulse + f * rage * 1.6f;
            r = er;
            g = eg * (1f - 0.6f * f);
            b = eb * (1f - 0.6f * f);
            strength *= k * (CONFIG.meleeEyeIntensity / 100f);
        } else if (CONFIG.meleeGlowUseTheme && Palette.animates(Palette.Element.WEAPON)) {
            Color c = Palette.getColor(0f);
            r = c.getRed() / 255f;
            g = c.getGreen() / 255f;
            b = c.getBlue() / 255f;
        }
        return new float[]{r, g, b, Math.max(0f, Math.min(1.5f, strength))};
    }
}
