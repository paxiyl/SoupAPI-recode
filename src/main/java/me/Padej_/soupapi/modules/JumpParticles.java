package me.Padej_.soupapi.modules;

import me.Padej_.soupapi.compat.Cameras;

import me.Padej_.soupapi.compat.Maths;

import me.Padej_.soupapi.config.ConfigurableModule;
import me.Padej_.soupapi.render.Render2D;
import me.Padej_.soupapi.utils.MathUtility;
import me.Padej_.soupapi.utils.Palette;
import me.Padej_.soupapi.utils.TexturesManager;
import me.Padej_.soupapi.event.WorldRenderContext;
import net.minecraft.block.BlockAir;
import me.Padej_.soupapi.compat.MatrixStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import me.Padej_.soupapi.compat.RotationAxis;
import net.minecraft.util.Vec3;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

public class JumpParticles extends ConfigurableModule {
    public static final CopyOnWriteArrayList<Particle> particles = new CopyOnWriteArrayList<>();
    private static final List<ResourceLocation> AVAILABLE_TEXTURES = new ArrayList<>();
    private static boolean wasJumping = false;

    public static void onTick() {
        if (mc.thePlayer == null || !CONFIG.jumpParticlesEnabled) return;
        updateAvailableTextures();

        boolean isJumping = !mc.thePlayer.onGround;
        if (isJumping && !wasJumping && mc.gameSettings.keyBindJump.isKeyDown()) {
            spawnParticles();
        }
        wasJumping = isJumping;

        particles.removeIf(Particle::update);
    }

    public static void render(WorldRenderContext context) {
        if (mc.thePlayer != null && mc.theWorld != null) {
            for (Particle particle : particles) {
                particle.render(context.matrixStack(), context.tickCounter().getTickDelta(true));
            }
        }
    }

    private static void updateAvailableTextures() {
        AVAILABLE_TEXTURES.clear();

        if (CONFIG.jumpParticlesIncludeFirefly) AVAILABLE_TEXTURES.add(TexturesManager.themed(TexturesManager.FIREFLY_ALT));
        if (CONFIG.jumpParticlesIncludeDollar) AVAILABLE_TEXTURES.add(TexturesManager.themed(TexturesManager.DOLLAR));
        if (CONFIG.jumpParticlesIncludeSnowflake) AVAILABLE_TEXTURES.add(TexturesManager.themed(TexturesManager.SNOWFLAKE));
        if (CONFIG.jumpParticlesIncludeHeart) AVAILABLE_TEXTURES.add(TexturesManager.themed(TexturesManager.HEART));
        if (CONFIG.jumpParticlesIncludeStar) AVAILABLE_TEXTURES.add(TexturesManager.themed(TexturesManager.STAR));
        if (CONFIG.jumpParticlesIncludeGlyphs) {
            Collections.addAll(AVAILABLE_TEXTURES, TexturesManager.GLYPH_TEXTURES);
        }

        if (AVAILABLE_TEXTURES.isEmpty()) {
            AVAILABLE_TEXTURES.add(TexturesManager.themed(TexturesManager.FIREFLY));
        }
    }

    private static void spawnParticles() {
        if (mc.thePlayer == null) return;
        Vec3 pos = mc.thePlayer.getPositionVector();
        for (int i = 0; i < CONFIG.jumpParticlesCount; i++) {
            Color color = Palette.getRandomColor();
            particles.add(new Particle(
                    (float) pos.xCoord, (float) pos.yCoord, (float) pos.zCoord,
                    color,
                    MathUtility.random(0, 180),
                    MathUtility.random(10f, 60f)
            ));
        }
    }

    public static class Particle {
        protected float x, y, z;
        protected float px, py, pz;
        protected float motionX, motionY, motionZ;
        protected float rotationAngle, rotationSpeed;
        protected ResourceLocation glyphTexture;
        protected long time;

        private final long lifeTimeMs;
        private float alpha = 1.0f;
        private float scale = CONFIG.jumpParticlesScale;

