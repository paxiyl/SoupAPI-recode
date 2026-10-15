package me.Padej_.soupapi.render;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.util.ResourceLocation;

/**
 * Applies the energy-swirl overlay to players.
 *
 * <p>{@code soupapi:textures/i.png} was one of the 39 textures the code referenced but that were
 * never committed upstream; it is authored for this port. Because the overlay is blended
 * {@code GL_ONE}/{@code GL_ONE} with the GL texture matrix scrolling, the artwork is emissive on
 * black (the source alpha is ignored) and tiles seamlessly in both axes.
 */
public class ChargedPlayerFeatureRenderer
        extends CustomEnergySwirlOverlayFeatureRenderer<AbstractClientPlayer> {

    private static final ResourceLocation SKIN = new ResourceLocation("soupapi", "textures/i.png");

    private final ModelPlayer model;

    public ChargedPlayerFeatureRenderer(ModelPlayer model) {
        this.model = model;
    }

    @Override
    protected boolean shouldRender(AbstractClientPlayer entity) {
        return true;
    }

    @Override
    protected float getEnergySwirlX(float partialAge) {
        return partialAge * 0.01f;
    }

    @Override
    protected ResourceLocation getEnergySwirlTexture() {
        return SKIN;
    }

    @Override
    protected ModelBase getEnergySwirlModel() {
        return model;
    }
}
