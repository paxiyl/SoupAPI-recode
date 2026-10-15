package me.Padej_.soupapi.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

/**
 * Window metrics, replacing 1.21.4's {@code MinecraftClient.getWindow()}.
 *
 * <p>1.8.9's equivalent is {@link ScaledResolution}, which has no accessor: it is constructed on
 * demand and recomputes the GUI scale from the current options each time. These helpers construct
 * one per call, which is what vanilla 1.8.9 code does too.
 */
public final class Windows {

    private Windows() {
    }

    private static ScaledResolution res() {
        return new ScaledResolution(Minecraft.getMinecraft());
    }

    /** GUI-space width, i.e. the coordinate space HUD rendering uses. */
    public static int scaledWidth() {
        return res().getScaledWidth();
    }

    public static int scaledHeight() {
        return res().getScaledHeight();
    }

    public static double scaledWidthD() {
        return res().getScaledWidth_double();
    }

    public static double scaledHeightD() {
        return res().getScaledHeight_double();
    }

    public static int scaleFactor() {
        return res().getScaleFactor();
    }

    /** Physical framebuffer width in pixels. */
    public static int width() {
        return Minecraft.getMinecraft().displayWidth;
    }

    public static int height() {
        return Minecraft.getMinecraft().displayHeight;
    }
}
