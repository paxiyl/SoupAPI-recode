package me.Padej_.soupapi.modules;

import me.Padej_.soupapi.config.ConfigurableModule;
import me.Padej_.soupapi.event.WorldRenderContext;
import me.Padej_.soupapi.render.Render2D;
import me.Padej_.soupapi.render.Render3D_Shapes;
import me.Padej_.soupapi.utils.Palette;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemEgg;
import net.minecraft.item.ItemEnderPearl;
import net.minecraft.item.ItemExpBottle;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemSnowball;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import java.awt.Color;

/**
 * Draws the predicted flight path of the held projectile.
 *
 * <p>Reduced to what 1.8.9 actually has. Dropped from the 1.21.4 version:
 * <ul>
 *   <li><b>Crossbows</b> and their Multishot triple-trajectory — crossbows are 1.14+.</li>
 *   <li><b>The off-hand</b> — added in 1.9, so only the held item is considered.</li>
 *   <li><b>Tridents</b> and <b>lingering potions</b> — 1.13 and 1.9 respectively.</li>
 * </ul>
 *
 * <p>Splash potions are still handled: in 1.8.9 they are {@code ItemPotion} with the splash flag
 * set rather than a distinct class.
 *
 * <p>The collision walk uses {@code World.rayTraceBlocks(Vec3, Vec3)}; 1.8.9 has no
 * {@code RaycastContext} with selectable shape and fluid handling.
 */
public class Trajectories extends ConfigurableModule {

    public static void onTick() {
    }

    public static void render(WorldRenderContext context) {
        if (mc.gameSettings.hideGUI) return;
        if (mc.thePlayer == null || mc.theWorld == null
                || mc.gameSettings.thirdPersonView != 0
                || !CONFIG.trajectoriesPreviewEnabled) {
            return;
        }

        float tickDelta = context.tickCounter().getTickDelta(true);
        ItemStack held = mc.thePlayer.getHeldItem();
        if (held == null || held.getItem() == null) return;

        Item item = held.getItem();
        if (!(item instanceof ItemBow) && !isThrowable(item)) return;

        // View bobbing would make the preview wobble away from where the projectile will go.
        boolean prevBob = mc.gameSettings.viewBobbing;
        mc.gameSettings.viewBobbing = false;
        try {
            calcTrajectory(item, mc.thePlayer.rotationYaw, tickDelta);
        } finally {
            mc.gameSettings.viewBobbing = prevBob;
        }
    }

    private static boolean isThrowable(Item item) {
        return item instanceof ItemEnderPearl
                || item instanceof ItemExpBottle
                || item instanceof ItemSnowball
                || item instanceof ItemEgg
                || item instanceof ItemPotion;
    }

    private static float getDistance(Item item) {
        return item instanceof ItemBow ? 1.0f : 0.4f;
    }

    private static float getThrowVelocity(Item item) {
        if (item instanceof ItemPotion) return 0.5f;
        if (item instanceof ItemExpBottle) return 0.59f;
        return 1.5f;
    }

    private static int getThrowPitch(Item item) {
        if (item instanceof ItemPotion || item instanceof ItemExpBottle) return 20;
        return 0;
    }

    private static void calcTrajectory(Item item, float yaw, float tickDelta) {
        EntityPlayerSP player = mc.thePlayer;
        if (player == null) return;

        double x = Render2D.interpolate(player.prevPosX, player.posX, tickDelta);
        double y = Render2D.interpolate(player.prevPosY, player.posY, tickDelta);
        double z = Render2D.interpolate(player.prevPosZ, player.posZ, tickDelta);

        y = y + player.getEyeHeight() - 0.1000000014901161;

        // 1.8.9 only ever throws from the main hand, so the offset is always the left-hand one.
        x = x - MathHelper.cos(yaw / 180.0f * 3.1415927f) * 0.16f;
        z = z - MathHelper.sin(yaw / 180.0f * 3.1415927f) * 0.16f;

        final float maxDist = getDistance(item);
        double motionX = -MathHelper.sin(yaw / 180.0f * 3.1415927f)
                * MathHelper.cos(player.rotationPitch / 180.0f * 3.1415927f) * maxDist;
        double motionY = -MathHelper.sin((player.rotationPitch - getThrowPitch(item)) / 180.0f * 3.141593f) * maxDist;
        double motionZ = MathHelper.cos(yaw / 180.0f * 3.1415927f)
                * MathHelper.cos(player.rotationPitch / 180.0f * 3.1415927f) * maxDist;

        float power = player.getItemInUseDuration() / 20.0f;
        power = (power * power + power * 2.0f) / 3.0f;
        if (power > 1.0f || power == 0) {
            power = 1.0f;
        }

        final float distance = MathHelper.sqrt_double(
                motionX * motionX + motionY * motionY + motionZ * motionZ);
        if (distance == 0f) return;
        motionX /= distance;
        motionY /= distance;
        motionZ /= distance;

        final float pow = (item instanceof ItemBow ? (power * 2.0f) : 1.0f) * getThrowVelocity(item);
        motionX *= pow;
        motionY *= pow;
        motionZ *= pow;
        if (!player.onGround) {
            motionY += player.motionY;
        }

        Color color = Render2D.injectAlpha(Palette.getColor(0.33f), 220).brighter();

        for (int i = 0; i < 300; i++) {
            Vec3 lastPos = new Vec3(x, y, z);
            x += motionX;
            y += motionY;
            z += motionZ;

            if (mc.theWorld.getBlockState(new BlockPos((int) x, (int) y, (int) z)).getBlock() == Blocks.water) {
                motionX *= 0.8;
                motionY *= 0.8;
                motionZ *= 0.8;
            } else {
                motionX *= 0.99;
                motionY *= 0.99;
                motionZ *= 0.99;
            }

            motionY -= item instanceof ItemBow ? 0.05000000074505806 : 0.03f;

            Vec3 pos = new Vec3(x, y, z);
            MovingObjectPosition hit = mc.theWorld.rayTraceBlocks(lastPos, pos);
            if (hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                BlockPos blockPos = hit.getBlockPos();
                AxisAlignedBB box = AxisAlignedBB.fromBounds(
                        blockPos.getX(), blockPos.getY(), blockPos.getZ(),
                        blockPos.getX() + 1, blockPos.getY() + 1, blockPos.getZ() + 1);
                if (CONFIG.trajectoriesPreviewLandSideOutline) {
                    Render3D_Shapes.OUTLINE_SIDE_QUEUE.add(
                            new Render3D_Shapes.OutlineSideAction(box, color, 2f, hit.sideHit));
                }
                if (CONFIG.trajectoriesPreviewLandSideFill) {
                    Render3D_Shapes.FILLED_SIDE_QUEUE.add(new Render3D_Shapes.FillSideAction(box, color, hit.sideHit));
                }
                break;
            }

            if (y <= -65) break;
            if (motionX == 0 && motionY == 0 && motionZ == 0) continue;

            Render3D_Shapes.drawLine(lastPos, pos, color);
        }
    }
}
