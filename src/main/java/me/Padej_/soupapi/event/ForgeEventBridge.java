package me.Padej_.soupapi.event;

import me.Padej_.soupapi.compat.Cameras;
import me.Padej_.soupapi.compat.DrawContext;
import me.Padej_.soupapi.compat.MatrixStack;
import me.Padej_.soupapi.compat.RenderSystem;
import me.Padej_.soupapi.compat.RotationAxis;
import me.Padej_.soupapi.font.FontRenderers;
import me.Padej_.soupapi.interfaces.OverlayReloadListener;
import me.Padej_.soupapi.modules.AmbientParticle;
import me.Padej_.soupapi.modules.AspectRatio;
import me.Padej_.soupapi.modules.CustomFog;
import me.Padej_.soupapi.modules.Halo;
import me.Padej_.soupapi.modules.HitBubbles;
import me.Padej_.soupapi.modules.HitParticle;
import me.Padej_.soupapi.modules.JumpCircles;
import me.Padej_.soupapi.modules.JumpParticles;
import me.Padej_.soupapi.modules.MouseMove;
import me.Padej_.soupapi.modules.RPC;
import me.Padej_.soupapi.modules.TargetHud;
import me.Padej_.soupapi.modules.TargetRender;
import me.Padej_.soupapi.modules.TotemPopParticles;
import me.Padej_.soupapi.modules.Trails;
import me.Padej_.soupapi.modules.Trajectories;
import me.Padej_.soupapi.modules.Translator;
import me.Padej_.soupapi.render.Render3D_Shapes;
import me.Padej_.soupapi.render.WatermarkRenderer;
import me.Padej_.soupapi.screen.SoupSettingsShortcut;
import me.Padej_.soupapi.utils.EntityUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.awt.Color;

/**
 * Bridges Forge's event bus to the mod's own events and modules.
 *
 * <p>This is the Forge replacement for the mixin package the Fabric port used. Forge 1.8.9 already
 * publishes events at every point the mod needed, which is why this port needs no mixins at all —
 * and therefore no coremod, no tweaker argument, and no Mixin bootstrap. The mod is a plain jar
 * dropped into {@code mods/}, which also makes it trivial to install under PojavLauncher.
 *
 * <p>Event mapping:
 * <ul>
 *   <li>{@code TickEvent.ClientTickEvent} (END) → the per-tick module updates.</li>
 *   <li>{@code RenderWorldLastEvent} → world rendering, with the camera transform still applied so
 *       mod geometry lands in camera space, as the ported render code assumes.</li>
 *   <li>{@code RenderGameOverlayEvent.Post(ALL)} → HUD rendering in scaled GUI space.</li>
 *   <li>{@code RenderGameOverlayEvent.Pre(ARMOR/HEALTH/FOOD/AIR/EXPERIENCE)} → cancels individual
 *       vanilla bars for the custom HUD styles. This is the part that could not be done on Legacy
 *       Fabric, where 1.8.9 draws all four inside one private method.</li>
 *   <li>{@code AttackEntityEvent} → the hit particles, bubbles and sound.</li>
 *   <li>{@code EntityViewRenderEvent.FogColors} / {@code FogDensity} → the custom fog, replacing
 *       both the 1.21.4 Fog record and the Fabric port's GameRenderer mixin.</li>
 * </ul>
 */
public final class ForgeEventBridge {

    private static final ForgeEventBridge INSTANCE = new ForgeEventBridge();

    private ForgeEventBridge() {
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.register(INSTANCE);
        net.minecraftforge.fml.common.FMLCommonHandler.instance().bus().register(INSTANCE);
    }

