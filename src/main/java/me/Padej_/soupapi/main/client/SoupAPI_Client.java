package me.Padej_.soupapi.main.client;

import me.Padej_.soupapi.compat.Caps;
import me.Padej_.soupapi.compat.DrawContext;
import me.Padej_.soupapi.compat.SoupLog;
import me.Padej_.soupapi.config.AutoConfig;
import me.Padej_.soupapi.config.ConfigManager;
import me.Padej_.soupapi.event.SoupEvents;
import me.Padej_.soupapi.event.WorldRenderContext;
import me.Padej_.soupapi.font.FontRenderers;
import me.Padej_.soupapi.gui.SoupSettingsScreen;
import me.Padej_.soupapi.interfaces.OverlayReloadListener;
import me.Padej_.soupapi.modules.AmbientParticle;
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
import me.Padej_.soupapi.utils.EntityUtils;
import me.Padej_.soupapi.utils.HitSound;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.input.Keyboard;

/**
 * Client entrypoint: wires the modules onto the mod's own event bus.
 *
 * <p>Differences from the 1.21.4 version, all forced by the 1.8.9 platform:
 *
 * <ul>
 *   <li>Events come from {@link SoupEvents}, driven by the mod's mixins, rather than Fabric API.
 *       {@code WorldRenderEvents.AFTER_ENTITIES} had no listeners and is dropped.</li>
 *   <li>The settings-screen shortcut is read through LWJGL 2's {@code Keyboard} rather than GLFW
 *       key codes against a window handle, which is how 1.8.9 and PojavLauncher's LWJGL 2 stub
 *       report input.</li>
 *   <li>Particle-factory registration is gone: 1.8.9 has no particle registry to extend, and the
 *       particle it registered was never spawned (see {@code SoupAPI_Main}).</li>
 * </ul>
 */
public class SoupAPI_Client implements ClientModInitializer {

    /** Guards against re-opening the settings screen while the shortcut is held. */
    private boolean settingsShortcutHeld;

    @Override
    public void onInitializeClient() {
        SoupEvents.END_CLIENT_TICK.register(this::doEndClientTick);
        SoupEvents.WORLD_RENDER_LAST.register(this::doRenderLast);
        SoupEvents.HUD_RENDER.register(this::renderHud);

        registerOnHit();
        Translator.loadCache();
        ConfigManager.loadConfig();

        // The settings screen is the mod's only config UI now that ModMenu and Cloth Config are
        // gone; AutoConfig.getConfigScreen() routes to it.
        // The settings screen has no parent-screen constructor, so the parent is ignored.
        AutoConfig.setScreenFactory(parent -> new SoupSettingsScreen());

        SoupLog.info("SoupAPI client ready (" + Caps.describe() + ")");
    }

    private void renderHud(SoupEvents.HudRenderContext context) {
        DrawContext drawContext = context.drawContext();
        TargetHud.render(drawContext, context.tickCounter());
        WatermarkRenderer.render(drawContext);
        MouseMove.render(drawContext);
    }

    private void doEndClientTick(MinecraftClient client) {
        FontRenderers.ensureLoaded();
        EntityUtils.updateEntities(client);
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

        handleSettingsShortcut(client);
    }

    /**
     * Opens the settings screen on right-ctrl + right-alt, matching the 1.21.4 binding.
     *
     * <p>Edge-triggered: the 1.21.4 version re-opened the screen on every tick the combination was
     * held, which also meant it called {@code setScreen} twice in a row.
     */
    private void handleSettingsShortcut(MinecraftClient client) {
        Screen currentScreen = client.currentScreen;
        if (currentScreen instanceof ChatScreen) return;

        boolean held;
        try {
            held = Keyboard.isKeyDown(Keyboard.KEY_RCONTROL) && Keyboard.isKeyDown(Keyboard.KEY_RMENU);
        } catch (Throwable t) {
            // Pojav's LWJGL 2 stub may not implement every key query; never let input probing
            // break the tick loop.
            return;
        }

        if (held && !settingsShortcutHeld && !(currentScreen instanceof SoupSettingsScreen)) {
            client.setScreen(new SoupSettingsScreen());
        }
        settingsShortcutHeld = held;
    }

    private void doRenderLast(WorldRenderContext context) {
        TargetRender.renderTarget(context);
        HitBubbles.render(context);
        Halo.render(context);
        Trails.renderTrail(context);
        JumpCircles.renderCircles(context);
        Trajectories.render(context);

        renderParticles(context);
        Render3D_Shapes.render(context);
    }

    private void registerOnHit() {
        HitBubbles.registerOnHit();
        HitSound.registerOnHit();
        HitParticle.registerOnHit();
    }

    private void renderParticles(WorldRenderContext context) {
        JumpParticles.render(context);
        HitParticle.render(context);
        TotemPopParticles.render(context);
        AmbientParticle.render(context);
    }
}
