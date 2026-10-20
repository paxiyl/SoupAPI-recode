package me.Padej_.soupapi.compat;

import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;

/**
 * Factory methods for chat components, replacing the static {@code Text.translatable} /
 * {@code Text.literal} helpers added in 1.19.
 *
 * <p>1.8.9 calls the type {@link IChatComponent} and builds instances through the
 * {@code ChatComponentText} / {@code ChatComponentTranslation} subclasses.
 */
public final class Texts {

    private Texts() {
    }

    public static IChatComponent literal(String text) {
        return new ChatComponentText(text == null ? "" : text);
    }

    public static IChatComponent translatable(String key) {
        return new ChatComponentTranslation(key);
    }

    public static IChatComponent translatable(String key, Object... args) {
        return new ChatComponentTranslation(key, args);
    }

    /** Flattens a component to a plain string. 1.8.9 spells this {@code getUnformattedText}. */
    public static String toPlainString(IChatComponent text) {
        return text == null ? "" : text.getUnformattedText();
    }
}
