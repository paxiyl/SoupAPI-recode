package me.Padej_.soupapi.modules;

import me.Padej_.soupapi.compat.RenderSystem;
import me.Padej_.soupapi.config.ConfigurableModule;
import me.Padej_.soupapi.render.Render2D;
import me.Padej_.soupapi.render.Render3D_Shapes;
import me.Padej_.soupapi.render.TargetHudRenderer;
import me.Padej_.soupapi.utils.Palette;
import me.Padej_.soupapi.event.WorldRenderContext;
import net.minecraft.init.Blocks;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.item.*;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.BlockPos;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;

import java.awt.*;

public class Trajectories extends ConfigurableModule {

    private static final Color red = new Color(0x88ff0000, true);
    private static final Color green = new Color(0x8800ff00, true);
    private static final Color blue = new Color(0x880000ff, true);
    private static final Color yellow = new Color(0x88ffff00, true);
    private static final Color cyan = new Color(0x8800ffff, true);

    public static void onTick() {
    }

    public static void render(WorldRenderContext context) {
        if (mc.gameSettings.hudHidden) return;
        if (mc.thePlayer == null || mc.theWorld == null || !mc.gameSettings.thirdPersonView == 0 || !CONFIG.trajectoriesPreviewEnabled) return;
        Hand hand;
        float tickDelta = context.tickCounter().getTickDelta(true);

        ItemStack mainHand = mc.thePlayer.getHeldItem();
        ItemStack offHand = mc.thePlayer.getOffHandStack();

        if (mainHand.getItem() instanceof BowItem || mainHand.getItem() instanceof CrossbowItem || isThrowable(mainHand.getItem())) {
            hand = Hand.MAIN_HAND;
        } else if (offHand.getItem() instanceof BowItem || offHand.getItem() instanceof CrossbowItem || isThrowable(offHand.getItem())) {
            hand = Hand.OFF_HAND;
        } else return;

        boolean prev_bob = mc.gameSettings.gameSettings.viewBobbing.getValue();
        mc.gameSettings.gameSettings.viewBobbing.setValue(false);

        RegistryWrapper.Impl<Enchantment> enchantmentWrapper =
                mc.theWorld.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);

        RegistryEntry<Enchantment> multishotEntry =
                enchantmentWrapper.getOptional(RegistryKey.of(RegistryKeys.ENCHANTMENT, Enchantments.MULTISHOT.getValue()))
                        .orElse(null);

