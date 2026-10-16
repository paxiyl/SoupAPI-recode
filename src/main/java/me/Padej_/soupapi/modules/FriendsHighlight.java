package me.Padej_.soupapi.modules;

import me.Padej_.soupapi.config.ConfigurableModule;
import me.Padej_.soupapi.event.WorldRenderContext;
import me.Padej_.soupapi.render.Render2D;
import me.Padej_.soupapi.render.Render3D_Shapes;
import me.Padej_.soupapi.utils.EntityUtils;
import me.Padej_.soupapi.utils.Palette;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;

import java.awt.Color;
import java.util.List;

/**
 * Marks players on the friends list.
 *
 * <p>1.21.4 did this by giving friends the Glowing effect's outline from a mixin. Glowing is 1.9+,
 * so the outline is drawn directly here instead: a box around the friend, interpolated to their
 * render position so it tracks rather than stutters.
 *
 * <p>All four options were read by nothing before — the friends list itself only fed the target-HUD
 * and target-render exclusions, so adding a friend had no visible effect.
 *
 * <ul>
 *   <li>{@code friendsHighlightOnlyArmor} restricts it to friends wearing armour, which on 1.8.9 is
 *       the only way to spot an invisible player (their armour still renders).</li>
 *   <li>{@code friendsHighlightSyncColor} takes the colour from the active palette instead of the
 *       fixed one, so it matches the rest of the HUD.</li>
 * </ul>
 */
public class FriendsHighlight extends ConfigurableModule {

    public static void render(WorldRenderContext context) {
        if (!CONFIG.friendsHighlight) return;
        if (mc.theWorld == null || mc.thePlayer == null) return;

        float tickDelta = context.tickCounter().getTickDelta(true);
        Color colour = colour();

        List<EntityPlayer> players = mc.theWorld.playerEntities;
        for (int i = 0; i < players.size(); i++) {
            EntityPlayer player = players.get(i);
            if (player == mc.thePlayer) continue;
            if (!EntityUtils.isFriend(player) || EntityUtils.isSpectator(player)) continue;
            if (CONFIG.friendsHighlightOnlyArmor && !hasArmour(player)) continue;

            Render3D_Shapes.OUTLINE_QUEUE.add(
                    new Render3D_Shapes.OutlineAction(interpolatedBox(player, tickDelta), colour, 2f));
        }
    }

    private static Color colour() {
        if (CONFIG.friendsHighlightSyncColor) {
            return Render2D.injectAlpha(Palette.getColor(0f), 220);
        }
        int rgb = CONFIG.friendCustomColor;
        return new Color((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, 220);
    }

    private static boolean hasArmour(EntityPlayer player) {
        for (int slot = 1; slot <= 4; slot++) {
            if (player.getEquipmentInSlot(slot) != null) return true;
        }
        return false;
    }

    /** The entity's box at the current partial tick, so the outline does not lag the model. */
    private static AxisAlignedBB interpolatedBox(Entity entity, float tickDelta) {
        double x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * tickDelta;
        double y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * tickDelta;
        double z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * tickDelta;
        float halfWidth = entity.width / 2f;
        return AxisAlignedBB.fromBounds(
                x - halfWidth, y, z - halfWidth,
                x + halfWidth, y + entity.height, z + halfWidth);
    }
}
