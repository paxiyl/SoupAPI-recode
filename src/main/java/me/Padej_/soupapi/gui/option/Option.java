package me.Padej_.soupapi.gui.option;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * One editable thing in the settings screen.
 *
 * <p>Deliberately decoupled from where the value lives. The mod carries two config systems — the
 * annotated {@code SoupAPI_Config} fields that Cloth Config used to render, and the newer
 * {@code Setting} objects on {@code SoupModule}s — and the old screen could only show the second,
 * which is why most of the mod was unreachable from the GUI. Each kind below is built from a getter
 * and a setter, so {@link ConfigSchema} can adapt both systems onto the same widgets.
 */
public abstract class Option {

    private final String label;
    private final String tooltip;

    protected Option(String label, String tooltip) {
        this.label = label == null ? "" : label;
        this.tooltip = tooltip;
    }

    public String label() {
        return label;
    }

    /** Hover text, or {@code null}. */
    public String tooltip() {
        return tooltip;
    }

    /** True for rows that are headers rather than controls. */
    public boolean interactive() {
        return true;
    }

    /** Puts the value back to the one the mod ships with. */
    public void reset() {
    }

    /** Whether the value currently equals the default; drives the "modified" marker. */
    public boolean isDefault() {
        return true;
    }

    /** A group heading. Not editable; drawn as a separator with a caption. */
    public static final class Header extends Option {
        public Header(String label) {
            super(label, null);
        }

        @Override
        public boolean interactive() {
            return false;
        }
    }

    public static final class Toggle extends Option {
        private final BooleanSupplier getter;
        private final Consumer<Boolean> setter;
        private final boolean defaultValue;

        public Toggle(String label, String tooltip, BooleanSupplier getter, Consumer<Boolean> setter,
                      boolean defaultValue) {
            super(label, tooltip);
            this.getter = getter;
            this.setter = setter;
            this.defaultValue = defaultValue;
        }

        public boolean get() {
            return getter.getAsBoolean();
        }

        public void set(boolean value) {
            setter.accept(value);
        }

        public void toggle() {
            set(!get());
        }

        @Override
        public void reset() {
            set(defaultValue);
        }

        @Override
        public boolean isDefault() {
            return get() == defaultValue;
        }
    }

    /**
     * Any number: {@code int} or {@code float}, bounded or not.
     *
     * <p>A bounded entry (Cloth's {@code @BoundedDiscrete}) draws as a slider. An unbounded one —
     * HUD coordinates, hand offsets — draws as a value you drag or type into, because inventing a
     * range for it would quietly stop the value going where the user needs it.
     */
    public static final class Num extends Option {
        private final DoubleSupplier getter;
        private final DoubleConsumer setter;
        private final double min;
        private final double max;
        private final boolean bounded;
        private final boolean integral;
        private final double defaultValue;
        private final double dragStep;

        public Num(String label, String tooltip, DoubleSupplier getter, DoubleConsumer setter,
                   double min, double max, boolean bounded, boolean integral,
                   double defaultValue, double dragStep) {
            super(label, tooltip);
            this.getter = getter;
            this.setter = setter;
            this.min = min;
            this.max = max;
            this.bounded = bounded;
            this.integral = integral;
            this.defaultValue = defaultValue;
            this.dragStep = dragStep;
        }

        public double get() {
            return getter.getAsDouble();
        }

        public void set(double value) {
            if (bounded) {
                value = Math.max(min, Math.min(max, value));
            }
            setter.accept(integral ? Math.round(value) : value);
        }

        public double min() {
            return min;
        }

        public double max() {
            return max;
        }

        public boolean bounded() {
            return bounded;
        }

        public boolean integral() {
            return integral;
        }

        public double dragStep() {
            return dragStep;
        }

        /** Position in 0..1 across the range; only meaningful when {@link #bounded()}. */
        public double fraction() {
            if (!bounded || max <= min) return 0.0;
            return (get() - min) / (max - min);
        }

        public void setFraction(double fraction) {
            set(min + Math.max(0.0, Math.min(1.0, fraction)) * (max - min));
        }

        public String display() {
            double v = get();
            return integral ? String.valueOf((long) Math.round(v)) : trim(v);
        }

        private static String trim(double v) {
            String s = String.format(java.util.Locale.ROOT, "%.2f", v);
            if (s.contains(".")) {
                s = s.replaceAll("0+$", "");
                if (s.endsWith(".")) s = s.substring(0, s.length() - 1);
            }
            return s;
        }

        @Override
        public void reset() {
            set(defaultValue);
        }

        @Override
        public boolean isDefault() {
            return Math.abs(get() - defaultValue) < 1.0E-6;
        }
    }

    /** An enum. Cycles in place when short; opens a picker when there are too many to cycle. */
    public static final class Choice extends Option {
        private final Supplier<Enum<?>> getter;
        private final Consumer<Enum<?>> setter;
        private final Enum<?>[] values;
        private final Enum<?> defaultValue;

