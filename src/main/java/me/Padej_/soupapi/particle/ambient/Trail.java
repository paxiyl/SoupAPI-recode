package me.Padej_.soupapi.particle.ambient;

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
        this.ticks = AmbientParticle.trailLenght.getValue().intValue();
        this.color = color;
    }

    public Vec3 interpolate(float pt) {
        Vec3 cameraPos = new net.minecraft.util.math.Vec3d(mc.getRenderManager().cameraX, mc.getRenderManager().cameraY, mc.getRenderManager().cameraZ);
        double x = from.xCoord + ((to.xCoord - from.xCoord) * pt) - cameraPos.x;
        double y = from.yCoord + ((to.yCoord - from.yCoord) * pt) - cameraPos.y;
        double z = from.zCoord + ((to.zCoord - from.zCoord) * pt) - cameraPos.z;
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
