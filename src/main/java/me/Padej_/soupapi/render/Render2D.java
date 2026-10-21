package me.Padej_.soupapi.render;

import me.Padej_.soupapi.compat.Maths;

import me.Padej_.soupapi.compat.BlendFactor;
import me.Padej_.soupapi.compat.ShaderKey;
import me.Padej_.soupapi.compat.Windows;

import net.minecraft.client.renderer.GlStateManager;
import me.Padej_.soupapi.compat.RenderSystem;
import me.Padej_.soupapi.utils.GaussianFilter;
import me.Padej_.soupapi.utils.Palette;
import me.Padej_.soupapi.utils.Texture;
import net.minecraft.client.Minecraft;
import me.Padej_.soupapi.compat.BufferBuilder;
import me.Padej_.soupapi.compat.BufferRenderer;
import me.Padej_.soupapi.compat.Tessellator;
import me.Padej_.soupapi.compat.VertexFormat;
import me.Padej_.soupapi.compat.VertexFormats;
import me.Padej_.soupapi.compat.VertexConsumer;
import me.Padej_.soupapi.compat.VertexConsumerProvider;
import me.Padej_.soupapi.compat.RenderLayer;
import me.Padej_.soupapi.compat.Camera;
import me.Padej_.soupapi.compat.RenderTickCounter;
import net.minecraft.client.renderer.texture.DynamicTexture;
import me.Padej_.soupapi.compat.MatrixStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.MathHelper;
import org.apache.commons.lang3.RandomStringUtils;
import me.Padej_.soupapi.compat.Mat4;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;

import static me.Padej_.soupapi.config.ConfigurableModule.CONFIG;

public class Render2D {
    private final static Minecraft mc = Minecraft.getMinecraft();
    public static HashMap<Long, BlurredShadow> shadowCache = new HashMap<>();
    public static HashMap<Long, BlurredShadow> shadowCache1 = new HashMap<>();


    public static void renderMiscTexture(MatrixStack matrices, ResourceLocation textureId, float scale) {
        int screenWidth = Windows.scaledWidth();
        int screenHeight = Windows.scaledHeight();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(BlendFactor.SrcFactor.SRC_ALPHA, BlendFactor.DstFactor.ONE_MINUS_SRC_COLOR);
        RenderSystem.setShaderTexture(0, textureId);
        RenderSystem.setShader(ShaderKey.POSITION_TEX_COLOR);

        matrices.push();
        matrices.translate(screenWidth / 2f, screenHeight / 2f, 0);
        matrices.scale(scale, scale, 1f);
        matrices.translate(-screenWidth / 2f, -screenHeight / 2f, 0);

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        Mat4 matrix = matrices.peek().getPositionMatrix();

        float x0 = 0f;
        float y0 = 0f;
        float x1 = screenWidth;
        float y1 = screenHeight;

        Color c1 = Palette.getColor(0f);   // Нижний левый
        Color c2 = Palette.getColor(0.33f); // Нижний правый
        Color c3 = Palette.getColor(0.66f); // Верхний правый
        Color c4 = Palette.getColor(1f);   // Верхний левый

        buffer.vertex(matrix, x0, y1, 0).texture(0.0f, 1.0f).color(c1.getRed() / 255f, c1.getGreen() / 255f, c1.getBlue() / 255f, 1);
        buffer.vertex(matrix, x1, y1, 0).texture(1.0f, 1.0f).color(c2.getRed() / 255f, c2.getGreen() / 255f, c2.getBlue() / 255f, 1);
        buffer.vertex(matrix, x1, y0, 0).texture(1.0f, 0.0f).color(c3.getRed() / 255f, c3.getGreen() / 255f, c3.getBlue() / 255f, 1);
        buffer.vertex(matrix, x0, y0, 0).texture(0.0f, 0.0f).color(c4.getRed() / 255f, c4.getGreen() / 255f, c4.getBlue() / 255f, 1);

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        matrices.pop();
        RenderSystem.disableBlend();
    }

