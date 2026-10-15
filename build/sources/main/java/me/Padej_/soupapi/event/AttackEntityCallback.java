package me.Padej_.soupapi.event;

import me.Padej_.soupapi.compat.SoupLog;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Replacement for Fabric API's {@code AttackEntityCallback}.
 *
 * <p>Fired from {@code ClientPlayerInteractionManagerMixin} when the player swings at an entity.
 * The listener signature keeps Fabric's five parameters so the three ported lambdas are unchanged,
 * even though 1.8.9 has no off-hand (hand is always the main hand) and no entity hit result for
 * this path (it is passed as {@code null}).
 */
public final class AttackEntityCallback {

    /** Mirrors Fabric's {@code ActionResult} for the subset the listeners return. */
    public enum Result {
        PASS,
        SUCCESS,
        FAIL
    }

    public interface Listener {
        Result attack(EntityPlayer player, World world, Hand hand, Entity entity, Object hitResult);
    }

    /** 1.8.9 has no off-hand; this exists only so the ported lambdas keep their arity. */
    public enum Hand {
        MAIN_HAND
    }

    public static final AttackEntityCallback EVENT = new AttackEntityCallback();

    private final List<Listener> listeners = new ArrayList<Listener>();

    private AttackEntityCallback() {
    }

    public void register(Listener listener) {
        listeners.add(listener);
    }

    public void invoke(EntityPlayer player, World world, Entity entity) {
        for (int i = 0; i < listeners.size(); i++) {
            try {
                listeners.get(i).attack(player, world, Hand.MAIN_HAND, entity, null);
            } catch (Throwable t) {
                SoupLog.error("AttackEntityCallback listener threw", t);
            }
        }
    }
}
