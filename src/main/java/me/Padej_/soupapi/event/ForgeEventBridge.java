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

    /** Call sites that have already reported a failure, so each one is logged once. */
    private static final java.util.Set<String> REPORTED =
            java.util.Collections.synchronizedSet(new java.util.HashSet<String>());

    /**
     * Runs one module's hook so a failure in it cannot take the client down.
     *
     * <p>A Forge event handler that throws crashes the game, which means any single module — a
     * missing native, a driver quirk on gl4es, a bad entity mid-frame — can end the session. Each
     * hook is therefore isolated: the first failure per call site is logged in full, later ones are
     * counted silently, and the rest of the frame carries on. The module is not disabled, since a
     * transient throw should not cost the feature for the session.
     *
     * <p>{@link VirtualMachineError} (out of memory and friends) and {@link ThreadDeath} are
     * rethrown: those are not a module's to swallow.
     */
    private static void guard(String label, Runnable body) {
        try {
            body.run();
        } catch (ThreadDeath | VirtualMachineError fatal) {
            throw fatal;
        } catch (Throwable t) {
            // A module that threw part-way through a draw leaves the shared vertex buffer building
            // and whatever GL state it had set still applied. Both have to go back, or the next
            // thing to draw - vanilla, or another mod - fails instead.
            me.Padej_.soupapi.compat.Tessellator.discardPending();
            RenderSystem.restoreDefaults();
            if (REPORTED.add(label)) {
                me.Padej_.soupapi.compat.SoupLog.error(
                        "[SoupAPI] " + label + " threw; it is being skipped for this frame. "
                                + "Further failures here will not be logged.", t);
            }
        }
    }

    /**
     * As {@link #guard(String, Runnable)}, with the palette scoped to what is drawing, so each
     * part of the mod can have its RGB animation switched on or off on its own.
     */
    private static void guard(String label, me.Padej_.soupapi.utils.Palette.Element element, Runnable body) {
        me.Padej_.soupapi.utils.Palette.Element previous = me.Padej_.soupapi.utils.Palette.scope(element);
        try {
            guard(label, body);
        } finally {
            me.Padej_.soupapi.utils.Palette.scope(previous);
        }
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

        guard("fonts", FontRenderers::ensureLoaded);
        guard("playerLayers", me.Padej_.soupapi.render.PlayerLayers::ensureRegistered);
        guard("entities", () -> EntityUtils.updateEntities(mc));
        guard("overlayReload", OverlayReloadListener::callEvent);

        guard("Trails.tick", me.Padej_.soupapi.utils.Palette.Element.TRAILS, Trails::onTick);
        guard("PlayerEffects.tick", me.Padej_.soupapi.utils.Palette.Element.PARTICLES, me.Padej_.soupapi.modules.PlayerEffects::onTick);
        guard("KillEffects.tick", me.Padej_.soupapi.utils.Palette.Element.PARTICLES, me.Padej_.soupapi.modules.KillEffects::onTick);
        guard("LandingImpact.tick", me.Padej_.soupapi.utils.Palette.Element.PARTICLES, me.Padej_.soupapi.modules.LandingImpact::onTick);
        guard("FxParticles.tick", me.Padej_.soupapi.utils.Palette.Element.PARTICLES, me.Padej_.soupapi.render.FxParticles::tick);
        guard("SmokeFx.tick", me.Padej_.soupapi.utils.Palette.Element.PARTICLES, me.Padej_.soupapi.render.SmokeFx::tick);
        guard("JumpCircles.tick", me.Padej_.soupapi.utils.Palette.Element.PARTICLES, JumpCircles::onTick);
        guard("RPC.tick", RPC::onTick);
        guard("Translator.tick", Translator::onTick);
        guard("AnimatedSky.tick", me.Padej_.soupapi.utils.Palette.Element.SKY, me.Padej_.soupapi.modules.AnimatedSky::onTick);
        guard("WorldChanger.tick", me.Padej_.soupapi.modules.WorldChanger::onTick);

        guard("HitParticle.tick", me.Padej_.soupapi.utils.Palette.Element.PARTICLES, HitParticle::onTick);
        guard("TotemPopParticles.tick", me.Padej_.soupapi.utils.Palette.Element.PARTICLES, TotemPopParticles::onTick);
        guard("JumpParticles.tick", me.Padej_.soupapi.utils.Palette.Element.PARTICLES, JumpParticles::onTick);
        guard("AmbientParticle.tick", me.Padej_.soupapi.utils.Palette.Element.AMBIENCE, AmbientParticle::onTick);

        guard("Melee.tick", me.Padej_.soupapi.utils.Palette.Element.WEAPON, me.Padej_.soupapi.melee.Melee::onTick);
        guard("WorldAmbience.tick", me.Padej_.soupapi.utils.Palette.Element.AMBIENCE, me.Padej_.soupapi.modules.WorldAmbience::onTick);

        guard("settingsShortcut", () -> SoupSettingsShortcut.tick(mc));
        guard("devWeapons", () -> me.Padej_.soupapi.devprobe.WeaponShots.tick(mc));
        guard("endClientTick", () -> SoupEvents.fireEndClientTick(mc));
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

        WorldRenderContext context = SoupEvents.worldContext();
        context.begin(cameraEntity, tickDelta);

        guard("WorldAmbience", me.Padej_.soupapi.utils.Palette.Element.AMBIENCE, () -> me.Padej_.soupapi.modules.WorldAmbience.render(context));
        guard("TargetRender", me.Padej_.soupapi.utils.Palette.Element.PARTICLES, () -> TargetRender.renderTarget(context));
        guard("HitBubbles", me.Padej_.soupapi.utils.Palette.Element.PARTICLES, () -> HitBubbles.render(context));
        guard("Halo", me.Padej_.soupapi.utils.Palette.Element.PARTICLES, () -> Halo.render(context));
        guard("Trails", me.Padej_.soupapi.utils.Palette.Element.TRAILS, () -> Trails.renderTrail(context));
        guard("JumpCircles", me.Padej_.soupapi.utils.Palette.Element.PARTICLES, () -> JumpCircles.renderCircles(context));
        guard("Trajectories", me.Padej_.soupapi.utils.Palette.Element.GENERAL, () -> Trajectories.render(context));

        guard("JumpParticles", me.Padej_.soupapi.utils.Palette.Element.PARTICLES, () -> JumpParticles.render(context));
        guard("HitParticle", me.Padej_.soupapi.utils.Palette.Element.PARTICLES, () -> HitParticle.render(context));
        guard("TotemPopParticles", me.Padej_.soupapi.utils.Palette.Element.PARTICLES, () -> TotemPopParticles.render(context));
        guard("AmbientParticle", me.Padej_.soupapi.utils.Palette.Element.AMBIENCE, () -> AmbientParticle.render(context));

        guard("PlayerEffects", me.Padej_.soupapi.utils.Palette.Element.PARTICLES, () -> me.Padej_.soupapi.modules.PlayerEffects.render(context));
        guard("SmokeFx", me.Padej_.soupapi.utils.Palette.Element.PARTICLES, () -> me.Padej_.soupapi.render.SmokeFx.render(
                context.tickCounter().getTickDelta(true)));
        guard("KillEffects", me.Padej_.soupapi.utils.Palette.Element.PARTICLES, () -> me.Padej_.soupapi.modules.KillEffects.render(
                context.tickCounter().getTickDelta(true)));
        guard("FxParticles", me.Padej_.soupapi.utils.Palette.Element.PARTICLES, () -> me.Padej_.soupapi.render.FxParticles.render(
                context.tickCounter().getTickDelta(true)));
        guard("FriendsHighlight", me.Padej_.soupapi.utils.Palette.Element.GENERAL, () -> me.Padej_.soupapi.modules.FriendsHighlight.render(context));
        guard("Render3D_Shapes", me.Padej_.soupapi.utils.Palette.Element.GENERAL, () -> Render3D_Shapes.render(context));

        guard("worldRenderLast", () -> SoupEvents.WORLD_RENDER_LAST.invoke(context));

        // Flush any layer a module left open, then hand the world pass back as it was found.
        // 1.8.9 has one shared vertex buffer: a layer left open is a buffer left "building", and
        // the next mod to begin one crashes with "Already building!" - which is how this first
        // showed up, in MbMotionblur's overlay handler rather than here.
        guard("flushLayers", () -> me.Padej_.soupapi.compat.VertexConsumerProvider.immediate().draw());
        me.Padej_.soupapi.compat.Tessellator.discardPending();
        RenderSystem.restoreDefaults();
    }

    /**
     * Records the projection, model-view and camera-space matrices the target HUD needs to project
     * world positions onto the screen. Read here, inside the world pass, so the matrices are the
     * ones actually in effect for this frame.
     */
    // ---- HUD -------------------------------------------------------------------------------

    @SubscribeEvent
    public void onRenderOverlayPost(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;

        FontRenderers.ensureLoaded();
        DrawContext context = new DrawContext(Minecraft.getMinecraft());
        SoupEvents.HudRenderContext hud = SoupEvents.hudContext();
        hud.begin(event.partialTicks, context);

        guard("devWeaponsFrame", () -> me.Padej_.soupapi.devprobe.WeaponShots.frame(Minecraft.getMinecraft()));
        guard("BetterHud", me.Padej_.soupapi.utils.Palette.Element.HUD, () -> me.Padej_.soupapi.render.BetterHudRenderer.render(context));
        guard("Crosshair", me.Padej_.soupapi.utils.Palette.Element.CROSSHAIR, () -> me.Padej_.soupapi.modules.Crosshair.render(context));
        guard("GradientColors", () -> me.Padej_.soupapi.render.GradientColors.tick());
        guard("PotionsHud", me.Padej_.soupapi.utils.Palette.Element.HUD, () -> {
            if (me.Padej_.soupapi.config.ConfigurableModule.CONFIG.hudBetterPotionsHudEnabled) {
                me.Padej_.soupapi.modules.PotionsHud.render(context);
            }
        });
        guard("Watermark", me.Padej_.soupapi.utils.Palette.Element.HUD, () -> WatermarkRenderer.render(context));
        guard("MouseMove", me.Padej_.soupapi.utils.Palette.Element.HUD, () -> MouseMove.render(context));
        guard("NoRender", me.Padej_.soupapi.utils.Palette.Element.HUD, () -> me.Padej_.soupapi.modules.NoRender.renderFireReplacement(context));

        guard("hudRender", () -> SoupEvents.HUD_RENDER.invoke(hud));

        // The next thing to touch the shared buffer is the following frame's overlay pass, which
        // may well belong to another mod; leave nothing of ours applied.
        guard("flushLayers", () -> me.Padej_.soupapi.compat.VertexConsumerProvider.immediate().draw());
        me.Padej_.soupapi.compat.Tessellator.discardPending();
        RenderSystem.restoreDefaults();
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
        if (me.Padej_.soupapi.modules.BetterHudStyles.shouldCancelVanilla(event.type)
                || me.Padej_.soupapi.modules.Crosshair.shouldCancelVanilla(event.type)) {
            event.setCanceled(true);
        }
    }

    // ---- combat ----------------------------------------------------------------------------

    @SubscribeEvent
    public void onAttackEntity(AttackEntityEvent event) {
        if (event.entityPlayer == null || event.target == null) return;
        AttackEntityCallback.EVENT.invoke(event.entityPlayer, event.entityPlayer.worldObj, event.target);
    }

    // ---- deaths and world changes -----------------------------------------------------------

    /**
     * A death, as the client learns of it: entity status 3 runs {@code onDeath()} on the client
     * thread and Forge raises this from inside it, while the entity is still in the world.
     */
    @SubscribeEvent
    public void onLivingDeath(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        if (event.entityLiving == null || event.entityLiving.worldObj == null
                || !event.entityLiving.worldObj.isRemote) return;
        guard("KillEffects.death", me.Padej_.soupapi.utils.Palette.Element.PARTICLES,
                () -> me.Padej_.soupapi.modules.KillEffects.onDeath(event.entityLiving));
    }

    /** Leaving a world drops everything that referred to its entities. */
    @SubscribeEvent
    public void onWorldUnload(net.minecraftforge.event.world.WorldEvent.Unload event) {
        if (event.world == null || !event.world.isRemote) return;
        guard("worldUnload", () -> {
            me.Padej_.soupapi.modules.KillEffects.onWorldUnload();
            me.Padej_.soupapi.render.SmokeFx.clear();
            me.Padej_.soupapi.render.FxParticles.clear();
        });
    }

    // ---- first-person hand -----------------------------------------------------------------

    /**
     * Applies the Swing Hand placement to the first-person item.
     *
     * <p>1.8.9 keeps the first-person transform inside a private method, and Forge's
     * {@code RenderHandEvent} is the only hook near it — but it is cancellable and
     * {@code ItemRenderer.renderItemInFirstPerson} is public, so the vanilla call is replaced with
     * the same call wrapped in the mod's transform. Nothing else about hand rendering changes.
     */
    @SubscribeEvent
    public void onRenderHand(net.minecraftforge.client.event.RenderHandEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.gameSettings.thirdPersonView != 0 || mc.gameSettings.hideGUI) return;

        // A carried melee weapon replaces the hand outright, animations and all.
        if (me.Padej_.soupapi.melee.Melee.active() != null) {
            event.setCanceled(true);
            me.Padej_.soupapi.melee.MeleeRenderer.renderFirstPerson(event.partialTicks);
            return;
        }

        if (!me.Padej_.soupapi.config.ConfigurableModule.CONFIG.swingHandEnabled) return;
        if (mc.entityRenderer == null || mc.entityRenderer.itemRenderer == null) return;

        event.setCanceled(true);
        net.minecraft.client.renderer.GlStateManager.pushMatrix();
        try {
            me.Padej_.soupapi.modules.SwingHand.applyTransform(event.partialTicks);
            mc.entityRenderer.itemRenderer.renderItemInFirstPerson(event.partialTicks);
        } catch (Throwable t) {
            me.Padej_.soupapi.compat.SoupLog.error(
                    "[SoupAPI] the first-person hand transform failed; it is now disabled", t);
            me.Padej_.soupapi.config.ConfigurableModule.CONFIG.swingHandEnabled = false;
        } finally {
            net.minecraft.client.renderer.GlStateManager.popMatrix();
        }
    }

    /** Draws the carried melee weapon on the player's own body in third person. */
    @SubscribeEvent
    public void onRenderPlayerPost(net.minecraftforge.client.event.RenderPlayerEvent.Post event) {
        if (event.entityPlayer == null || event.renderer == null) return;
        guard("Melee.thirdPerson", me.Padej_.soupapi.utils.Palette.Element.WEAPON, () -> me.Padej_.soupapi.melee.MeleeThirdPerson.render(
                event.entityPlayer, event.renderer, event.partialRenderTick));
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

    /**
     * Applies the fog's distance curve.
     *
     * <p>{@code FogDensity} only covers the exponential curve, so without this the sphere/cylinder
     * choice did nothing at all. This event fires at the end of vanilla's fog setup, which is where
     * the 1.21.4 mixin applied the same state.
     */
    @SubscribeEvent
    public void onRenderFog(EntityViewRenderEvent.RenderFogEvent event) {
        if (!CustomFog.shouldApply()) return;
        CustomFog.applyFogState();
    }

    // ---- projection ------------------------------------------------------------------------

    @SubscribeEvent
    public void onCameraSetup(EntityViewRenderEvent.CameraSetup event) {
        AspectRatio.applyToProjection();
        // The carried weapon's swing bakes its recoil into a "camera" bone; apply it to the real
        // camera here, the way the pack's own renderer does, so it reads as a kick.
        if (me.Padej_.soupapi.config.ConfigurableModule.CONFIG.meleeEnabled) {
            guard("Melee.cameraShake", me.Padej_.soupapi.utils.Palette.Element.WEAPON, () -> me.Padej_.soupapi.melee.MeleeRenderer.applyCameraShake(event));
        }
    }

    // ---- overlays --------------------------------------------------------------------------

    /** Suppresses the burning overlay; {@code NoRender} draws its replacement in the HUD pass. */
    @SubscribeEvent
    public void onBlockOverlay(net.minecraftforge.client.event.RenderBlockOverlayEvent event) {
        if (event.overlayType == net.minecraftforge.client.event.RenderBlockOverlayEvent.OverlayType.FIRE
                && me.Padej_.soupapi.modules.NoRender.shouldCancelFire()) {
            event.setCanceled(true);
        }
    }

    // ---- sound -----------------------------------------------------------------------------

    /**
     * Mutes vanilla's hurt sound for the player's own hits when the mod replaces it.
     *
     * <p>{@code hitSoundOverwriteEnabled} was read by nothing. 1.8.9 has no attacker-side hit sound
     * to replace — what you hear is the victim's hurt sound — so "overwrite" is implemented as
     * silencing that, and only inside a short window after one of the player's own hits, so damage
     * taken and other players' fights still sound normal.
     */
    @SubscribeEvent
    public void onPlaySound(net.minecraftforge.client.event.sound.PlaySoundEvent event) {
        me.Padej_.soupapi.config.SoupAPI_Config config =
                me.Padej_.soupapi.config.ConfigurableModule.CONFIG;
        if (!config.hitSoundOverwriteEnabled || !config.hitSoundEnabled) return;
        if (event.sound == null || event.name == null) return;
        if (!event.name.contains(".hurt")) return;
        if (me.Padej_.soupapi.utils.HitSound.sinceLastHitMs() > 300L) return;
        event.result = null;
    }

    // ---- chat ------------------------------------------------------------------------------

    /**
     * Feeds Russian chat lines to the translator and shows the result alongside them.
     *
     * <p>Nothing called {@code Translator.queueTranslation} on this port — the 1.21.4 version fed it
     * from a chat mixin — so the translator had no input and its option did nothing.
     */
    @SubscribeEvent
    public void onChatReceived(net.minecraftforge.client.event.ClientChatReceivedEvent event) {
        if (!me.Padej_.soupapi.config.ConfigurableModule.CONFIG.translatorBruhEnabled) return;
        if (event.message == null) return;

        String text = event.message.getUnformattedText();
        if (text == null || text.isEmpty() || !Translator.containsCyrillic(text)) return;

        String translated = Translator.cachedTranslation(text);
        if (translated != null && !translated.isEmpty()) {
            event.message = me.Padej_.soupapi.compat.Texts.literal(
                    text + " \u00a77(" + translated + ")");
        } else {
            // Queued now; the next identical line is shown translated, which is as fast as a
            // rate-limited public translation API allows.
            Translator.queueTranslation(text);
        }
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
