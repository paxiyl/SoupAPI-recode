package me.Padej_.soupapi.compat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Replacement for {@code net.minecraft.client.render.Camera} as 1.21.4 exposed it.
 *
 * <p>1.8.9 <i>has</i> a {@code Camera} class, but it is a static utility holding the interpolated
 * eye position rather than an object with {@code getPos}/{@code getYaw}/{@code getPitch}. This wraps
 * the camera entity and the current frame's tick delta to provide the newer accessors, which the
 * ported world-render code calls 58 times.
 *
 * <p>Positions are interpolated between the previous and current tick exactly as vanilla's own
 * entity rendering does, so world geometry lines up with entities rather than lagging a frame.
 */
public final class Camera {

    private Entity focused;
    private float tickDelta;

    /** Refreshed once per frame by the render hook before any world rendering runs. */
    public void update(Entity focused, float tickDelta) {
        this.focused = focused;
        this.tickDelta = tickDelta;
    }

    public Entity getFocusedEntity() {
        if (focused == null) {
            MinecraftClient mc = MinecraftClient.getInstance();
            focused = mc.getCameraEntity() != null ? mc.getCameraEntity() : mc.player;
        }
        return focused;
    }

    public float getTickDelta() {
        return tickDelta;
    }

    /** The interpolated eye position of the camera entity. */
    public Vec3d getPos() {
        Entity e = getFocusedEntity();
        if (e == null) {
            return new Vec3d(0.0, 0.0, 0.0);
        }
        double x = e.prevX + (e.x - e.prevX) * tickDelta;
        double y = e.prevY + (e.y - e.prevY) * tickDelta;
        double z = e.prevZ + (e.z - e.prevZ) * tickDelta;
        // 1.8.9 stores the entity origin at the feet; the camera sits at eye height.
        return new Vec3d(x, y + e.getEyeHeight(), z);
    }

    public float getYaw() {
        Entity e = getFocusedEntity();
        if (e == null) return 0f;
        return e.prevYaw + (e.yaw - e.prevYaw) * tickDelta;
    }

    public float getPitch() {
        Entity e = getFocusedEntity();
        if (e == null) return 0f;
        return e.prevPitch + (e.pitch - e.prevPitch) * tickDelta;
    }

    /** Horizontal look direction, matching 1.21.4's {@code getHorizontalPlane()} usage. */
    public Vec3d getHorizontalPlane() {
        float yawRad = -MathHelper.wrapDegrees(getYaw()) * ((float) Math.PI / 180f);
        return new Vec3d(MathHelper.sin(yawRad), 0.0, MathHelper.cos(yawRad));
    }
}
