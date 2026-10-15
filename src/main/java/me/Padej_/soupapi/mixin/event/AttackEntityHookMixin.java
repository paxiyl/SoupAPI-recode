package me.Padej_.soupapi.mixin.event;

import me.Padej_.soupapi.event.AttackEntityCallback;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Drives {@link AttackEntityCallback}, replacing Fabric API's event of the same name.
 *
 * <p>Fires at the head of {@code attackEntity}, so listeners see the swing before the packet goes
 * out — the same ordering the hit particles, bubbles and hit sound relied on.
 */
@Mixin(ClientPlayerInteractionManager.class)
public class AttackEntityHookMixin {

    @Inject(method = "attackEntity", at = @At("HEAD"))
    private void soupapi$onAttackEntity(PlayerEntity player, Entity target, CallbackInfo ci) {
        if (player == null || target == null || player.world == null) return;
        AttackEntityCallback.EVENT.invoke(player, player.world, target);
    }
}
