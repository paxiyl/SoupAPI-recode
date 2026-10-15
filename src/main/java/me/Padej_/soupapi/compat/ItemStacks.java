package me.Padej_.soupapi.compat;

import net.minecraft.item.ItemStack;

/**
 * Helpers for the 1.8.9 {@link ItemStack} model.
 *
 * <p>{@code ItemStack.isEmpty()} and {@code getCount()} arrived in 1.11 along with the "empty
 * stack" sentinel. In 1.8.9 an absent item is a {@code null} reference and the size lives in the
 * public {@code stackSize} field, so every ported {@code stack.isEmpty()} becomes
 * {@link #isEmpty(ItemStack)} — which also keeps the null check that 1.8.9 genuinely needs.
 */
public final class ItemStacks {

    private ItemStacks() {
    }

    public static boolean isEmpty(ItemStack stack) {
        return stack == null || stack.getItem() == null || stack.stackSize <= 0;
    }

    public static int getCount(ItemStack stack) {
        return isEmpty(stack) ? 0 : stack.stackSize;
    }
}
