package me.Padej_.soupapi.modules;

import com.mojang.blaze3d.platform.GlStateManager;
import me.Padej_.soupapi.compat.RenderSystem;
import me.Padej_.soupapi.config.ConfigurableModule;
import me.Padej_.soupapi.render.Render2D;
import me.Padej_.soupapi.utils.EntityUtils;
import me.Padej_.soupapi.utils.Palette;
import me.Padej_.soupapi.utils.TexturesManager;
import me.Padej_.soupapi.utils.Timer;
import me.Padej_.soupapi.event.WorldRenderContext;
import me.Padej_.soupapi.event.AttackEntityCallback;
import me.Padej_.soupapi.compat.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import me.Padej_.soupapi.compat.RotationAxis;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;

public class HitBubbles extends ConfigurableModule {

    private static final ArrayList<HitBubble> bubbles = new ArrayList<>();
    private static long lastHitTime = 0;

    public static void registerOnHit() {
        AttackEntityCallback.EVENT.register((playerEntity, world, hand, entity, hitResult) -> {
            if ((entity instanceof LivingEntity && !EntityUtils.isFriend(entity) && !entity.isSpectator()) && CONFIG.hitBubblesEnabled) {
                long now = System.currentTimeMillis();
                if (now - lastHitTime < 150) return ActionResult.PASS; // защита от спама (150мс)
                lastHitTime = now;

                float yaw = playerEntity.getYaw();
                float pitch = playerEntity.getPitch();
                double distance = 5.0;
                Vec3d point = getRtxPoint(yaw, pitch, distance);

                if (point != null) {
                    bubbles.add(new HitBubble(
                            (float) point.x, (float) point.y, (float) point.z,
                            -yaw, pitch, new Timer()
                    ));
                }
            }
            return ActionResult.PASS;
        });
    }

    public static void render(WorldRenderContext context) {
        RenderSystem.disableDepthTest();
        ArrayList<HitBubble> copy = new ArrayList<>(bubbles);
        MatrixStack matrixStack = context.matrixStack();

        for (HitBubble b : copy) {
            matrixStack.push();
            matrixStack.translate(
                    b.x - mc.gameRenderer.getCamera().getPos().getX(),
                    b.y - mc.gameRenderer.getCamera().getPos().getY(),
                    b.z - mc.gameRenderer.getCamera().getPos().getZ()
            );
            matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(b.yaw));
            matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(b.pitch));

            float angle = -b.life.getPassedTimeMs() / 4f;
            float factor = b.life.getPassedTimeMs() / (CONFIG.hitBubblesRenderTime * 50f);
            drawBubble(matrixStack, angle, factor);

            matrixStack.pop();
        }

        RenderSystem.enableDepthTest();
        bubbles.removeIf(b -> b.life.passedMs(CONFIG.hitBubblesRenderTime * 50L));
    }

    private static void drawBubble(MatrixStack matrices, float angle, float factor) {
        Render2D.setupRender();
        RenderSystem.disableCull();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.setShaderTexture(0, TexturesManager.getHitBubbleTexture());

        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(angle));
        float scale = factor * 2f * (CONFIG.hitBubblesScale / 100f);
        Render2D.renderGradientTexture(matrices,
                -scale / 2, -scale / 2, scale, scale,
                0, 0, 128, 128, 128, 128,
                Render2D.applyOpacity(Palette.getColor(0), 1f - factor),
                Render2D.applyOpacity(Palette.getColor(0.33f), 1f - factor),
                Render2D.applyOpacity(Palette.getColor(0.66f), 1f - factor),
                Render2D.applyOpacity(Palette.getColor(1), 1f - factor));
        Render2D.endRender();
        RenderSystem.enableCull();
    }

    private static Vec3d getRtxPoint(float yaw, float pitch, double distance) {
        Vec3d from = mc.player.getPos().add(0, mc.player.getEyeHeight(mc.player.getPose()), 0);
        Vec3d direction = getRotationVector(pitch, yaw);
        Vec3d to = from.add(direction.multiply(distance));
        double distanceSq = distance * distance;

        Box box = mc.player.getBoundingBox().stretch(direction.multiply(distance)).expand(1.0, 1.0, 1.0);
        EntityHitResult result = ProjectileUtil.raycast(
                mc.player,
                from,
                to,
                box,
                (entity) -> !entity.isSpectator() && entity.canHit(),
                distanceSq
        );

        if (result != null && result.getEntity() instanceof LivingEntity) {
            return result.getPos();
        }
        return null;
    }

    private static Vec3d getRotationVector(float pitch, float yaw) {
        float f = (float) Math.cos(-yaw * 0.017453292F - Math.PI);
        float g = (float) Math.sin(-yaw * 0.017453292F - Math.PI);
        float h = (float) -Math.cos(-pitch * 0.017453292F);
        float i = (float) Math.sin(-pitch * 0.017453292F);
        return new Vec3d(g * h, i, f * h);
    }

    public enum Style {
        CIRCLE, CIRCLE_BOLD, HEXAGON, PORTAL, PORTAL_2
    }
public static final class HitBubble {
    private final float x;
    private final float y;
    private final float z;
    private final float yaw;
    private final float pitch;
    private final Timer life;

    HitBubble(float x, float y, float z, float yaw, float pitch, Timer life) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.life = life;
    }

    public float x() {
        return x;
    }

    public float y() {
        return y;
    }

    public float z() {
        return z;
    }

    public float yaw() {
        return yaw;
    }

    public float pitch() {
        return pitch;
    }

    public Timer life() {
        return life;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HitBubble)) return false;
        HitBubble other = (HitBubble) o;
        if (Float.compare(x, other.x) != 0) return false;
        if (Float.compare(y, other.y) != 0) return false;
        if (Float.compare(z, other.z) != 0) return false;
        if (Float.compare(yaw, other.yaw) != 0) return false;
        if (Float.compare(pitch, other.pitch) != 0) return false;
        if (life == null ? other.life != null : !life.equals(other.life)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + Float.floatToIntBits(x);
        result = 31 * result + Float.floatToIntBits(y);
        result = 31 * result + Float.floatToIntBits(z);
        result = 31 * result + Float.floatToIntBits(yaw);
        result = 31 * result + Float.floatToIntBits(pitch);
        result = 31 * result + (life == null ? 0 : life.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "HitBubble[" + "x=" + x + ", " + "y=" + y + ", " + "z=" + z + ", " + "yaw=" + yaw + ", " + "pitch=" + pitch + ", " + "life=" + life + "]";
    }
    }
}
