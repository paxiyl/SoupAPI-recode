package me.Padej_.soupapi.render;

import me.Padej_.soupapi.compat.RenderLayer;
import me.Padej_.soupapi.compat.VertexFormat;
import me.Padej_.soupapi.compat.VertexFormats;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * The mod's custom render layers.
 *
 * <p>1.21.4 built these from {@code RenderLayer.of(...)} with a chain of {@code RenderPhase}
 * objects naming core shader programs. 1.8.9 has no programmable pipeline, so each layer is
 * expressed through the compat {@link RenderLayer} builder as the fixed-function state it actually
 * implies — draw mode, vertex format, texture, blending, culling and depth.
 *
 * <p>Notable translations:
 * <ul>
 *   <li>{@code ALWAYS_DEPTH_TEST} becomes "depth test disabled", which is the same thing for a
 *       pass that should never be occluded.</li>
 *   <li>{@code OffsetTexturing} (the scrolling swirl UVs) becomes a texture-matrix translate, which
 *       is how vanilla 1.8.9 animates the charged-creeper overlay.</li>
 *   <li>The overlay (damage-flash) phase has no 1.8.9 equivalent and is dropped; 1.8.9 has no
 *       overlay texture stage.</li>
 * </ul>
 *
 * <p>{@code Util.memoize} does not exist in 1.8.9, so the caches are plain maps. They are only
 * touched from the render thread.
 */
public class CustomRenderLayers {

    private static final Map<Double, RenderLayer> CHINA_HAT_CACHE = new HashMap<Double, RenderLayer>();
    private static final Map<String, RenderLayer> ENERGY_SWIRL_CACHE = new HashMap<String, RenderLayer>();

    /**
     * Translucent, unculled triangle strip used for the china hat.
     *
     * <p>Kept as a lookup keyed by line width to match the old {@code Function<Double, ...>} call
     * shape, although 1.8.9's fixed-function state does not vary with it.
     */
    public static final Lookup CHINA_HAT_LAYER = new Lookup() {
        @Override
        public RenderLayer apply(double lineWidth) {
            Double key = Double.valueOf(lineWidth);
            RenderLayer layer = CHINA_HAT_CACHE.get(key);
            if (layer == null) {
                layer = RenderLayer.builder("soupapi:china_hat_layer",
                                VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR)
                        .transparency(RenderLayer.Transparency.TRANSLUCENT)
                        .cull(false)
                        .build();
                CHINA_HAT_CACHE.put(key, layer);
            }
            return layer;
        }
    };

    /** Mirrors the {@code Function<Double, RenderLayer>} the 1.21.4 call site used. */
    public interface Lookup {
        RenderLayer apply(double lineWidth);
    }

    /** Additive, lightmap-lit swirl overlay with scrolling texture coordinates. */
    public static RenderLayer getCustomEnergySwirl(Identifier texture, float x, float y) {
        String key = texture + "@" + x + "," + y;
        RenderLayer layer = ENERGY_SWIRL_CACHE.get(key);
        if (layer == null) {
            layer = RenderLayer.builder("soupapi:custom_energy_swirl",
                            VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR)
                    .texture(texture)
                    .transparency(RenderLayer.Transparency.ADDITIVE)
                    .textureOffset(x, y)
                    .depthTest(true)
                    .build();
            ENERGY_SWIRL_CACHE.put(key, layer);
        }
        return layer;
    }
}
