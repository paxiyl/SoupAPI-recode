package me.Padej_.soupapi.compat;

/**
 * Replacement for {@code net.minecraft.client.render.VertexConsumer}.
 *
 * <p>1.21.4 wrote world geometry into a consumer obtained from a {@code VertexConsumerProvider},
 * which batched by render layer and flushed later. 1.8.9 is immediate-mode: there is one shared
 * tessellator and geometry is drawn when the buffer is ended. So the only implementation here is
 * {@link BufferBuilder}, and {@link VertexConsumerProvider} hands it out after applying the
 * layer's GL state.
 */
public interface VertexConsumer {

    VertexConsumer vertex(Mat4 matrix, float x, float y, float z);

    VertexConsumer vertex(double x, double y, double z);

    VertexConsumer texture(float u, float v);

    VertexConsumer color(int argb);

    VertexConsumer color(int red, int green, int blue, int alpha);

    VertexConsumer color(float red, float green, float blue, float alpha);

    VertexConsumer light(int packed);

    VertexConsumer normal(float x, float y, float z);

    VertexConsumer overlay(int packed);
}
