package me.Padej_.soupapi.mixin.event;

import me.Padej_.soupapi.event.SoupEvents;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Drives {@link SoupEvents#END_CLIENT_TICK}, replacing Fabric API's {@code ClientTickEvents}. */
@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void soupapi$endClientTick(CallbackInfo ci) {
        SoupEvents.fireEndClientTick((MinecraftClient) (Object) this);
    }
}
