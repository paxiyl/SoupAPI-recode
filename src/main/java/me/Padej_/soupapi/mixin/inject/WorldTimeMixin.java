package me.Padej_.soupapi.mixin.inject;

import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static me.Padej_.soupapi.config.ConfigurableModule.CONFIG;

/**
 * Overrides the world time for the client-side time changer.
 *
 * <p>Retargeted from {@code ClientWorld.Properties}, which does not exist in 1.8.9. Here
 * {@code getTimeOfDay()} is declared on {@link World} itself, so that is the injection point.
 */
@Mixin(World.class)
public abstract class WorldTimeMixin {

    @Inject(method = "getTimeOfDay", at = @At("HEAD"), cancellable = true)
    private void soupapi$overrideTimeOfDay(CallbackInfoReturnable<Long> cir) {
        if (!CONFIG.timeChangerEnabled) return;
        long time = (long) ((CONFIG.timeChangerTime - 1) / 99.0 * 24000);
        cir.setReturnValue(Long.valueOf(time));
    }
}