        public Particle(float x, float y, float z, Color color, float rotationAngle, float rotationSpeed) {
            this.x = px = x;
            this.y = py = y;
            this.z = pz = z;
            this.rotationAngle = rotationAngle;
            this.rotationSpeed = rotationSpeed;
            this.time = System.currentTimeMillis();
            this.lifeTimeMs = CONFIG.jumpParticlesLiveTime * 1000L;

            int speed = CONFIG.jumpParticlesSpeed;
            this.motionX = MathUtility.random(-speed / 50f, speed / 50f);
            this.motionY = 0;
            this.motionZ = MathUtility.random(-speed / 50f, speed / 50f);

            if (!AVAILABLE_TEXTURES.isEmpty()) {
                this.glyphTexture = AVAILABLE_TEXTURES.get(new Random().nextInt(AVAILABLE_TEXTURES.size()));
            }

            this.color = color;
        }

        private final Color color;

        public boolean update() {
            double sp = Math.sqrt(motionX * motionX + motionZ * motionZ);

            px = x;
            py = y;
            pz = z;
            x += motionX;
            y += motionY;
            z += motionZ;

            boolean bounce = CONFIG.jumpParticlesPhysic == Physic.BOUNCE;

            if (posBlock(x, y - CONFIG.jumpParticlesScale / 10f, z)) {
                if (bounce) {
                    motionY = 0.18f + MathUtility.random(0.0f, 0.05f);
                    motionX += MathUtility.random(-0.005f, 0.005f);
                    motionZ += MathUtility.random(-0.005f, 0.005f);
                } else {
                    motionY = -motionY * 0.8f;
                }

                motionX *= 0.98f;
                motionZ *= 0.98f;
            } else if (posBlock(x - sp, y, z - sp) || posBlock(x + sp, y, z + sp) ||
                    posBlock(x + sp, y, z - sp) || posBlock(x - sp, y, z + sp) ||
                    posBlock(x + sp, y, z) || posBlock(x - sp, y, z) ||
                    posBlock(x, y, z + sp) || posBlock(x, y, z - sp)) {
                motionX = -motionX * 0.9f;
                motionZ = -motionZ * 0.9f;
            }

            if (bounce) motionY -= 0.035f;
            motionY *= 0.99f;

            long elapsed = System.currentTimeMillis() - time;
            float lifeFraction = Math.min(1.0f, (float) elapsed / lifeTimeMs);

            if (lifeFraction >= 0.8f) {
                float fade = 1.0f - (lifeFraction - 0.8f) / 0.2f;
                if (CONFIG.jumpParticlesDisappear == Disappear.ALPHA) {
                    alpha = fade;
                } else if (CONFIG.jumpParticlesDisappear == Disappear.SCALE) {
                    scale = CONFIG.jumpParticlesScale * fade;
                }
            }

            return elapsed > lifeTimeMs;
        }

        public void render(MatrixStack matrixStack, float tickDelta) {
            if (!CONFIG.jumpParticlesEnabled) return;

            float baseScale = 0.07f;
            float renderScale = baseScale * scale;

            double posX = Maths.lerp(tickDelta, px, x) - mc.getRenderManager().viewerPosX;
            double posY = Maths.lerp(tickDelta, py, y) - mc.getRenderManager().viewerPosY;
            double posZ = Maths.lerp(tickDelta, pz, z) - mc.getRenderManager().viewerPosZ;

            matrixStack.push();
            matrixStack.translate(posX, posY, posZ);
            matrixStack.scale(renderScale, renderScale, renderScale);
            matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-Cameras.get().getYaw()));
            matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(Cameras.get().getPitch()));
            matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotationAngle += (1f / net.minecraft.client.Minecraft.getDebugFPS()) * rotationSpeed));

            if (glyphTexture != null) {
                Render2D.drawGlyphs(matrixStack, glyphTexture, new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (alpha * 255)), 1f);
            }

            matrixStack.pop();
        }

        private boolean posBlock(double x, double y, double z) {
            if (mc.thePlayer == null || mc.theWorld == null) return false;
            return (!(mc.theWorld.getBlockState(new BlockPos(x, y, z)).getBlock() instanceof BlockAir));
        }
    }

    public enum Physic {
        BOUNCE, FLY
    }

    public enum Disappear {
        ALPHA, SCALE
    }
}