        public Choice(String label, String tooltip, Supplier<Enum<?>> getter, Consumer<Enum<?>> setter,
                      Enum<?>[] values, Enum<?> defaultValue) {
            super(label, tooltip);
            this.getter = getter;
            this.setter = setter;
            this.values = values;
            this.defaultValue = defaultValue;
        }

        public Enum<?> get() {
            return getter.get();
        }

        public void set(Enum<?> value) {
            setter.accept(value);
        }

        public Enum<?>[] values() {
            return values;
        }

        /** True when the list is long enough that clicking through it is unreasonable. */
        public boolean needsPicker() {
            return values.length > 8;
        }

        public void cycle(int direction) {
            if (values.length == 0) return;
            int index = 0;
            Enum<?> current = get();
            for (int i = 0; i < values.length; i++) {
                if (values[i] == current) {
                    index = i;
                    break;
                }
            }
            index = (index + direction) % values.length;
            if (index < 0) index += values.length;
            set(values[index]);
        }

        public String display() {
            return prettify(get());
        }

        /** {@code CIRCLE_BOLD} reads as "Circle Bold"; {@code _16_9_} as "16:9". */
        public static String prettify(Enum<?> value) {
            if (value == null) return "-";
            String raw = value.name();
            if (raw.startsWith("_") && raw.endsWith("_") && raw.length() > 2) {
                String inner = raw.substring(1, raw.length() - 1);
                if (inner.matches("\\d+_\\d+")) {
                    return inner.replace('_', ':');
                }
            }
            String[] words = raw.toLowerCase(java.util.Locale.ROOT).split("_");
            StringBuilder out = new StringBuilder();
            for (String word : words) {
                if (word.isEmpty()) continue;
                if (out.length() > 0) out.append(' ');
                out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
            }
            return out.length() == 0 ? raw : out.toString();
        }

        @Override
        public void reset() {
            set(defaultValue);
        }

        @Override
        public boolean isDefault() {
            return get() == defaultValue;
        }
    }

    /** A packed colour. {@code allowAlpha} decides whether the alpha byte is editable. */
    public static final class Colour extends Option {
        private final IntSupplier getter;
        private final IntConsumer setter;
        private final boolean allowAlpha;
        private final int defaultValue;

        public Colour(String label, String tooltip, IntSupplier getter, IntConsumer setter,
                      boolean allowAlpha, int defaultValue) {
            super(label, tooltip);
            this.getter = getter;
            this.setter = setter;
            this.allowAlpha = allowAlpha;
            this.defaultValue = defaultValue;
        }

        public int get() {
            return getter.getAsInt();
        }

        public void set(int value) {
            setter.accept(value);
        }

        public boolean allowAlpha() {
            return allowAlpha;
        }

        public int component(int shift) {
            return (get() >> shift) & 0xFF;
        }

        public void setComponent(int shift, int value) {
            int v = Math.max(0, Math.min(255, value));
            set((get() & ~(0xFF << shift)) | (v << shift));
        }

        public String hex() {
            return allowAlpha
                    ? String.format("%08X", get())
                    : String.format("%06X", get() & 0xFFFFFF);
        }

        @Override
        public void reset() {
            set(defaultValue);
        }

        @Override
        public boolean isDefault() {
            return get() == defaultValue;
        }
    }

    /** A single line of text. */
    public static final class Text extends Option {
        private final Supplier<String> getter;
        private final Consumer<String> setter;
        private final String defaultValue;

        public Text(String label, String tooltip, Supplier<String> getter, Consumer<String> setter,
                    String defaultValue) {
            super(label, tooltip);
            this.getter = getter;
            this.setter = setter;
            this.defaultValue = defaultValue;
        }

        public String get() {
            String v = getter.get();
            return v == null ? "" : v;
        }

        public void set(String value) {
            setter.accept(value);
        }

        @Override
        public void reset() {
            set(defaultValue);
        }

        @Override
        public boolean isDefault() {
            return get().equals(defaultValue == null ? "" : defaultValue);
        }
    }

    /** A list of strings — the friends list. Edited on its own screen. */
    public static final class TextList extends Option {
        private final Supplier<String[]> getter;
        private final Consumer<String[]> setter;

        public TextList(String label, String tooltip, Supplier<String[]> getter, Consumer<String[]> setter) {
            super(label, tooltip);
            this.getter = getter;
            this.setter = setter;
        }

        public String[] get() {
            String[] v = getter.get();
            return v == null ? new String[0] : v;
        }

        public void set(String[] value) {
            setter.accept(value == null ? new String[0] : value);
        }

        @Override
        public void reset() {
            set(new String[0]);
        }

        @Override
        public boolean isDefault() {
            return get().length == 0;
        }
    }

    /** A button that does something — opens a sub-screen, reloads a cape. */
    public static final class Action extends Option {
        private final String buttonText;
        private final Runnable action;

        public Action(String label, String tooltip, String buttonText, Runnable action) {
            super(label, tooltip);
            this.buttonText = buttonText;
            this.action = action;
        }

        public String buttonText() {
            return buttonText;
        }

        public void run() {
            if (action != null) action.run();
        }
    }
}
