package me.Padej_.soupapi.reduce;

import me.Padej_.soupapi.config.ConfigurableModule;
import me.Padej_.soupapi.utils.MC_Tiers;

public class ModuleSupressor extends ConfigurableModule {

    public static boolean disableItemOverlay() {
        // 1.8.9 keeps the connected server on the client, not the network handler.
        if (mc.getCurrentServerEntry() == null || mc.getCurrentServerEntry().address == null) return false;
        String ip = mc.getCurrentServerEntry().address.toLowerCase();
        return
                // servers
                ip.contains("minefun") ||
                ip.contains(".minehub.") ||

                // MC Tiers
                isVanilla() ||
                isUHC() ||
                isSMP() ||
                isNethop() ||
                isPot()
                ;
    }

    public static boolean disableHPBar() {
        // 1.8.9 keeps the connected server on the client, not the network handler.
        if (mc.getCurrentServerEntry() == null || mc.getCurrentServerEntry().address == null) return false;
        String ip = mc.getCurrentServerEntry().address.toLowerCase();
        return
                // servers
                ip.contains(".minehub.") ||

                // MC Tiers
                isVanilla() ||
                isUHC() ||
                isSMP() ||
                isNethop() ||
                isPot()
                ;
    }

    private static boolean isLTMs() {
        return CONFIG.mctiersEnabled && CONFIG.mctiersGameMode.equals(MC_Tiers.TierGameModes.LTMs);
    }

    private static boolean isVanilla() {
        return CONFIG.mctiersEnabled && CONFIG.mctiersGameMode.equals(MC_Tiers.TierGameModes.VANILLA);
    }

    private static boolean isUHC() {
        return CONFIG.mctiersEnabled && CONFIG.mctiersGameMode.equals(MC_Tiers.TierGameModes.UHC);
    }

    private static boolean isPot() {
        return CONFIG.mctiersEnabled && CONFIG.mctiersGameMode.equals(MC_Tiers.TierGameModes.POT);
    }

    private static boolean isNethop() {
        return CONFIG.mctiersEnabled && CONFIG.mctiersGameMode.equals(MC_Tiers.TierGameModes.NETHER_OP);
    }

    private static boolean isSMP() {
        return CONFIG.mctiersEnabled && CONFIG.mctiersGameMode.equals(MC_Tiers.TierGameModes.SMP);
    }

    private static boolean isSword() {
        return CONFIG.mctiersEnabled && CONFIG.mctiersGameMode.equals(MC_Tiers.TierGameModes.SWORD);
    }

    private static boolean isMace() {
        return CONFIG.mctiersEnabled && CONFIG.mctiersGameMode.equals(MC_Tiers.TierGameModes.MACE);
    }

}
