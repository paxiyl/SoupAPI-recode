package me.Padej_.soupapi.render;

import me.Padej_.soupapi.compat.BufferBuilder;
import me.Padej_.soupapi.compat.MatrixStack;
import me.Padej_.soupapi.compat.RenderLayer;
import me.Padej_.soupapi.compat.RenderSystem;
import me.Padej_.soupapi.compat.VertexConsumerProvider;
import me.Padej_.soupapi.main.SoupAPI_Main;
import me.Padej_.soupapi.utils.EntityUtils;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.entity.AbstractClientPlayer;

/**
 * Draws the "china hat" cone above a player's head.
 *
 * <p>Rewritten for 1.8.9's immediate-mode feature pipeline. The 1.21.4 version was a drop-in
 * replacement for {@code HeadFeatureRenderer} — it re-implemented vanilla's worn-head and skull
 * rendering because it took that renderer's place in the feature list, and it consumed a
 * {@code LivingEntityRenderState}. Neither applies here: 1.8.9 has no render states, and this is
 * registered as an <i>additional</i> feature alongside vanilla's head renderer, so all of that
 * re-implementation is dropped and only the hat remains.
 *
 * <p>Positioning follows the 1.8.9 idiom used by vanilla's own head feature: push the GL matrix,
 * call {@link net.minecraft.client.render.model.ModelPart#preRender(float)} on the head part to
 * inherit its pivot and rotation, then draw in block units. The entity render context has a
 * {@code scale(-1, -1, 1)} applied by {@code RendererLivingEntity}, so Y is flipped back here
 * before drawing the cone, which is authored Y-up.
 */
public class ChinaHatFeatureRenderer implements LayerRenderer<AbstractClientPlayer> {

    private final RenderPlayer playerRenderer;

    public ChinaHatFeatureRenderer(RenderPlayer playerRenderer) {
        this.playerRenderer = playerRenderer;
    }

    @Override
    public void render(AbstractClientPlayer entity, float limbAngle, float limbDistance,
                       float tickDelta, float age, float headYaw, float headPitch, float scale) {
        if (!SoupAPI_Main.configHolder.get().chinaHatEnabled) return;

        Minecraft client = Minecraft.getMinecraft();
        if (client.thePlayer == null || entity == null) return;

        // Only the local player and friends get a hat, as before. The game profile gives the
        // raw username; Entity.getName() returns a IChatComponent carrying display formatting in 1.8.9.
        String name = entity.getGameProfile() != null ? entity.getGameProfile().getName() : null;
        if (name == null) return;
        boolean isLocalPlayer = name.equals(client.thePlayer.getGameProfile().getName());
        if (!isLocalPlayer && !EntityUtils.isFriend(name)) return;

        GlStateManager.pushMatrix();
        try {
            if (entity.isSneaking()) {
                GlStateManager.translate(0f, 0.2f, 0f);
            }
            // Inherit the head's pivot and rotation.
            this.playerRenderer.getModel().head.preRender(scale);
            // Undo the entity context's inverted Y so the cone is drawn upright.
            GlStateManager.scale(1f, -1f, 1f);

            RenderLayer layer = CustomRenderLayers.CHINA_HAT_LAYER.apply(1.0);
            layer.startDrawing();
            RenderSystem.enableDepthTest();
            RenderSystem.enableBlend();

            VertexConsumerProvider provider = VertexConsumerProvider.immediate();
            BufferBuilder buffer = (BufferBuilder) provider.getBuffer(layer);
            Render3D.renderChinaHat(new MatrixStack(), buffer);
            provider.draw();

            RenderSystem.disableBlend();
        } finally {
            GlStateManager.popMatrix();
        }
    }

    @Override
    public boolean combineTextures() {
        return false;
    }
}
