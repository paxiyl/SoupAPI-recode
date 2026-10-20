package me.Padej_.soupapi.melee;

/**
 * Alternative materials a weapon can ship, as texture variants beside its base texture
 * ({@code <texture>_<id>.png} and its {@code _s} glow mask). A weapon without the chosen finish keeps
 * its own look.
 */
public enum MeleeFinish {
    DEFAULT(""),
    OBSIDIAN("obsidian"),
    MOLTEN("molten"),
    SPECTRAL("spectral"),
    DARK_METAL("dark_metal");

    private final String id;

    MeleeFinish(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}