        if (multishotEntry != null && ((offHand.getItem() instanceof CrossbowItem && EnchantmentHelper.getLevel(multishotEntry, offHand) > 0) ||
                        (mainHand.getItem() instanceof CrossbowItem && EnchantmentHelper.getLevel(multishotEntry, mainHand) > 0))) {

            calcTrajectory(hand == Hand.OFF_HAND ? offHand.getItem() : mainHand.getItem(), mc.thePlayer.rotationYaw - 10, tickDelta);
            calcTrajectory(hand == Hand.OFF_HAND ? offHand.getItem() : mainHand.getItem(), mc.thePlayer.rotationYaw, tickDelta);
            calcTrajectory(hand == Hand.OFF_HAND ? offHand.getItem() : mainHand.getItem(), mc.thePlayer.rotationYaw + 10, tickDelta);

        } else
            calcTrajectory(hand == Hand.OFF_HAND ? offHand.getItem() : mainHand.getItem(), mc.thePlayer.rotationYaw, tickDelta);
        mc.gameSettings.gameSettings.viewBobbing.setValue(prev_bob);
    }

    private static boolean isThrowable(Item item) {
        return item instanceof EnderPearlItem || item instanceof TridentItem || item instanceof ExperienceBottleItem || item instanceof SnowballItem || item instanceof EggItem || item instanceof SplashPotionItem || item instanceof LingeringPotionItem;
    }

    private static float getDistance(Item item) {
        return item instanceof BowItem ? 1.0f : 0.4f;
    }

    private static float getThrowVelocity(Item item) {
        if (item instanceof SplashPotionItem || item instanceof LingeringPotionItem) return 0.5f;
        if (item instanceof ExperienceBottleItem) return 0.59f;
        if (item instanceof TridentItem) return 2f;
        return 1.5f;
    }

    private static int getThrowPitch(Item item) {
        if (item instanceof SplashPotionItem || item instanceof LingeringPotionItem || item instanceof ExperienceBottleItem)
            return 20;
        return 0;
    }

    private static void calcTrajectory(Item item, float yaw, float tickDelta) {
        EntityPlayerSP player = mc.thePlayer;
        if (player == null) return;
        double x = Render2D.interpolate(player.prevPosX, player.posX, tickDelta);
        double y = Render2D.interpolate(player.prevPosY, player.posY, tickDelta);
        double z = Render2D.interpolate(player.prevPosZ, player.posZ, tickDelta);

        y = y + player.getEyeHeight(player.getPose()) - 0.1000000014901161;

        if (item == player.getHeldItem().getItem()) {
            x = x - MathHelper.cos(yaw / 180.0f * 3.1415927f) * 0.16f;
            z = z - MathHelper.sin(yaw / 180.0f * 3.1415927f) * 0.16f;
        } else {
            x = x + MathHelper.cos(yaw / 180.0f * 3.1415927f) * 0.16f;
            z = z + MathHelper.sin(yaw / 180.0f * 3.1415927f) * 0.16f;
        }

        final float maxDist = getDistance(item);
        double motionX = -MathHelper.sin(yaw / 180.0f * 3.1415927f) * MathHelper.cos(player.rotationPitch / 180.0f * 3.1415927f) * maxDist;
        double motionY = -MathHelper.sin((player.rotationPitch - getThrowPitch(item)) / 180.0f * 3.141593f) * maxDist;
        double motionZ = MathHelper.cos(yaw / 180.0f * 3.1415927f) * MathHelper.cos(player.rotationPitch / 180.0f * 3.1415927f) * maxDist;

        float power = player.getItemUseTime() / 20.0f;
        power = (power * power + power * 2.0f) / 3.0f;

        if (power > 1.0f || power == 0) {
            power = 1.0f;
        }

        final float distance = MathHelper.sqrt((float) (motionX * motionX + motionY * motionY + motionZ * motionZ));
        motionX /= distance;
        motionY /= distance;
        motionZ /= distance;

        final float pow = (item instanceof BowItem ? (power * 2.0f) : item instanceof CrossbowItem ? (2.2f) : 1.0f) * getThrowVelocity(item);

        motionX *= pow;
        motionY *= pow;
        motionZ *= pow;
        if (!player.onGround)
            motionY += player.getVelocity().getY();

        Vec3 lastPos;
        for (int i = 0; i < 300; i++) {
            lastPos = new Vec3(x, y, z);
            x += motionX;
            y += motionY;
            z += motionZ;
            if (mc.theWorld.getBlockState(new BlockPos((int) x, (int) y, (int) z)).getBlock() == Blocks.WATER) {
                motionX *= 0.8;
                motionY *= 0.8;
                motionZ *= 0.8;
            } else {
                motionX *= 0.99;
                motionY *= 0.99;
                motionZ *= 0.99;
            }

            if (item instanceof BowItem) motionY -= 0.05000000074505806;
            else if (player.getHeldItem().getItem() instanceof CrossbowItem) motionY -= 0.05000000074505806;
            else motionY -= 0.03f;


            Vec3 pos = new Vec3(x, y, z);

            MovingObjectPosition bhr = mc.theWorld.raycast(new RaycastContext(lastPos, pos, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, player));
            Color color = Render2D.injectAlpha(Palette.getColor(0.33f), 220).brighter();
            if (bhr != null && bhr.getType() == HitResult.Type.BLOCK) {
                if (CONFIG.trajectoriesPreviewLandSideOutline) {
                    Render3D_Shapes.OUTLINE_SIDE_QUEUE.add(new Render3D_Shapes.OutlineSideAction(new AxisAlignedBB(bhr.getBlockPos()), color, 2f, bhr.getSide())); // Land Side Outline
                }
                if (CONFIG.trajectoriesPreviewLandSideFill) {
                    Render3D_Shapes.FILLED_SIDE_QUEUE.add(new Render3D_Shapes.FillSideAction(new AxisAlignedBB(bhr.getBlockPos()), color, bhr.getSide())); // Land Side Fill
                }
                break;
            }

            if (y <= -65) break;
            if (motionX == 0 && motionY == 0 && motionZ == 0) continue;

            Render3D_Shapes.drawLine(lastPos, pos, color);
        }
    }
}
