package me.Padej_.soupapi.render;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.util.Identifier;

/**
 * Applies the energy-swirl overlay to players.
 *
 * <p>Note: {@code soupapi:textures/i.png} is referenced here but is not present in this
 * repository — it is one of 39 textures the code refers to that were never committed upstream. The
 * renderer is ported faithfully, but until that file is added the overlay will sample the
 * missing-texture placeholder. This was equally true on 1.21.4.
 */
public class ChargedPlayerFeatureRenderer
        extends CustomEnergySwirlOverlayFeatureRenderer<AbstractClientPlayerEntity> {

    private static final Identifier SKIN = new Identifier("soupapi", "textures/i.png");

    private final PlayerEntityModel model;

    public ChargedPlayerFeatureRenderer(PlayerEntityModel model) {
        this.model = model;
    }

    @Override
    protected boolean shouldRender(AbstractClientPlayerEntity entity) {
        return true;
    }

    @Override
    protected float getEnergySwirlX(float partialAge) {
        return partialAge * 0.01f;
    }

    @Override
    protected Identifier getEnergySwirlTexture() {
        return SKIN;
    }

    @Override
    protected EntityModel getEnergySwirlModel() {
        return model;
    }
}
