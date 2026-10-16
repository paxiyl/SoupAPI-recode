package me.Padej_.soupapi.compat;

import me.Padej_.soupapi.compat.Maths;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

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
            Minecraft mc = Minecraft.getMinecraft();
            focused = mc.getRenderViewEntity() != null ? mc.getRenderViewEntity() : mc.thePlayer;
        }
        return focused;
    }

    public float getTickDelta() {
        return tickDelta;
    }

    /**
     * The world position the world pass is drawn around.
     *
     * <p>Every call site subtracts this from a world position to get camera-relative geometry, so
     * it has to be the exact origin vanilla uses for the same job: {@code RenderManager.renderPosX/
     * Y/Z}, which interpolate {@code lastTickPos} to {@code pos} and carry <b>no eye height</b>.
     *
     * <p>This previously added {@code getEyeHeight()}, on the reasoning that the camera sits at eye
     * level. The camera does, but the origin does not — and everything the mod drew in the world
     * was therefore about 1.62 blocks too low: jump circles and trails ended up under the ground
     * where nothing could see them, and a hit bubble aimed at an opponent's chest landed at their
     * feet.
     */
    public Vec3 getPos() {
        // Read vanilla's own value where it exists, so the two can never drift apart.
        net.minecraft.client.renderer.entity.RenderManager manager =
                Minecraft.getMinecraft().getRenderManager();
        if (manager != null && (manager.viewerPosX != 0.0 || manager.viewerPosY != 0.0
                || manager.viewerPosZ != 0.0)) {
            return new Vec3(manager.viewerPosX, manager.viewerPosY, manager.viewerPosZ);
        }

        Entity e = getFocusedEntity();
        if (e == null) {
            return new Vec3(0.0, 0.0, 0.0);
        }
        double x = e.lastTickPosX + (e.posX - e.lastTickPosX) * tickDelta;
        double y = e.lastTickPosY + (e.posY - e.lastTickPosY) * tickDelta;
        double z = e.lastTickPosZ + (e.posZ - e.lastTickPosZ) * tickDelta;
        return new Vec3(x, y, z);
    }

    /** The interpolated eye position, for ray casts and anything measured from the viewer's eyes. */
    public Vec3 getEyePos() {
        Entity e = getFocusedEntity();
        if (e == null) {
            return new Vec3(0.0, 0.0, 0.0);
        }
        Vec3 pos = getPos();
        return new Vec3(pos.xCoord, pos.yCoord + e.getEyeHeight(), pos.zCoord);
    }

    /** Alias of {@link #getPos()}; MCP spells Entity's equivalent {@code getPositionVector}. */
    public Vec3 getPositionVector() {
        return getPos();
    }

    public float getYaw() {
        Entity e = getFocusedEntity();
        if (e == null) return 0f;
        return e.prevRotationYaw + (e.rotationYaw - e.prevRotationYaw) * tickDelta;
    }

    public float getPitch() {
        Entity e = getFocusedEntity();
        if (e == null) return 0f;
        return e.prevRotationPitch + (e.rotationPitch - e.prevRotationPitch) * tickDelta;
    }

    /** Horizontal look direction, matching 1.21.4's {@code getHorizontalPlane()} usage. */
    public Vec3 getHorizontalPlane() {
        float yawRad = -Maths.wrapDegrees(getYaw()) * ((float) Math.PI / 180f);
        return new Vec3(MathHelper.sin(yawRad), 0.0, MathHelper.cos(yawRad));
    }
}
