package me.Padej_.soupapi.modules;

import me.Padej_.soupapi.compat.ShaderKey;

import me.Padej_.soupapi.compat.RenderSystem;
import me.Padej_.soupapi.config.ConfigurableModule;
import me.Padej_.soupapi.render.Render2D;
import me.Padej_.soupapi.render.TargetHudRenderer;
import me.Padej_.soupapi.utils.EntityUtils;
import me.Padej_.soupapi.event.WorldRenderContext;
import me.Padej_.soupapi.compat.BufferBuilder;
import me.Padej_.soupapi.compat.Tessellator;
import me.Padej_.soupapi.compat.VertexFormat;
import me.Padej_.soupapi.compat.VertexFormats;
import me.Padej_.soupapi.compat.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Vec3;
import me.Padej_.soupapi.compat.Mat4;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class Trails extends ConfigurableModule {

    /**
     * Per-entity trail history.
     *
     * <p>1.21.4 stored this on the entity itself through a mixin-implemented {@code TrailEntity}
     * interface. There are no mixins here, so nothing implemented it and every tick threw a
     * {@link ClassCastException} on the first player it looked at — which is why trails did nothing
     * whatever they were set to. A weak-keyed side map does the same job without touching the
     * entity class, and lets entities be collected with the world.
     */
    private static final Map<Entity, List<TrailSegment>> TRAILS =
            new WeakHashMap<Entity, List<TrailSegment>>();

    private static List<TrailSegment> trailsOf(Entity entity) {
        List<TrailSegment> list = TRAILS.get(entity);
        if (list == null) {
            list = new ArrayList<TrailSegment>();
            TRAILS.put(entity, list);
        }
        return list;
    }

    /** Whether this entity should leave a trail, by the configured target. */
    public static boolean isTarget(Entity entity) {
        boolean self = entity == mc.thePlayer;
        switch (CONFIG.trailsTarget) {
            case SELF:
                return self;
            case OTHERS:
                return !self;
            case BOTH:
            default:
                return true;
        }
    }

    public static void onTick() {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (!CONFIG.trailsEnabled) {
            if (!TRAILS.isEmpty()) TRAILS.clear();
            return;
        }

        int trailLifetime = CONFIG.trailsLenght;
        float minStep = 0.001f;

        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (!isTarget(player)) continue;
            Vec3 prevPos = new Vec3(player.prevPosX, player.prevPosY, player.prevPosZ);
            Vec3 currentPos = player.getPositionVector();
            List<TrailSegment> trails = trailsOf(player);
            if (prevPos.distanceTo(currentPos) > minStep) {
                trails.add(new TrailSegment(prevPos, currentPos, trailLifetime));
            }
            trails.removeIf(TrailSegment::update);
        }

        // Anything else alive, when the option asks for it. Elytra are 1.9+, so on 1.8.9 this is
        // "other living entities" rather than "gliders".
        if (CONFIG.trailsForGliders) {
            for (Entity entity : mc.theWorld.loadedEntityList) {
                if (entity instanceof EntityPlayer || !(entity instanceof EntityLivingBase)) continue;
                if (!isTarget(entity)) continue;

                Vec3 prevPos = new Vec3(entity.prevPosX, entity.prevPosY, entity.prevPosZ);
                Vec3 currentPos = entity.getPositionVector();
                List<TrailSegment> trails = trailsOf(entity);
                if (prevPos.distanceTo(currentPos) > minStep) {
                    trails.add(new TrailSegment(prevPos, currentPos, trailLifetime));
                }
                trails.removeIf(TrailSegment::update);
            }
        }
    }

    public static void renderTrail(WorldRenderContext context) {
        MatrixStack matrixStack = context.matrixStack();
        float tickDelta = context.tickCounter().getTickDelta(true);

        matrixStack.push();
        Mat4 matrix = matrixStack.peek().getPositionMatrix();

        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        // Glow adds the trail's colour to what is behind it instead of blending over it, which is
        // what makes a bright trail read as light rather than paint.
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA,
                CONFIG.trailsGlow ? GL11.GL_ONE : GL11.GL_ONE_MINUS_SRC_ALPHA);

        RenderSystem.setShader(ShaderKey.POSITION_COLOR);

        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        for (Map.Entry<Entity, List<TrailSegment>> entry : TRAILS.entrySet()) {
            Entity entity = entry.getKey();
            if (entity == null || entity.isDead || !(entity instanceof EntityLivingBase)) continue;
            if (!isTarget(entity)) continue;
            if (entity != mc.thePlayer && !(entity instanceof EntityPlayer) && !CONFIG.trailsForGliders) continue;

            // Your own trail is invisible from inside your own head unless you ask for it.
            if (entity == mc.thePlayer && mc.gameSettings.thirdPersonView == 0 && !CONFIG.trailsFirstPerson)
                continue;

            List<TrailSegment> trails = entry.getValue();
            if (trails == null || trails.isEmpty()) continue;

            if (CONFIG.trailsStyle == Trails.Style.SYMBOLS) {
                renderSymbolTrail(trails, tickDelta);
                continue;
            }

            float height = entity.height * (CONFIG.trailsHeight / 100f);
            int baseAlpha = 255;

            for (int i = 0; i < trails.size() - 1; i++) {
                TrailSegment current = trails.get(i);
                TrailSegment next = trails.get(i + 1);

                Vec3 currentPos = current.interpolate(tickDelta);
                Vec3 nextPos = next.interpolate(tickDelta);

                float currentProgress = (float) current.animation(tickDelta);
                float nextProgress = (float) next.animation(tickDelta);
                float currentAlphaFactor = currentProgress * baseAlpha;
                float nextAlphaFactor = nextProgress * baseAlpha;

                float currentProgressColor = current.getProgress(tickDelta);
                float nextProgressColor = next.getProgress(tickDelta);
                Color currentColor = colorAt(currentProgressColor);
                Color nextColor = colorAt(nextProgressColor);

                float x1 = (float) currentPos.xCoord;
                float y1 = (float) currentPos.yCoord;
                float z1 = (float) currentPos.zCoord;

                float x2 = (float) nextPos.xCoord;
                float y2 = (float) nextPos.yCoord;
                float z2 = (float) nextPos.zCoord;

                int currentBottomAlpha;
                int currentMidAlpha;
                int currentTopAlpha;
                int nextBottomAlpha;
                int nextMidAlpha;
                int nextTopAlpha;

                if (CONFIG.trailsStyle == Trails.Style.FADED) {
                    currentBottomAlpha = (int) (computeFadedAlpha(0, height) * (currentAlphaFactor / 255.0f));
                    currentMidAlpha = (int) (computeFadedAlpha(height / 2.0f, height) * (currentAlphaFactor / 255.0f));
                    currentTopAlpha = CONFIG.trailsRenderHalf ? 0 : (int) (computeFadedAlpha(height, height) * (currentAlphaFactor / 255.0f));
                    nextBottomAlpha = (int) (computeFadedAlpha(0, height) * (nextAlphaFactor / 255.0f));
                    nextMidAlpha = (int) (computeFadedAlpha(height / 2.0f, height) * (nextAlphaFactor / 255.0f));
                    nextTopAlpha = CONFIG.trailsRenderHalf ? 0 : (int) (computeFadedAlpha(height, height) * (nextAlphaFactor / 255.0f));
                } else if (CONFIG.trailsStyle == Trails.Style.FADED_INVERT) {
                    currentBottomAlpha = (int) (computeFadedAlphaInvert(0, height) * (currentAlphaFactor / 255.0f));
                    currentMidAlpha = (int) (computeFadedAlphaInvert(height / 2.0f, height) * (currentAlphaFactor / 255.0f));
                    currentTopAlpha = CONFIG.trailsRenderHalf ? 0 : (int) (computeFadedAlphaInvert(height, height) * (currentAlphaFactor / 255.0f));
                    nextBottomAlpha = (int) (computeFadedAlphaInvert(0, height) * (nextAlphaFactor / 255.0f));
                    nextMidAlpha = (int) (computeFadedAlphaInvert(height / 2.0f, height) * (nextAlphaFactor / 255.0f));
                    nextTopAlpha = CONFIG.trailsRenderHalf ? 0 : (int) (computeFadedAlphaInvert(height, height) * (nextAlphaFactor / 255.0f));
                } else if (CONFIG.trailsStyle == Trails.Style.PULSE) {
                    // A wave of brightness travelling back along the trail.
                    float wave = (float) (0.55 + 0.45 * Math.sin(
                            currentProgressColor * Math.PI * 4 - System.currentTimeMillis() / 160.0));
                    float waveNext = (float) (0.55 + 0.45 * Math.sin(
                            nextProgressColor * Math.PI * 4 - System.currentTimeMillis() / 160.0));
                    currentBottomAlpha = (int) (currentAlphaFactor * wave);
                    currentMidAlpha = (int) (currentAlphaFactor * wave);
                    currentTopAlpha = CONFIG.trailsRenderHalf ? 0 : (int) (currentAlphaFactor * wave * 0.4f);
                    nextBottomAlpha = (int) (nextAlphaFactor * waveNext);
                    nextMidAlpha = (int) (nextAlphaFactor * waveNext);
                    nextTopAlpha = CONFIG.trailsRenderHalf ? 0 : (int) (nextAlphaFactor * waveNext * 0.4f);
                } else if (CONFIG.trailsStyle == Trails.Style.RIBBON) {
                    // A narrow band through the middle, nothing at the edges.
                    currentBottomAlpha = 0;
                    currentMidAlpha = (int) currentAlphaFactor;
                    currentTopAlpha = 0;
                    nextBottomAlpha = 0;
                    nextMidAlpha = (int) nextAlphaFactor;
                    nextTopAlpha = 0;
                } else if (CONFIG.trailsStyle == Trails.Style.DOTTED) {
                    // Every other segment, so the trail reads as a dashed line.
                    boolean on = (i % 2) == 0;
                    int value = on ? (int) currentAlphaFactor : 0;
                    int valueNext = on ? (int) nextAlphaFactor : 0;
                    currentBottomAlpha = value;
                    currentMidAlpha = value;
                    currentTopAlpha = CONFIG.trailsRenderHalf ? 0 : value;
                    nextBottomAlpha = valueNext;
                    nextMidAlpha = valueNext;
                    nextTopAlpha = CONFIG.trailsRenderHalf ? 0 : valueNext;
                } else if (CONFIG.trailsStyle == Trails.Style.NEON) {
                    // Bright core, faint shoulders.
                    currentBottomAlpha = (int) (currentAlphaFactor * 0.25f);
                    currentMidAlpha = (int) currentAlphaFactor;
                    currentTopAlpha = CONFIG.trailsRenderHalf ? 0 : (int) (currentAlphaFactor * 0.25f);
                    nextBottomAlpha = (int) (nextAlphaFactor * 0.25f);
                    nextMidAlpha = (int) nextAlphaFactor;
                    nextTopAlpha = CONFIG.trailsRenderHalf ? 0 : (int) (nextAlphaFactor * 0.25f);
                } else if (CONFIG.trailsStyle == Trails.Style.GRADIENT) {
                    // Solid at the feet, gone by the top, and fading with age along its length.
                    currentBottomAlpha = (int) currentAlphaFactor;
                    currentMidAlpha = (int) (currentAlphaFactor * 0.5f);
                    currentTopAlpha = CONFIG.trailsRenderHalf ? 0 : 0;
                    nextBottomAlpha = (int) nextAlphaFactor;
                    nextMidAlpha = (int) (nextAlphaFactor * 0.5f);
                    nextTopAlpha = CONFIG.trailsRenderHalf ? 0 : 0;
                } else { // SOLID
                    currentBottomAlpha = (int) currentAlphaFactor;
                    currentMidAlpha = (int) currentAlphaFactor;
                    currentTopAlpha = CONFIG.trailsRenderHalf ? 0 : (int) currentAlphaFactor;
                    nextBottomAlpha = (int) nextAlphaFactor;
                    nextMidAlpha = (int) nextAlphaFactor;
                    nextTopAlpha = CONFIG.trailsRenderHalf ? 0 : (int) nextAlphaFactor;
                }

                // Нижний QUAD: от низа до середины
                bufferBuilder.vertex(matrix, x1, y1, z1)
                        .color(currentColor.getRed(), currentColor.getGreen(), currentColor.getBlue(), currentBottomAlpha);
                bufferBuilder.vertex(matrix, x2, y2, z2)
                        .color(nextColor.getRed(), nextColor.getGreen(), nextColor.getBlue(), nextBottomAlpha);
                bufferBuilder.vertex(matrix, x2, y2 + height / 2.0f, z2)
                        .color(nextColor.getRed(), nextColor.getGreen(), nextColor.getBlue(), nextMidAlpha);
                bufferBuilder.vertex(matrix, x1, y1 + height / 2.0f, z1)
                        .color(currentColor.getRed(), currentColor.getGreen(), currentColor.getBlue(), currentMidAlpha);

                // Верхний QUAD: от середины до верха (если не trailsRenderHalf)
                if (!CONFIG.trailsRenderHalf) {
                    bufferBuilder.vertex(matrix, x1, y1 + height / 2.0f, z1)
                            .color(currentColor.getRed(), currentColor.getGreen(), currentColor.getBlue(), currentMidAlpha);
                    bufferBuilder.vertex(matrix, x2, y2 + height / 2.0f, z2)
                            .color(nextColor.getRed(), nextColor.getGreen(), nextColor.getBlue(), nextMidAlpha);
                    bufferBuilder.vertex(matrix, x2, y2 + height, z2)
                            .color(nextColor.getRed(), nextColor.getGreen(), nextColor.getBlue(), nextTopAlpha);
                    bufferBuilder.vertex(matrix, x1, y1 + height, z1)
                            .color(currentColor.getRed(), currentColor.getGreen(), currentColor.getBlue(), currentTopAlpha);
                }
            }
        }

        Render2D.endBuilding(bufferBuilder);

        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);

        matrixStack.pop();
    }

    /**
     * A trail drawn as a line of symbols rather than a ribbon.
     *
     * <p>The sprite is whichever symbol is chosen in the settings, so this is 43 trails rather than
     * one.
     */
    private static void renderSymbolTrail(List<TrailSegment> trails, float tickDelta) {
        net.minecraft.util.ResourceLocation texture =
                me.Padej_.soupapi.utils.TexturesManager.getRandomGlyphParticle();
        me.Padej_.soupapi.compat.BufferBuilder buffer =
                me.Padej_.soupapi.render.Fx.beginSprites(texture);
        net.minecraft.util.Vec3 cameraOffset = me.Padej_.soupapi.compat.Cameras.get().getPos();
        for (int i = 0; i < trails.size(); i++) {
            TrailSegment segment = trails.get(i);
            Vec3 pos = segment.interpolate(tickDelta);
            float progress = segment.getProgress(tickDelta);
            float alpha = Math.max(0f, 1f - progress);
            if (alpha <= 0.02f) continue;
            // interpolate() is already camera-relative; the sprite helper wants world space.
            me.Padej_.soupapi.render.Fx.appendSprite(buffer,
                    pos.xCoord + mc.getRenderManager().viewerPosX,
                    pos.yCoord + mc.getRenderManager().viewerPosY + 0.9,
                    pos.zCoord + mc.getRenderManager().viewerPosZ,
                    0.45f * (CONFIG.trailsHeight / 100f),
                    me.Padej_.soupapi.render.Fx.withAlpha(colorAt(progress), alpha), false);
        }
        me.Padej_.soupapi.render.Fx.endSprites(buffer);
    }

    public static class TrailSegment {
        private final Vec3 from;
        private final Vec3 to;
        private int ticks;
        private int prevTicks;
        private final int maxLifetime;

        public TrailSegment(Vec3 from, Vec3 to, int lifetime) {
            this.from = from;
            this.to = to;
            this.ticks = lifetime;
            this.maxLifetime = lifetime;
        }

        public Vec3 interpolate(float pt) {
            double x = from.xCoord + ((to.xCoord - from.xCoord) * pt) - mc.getRenderManager().viewerPosX;
            double y = from.yCoord + ((to.yCoord - from.yCoord) * pt) - mc.getRenderManager().viewerPosY;
            double z = from.zCoord + ((to.zCoord - from.zCoord) * pt) - mc.getRenderManager().viewerPosZ;
            return new Vec3(x, y, z);
        }

        public double animation(float pt) {
            float age = (this.maxLifetime - (this.prevTicks + (this.ticks - this.prevTicks) * pt));
            return Math.max(0, 1 - age / maxLifetime);
        }

        public boolean update() {
            this.prevTicks = this.ticks;
            return this.ticks-- <= 0;
        }

        public float getProgress(float pt) {
            return (maxLifetime - (this.prevTicks + (this.ticks - this.prevTicks) * pt)) / (float) maxLifetime;
        }
    }

    private static int computeFadedAlpha(float yOffset, float height) {
        float yRelative = yOffset / height;
        if (yRelative <= 0.5f) {
            return (int) ((1.0f - yRelative / 0.5f) * 255);
        } else {
            return (int) (((yRelative - 0.5f) / 0.5f) * 255);
        }
    }

    private static int computeFadedAlphaInvert(float yOffset, float height) {
        float yRelative = yOffset / height;
        int alphaFactor = (int) (255 * (CONFIG.trailsAlphaFactor / 100.0f));
        if (yRelative <= 0.5f) {
            return (int) (alphaFactor + (yRelative / 0.5f) * (255 - alphaFactor));
        } else {
            return (int) (255 - ((yRelative - 0.5f) / 0.5f) * (255 - alphaFactor));
        }
    }

    /**
     * The colour at a point along a trail.
     *
     * <p>Three modes: the HUD's own four-corner gradient (as before), a palette that flows along the
     * trail over time, and a single flat colour.
     */
    public static Color colorAt(float progress) {
        switch (CONFIG.trailsColorMode) {
            case FLOW: {
                float phase = (System.currentTimeMillis() % 100000L) / 1000f * 0.25f;
                float p = (progress + phase) % 1f;
                return me.Padej_.soupapi.utils.Palette.getColor(p < 0 ? p + 1f : p);
            }
            case FIXED:
                return me.Padej_.soupapi.utils.Palette.getColor(0f);
            case GRADIENT_HUD:
            default:
                return getAnimatedColor(progress, 1 - progress);
        }
    }

    /** How a trail is coloured. */
    public enum ColorMode {
        GRADIENT_HUD, FLOW, FIXED
    }

    public static Color getAnimatedColor(float x, float y) {

        Color c1 = TargetHudRenderer.topLeft;
        Color c2 = TargetHudRenderer.topRight;
        Color c3 = TargetHudRenderer.bottomRight;
        Color c4 = TargetHudRenderer.bottomLeft;

        Color top = lerpColor(c1, c2, x);
        Color bottom = lerpColor(c4, c3, x);

        return lerpColor(top, bottom, y);
    }

    public static Color lerpColor(Color a, Color b, float t) {
        int r = (int) (a.getRed() + (b.getRed() - a.getRed()) * t);
        int g = (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t);
        int b_ = (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t);
        return new Color(r, g, b_);
    }

    public enum Style {
        FADED, SOLID, FADED_INVERT, GRADIENT, PULSE, RIBBON, DOTTED, NEON, SYMBOLS
    }

    /** Who leaves a trail. */
    public enum Targets {
        SELF, OTHERS, BOTH
    }
}
