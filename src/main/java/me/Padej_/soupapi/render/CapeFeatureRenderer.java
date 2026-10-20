package me.Padej_.soupapi.render;

import me.Padej_.soupapi.config.ConfigurableModule;
import me.Padej_.soupapi.modules.Capes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;

/**
 * Draws the mod's cape on the player.
 *
 * <p>The 1.21.4 version replaced the player's cape texture through a mixin on
 * {@code AbstractClientPlayerEntity.getSkinTextures}. This port has no mixins, so the cape is drawn
 * as an extra feature layer instead: same vanilla cape model and the same swing maths as
 * {@code LayerCape}, with the mod's texture bound.
 *
 * <p>It only draws for the player running the client. The cape exists on this machine only — no
 * other player's client knows about it — so drawing it on everyone would be a lie about what other
 * people are wearing.
 */
public class CapeFeatureRenderer implements LayerRenderer<AbstractClientPlayer> {

    private final RenderPlayer playerRenderer;

    public CapeFeatureRenderer(RenderPlayer playerRenderer) {
        this.playerRenderer = playerRenderer;
    }

    @Override
    public void doRenderLayer(AbstractClientPlayer player, float limbAngle, float limbDistance,
                              float tickDelta, float age, float headYaw, float headPitch, float scale) {
        // A ghost is a re-render of the same model; its layers must not run again.
        if (me.Padej_.soupapi.modules.PlayerEffects.isDrawingGhost()) return;
        if (!ConfigurableModule.CONFIG.capesEnabled) return;
        if (player != Minecraft.getMinecraft().thePlayer) return;
        if (player.isInvisible()) return;

        ResourceLocation texture = Capes.activeCapeTexture();
        if (texture == null) return;

        ModelPlayer model = playerRenderer.getMainModel();
        if (model == null) return;

        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);

        GlStateManager.pushMatrix();
        try {
            GlStateManager.translate(0.0F, 0.0F, 0.125F);

            // Vanilla's cape swing: the difference between where the cape is chasing and where the
            // player actually is, converted into three rotations.
            double dx = interpolate(player.prevChasingPosX, player.chasingPosX, tickDelta)
                    - interpolate(player.prevPosX, player.posX, tickDelta);
            double dy = interpolate(player.prevChasingPosY, player.chasingPosY, tickDelta)
                    - interpolate(player.prevPosY, player.posY, tickDelta);
            double dz = interpolate(player.prevChasingPosZ, player.chasingPosZ, tickDelta)
                    - interpolate(player.prevPosZ, player.posZ, tickDelta);

            float bodyYaw = player.prevRenderYawOffset
                    + (player.renderYawOffset - player.prevRenderYawOffset) * tickDelta;
            double sin = MathHelper.sin(bodyYaw * (float) Math.PI / 180.0F);
            double cos = -MathHelper.cos(bodyYaw * (float) Math.PI / 180.0F);

            float swingY = (float) dy * 10.0F;
            swingY = MathHelper.clamp_float(swingY, -6.0F, 32.0F);

            float swingX = (float) (dx * sin + dz * cos) * 100.0F;
            swingX = MathHelper.clamp_float(swingX, 0.0F, 150.0F);

            float swingZ = (float) (dx * cos - dz * sin) * 100.0F;
            swingZ = MathHelper.clamp_float(swingZ, -20.0F, 20.0F);

            if (swingX < 0.0F) {
                swingX = 0.0F;
            }

            float bob = player.prevCameraYaw + (player.cameraYaw - player.prevCameraYaw) * tickDelta;
            swingY += MathHelper.sin(
                    (player.prevDistanceWalkedModified
                            + (player.distanceWalkedModified - player.prevDistanceWalkedModified) * tickDelta)
                            * 6.0F) * 32.0F * bob;

            if (player.isSneaking()) {
                swingY += 25.0F;
                GlStateManager.translate(0.0F, 0.142F, -0.0178F);
            }

            GlStateManager.rotate(6.0F + swingX / 2.0F + swingY, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(swingZ / 2.0F, 0.0F, 0.0F, 1.0F);
            GlStateManager.rotate(-swingZ / 2.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);

            model.renderCape(0.0625F);
        } finally {
            GlStateManager.popMatrix();
        }
    }

    private static double interpolate(double previous, double current, float tickDelta) {
        return previous + (current - previous) * tickDelta;
    }

    @Override
    public boolean shouldCombineTextures() {
        return false;
    }
}
