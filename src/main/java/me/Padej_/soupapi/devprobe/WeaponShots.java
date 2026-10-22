package me.Padej_.soupapi.devprobe;

import me.Padej_.soupapi.SoupAPI;
import me.Padej_.soupapi.config.SoupAPI_Config;
import me.Padej_.soupapi.melee.Melee;
import me.Padej_.soupapi.melee.MeleePack;
import me.Padej_.soupapi.melee.MeleeWeapon;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;

import java.util.List;

/** TEMPORARY: equips each weapon in a flat world and photographs it from a few angles. */
public final class WeaponShots {

    private WeaponShots() {
    }

    private static final String[] IDS = System.getProperty("soupapi.sweep") != null
            ? new String[]{System.getProperty("soupapi.sweep")}
            : new String[]{"fractured_eye_katana", "raven_claw", "spectral_blade"};

    /** size, distance, offsetX, offsetY — one placement per shot, when sweeping. */
    private static final int[][] PLACEMENTS = {
            {16, 30, 14, -16}, {16, 60, 14, -16}, {16, 95, 14, -16},
            {24, 30, 18, -22}, {24, 60, 18, -22}, {24, 95, 18, -22},
            {34, 30, 24, -30}, {34, 60, 24, -30}, {34, 95, 24, -30},
    };
    private static final boolean SWEEP = System.getProperty("soupapi.sweep") != null
            && System.getProperty("soupapi.summon") == null;
    /** Summon mode: shoot the whole arrival, a frame every few ticks. */
    private static final boolean SUMMON = System.getProperty("soupapi.summon") != null;
    private static final int SUMMON_SHOTS = 8;
    /** pitch, yaw — the camera per shot when not sweeping. */
    private static final float[][] VIEWS = {
            {0, 0, 0, 0, 0, 0},
            {-20, 0, 0, 0, 0, 0},
    };
    private static int shots() {
        return SWEEP ? PLACEMENTS.length : (SUMMON ? SUMMON_SHOTS : VIEWS.length);
    }
    private static int ticks, phase, index, step, wait;
    private static net.minecraft.client.gui.GuiScreen wanted;

    public static void frame(Minecraft mc) {
        if (System.getProperty("soupapi.weapons") == null || ticks < 40) return;
        hold(mc);
    }

    private static void hold(Minecraft mc) {
        try {
            mc.inGameHasFocus = true;
            if (mc.currentScreen != wanted) mc.displayGuiScreen(wanted);
            if (mc.theWorld != null) mc.theWorld.setWorldTime(1000L);
            if (mc.guiAchievement != null) mc.guiAchievement.clearAchievements();
        } catch (Throwable ignored) {
        }
    }

    public static void tick(Minecraft mc) {
        if (System.getProperty("soupapi.weapons") == null) return;
        ticks++;
        if (ticks < 40) return;
        hold(mc);
        switch (phase) {
            case 0:
                log("launching a flat world");
                WorldSettings settings = new WorldSettings(1234L, WorldSettings.GameType.CREATIVE,
                        false, false, WorldType.FLAT);
                settings.setWorldName("3;minecraft:bedrock,3*minecraft:dirt,1*minecraft:grass;2;");
                mc.launchIntegratedServer("devweapons", "devweapons", settings);
                phase = 1;
                wait = 160;
                return;
            case 1:
                if (wait-- > 0) return;
                if (mc.theWorld == null || mc.thePlayer == null) return;
                log("world up");
                phase = 2;
                return;
            case 2: {
                if (index >= IDS.length) {
                    log("done");
                    mc.shutdown();
                    phase = 9;
                    return;
                }
                SoupAPI_Config c = SoupAPI.configHolder.get();
                c.meleeEnabled = true;
                c.meleeSummonEnabled = SUMMON;
                c.meleeSummonOnEquip = SUMMON;
                c.meleeWeapon = IDS[index];
                c.meleeShowWhen = me.Padej_.soupapi.melee.MeleeTrigger.ALWAYS;
                c.hudBetterHotbarEnabled = false;
                for (MeleeWeapon w : MeleePack.weapons()) MeleePack.setDisabled(w, false);
                mc.thePlayer.inventory.mainInventory[0] =
                        new net.minecraft.item.ItemStack(net.minecraft.init.Items.diamond_sword);
                mc.thePlayer.inventory.currentItem = 0;
                mc.thePlayer.setPositionAndRotation(0.5, 5.0, 0.5, 0f, 0f);
                mc.gameSettings.hideGUI = false;   // the hand hook bails on hideGUI, so the weapon would vanish
                log("equipping " + IDS[index]);
                step = 0;
                wait = 400;
                phase = 4;
                return;
            }
            case 4: {
                if (Melee.active() == null) {
                    if (wait-- > 0) return;
                    log("gave up waiting for " + IDS[index]);
                    index++;
                    phase = 2;
                    return;
                }
                wait = SUMMON ? 0 : 60;
                phase = 3;
                return;
            }
            case 3: {
                if (wait-- > 0) return;
                String name;
                if (SUMMON) {
                    name = IDS[index] + "-t" + step;
                } else if (SWEEP) {
                    int[] p = PLACEMENTS[Math.min(step, PLACEMENTS.length - 1)];
                    SoupAPI_Config cfg = SoupAPI.configHolder.get();
                    cfg.meleeSize = p[0];
                    cfg.meleeDistance = p[1];
                    cfg.meleeOffsetX = p[2];
                    cfg.meleeOffsetY = p[3];
                    name = IDS[index] + "-" + p[0] + "x" + p[1];
                } else {
                    float[] view = VIEWS[Math.min(step, VIEWS.length - 1)];
                    mc.thePlayer.rotationPitch = view[0];
                    mc.thePlayer.prevRotationPitch = view[0];
                    mc.thePlayer.rotationYaw = view[1];
                    mc.thePlayer.prevRotationYaw = view[1];
                    name = IDS[index] + "-" + step;
                }
                try {
                    ScreenShotHelper.saveScreenshot(mc.mcDataDir, name + ".png",
                            mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
                    log("shot " + name);
                } catch (Throwable t) {
                    log("could not save " + name + ": " + t);
                }
                if (step < shots() - 1) {
                    step++;
                    wait = SUMMON ? 5 : (SWEEP ? 3 : 4);
                    return;
                }
                index++;
                phase = 2;
                return;
            }
            default:
        }
    }

    private static void log(String message) {
        System.out.println("[WPN] " + message);
    }
}
