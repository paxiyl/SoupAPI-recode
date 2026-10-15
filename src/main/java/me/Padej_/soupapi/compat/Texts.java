package me.Padej_.soupapi.compat;

import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

/**
 * Factory methods for {@link Text}, replacing the static {@code Text.translatable} /
 * {@code Text.literal} helpers added in 1.19.
 *
 * <p>1.8.9 has {@code Text} but builds instances through the {@code LiteralText} and
 * {@code TranslatableText} subclasses.
 */
public final class Texts {

    private Texts() {
    }

    public static Text literal(String text) {
        return new LiteralText(text);
    }

    public static Text translatable(String key) {
        return new TranslatableText(key);
    }

    public static Text translatable(String key, Object... args) {
        return new TranslatableText(key, args);
    }

    /**
     * Flattens a {@link Text} to a plain string. 1.8.9 spells this {@code asUnformattedString};
     * 1.21.4 called it {@code getString()}.
     */
    public static String toPlainString(Text text) {
        return text == null ? "" : text.asUnformattedString();
    }
}
