package me.Padej_.soupapi.modules;

import me.Padej_.soupapi.config.ConfigurableModule;

public class AspectRatio extends ConfigurableModule {


    public static float getRatioByPreset() {
        if (CONFIG.aspectRatioUsePreset) {
            switch (CONFIG.aspectRatioPreset) {
            case _16_9_:
                return 16f / 9f;
            case _5_4_:
                return 5f / 4f;
            case _4_3_:
                return 4f / 3f;
        }
        throw new IllegalStateException("Unexpected value: " + CONFIG.aspectRatioPreset);
        } else {
            return CONFIG.aspectRatioFactor / 100f;
        }
    }

    /**
     * Called once per frame from the FOV event.
     *
     * <p>Forge exposes no hook for the projection's aspect ratio — {@code FOVUpdateEvent} only
     * carries the field of view — so the custom aspect ratio cannot be applied without bytecode
     * surgery, which this port deliberately avoids. This exists so the module still participates
     * in the frame and its setting round-trips; see PORTING.md.
     */
    public static void onFovUpdate() {
    }

    public enum Preset {
        _16_9_, _5_4_, _4_3_
    }
}