    // ---- tick ------------------------------------------------------------------------------

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) return;

        FontRenderers.ensureLoaded();
        EntityUtils.updateEntities(mc);
        OverlayReloadListener.callEvent();

        Trails.onTick();
        JumpCircles.onTick();
        TargetHud.onTick();
        RPC.onTick();
        Translator.onTick();

        HitParticle.onTick();
        TotemPopParticles.onTick();
        JumpParticles.onTick();
        AmbientParticle.onTick();

        SoupSettingsShortcut.tick(mc);
        SoupEvents.fireEndClientTick(mc);
    }

    // ---- world rendering -------------------------------------------------------------------

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.theWorld == null) return;

        float tickDelta = event.partialTicks;
        Entity cameraEntity = mc.getRenderViewEntity() != null
                ? mc.getRenderViewEntity() : mc.thePlayer;
        Cameras.update(cameraEntity, tickDelta);
        captureWorldSpaceMatrices();

        WorldRenderContext context = SoupEvents.worldContext();
        context.begin(cameraEntity, tickDelta);

        TargetRender.renderTarget(context);
        HitBubbles.render(context);
        Halo.render(context);
        Trails.renderTrail(context);
        JumpCircles.renderCircles(context);
        Trajectories.render(context);

        JumpParticles.render(context);
        HitParticle.render(context);
        TotemPopParticles.render(context);
        AmbientParticle.render(context);

        Render3D_Shapes.render(context);

        SoupEvents.WORLD_RENDER_LAST.invoke(context);
    }

    /**
     * Records the projection, model-view and camera-space matrices the target HUD needs to project
     * world positions onto the screen. Read here, inside the world pass, so the matrices are the
     * ones actually in effect for this frame.
     */
    private static void captureWorldSpaceMatrices() {
        me.Padej_.soupapi.compat.Camera camera = Cameras.get();
        MatrixStack matrixStack = new MatrixStack();
        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0f));

        TargetHud.lastProjMat.set(RenderSystem.getProjectionMatrix());
        TargetHud.lastModMat.set(RenderSystem.getModelViewMatrix());
        TargetHud.lastWorldSpaceMatrix.set(matrixStack.peek().getPositionMatrix());
    }

    // ---- HUD -------------------------------------------------------------------------------

    @SubscribeEvent
    public void onRenderOverlayPost(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;

        FontRenderers.ensureLoaded();
        DrawContext context = new DrawContext(Minecraft.getMinecraft());
        SoupEvents.HudRenderContext hud = SoupEvents.hudContext();
        hud.begin(event.partialTicks, context);

        TargetHud.render(context, hud.tickCounter());
        WatermarkRenderer.render(context);
        MouseMove.render(context);

        SoupEvents.HUD_RENDER.invoke(hud);
    }

    /**
     * Cancels the vanilla bars the custom HUD styles replace.
     *
     * <p>Forge raises a separate, cancellable event per element, so each bar can be suppressed
     * independently — something 1.8.9 itself does not allow, since it draws health, armour, food
     * and air inside a single private method.
     */
    @SubscribeEvent
    public void onRenderOverlayPre(RenderGameOverlayEvent.Pre event) {
        if (me.Padej_.soupapi.modules.BetterHudStyles.shouldCancelVanilla(event.type)) {
            event.setCanceled(true);
        }
    }

    // ---- combat ----------------------------------------------------------------------------

    @SubscribeEvent
    public void onAttackEntity(AttackEntityEvent event) {
        if (event.entityPlayer == null || event.target == null) return;
        AttackEntityCallback.EVENT.invoke(event.entityPlayer, event.entityPlayer.worldObj, event.target);
    }

    // ---- fog -------------------------------------------------------------------------------

    @SubscribeEvent
    public void onFogColors(EntityViewRenderEvent.FogColors event) {
        if (!CustomFog.shouldApply()) return;
        Color c = CustomFog.getFogColor();
        event.red = c.getRed() / 255f;
        event.green = c.getGreen() / 255f;
        event.blue = c.getBlue() / 255f;
    }

    @SubscribeEvent
    public void onFogDensity(EntityViewRenderEvent.FogDensity event) {
        if (!CustomFog.shouldApply()) return;
        event.density = CustomFog.getDensity() * 0.1f;
        event.setCanceled(true);
    }

    // ---- field of view / aspect ------------------------------------------------------------

    @SubscribeEvent
    public void onFovUpdate(net.minecraftforge.client.event.FOVUpdateEvent event) {
        // AspectRatio stretches the projection, which Forge does not expose an event for; the
        // module applies itself from the world pass instead. Hooking FOV here would change the
        // field of view rather than the aspect, so this intentionally does nothing and exists
        // only to keep the module's state refreshed once per frame.
        AspectRatio.onFovUpdate();
    }
}
