package me.Padej_.soupapi.modules;

import me.Padej_.soupapi.compat.Maths;

import me.Padej_.soupapi.compat.Cameras;

import me.Padej_.soupapi.compat.BlendFactor;
import me.Padej_.soupapi.compat.ShaderKey;

import net.minecraft.client.renderer.GlStateManager;
import me.Padej_.soupapi.compat.RenderSystem;
import me.Padej_.soupapi.config.ConfigurableModule;
import me.Padej_.soupapi.render.Render2D;
import me.Padej_.soupapi.utils.ConfigUtils;
import me.Padej_.soupapi.utils.Palette;
import me.Padej_.soupapi.utils.TexturesManager;
import me.Padej_.soupapi.event.WorldRenderContext;
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
import me.Padej_.soupapi.compat.MatrixStack;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import me.Padej_.soupapi.compat.RotationAxis;
import net.minecraft.util.Vec3;
import me.Padej_.soupapi.compat.Mat4;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class JumpCircles extends ConfigurableModule {

    private static final List<JumpCircle> CIRCLES = new ArrayList<>();
    private static boolean wasJumping = false;

    public static void onTick() {
        if (mc.thePlayer == null) return;
        EntityPlayer player = mc.thePlayer;
        boolean airborne = !player.onGround;

        // Fires on leaving the ground with upward motion. The old test also required the jump key
        // to still be held on the tick the player left the ground, which is a coin toss with a
        // touch control - on PojavLauncher the button is usually released by then, so most jumps
        // produced no circle at all.
        if (airborne && !wasJumping && player.motionY > 0.0) {
            CIRCLES.add(new JumpCircle(player.getPositionVector()));
        }

        wasJumping = airborne;
    }

    public static void renderCircles(WorldRenderContext context) {
        if (!CONFIG.jumpCirclesEnabled) {
            CIRCLES.clear();
            return;
        }

        double cameraX = Cameras.get().getPositionVector().xCoord;
        double cameraY = Cameras.get().getPositionVector().yCoord;
        double cameraZ = Cameras.get().getPositionVector().zCoord;

        CIRCLES.removeIf(JumpCircle::isExpired);
        for (JumpCircle circle : CIRCLES) {
            circle.render(context, cameraX, cameraY, cameraZ);
        }
    }

    private static class JumpCircle {
        private final Vec3 position;
        private final double offsetY;
        private final long startTime; // Время создания круга
        private float rotationAngle;
        private final float angularVelocity;
        private long lastUpdateTime; // Время последнего обновления
        private final boolean isFadeOut; // Флаг для эффекта затухания

        public JumpCircle(Vec3 position) {
            this.position = position;
            this.offsetY = mc.thePlayer.motionY;
            this.rotationAngle = 0f;
            this.angularVelocity = (float) Math.toRadians(CONFIG.jumpCirclesSpinSpeed);
            this.startTime = System.currentTimeMillis(); // Запоминаем время создания
            this.lastUpdateTime = startTime; // Инициализация времени обновления
            this.isFadeOut = CONFIG.jumpCirclesFadeOut; // Инициализация флага из конфигурации
        }

        public boolean isExpired() {
            long currentTime = System.currentTimeMillis();
            float elapsedTime = (currentTime - startTime) / 1000f; // Прошедшее время в секундах
            return elapsedTime > CONFIG.jumpCirclesLiveTime; // Сравниваем с временем жизни в секундах
        }

        public void render(WorldRenderContext context, double cameraX, double cameraY, double cameraZ) {
            double x = position.xCoord - cameraX;
            double y = position.yCoord - cameraY - offsetY;
            double z = position.zCoord - cameraZ;

            updateRotation();
            renderGlowCircleBufferBuilder(context.matrixStack(), x, y, z);
        }

        private void updateRotation() {
            long currentTime = System.currentTimeMillis();
            float deltaTime = (currentTime - lastUpdateTime) / 1000f;
            lastUpdateTime = currentTime;

            deltaTime = Math.min(deltaTime, 0.1f);

            float totalLifetime = CONFIG.jumpCirclesLiveTime;
            float elapsedTime = (currentTime - startTime) / 1000f;
            float remainingFraction = Maths.clamp((totalLifetime - elapsedTime) / totalLifetime, 0, 1);

            // Вращение до начала исчезания
            if (remainingFraction > 0.3f) {
                float frameTime = 1.0f / 60.0f;
                float normalizedDelta = deltaTime / frameTime;
                rotationAngle -= angularVelocity * normalizedDelta;
            } else {
                float fadeFactor = remainingFraction / 0.3f; // от 1 до 0
                float frameTime = 1.0f / 60.0f;
                float normalizedDelta = deltaTime / frameTime;
                rotationAngle += angularVelocity * normalizedDelta * fadeFactor;
            }
        }

        private void renderGlowCircleBufferBuilder(MatrixStack modelMatrix, double x, double y, double z) {
            int glowAlpha = ConfigUtils.intPercentToHexInt(CONFIG.jumpCirclesAlpha);
            float liveTime = CONFIG.jumpCirclesLiveTime;

            RenderSystem.disableCull();
            RenderSystem.enableBlend();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.blendFunc(BlendFactor.SrcFactor.SRC_ALPHA, BlendFactor.DstFactor.ONE);

            RenderSystem.setShaderTexture(0, TexturesManager.getJumpCircle());
            RenderSystem.setShader(ShaderKey.POSITION_TEX_COLOR);

            BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

            float elapsedTime = (System.currentTimeMillis() - startTime) / 1000f;
            float ageFraction = elapsedTime / (liveTime / 3);
            ageFraction *= 3;
            float scaleMultiplier = CONFIG.jumpCirclesScale / 100f;
            ageFraction = Maths.clamp(ageFraction * scaleMultiplier, 0, scaleMultiplier);

            float remainingFraction = Maths.clamp((liveTime - elapsedTime) / liveTime, 0, 1);

            if (remainingFraction < 0.3f) {
                float shrinkFactor = remainingFraction / 0.3f;
                ageFraction *= shrinkFactor;
            }

            float interpolatedRadius = ageFraction;
            float colorAnim = isFadeOut ? 1f - remainingFraction : 1.0f;
            float scale = interpolatedRadius * 2f;

            // jumpCirclesColorSpinSpeed rotates which palette colour sits at which corner, so the
            // gradient travels around the ring instead of being pinned to it. It was read by
            // nothing before, which is why the option appeared to do nothing.
            float colorPhase = (System.currentTimeMillis() % 100000L) / 1000f
                    * (CONFIG.jumpCirclesColorSpinSpeed / 10f);
            Color color1 = Palette.getInterpolatedPaletteColor(wrap(colorPhase));
            Color color2 = Palette.getInterpolatedPaletteColor(wrap(colorPhase + 0.33f));
            Color color3 = Palette.getInterpolatedPaletteColor(wrap(colorPhase + 0.66f));
            Color color4 = Palette.getInterpolatedPaletteColor(wrap(colorPhase + 1.0f));

            modelMatrix.push();
            modelMatrix.translate(x, y, z);
            modelMatrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));
            modelMatrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) Math.toDegrees(rotationAngle)));
            Mat4 matrix = modelMatrix.peek().getPositionMatrix();

            buffer.vertex(matrix, -interpolatedRadius, -interpolatedRadius + scale, 0)
                    .texture(0, 1)
                    .color(color1.getRed(), color1.getGreen(), color1.getBlue(), (int) (glowAlpha * colorAnim));
            buffer.vertex(matrix, -interpolatedRadius + scale, -interpolatedRadius + scale, 0)
                    .texture(1, 1)
                    .color(color2.getRed(), color2.getGreen(), color2.getBlue(), (int) (glowAlpha * colorAnim));
            buffer.vertex(matrix, -interpolatedRadius + scale, -interpolatedRadius, 0)
                    .texture(1, 0)
                    .color(color3.getRed(), color3.getGreen(), color3.getBlue(), (int) (glowAlpha * colorAnim));
            buffer.vertex(matrix, -interpolatedRadius, -interpolatedRadius, 0)
                    .texture(0, 0)
                    .color(color4.getRed(), color4.getGreen(), color4.getBlue(), (int) (glowAlpha * colorAnim));

            modelMatrix.pop();

            Render2D.endBuilding(buffer);
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
            RenderSystem.enableCull();
        }
    }

    /** Keeps a palette position inside 0..1, where the palette repeats. */
    private static float wrap(float position) {
        float v = position % 1f;
        return v < 0f ? v + 1f : v;
    }

    public enum JumCircleStyle {
        CIRCLE, CIRCLE_BOLD, HEXAGON, PORTAL, SOUP
    }
}
