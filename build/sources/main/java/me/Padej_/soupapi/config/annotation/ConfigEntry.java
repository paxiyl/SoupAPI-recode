package me.Padej_.soupapi.config.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Field metadata for config entries, replacing Cloth Config's {@code ConfigEntry}.
 *
 * <p>The nesting mirrors Cloth's exactly ({@code ConfigEntry.Category},
 * {@code ConfigEntry.Gui.PrefixText}, and so on) so that the 747-line config class ports with an
 * import change and nothing else. The annotations are read at runtime by
 * {@link me.Padej_.soupapi.config.ConfigSchema}, which the in-game settings screen uses to lay out
 * and clamp each entry — the same job Cloth's generated screen used to do.
 */
public final class ConfigEntry {

    private ConfigEntry() {
    }

    /** Groups entries into a settings page. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Category {
        String value();
    }

    /** Inclusive numeric bounds; sliders clamp to this range. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface BoundedDiscrete {
        long min() default 0L;

        long max();
    }

    /** Renders an int entry as a packed 0xRRGGBB colour swatch rather than a number. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface ColorPicker {
        boolean allowAlpha() default false;
    }

    /** Presentation-only hints. */
    public static final class Gui {

        private Gui() {
        }

        /** Starts a new visual group above this entry. */
        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.FIELD)
        public @interface PrefixText {
        }

        /** Hover text key for this entry. */
        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.FIELD)
        public @interface Tooltip {
            int count() default 1;
        }

        /** How an enum entry is presented. */
        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.FIELD)
        public @interface EnumHandler {
            EnumDisplayOption option() default EnumDisplayOption.BUTTON;

            enum EnumDisplayOption {
                /** Click to advance to the next constant. */
                BUTTON,
                /** Open a list of every constant. */
                DROPDOWN
            }
        }
    }
}
