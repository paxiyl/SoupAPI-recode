package me.Padej_.soupapi.render;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.util.ResourceLocation;

/**
 * Applies the energy-swirl overlay to players.
 *
 * <p>Note: {@code soupapi:textures/i.png} is referenced here but is not present in this
 * repository — it is one of 39 textures the code refers to that were never committed upstream. The
 * renderer is ported faithfully, but until that file is added the overlay will sample the
 * missing-texture placeholder. This was equally true on 1.21.4.
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
