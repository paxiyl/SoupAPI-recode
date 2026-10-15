package me.Padej_.soupapi.render;

import net.minecraft.client.renderer.GlStateManager;
import me.Padej_.soupapi.compat.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.model.ModelBase;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/**
 * Re-renders an entity's model with a scrolling, additively blended overlay texture — the
 * "charged creeper" look, applied to arbitrary entities.
 *
 * <p>Fully rewritten for 1.8.9. The 1.21.4 version obtained a {@code VertexConsumer} for a render
 * layer carrying an {@code OffsetTexturing} phase and submitted the model into it. 1.8.9 draws
 * immediately and animates such overlays by translating the <i>texture matrix</i>, so this follows
 * vanilla 1.8.9's own charged-creeper feature renderer: bind the overlay texture, scroll the
 * texture matrix, switch to additive blending with lighting off, draw the model again, then restore
 * every piece of state it touched.
 *
 * <p>Restoring state matters more here than it did in 1.21.4: the fixed-function pipeline is
 * global, so a leaked texture matrix or blend func would corrupt everything drawn afterwards in the
 * frame.
 */
@Environment(EnvType.CLIENT)
public abstract class CustomEnergySwirlOverlayFeatureRenderer<E extends EntityLivingBase>
        implements LayerRenderer<E> {

    @Override
    public void render(E entity, float limbAngle, float limbDistance, float tickDelta,
                       float age, float headYaw, float headPitch, float scale) {
        if (!shouldRender(entity)) return;

        ModelBase model = getEnergySwirlModel();
        if (model == null) return;

        boolean invisible = entity.isInvisible();
        GlStateManager.depthMask(!invisible);

        Minecraft.getMinecraft().getTextureManager().bindTexture(getEnergySwirlTexture());

        GlStateManager.matrixMode(GL11.GL_TEXTURE);
        GlStateManager.pushMatrix();
        GlStateManager.loadIdentity();
        float offset = getEnergySwirlX(age) % 1.0f;
        GlStateManager.translate(offset, offset, 0f);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);

        GlStateManager.enableBlend();
        // Additive, matching the 1.21.4 layer's ADDITIVE_TRANSPARENCY.
        GlStateManager.blendFunc(GL11.GL_ONE, GL11.GL_ONE);
        GlStateManager.disableLighting();
        GlStateManager.color(0.5f, 0.5f, 0.5f, 1.0f);

        try {
            model.render(entity, limbAngle, limbDistance, age, headYaw, headPitch, scale);
        } finally {
            // Restore: the fixed-function pipeline is global state.
            GlStateManager.matrixMode(GL11.GL_TEXTURE);
            GlStateManager.popMatrix();
            GlStateManager.matrixMode(GL11.GL_MODELVIEW);

            GlStateManager.enableLighting();
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            RenderSystem.defaultBlendFunc();
            GlStateManager.disableBlend();
            GlStateManager.depthMask(true);
        }
    }

    @Override
    public boolean combineTextures() {
        return false;
    }

    protected abstract boolean shouldRender(E entity);

    /** Texture-coordinate scroll for the given entity age, in texture units per tick. */
    protected abstract float getEnergySwirlX(float partialAge);

    protected abstract ResourceLocation getEnergySwirlTexture();

    protected abstract ModelBase getEnergySwirlModel();
}
