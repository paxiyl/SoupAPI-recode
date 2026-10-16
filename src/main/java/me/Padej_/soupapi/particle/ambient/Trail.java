package me.Padej_.soupapi.particle.ambient;

import me.Padej_.soupapi.config.ConfigurableModule;

import me.Padej_.soupapi.modules.AmbientParticle;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Vec3;

import java.awt.*;

import static me.Padej_.soupapi.config.ConfigurableModule.CONFIG;

public class Trail {
    private static Minecraft mc = Minecraft.getMinecraft();
    private final Vec3 from;
    private final Vec3 to;
    private final Color color;
    private int ticks, prevTicks;

    public Trail(Vec3 from, Vec3 to, Color color) {
        this.from = from;
        this.to = to;
        this.ticks = ConfigurableModule.CONFIG.ambientParticlesParticleWithTrailLenght;
        this.color = color;
    }

    public Vec3 interpolate(float pt) {
        Vec3 cameraPos = new net.minecraft.util.Vec3(mc.getRenderManager().viewerPosX, mc.getRenderManager().viewerPosY, mc.getRenderManager().viewerPosZ);
        double x = from.xCoord + ((to.xCoord - from.xCoord) * pt) - cameraPos.xCoord;
        double y = from.yCoord + ((to.yCoord - from.yCoord) * pt) - cameraPos.yCoord;
        double z = from.zCoord + ((to.zCoord - from.zCoord) * pt) - cameraPos.zCoord;
        return new Vec3(x, y, z);
    }

    public double animation(float pt) {
        return (this.prevTicks + (this.ticks - this.prevTicks) * pt) / 10.;
    }

    public boolean update() {
        this.prevTicks = this.ticks;
        return this.ticks-- <= 0;
    }

    public Color color() {
        return color;
    }
}