    public static void setupRender() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    public static void endRender() {
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    public static Color injectAlpha(final Color color, final int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), Maths.clamp(alpha, 0, 255));
    }

    public static Color applyOpacity(Color color, float opacity) {
        opacity = Math.min(1, Math.max(0, opacity));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (color.getAlpha() * opacity));
    }

    public static int applyOpacity(int color_int, float opacity) {
        opacity = Math.min(1, Math.max(0, opacity));
        Color color = new Color(color_int);
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (color.getAlpha() * opacity)).getRGB();
    }

    public static void drawHudBase2(MatrixStack matrices, float x, float y, float width, float height, float radius, float blurStrenth, float blurOpacity, float animationFactor) {
        blurStrenth *= animationFactor;
        blurOpacity = (float) interpolate(1f, blurOpacity, animationFactor);
        Color c = interpolateColorC(Color.BLACK, Color.BLACK, animationFactor);
        drawRound(matrices, x, y, width, height, radius, c);
    }

    public static void renderGradientTexture(MatrixStack matrices, double x0, double y0, double width, double height, float u, float v, double regionWidth, double regionHeight, double textureWidth, double textureHeight, Color c1, Color c2, Color c3, Color c4) {
        RenderSystem.setShader(ShaderKey.POSITION_TEX_COLOR);
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        renderGradientTextureInternal(buffer, matrices, x0, y0, width, height, u, v, regionWidth, regionHeight, textureWidth, textureHeight, c1, c2, c3, c4);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
    }

    public static void renderGradientTextureInternal(BufferBuilder buff, MatrixStack matrices, double x0, double y0, double width, double height, float u, float v, double regionWidth, double regionHeight, double textureWidth, double textureHeight, Color c1, Color c2, Color c3, Color c4) {
        double x1 = x0 + width;
        double y1 = y0 + height;
        double z = 0;
        Mat4 matrix = matrices.peek().getPositionMatrix();
        buff.vertex(matrix, (float) x0, (float) y1, (float) z).texture((u) / (float) textureWidth, (v + (float) regionHeight) / (float) textureHeight).color(c1.getRGB());
        buff.vertex(matrix, (float) x1, (float) y1, (float) z).texture((u + (float) regionWidth) / (float) textureWidth, (v + (float) regionHeight) / (float) textureHeight).color(c2.getRGB());
        buff.vertex(matrix, (float) x1, (float) y0, (float) z).texture((u + (float) regionWidth) / (float) textureWidth, (v) / (float) textureHeight).color(c3.getRGB());
        buff.vertex(matrix, (float) x0, (float) y0, (float) z).texture((u) / (float) textureWidth, (v + 0.0F) / (float) textureHeight).color(c4.getRGB());
    }

    public static void renderTexture(MatrixStack matrices, ResourceLocation identifier, double x0, double y0, double width, double height, float u, float v, double regionWidth, double regionHeight, double textureWidth, double textureHeight) {
        double x1 = x0 + width;
        double y1 = y0 + height;
        double z = 0;
        Mat4 matrix = matrices.peek().getPositionMatrix();
        RenderSystem.setShaderTexture(0, identifier);
        RenderSystem.setShader(ShaderKey.POSITION_TEX);
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        buffer.vertex(matrix, (float) x0, (float) y1, (float) z).texture((u) / (float) textureWidth, (v + (float) regionHeight) / (float) textureHeight);
        buffer.vertex(matrix, (float) x1, (float) y1, (float) z).texture((u + (float) regionWidth) / (float) textureWidth, (v + (float) regionHeight) / (float) textureHeight);
        buffer.vertex(matrix, (float) x1, (float) y0, (float) z).texture((u + (float) regionWidth) / (float) textureWidth, (v) / (float) textureHeight);
        buffer.vertex(matrix, (float) x0, (float) y0, (float) z).texture((u) / (float) textureWidth, (v + 0.0F) / (float) textureHeight);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.setShaderTexture(0, 0);
    }

    public static void renderTexture(MatrixStack matrices, double x0, double y0, double width, double height, float u, float v, double regionWidth, double regionHeight, double textureWidth, double textureHeight) {
        double x1 = x0 + width;
        double y1 = y0 + height;
        double z = 0;
        Mat4 matrix = matrices.peek().getPositionMatrix();
        RenderSystem.setShader(ShaderKey.POSITION_TEX);
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        buffer.vertex(matrix, (float) x0, (float) y1, (float) z).texture((u) / (float) textureWidth, (v + (float) regionHeight) / (float) textureHeight);
        buffer.vertex(matrix, (float) x1, (float) y1, (float) z).texture((u + (float) regionWidth) / (float) textureWidth, (v + (float) regionHeight) / (float) textureHeight);
        buffer.vertex(matrix, (float) x1, (float) y0, (float) z).texture((u + (float) regionWidth) / (float) textureWidth, (v) / (float) textureHeight);
        buffer.vertex(matrix, (float) x0, (float) y0, (float) z).texture((u) / (float) textureWidth, (v + 0.0F) / (float) textureHeight);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.setShaderTexture(0, 0);
    }

    public static void renderRoundedQuad2(MatrixStack matrices, Color c, Color c2, Color c3, Color c4, double fromX, double fromY, double toX, double toY, double radius) {
        setupRender();
        RenderSystem.setShader(ShaderKey.POSITION_COLOR);
        renderRoundedQuadInternal2(matrices.peek().getPositionMatrix(), c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, c.getAlpha() / 255f, c2.getRed() / 255f, c2.getGreen() / 255f, c2.getBlue() / 255f, c2.getAlpha() / 255f, c3.getRed() / 255f, c3.getGreen() / 255f, c3.getBlue() / 255f, c3.getAlpha() / 255f, c4.getRed() / 255f, c4.getGreen() / 255f, c4.getBlue() / 255f, c4.getAlpha() / 255f, fromX, fromY, toX, toY, radius);
        endRender();
    }

    public static void drawGradientRound(MatrixStack ms, float x, float y, float wight, float height, float cornersRadius, Color darker, Color darker1, Color darker2, Color darker3) {
        renderRoundedQuad2(ms, darker, darker1, darker2, darker3, x, y, x + wight, y + height, cornersRadius);
    }

    public static void renderRoundedQuadInternal2(Mat4 matrix, float cr, float cg, float cb, float ca, float cr1, float cg1, float cb1, float ca1, float cr2, float cg2, float cb2, float ca2, float cr3, float cg3, float cb3, float ca3, double fromX, double fromY, double toX, double toY, double radC1) {
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);

        double[][] map = new double[][]{new double[]{toX - radC1, toY - radC1, radC1}, new double[]{toX - radC1, fromY + radC1, radC1}, new double[]{fromX + radC1, fromY + radC1, radC1}, new double[]{fromX + radC1, toY - radC1, radC1}};

        for (int i = 0; i < 4; i++) {
            double[] current = map[i];
            double rad = current[2];
            for (double r = i * 90; r < (90 + i * 90); r += 10) {
                float rad1 = (float) Math.toRadians(r);
                float sin = (float) (Math.sin(rad1) * rad);
                float cos = (float) (Math.cos(rad1) * rad);
                switch (i) {
                    case 0:
                        bufferBuilder.vertex(matrix, (float) current[0] + sin, (float) current[1] + cos, 0.0F).color(cr1, cg1, cb1, ca1);
                        break;
                    case 1:
                        bufferBuilder.vertex(matrix, (float) current[0] + sin, (float) current[1] + cos, 0.0F).color(cr, cg, cb, ca);
                        break;
                    case 2:
                        bufferBuilder.vertex(matrix, (float) current[0] + sin, (float) current[1] + cos, 0.0F).color(cr2, cg2, cb2, ca2);
                        break;
                    default:
                        bufferBuilder.vertex(matrix, (float) current[0] + sin, (float) current[1] + cos, 0.0F).color(cr3, cg3, cb3, ca3);
                        break;
                }
            }
        }
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
    }

    public static boolean isHovered(double mouseX, double mouseY, double x, double y, double width, double height) {
        return mouseX >= x && mouseX - width <= x && mouseY >= y && mouseY - height <= y;
    }

    public static Color interpolateColorC(Color color1, Color color2, float amount) {
        amount = Math.min(1, Math.max(0, amount));
        return new Color(interpolateInt(color1.getRed(), color2.getRed(), amount), interpolateInt(color1.getGreen(), color2.getGreen(), amount), interpolateInt(color1.getBlue(), color2.getBlue(), amount), interpolateInt(color1.getAlpha(), color2.getAlpha(), amount));
    }

    public static double interpolate(double oldValue, double newValue, double interpolationValue) {
        return (oldValue + (newValue - oldValue) * interpolationValue);
    }

    public static float interpolateFloat(float oldValue, float newValue, double interpolationValue) {
        return (float) interpolate(oldValue, newValue, (float) interpolationValue);
    }

    public static int interpolateInt(int oldValue, int newValue, double interpolationValue) {
        return (int) interpolate(oldValue, newValue, (float) interpolationValue);
    }

    public static void drawRect(MatrixStack matrices, float x, float y, float width, float height, Color c) {
        Mat4 matrix = matrices.peek().getPositionMatrix();
        setupRender();
        RenderSystem.setShader(ShaderKey.POSITION_COLOR);
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        buffer.vertex(matrix, x, y + height, 0.0F).color(c.getRGB());
        buffer.vertex(matrix, x + width, y + height, 0.0F).color(c.getRGB());
        buffer.vertex(matrix, x + width, y, 0.0F).color(c.getRGB());
        buffer.vertex(matrix, x, y, 0.0F).color(c.getRGB());
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        endRender();
    }

    public static void setRectPoints(BufferBuilder bufferBuilder, Mat4 matrix, float x, float y, float x1, float y1, Color c1, Color c2, Color c3, Color c4) {
        bufferBuilder.vertex(matrix, x, y1, 0.0F).color(c1.getRGB());
        bufferBuilder.vertex(matrix, x1, y1, 0.0F).color(c2.getRGB());
        bufferBuilder.vertex(matrix, x1, y, 0.0F).color(c3.getRGB());
        bufferBuilder.vertex(matrix, x, y, 0.0F).color(c4.getRGB());
    }

    /**
     * Registers a texture from raw PNG bytes.
     *
     * <p>1.8.9 has no NativeImage: DynamicTexture is constructed from a BufferedImage,
     * which ImageIO decodes. Rendering in 1.8.9 is single-threaded (the client thread is the
     * render thread), so the upload happens inline rather than being scheduled.
     */
    private static void registerTexture(Texture i, byte[] content) {
        try {
            BufferedImage image = ImageIO.read(new java.io.ByteArrayInputStream(content));
            if (image == null) return;
            DynamicTexture tex = new DynamicTexture(image);
            tex.updateDynamicTexture();
            mc.getTextureManager().loadTexture(i.getId(), tex);
        } catch (Exception ignored) {
        }
    }

    public static void registerBufferedImageTexture(Texture i, BufferedImage bi) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bi, "png", baos);
            byte[] bytes = baos.toByteArray();
            registerTexture(i, bytes);
        } catch (Exception ignored) {
        }
    }

    /**
     * A rounded rectangle with a colour at each corner.
     *
     * <p>Colours are given bottom-left, bottom-right, top-right, top-left and interpolated across
     * the shape's own vertices.
     *
     * <p>This used to be drawn with the destination-alpha trick it was ported with: clear the whole
     * framebuffer's alpha channel behind a colour mask, stamp the rounded shape into that alpha,
     * then blend the gradient against it. Two things were wrong with that. It needs a framebuffer
     * with an alpha channel and a driver that honours a colour mask during a clear - and gl4es, the
     * GL translation layer PojavLauncher runs on, honours neither dependably, so the clear took the
     * whole frame with it and the game went black. It also meant a full-screen clear per bar, of
     * which the HUD draws upwards of twenty a frame. Drawing the gradient into the shape's vertices
     * needs neither.
     */
    public static void renderRoundedGradientRect(MatrixStack matrices, Color color1, Color color2, Color color3, Color color4, float x, float y, float width, float height, float Radius) {
        if (width <= 0f || height <= 0f) return;
        float radius = Math.min(Radius, Math.min(width, height) / 2f);
        // renderRoundedQuad2 takes its colours top-right, bottom-right, top-left, bottom-left.
        renderRoundedQuad2(matrices, color3, color2, color4, color1, x, y, x + width, y + height, radius);
    }

    /**
     * A texture drawn inside a rounded rectangle, tinted per corner.
     *
     * <p>The replacement for masking a texture through the framebuffer's alpha channel: the corners
     * are rounded by the geometry the texture is mapped onto, so nothing is cleared and nothing
     * depends on there being a destination alpha to blend against.
     */
    public static void renderRoundedTexture(MatrixStack matrices, ResourceLocation texture,
                                            float x, float y, float width, float height,
                                            float u, float v, float regionWidth, float regionHeight,
                                            float textureWidth, float textureHeight,
                                            float radius, Color tint) {
        if (width <= 0f || height <= 0f) return;
        radius = Math.min(radius, Math.min(width, height) / 2f);
        setupRender();
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShader(ShaderKey.POSITION_TEX_COLOR);
        Mat4 matrix = matrices.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_TEXTURE_COLOR);
        float x2 = x + width, y2 = y + height;
        float u0 = u / textureWidth, v0 = v / textureHeight;
        float du = regionWidth / textureWidth, dv = regionHeight / textureHeight;
        int rgb = tint.getRGB();
        // Centre first, then the four corner arcs, then back to the start: a fan that covers the
        // rounded rectangle whatever its radius.
        fanVertex(buffer, matrix, (x + x2) / 2f, (y + y2) / 2f, x, y, width, height, u0, v0, du, dv, rgb);
        float[][] corners = {{x2 - radius, y2 - radius}, {x2 - radius, y + radius},
                {x + radius, y + radius}, {x + radius, y2 - radius}};
        int steps = radius < 0.5f ? 0 : Math.max(2, Math.min(10, (int) radius));
        float firstX = 0f, firstY = 0f;
        for (int i = 0; i < 4; i++) {
            for (int k = 0; k <= steps; k++) {
                double a = Math.toRadians(i * 90 + (steps == 0 ? 0 : 90.0 * k / steps));
                float px = corners[i][0] + (float) Math.sin(a) * radius;
                float py = corners[i][1] + (float) Math.cos(a) * radius;
                if (i == 0 && k == 0) {
                    firstX = px;
                    firstY = py;
                }
                fanVertex(buffer, matrix, px, py, x, y, width, height, u0, v0, du, dv, rgb);
            }
        }
        fanVertex(buffer, matrix, firstX, firstY, x, y, width, height, u0, v0, du, dv, rgb);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.setShaderTexture(0, 0);
        endRender();
    }

    private static void fanVertex(BufferBuilder buffer, Mat4 matrix, float px, float py,
                                  float x, float y, float width, float height,
                                  float u0, float v0, float du, float dv, int rgb) {
        buffer.vertex(matrix, px, py, 0f)
                .texture(u0 + du * ((px - x) / width), v0 + dv * ((py - y) / height))
                .color(rgb);
    }

    /*
    public static void renderRoundedTextureRect(MatrixStack matrices, double x0, double y0, double width, double height, float u, float v, double regionWidth, double regionHeight, double textureWidth, double textureHeight, float Radius) {
        double x1 = x0 + width;
        double y1 = y0 + height;
        double z = 0;

        Mat4 matrix = matrices.peek().getPositionMatrix();

        drawRound(matrices, (float) x0, (float) y0, (float) width, (float) height, Radius, new Color(0xFFFF00FF, true));
        setupRender();
        RenderSystem.blendFunc(GL11.GL_DST_ALPHA, GL11.GL_ONE_MINUS_DST_ALPHA);
        RenderSystem.setShader(ShaderKey.POSITION_TEX);
        if (TargetHUD_N.target instanceof EntityPlayer) {
            RenderSystem.setShaderTexture(0, ((AbstractClientPlayer) TargetHUD_N.target).getLocationSkin());
        }
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        buffer.vertex(matrix, (float) x0, (float) y1, (float) z).texture((u) / (float) textureWidth, (v + (float) regionHeight) / (float) textureHeight);
        buffer.vertex(matrix, (float) x1, (float) y1, (float) z).texture((u + (float) regionWidth) / (float) textureWidth, (v + (float) regionHeight) / (float) textureHeight);
        buffer.vertex(matrix, (float) x1, (float) y0, (float) z).texture((u + (float) regionWidth) / (float) textureWidth, (v) / (float) textureHeight);
        buffer.vertex(matrix, (float) x0, (float) y0, (float) z).texture((u) / (float) textureWidth, (v + 0.0F) / (float) textureHeight);
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        endRender();
    }
     */

    public static void drawRound(MatrixStack matrices, float x, float y, float width, float height, float radius, Color color) {
        renderRoundedQuad(matrices, color, x, y, width + x, height + y, radius, 4);
    }

    public static void renderRoundedQuad(MatrixStack matrices, Color c, double fromX, double fromY, double toX, double toY, double radius, double samples) {
        setupRender();
        RenderSystem.setShader(ShaderKey.POSITION_COLOR);
        renderRoundedQuadInternal(matrices.peek().getPositionMatrix(), c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, c.getAlpha() / 255f, fromX, fromY, toX, toY, radius, samples);
        endRender();
    }

    public static void renderRoundedQuadInternal(Mat4 matrix, float cr, float cg, float cb, float ca, double fromX, double fromY, double toX, double toY, double radius, double samples) {
        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);

        // Центр прямоугольника
        double centerX = (fromX + toX) / 2.0;
        double centerY = (fromY + toY) / 2.0;
        bufferBuilder.vertex(matrix, (float) centerX, (float) centerY, 0.0F).color(cr, cg, cb, ca);

        // Определяем центры закруглений для каждого угла
        double[][] map = new double[][]{
                {toX - radius, toY - radius, radius},   // Верхний правый
                {toX - radius, fromY + radius, radius}, // Нижний правый
                {fromX + radius, fromY + radius, radius}, // Нижний левый
                {fromX + radius, toY - radius, radius}  // Верхний левый
        };

        // Рисуем закругленные углы
        for (int i = 0; i < 4; i++) {
            double[] current = map[i];
            double rad = current[2];
            for (double r = i * 90d; r <= (90 + i * 90d); r += (90 / samples)) {
                float rad1 = (float) Math.toRadians(r);
                float sin = (float) (Math.sin(rad1) * rad);
                float cos = (float) (Math.cos(rad1) * rad);
                bufferBuilder.vertex(matrix, (float) current[0] + sin, (float) current[1] + cos, 0.0F).color(cr, cg, cb, ca);
            }
        }

        // Закрываем TRIANGLE_FAN, добавляя первую вершину угла снова
        double[] firstCorner = map[0];
        float rad1 = (float) Math.toRadians(0);
        float sin = (float) (Math.sin(rad1) * firstCorner[2]);
        float cos = (float) (Math.cos(rad1) * firstCorner[2]);
        bufferBuilder.vertex(matrix, (float) firstCorner[0] + sin, (float) firstCorner[1] + cos, 0.0F).color(cr, cg, cb, ca);

        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
    }

    public static void drawBlurredShadow(MatrixStack matrices, float x, float y, float width, float height, int blurRadius, Color color) {
        width = width + blurRadius * 2;
        height = height + blurRadius * 2;
        x = x - blurRadius;
        y = y - blurRadius;

        BlurredShadow shadow = mask(shadowCache, (int) width, (int) height, blurRadius);
        shadow.bind();

        setupRender();
        RenderSystem.setShaderColor(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f);
        renderTexture(matrices, x, y, width, height, 0, 0, shadow.width, shadow.height, shadow.width, shadow.height);
        endRender();
    }

    /**
     * The blurred mask for a shadow of about this size, built once and kept.
     *
     * <p>Sizes are rounded up to {@value #MASK_STEP} pixels before they are looked up, which is the
     * whole reason this is usable at all: the callers are the crosshair, the potion timers, the
     * target HUD's health bar and the mouse-move dot, and every one of them changes width by a
     * fraction of a pixel per frame. Keyed exactly, each frame would build a new image, blur it,
     * encode it to PNG, decode it again and upload a new GL texture - none of which is ever freed.
     * Rounded, a few dozen masks cover everything the mod draws. The mask is then stretched over
     * whatever the caller actually asked for; nobody can see a pixel of difference in a blur.
     */
    private static BlurredShadow mask(java.util.Map<Long, BlurredShadow> cache, int width, int height, int blurRadius) {
        int w = Math.max(1, (width + MASK_STEP - 1) / MASK_STEP * MASK_STEP);
        int h = Math.max(1, (height + MASK_STEP - 1) / MASK_STEP * MASK_STEP);
        int r = Math.max(1, Math.min(blurRadius, Math.min(w, h) / 2));
        long identifier = ((long) w << 40) | ((long) h << 16) | (r & 0xFFFF);
        BlurredShadow shadow = cache.get(identifier);
        if (shadow == null) {
            // A bound in case something does find a way to ask for endless sizes: start again
            // rather than hold on to thousands of textures.
            if (cache.size() > MASK_LIMIT) cache.clear();
            shadow = new BlurredShadow(blurMask(w, h, r));
            cache.put(identifier, shadow);
        }
        return shadow;
    }

    /** Rounding applied to a shadow's size before it is looked up or built. */
    private static final int MASK_STEP = 16;
    /** How many masks are kept before the lot is thrown away and built again. */
    private static final int MASK_LIMIT = 96;

    /** The blurred rounded mask a soft shadow is drawn with. */
    private static BufferedImage blurMask(int w, int h, int blurRadius) {
        BufferedImage original = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics g = original.getGraphics();
        g.setColor(new Color(-1));
        g.fillRect(blurRadius, blurRadius, Math.max(1, w - blurRadius * 2), Math.max(1, h - blurRadius * 2));
        g.dispose();
        return new GaussianFilter(blurRadius).filter(original, null);
    }

    public static void drawGradientBlurredShadow1(MatrixStack matrices, float x, float y, float width, float height, int blurRadius, Color color1, Color color2, Color color3, Color color4) {
        if (!CONFIG.blurShadowEnabled) return;
        width = width + blurRadius * 2;
        height = height + blurRadius * 2;
        x = x - blurRadius;
        y = y - blurRadius;

        // Just the blurred mask. It used to be composited over an opaque black rectangle, which is
        // invisible under the additive blend below and a solid black box under any other.
        BlurredShadow shadow = mask(shadowCache1, (int) width, (int) height, blurRadius);

        shadow.bind();
        setupRender();
        RenderSystem.blendFunc(BlendFactor.SrcFactor.SRC_ALPHA, BlendFactor.DstFactor.ONE);
        renderGradientTexture(matrices, x, y, width, height, 0, 0, shadow.width, shadow.height,
                shadow.width, shadow.height, color1, color2, color3, color4);
        endRender();
    }

    public static void drawGlyphs(MatrixStack matrices, ResourceLocation texture, Color c, float scale) {
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.blendFunc(BlendFactor.SrcFactor.SRC_ALPHA, BlendFactor.DstFactor.ONE);
        RenderSystem.setShaderTexture(0, texture);

        RenderSystem.setShader(ShaderKey.POSITION_TEX_COLOR);

        RenderSystem.setShaderColor(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, c.getAlpha() / 255f);
        Render2D.renderGradientTexture(matrices, 0, 0, scale, scale, 0, 0, 128, 128, 128, 128, c, c, c, c);
        RenderSystem.setShaderColor(1, 1, 1, 1);

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    /**
     * Flushes a buffer. 1.21.4's endNullable() skipped the draw when nothing had been written;
     * 1.8.9's tessellator has no equivalent check, and callers only reach here with geometry
     * queued.
     */
    public static void endBuilding(BufferBuilder bb) {
        BufferRenderer.drawWithGlobalProgram(bb);
    }

    public static class BlurredShadow {
        Texture id;
        /** The canvas size, which is a power of two and so usually larger than what was drawn. */
        public final int width;
        public final int height;

        public BlurredShadow(BufferedImage bufferedImage) {
            this.id = new Texture("texture/remote/" + RandomStringUtils.randomAlphanumeric(16));
            this.width = bufferedImage.getWidth();
            this.height = bufferedImage.getHeight();
            registerBufferedImageTexture(id, bufferedImage);
        }

        public void bind() {
            RenderSystem.setShaderTexture(0, id.getId());
        }
    }
public static final class Rectangle {
    private final float x;
    private final float y;
    private final float x1;
    private final float y1;

    public Rectangle(float x, float y, float x1, float y1) {
        this.x = x;
        this.y = y;
        this.x1 = x1;
        this.y1 = y1;
    }

    public float x() {
        return x;
    }

    public float y() {
        return y;
    }

    public float x1() {
        return x1;
    }

    public float y1() {
        return y1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Rectangle)) return false;
        Rectangle other = (Rectangle) o;
        if (Float.compare(x, other.x) != 0) return false;
        if (Float.compare(y, other.y) != 0) return false;
        if (Float.compare(x1, other.x1) != 0) return false;
        if (Float.compare(y1, other.y1) != 0) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + Float.floatToIntBits(x);
        result = 31 * result + Float.floatToIntBits(y);
        result = 31 * result + Float.floatToIntBits(x1);
        result = 31 * result + Float.floatToIntBits(y1);
        return result;
    }

    @Override
    public String toString() {
        return "Rectangle[" + "x=" + x + ", " + "y=" + y + ", " + "x1=" + x1 + ", " + "y1=" + y1 + "]";
    }
        public boolean contains(double x, double y) {
            return x >= this.x && x <= x1 && y >= this.y && y <= y1;
        }
    }
}
