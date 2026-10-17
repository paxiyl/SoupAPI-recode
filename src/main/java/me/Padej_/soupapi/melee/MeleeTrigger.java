package me.Padej_.soupapi.melee;

import net.minecraft.item.Item;
import net.minecraft.item.ItemAxe;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemSpade;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.item.ItemTool;

/** When the carried weapon is drawn, based on what is in the player's hand. */
public enum MeleeTrigger {

    /** Whatever is held, including an empty hand. */
    ALWAYS,
    /** Only while a sword is held — the weapon stands in for it, and nothing else. */
    SWORD,
    /** A sword or an axe. */
    SWORD_OR_AXE,
    /** Anything that hits harder than a fist: swords, axes, picks, shovels, hoes. */
    ANY_TOOL,
    /** Anything at all, but not an empty hand. */
    ANY_ITEM;

    /** True when {@code held} should have the weapon drawn in its place. */
    public boolean matches(ItemStack held) {
        if (this == ALWAYS) return true;
        if (held == null) return false;
        Item item = held.getItem();
        if (item == null) return false;
        switch (this) {
            case SWORD:
                return item instanceof ItemSword;
            case SWORD_OR_AXE:
                return item instanceof ItemSword || item instanceof ItemAxe;
            case ANY_TOOL:
                return item instanceof ItemSword || item instanceof ItemTool
                        || item instanceof ItemAxe || item instanceof ItemPickaxe
                        || item instanceof ItemSpade || item instanceof ItemHoe;
            case ANY_ITEM:
                return true;
            default:
                return true;
        }
    }
}
