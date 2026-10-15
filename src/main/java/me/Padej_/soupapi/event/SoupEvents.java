package me.Padej_.soupapi.event;

import me.Padej_.soupapi.compat.DrawContext;
import me.Padej_.soupapi.compat.RenderTickCounter;
import net.minecraft.client.MinecraftClient;

/**
 * The mod's event bus, fired from its own mixins.
 *
 * <p>Replaces the Fabric API events the 1.21.4 sources used:
 * {@code ClientTickEvents.END_CLIENT_TICK}, {@code WorldRenderEvents.LAST} and
 * {@code HudRenderCallback.EVENT}. See the note in build.gradle for why Fabric API is not a
 * dependency here.
 *
 * <p>The contexts are reused across frames rather than reallocated, so neither world nor HUD
 * rendering allocates per frame — this runs on devices where that matters.
 */
public final class SoupEvents {

    private SoupEvents() {
    }

    /** Fired at the end of each client tick. */
    public static final SoupEvent<MinecraftClient> END_CLIENT_TICK =
            new SoupEvent<MinecraftClient>("END_CLIENT_TICK");

    /** Fired after the world and entities are drawn, with the camera transform still applied. */
    public static final SoupEvent<WorldRenderContext> WORLD_RENDER_LAST =
            new SoupEvent<WorldRenderContext>("WORLD_RENDER_LAST");

    /** Fired after the vanilla HUD is drawn, in scaled GUI space. */
    public static final SoupEvent<HudRenderContext> HUD_RENDER =
            new SoupEvent<HudRenderContext>("HUD_RENDER");

    private static final WorldRenderContext WORLD_CONTEXT = new WorldRenderContext();
    private static final HudRenderContext HUD_CONTEXT = new HudRenderContext();

    public static void fireEndClientTick(MinecraftClient client) {
        END_CLIENT_TICK.invoke(client);
    }

    public static void fireWorldRenderLast(net.minecraft.entity.Entity cameraEntity, float tickDelta) {
        WORLD_CONTEXT.begin(cameraEntity, tickDelta);
        WORLD_RENDER_LAST.invoke(WORLD_CONTEXT);
    }

    public static void fireHudRender(float tickDelta) {
        HUD_CONTEXT.begin(tickDelta);
        HUD_RENDER.invoke(HUD_CONTEXT);
    }

    /** What a HUD listener receives: a fresh {@link DrawContext} and the frame's tick delta. */
    public static final class HudRenderContext {
        private DrawContext drawContext;
        private final RenderTickCounter tickCounter = new RenderTickCounter(0f);

        void begin(float tickDelta) {
            tickCounter.setTickDelta(tickDelta);
            // A fresh DrawContext per frame: it caches the window size, which changes on resize.
            drawContext = new DrawContext(MinecraftClient.getInstance());
        }

        public DrawContext drawContext() {
            return drawContext;
        }

        public RenderTickCounter tickCounter() {
            return tickCounter;
        }
    }
}
