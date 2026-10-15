package me.Padej_.soupapi.render;

import me.Padej_.soupapi.compat.Maths;

import net.minecraft.util.MathHelper;

import java.awt.*;

public interface RenderWithAnimatedColor {
    static float getWaveInterpolation(double angle, float rotationOffset) {
        float positionOnCircle = (float) ((angle / (2 * Math.PI)) + rotationOffset) % 1.0f;
        float distanceToGradientPoint = Math.abs(positionOnCircle - 0.5f);
        return Maths.clamp(1.0f - distanceToGradientPoint * 2.0f, 0.0f, 1.0f);
    }

    static Color interpolateColor(Color startColor, Color endColor, float fraction) {
        int red = Maths.clamp((int) (startColor.getRed() + (endColor.getRed() - startColor.getRed()) * fraction), 0, 255);
        int green = Maths.clamp((int) (startColor.getGreen() + (endColor.getGreen() - startColor.getGreen()) * fraction), 0, 255);
        int blue = Maths.clamp((int) (startColor.getBlue() + (endColor.getBlue() - startColor.getBlue()) * fraction), 0, 255);
        return new Color(red, green, blue);
    }
}
