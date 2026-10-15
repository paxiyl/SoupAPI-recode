package me.Padej_.soupapi.utils;

import static me.Padej_.soupapi.config.ConfigurableModule.CONFIG;

public class MC_Tiers {
    public static String getMcTiersIcon() {
        return "mctiers";
    }

    public static String getMcTiersGameModeIcon() {
        switch (CONFIG.mctiersGameMode) {
            case LTMs:
                return "2v2";
            case VANILLA:
                return "vanilla";
            case UHC:
                return "uhc";
            case POT:
                return "pot";
            case NETHER_OP:
                return "nethop";
            case SMP:
                return "smp";
            case SWORD:
                return "sword";
            case AXE:
                return "axe";
            case MACE:
                return "mace";
        }
        throw new IllegalStateException("Unexpected value: " + CONFIG.mctiersGameMode);
    }

    public enum TierGameModes {
        LTMs,
        VANILLA,
        UHC,
        POT,
        NETHER_OP,
        SMP,
        SWORD,
        AXE,
        MACE
    }

}
