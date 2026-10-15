package me.Padej_.soupapi.render;

import me.Padej_.soupapi.compat.ShaderKey;

import me.Padej_.soupapi.compat.RenderSystem;
import me.Padej_.soupapi.event.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
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
import net.minecraft.util.math.*;
import org.jetbrains.annotations.NotNull;
import me.Padej_.soupapi.compat.Mat4;
import me.Padej_.soupapi.compat.Vector3f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Render3D_Shapes {
    public static List<FillAction> FILLED_QUEUE = new ArrayList<>();
    public static List<OutlineAction> OUTLINE_QUEUE = new ArrayList<>();
    public static List<FadeAction> FADE_QUEUE = new ArrayList<>();
    public static List<FillSideAction> FILLED_SIDE_QUEUE = new ArrayList<>();
    public static List<OutlineSideAction> OUTLINE_SIDE_QUEUE = new ArrayList<>();
    public static List<DebugLineAction> DEBUG_LINE_QUEUE = new ArrayList<>();
    public static List<LineAction> LINE_QUEUE = new ArrayList<>();

    private static MinecraftClient mc = MinecraftClient.getInstance();

    public static void render(WorldRenderContext context) {
        MatrixStack stack = context.matrixStack();

        if (!FILLED_QUEUE.isEmpty() || !FADE_QUEUE.isEmpty() || !FILLED_SIDE_QUEUE.isEmpty()) {
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            RenderSystem.disableDepthTest();
            setupRender();
            RenderSystem.setShader(ShaderKey.POSITION_COLOR);

            Mat4 matrix = stack.peek().getPositionMatrix();
            FILLED_QUEUE.forEach(action -> setFilledBoxVertexes(bufferBuilder, matrix, action.box(), action.color()));

            FADE_QUEUE.forEach(action -> setFilledFadePoints(action.box(), bufferBuilder, matrix, action.color(), action.color2()));

            FILLED_SIDE_QUEUE.forEach(action -> setFilledSidePoints(bufferBuilder, matrix, action.box, action.color(), action.side()));
            Render2D.endBuilding(bufferBuilder);

            endRender();
            RenderSystem.enableDepthTest();

            FADE_QUEUE.clear();
            FILLED_SIDE_QUEUE.clear();
            FILLED_QUEUE.clear();
        }

        if (!OUTLINE_QUEUE.isEmpty() || !OUTLINE_SIDE_QUEUE.isEmpty()) {
            setupRender();
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.LINES, VertexFormats.LINES);
            RenderSystem.disableCull();
            RenderSystem.disableDepthTest();
            RenderSystem.setShader(ShaderKey.RENDERTYPE_LINES);

            RenderSystem.lineWidth(2f);

            OUTLINE_QUEUE.forEach(action -> {
                setOutlinePoints(action.box(), matrixFrom(action.box().minX, action.box().minY, action.box().minZ), buffer, action.color());
            });

            OUTLINE_SIDE_QUEUE.forEach(action -> { // !!!
                setSideOutlinePoints(action.box, matrixFrom(action.box().minX, action.box().minY, action.box().minZ), buffer, action.color(), action.side());
            });

            Render2D.endBuilding(buffer);

            RenderSystem.enableCull();
            RenderSystem.enableDepthTest();
            endRender();
            OUTLINE_QUEUE.clear();
            OUTLINE_SIDE_QUEUE.clear();
        }

        if (!DEBUG_LINE_QUEUE.isEmpty()) {
            setupRender();
            RenderSystem.disableDepthTest();
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.LINES);

            RenderSystem.disableCull();
            RenderSystem.setShader(ShaderKey.RENDERTYPE_LINES);
            DEBUG_LINE_QUEUE.forEach(action -> {
                MatrixStack matrices = matrixFrom(action.start.getX(), action.start.getY(), action.start.getZ());
                vertexLine(matrices, buffer, 0f, 0f, 0f, (float) (action.end.getX() - action.start.getX()), (float) (action.end.getY() - action.start.getY()), (float) (action.end.getZ() - action.start.getZ()), action.color);
            });
            Render2D.endBuilding(buffer);
            RenderSystem.enableCull();
            RenderSystem.enableDepthTest();
            endRender();
            DEBUG_LINE_QUEUE.clear();
        }

        if (!LINE_QUEUE.isEmpty()) {
            setupRender();
            Tessellator tessellator = Tessellator.getInstance();
            RenderSystem.disableCull();
            RenderSystem.setShader(ShaderKey.RENDERTYPE_LINES);
            RenderSystem.lineWidth(2f);
            RenderSystem.disableDepthTest();
            BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.LINES, VertexFormats.LINES);
            LINE_QUEUE.forEach(action -> { // !!!
                MatrixStack matrices = matrixFrom(action.start.getX(), action.start.getY(), action.start.getZ());
                vertexLine(matrices, buffer, 0f, 0f, 0f, (float) (action.end.getX() - action.start.getX()), (float) (action.end.getY() - action.start.getY()), (float) (action.end.getZ() - action.start.getZ()), action.color);
            });
            Render2D.endBuilding(buffer);
            RenderSystem.enableCull();
            RenderSystem.lineWidth(1f);
            RenderSystem.enableDepthTest();
            endRender();
            LINE_QUEUE.clear();
        }
    }

    public static void setSideOutlinePoints(Box box, MatrixStack matrices, BufferBuilder buffer, Color color, Direction dir) {
        box = box.offset(new Vec3d(box.minX, box.minY, box.minZ).negate());

        float x1 = (float) box.minX;
        float y1 = (float) box.minY;
        float z1 = (float) box.minZ;
        float x2 = (float) box.maxX;
        float y2 = (float) box.maxY;
        float z2 = (float) box.maxZ;

        switch (dir) {
            case UP:
                {
                    vertexLine(matrices, buffer, x1, y2, z1, x2, y2, z1, color);
                    vertexLine(matrices, buffer, x2, y2, z1, x2, y2, z2, color);
                    vertexLine(matrices, buffer, x2, y2, z2, x1, y2, z2, color);
                    vertexLine(matrices, buffer, x1, y2, z2, x1, y2, z1, color);

                }
                break;
            case DOWN:
                {
                    vertexLine(matrices, buffer, x1, y1, z1, x2, y1, z1, color);
                    vertexLine(matrices, buffer, x2, y1, z1, x2, y1, z2, color);
                    vertexLine(matrices, buffer, x2, y1, z2, x1, y1, z2, color);
                    vertexLine(matrices, buffer, x1, y1, z2, x1, y1, z1, color);

                }
                break;
            case EAST:
                {
                    vertexLine(matrices, buffer, x2, y1, z1, x2, y2, z1, color);
                    vertexLine(matrices, buffer, x2, y1, z2, x2, y2, z2, color);
                    vertexLine(matrices, buffer, x2, y2, z2, x2, y2, z1, color);
                    vertexLine(matrices, buffer, x2, y1, z2, x2, y1, z1, color);

                }
                break;
            case WEST:
                {
                    vertexLine(matrices, buffer, x1, y1, z1, x1, y2, z1, color);
                    vertexLine(matrices, buffer, x1, y1, z2, x1, y2, z2, color);
                    vertexLine(matrices, buffer, x1, y2, z2, x1, y2, z1, color);
                    vertexLine(matrices, buffer, x1, y1, z2, x1, y1, z1, color);

                }
                break;
            case NORTH:
                {
                    vertexLine(matrices, buffer, x2, y1, z1, x2, y2, z1, color);
                    vertexLine(matrices, buffer, x1, y1, z1, x1, y2, z1, color);
                    vertexLine(matrices, buffer, x2, y1, z1, x1, y1, z1, color);
                    vertexLine(matrices, buffer, x2, y2, z1, x1, y2, z1, color);

                }
                break;
            case SOUTH:
                {
                    vertexLine(matrices, buffer, x1, y1, z2, x1, y2, z2, color);
                    vertexLine(matrices, buffer, x2, y1, z2, x2, y2, z2, color);
                    vertexLine(matrices, buffer, x1, y1, z2, x2, y1, z2, color);
                    vertexLine(matrices, buffer, x1, y2, z2, x2, y2, z2, color);

                }
                break;
        }
    }

    public static @NotNull MatrixStack matrixFrom(double x, double y, double z) {
        MatrixStack matrices = new MatrixStack();

        Camera camera = mc.gameRenderer.getCamera();

        matrices.translate(x - camera.getPos().x, y - camera.getPos().y, z - camera.getPos().z);

        return matrices;
    }

    public static void setOutlinePoints(Box box, MatrixStack matrices, BufferBuilder buffer, Color color) {
        box = box.offset(new Vec3d(box.minX, box.minY, box.minZ).negate());

        float x1 = (float) box.minX;
        float y1 = (float) box.minY;
        float z1 = (float) box.minZ;
        float x2 = (float) box.maxX;
        float y2 = (float) box.maxY;
        float z2 = (float) box.maxZ;

        vertexLine(matrices, buffer, x1, y1, z1, x2, y1, z1, color);
        vertexLine(matrices, buffer, x2, y1, z1, x2, y1, z2, color);
        vertexLine(matrices, buffer, x2, y1, z2, x1, y1, z2, color);
        vertexLine(matrices, buffer, x1, y1, z2, x1, y1, z1, color);
        vertexLine(matrices, buffer, x1, y1, z2, x1, y2, z2, color);
        vertexLine(matrices, buffer, x1, y1, z1, x1, y2, z1, color);
        vertexLine(matrices, buffer, x2, y1, z2, x2, y2, z2, color);
        vertexLine(matrices, buffer, x2, y1, z1, x2, y2, z1, color);
        vertexLine(matrices, buffer, x1, y2, z1, x2, y2, z1, color);
        vertexLine(matrices, buffer, x2, y2, z1, x2, y2, z2, color);
        vertexLine(matrices, buffer, x2, y2, z2, x1, y2, z2, color);
        vertexLine(matrices, buffer, x1, y2, z2, x1, y2, z1, color);
    }

    public static void vertexLine(@NotNull MatrixStack matrices, @NotNull VertexConsumer buffer, float x1, float y1, float z1, float x2, float y2, float z2, @NotNull Color lineColor) {
        Mat4 model = matrices.peek().getPositionMatrix();
        MatrixStack.Entry entry = matrices.peek();
        Vector3f normalVec = getNormal(x1, y1, z1, x2, y2, z2);
        buffer.vertex(model, x1, y1, z1).color(lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue(), lineColor.getAlpha()).normal(entry, normalVec.x(), normalVec.y(), normalVec.z());
        buffer.vertex(model, x2, y2, z2).color(lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue(), lineColor.getAlpha()).normal(entry, normalVec.x(), normalVec.y(), normalVec.z());
    }

    public static @NotNull Vector3f getNormal(float x1, float y1, float z1, float x2, float y2, float z2) {
        float xNormal = x2 - x1;
        float yNormal = y2 - y1;
        float zNormal = z2 - z1;
        float normalSqrt = MathHelper.sqrt(xNormal * xNormal + yNormal * yNormal + zNormal * zNormal);

        return new Vector3f(xNormal / normalSqrt, yNormal / normalSqrt, zNormal / normalSqrt);
    }

    public static void setFilledSidePoints(BufferBuilder buffer, Mat4 matrix, Box box, Color c, Direction dir) {
        float minX = (float) (box.minX - mc.getEntityRenderManager().cameraX);
        float minY = (float) (box.minY - mc.getEntityRenderManager().cameraY);
        float minZ = (float) (box.minZ - mc.getEntityRenderManager().cameraZ);
        float maxX = (float) (box.maxX - mc.getEntityRenderManager().cameraX);
        float maxY = (float) (box.maxY - mc.getEntityRenderManager().cameraY);
        float maxZ = (float) (box.maxZ - mc.getEntityRenderManager().cameraZ);

        if (dir == Direction.DOWN) {
            buffer.vertex(matrix, minX, minY, minZ).color(c.getRGB());
            buffer.vertex(matrix, maxX, minY, minZ).color(c.getRGB());
            buffer.vertex(matrix, maxX, minY, maxZ).color(c.getRGB());
            buffer.vertex(matrix, minX, minY, maxZ).color(c.getRGB());
        }

        if (dir == Direction.NORTH) {
            buffer.vertex(matrix, minX, minY, minZ).color(c.getRGB());
            buffer.vertex(matrix, minX, maxY, minZ).color(c.getRGB());
            buffer.vertex(matrix, maxX, maxY, minZ).color(c.getRGB());
            buffer.vertex(matrix, maxX, minY, minZ).color(c.getRGB());
        }

        if (dir == Direction.EAST) {
            buffer.vertex(matrix, maxX, minY, minZ).color(c.getRGB());
            buffer.vertex(matrix, maxX, maxY, minZ).color(c.getRGB());
            buffer.vertex(matrix, maxX, maxY, maxZ).color(c.getRGB());
            buffer.vertex(matrix, maxX, minY, maxZ).color(c.getRGB());
        }
        if (dir == Direction.SOUTH) {
            buffer.vertex(matrix, minX, minY, maxZ).color(c.getRGB());
            buffer.vertex(matrix, maxX, minY, maxZ).color(c.getRGB());
            buffer.vertex(matrix, maxX, maxY, maxZ).color(c.getRGB());
            buffer.vertex(matrix, minX, maxY, maxZ).color(c.getRGB());
        }

        if (dir == Direction.WEST) {
            buffer.vertex(matrix, minX, minY, minZ).color(c.getRGB());
            buffer.vertex(matrix, minX, minY, maxZ).color(c.getRGB());
            buffer.vertex(matrix, minX, maxY, maxZ).color(c.getRGB());
            buffer.vertex(matrix, minX, maxY, minZ).color(c.getRGB());
        }

        if (dir == Direction.UP) {
            buffer.vertex(matrix, minX, maxY, minZ).color(c.getRGB());
            buffer.vertex(matrix, minX, maxY, maxZ).color(c.getRGB());
            buffer.vertex(matrix, maxX, maxY, maxZ).color(c.getRGB());
            buffer.vertex(matrix, maxX, maxY, minZ).color(c.getRGB());
        }
    }

    public static void setFilledFadePoints(Box box, BufferBuilder buffer, Mat4 posMatrix, Color c, Color c1) {
        float minX = (float) (box.minX - mc.getEntityRenderManager().cameraX);
        float minY = (float) (box.minY - mc.getEntityRenderManager().cameraY);
        float minZ = (float) (box.minZ - mc.getEntityRenderManager().cameraZ);
        float maxX = (float) (box.maxX - mc.getEntityRenderManager().cameraX);
        float maxY = (float) (box.maxY - mc.getEntityRenderManager().cameraY);
        float maxZ = (float) (box.maxZ - mc.getEntityRenderManager().cameraZ);

        buffer.vertex(posMatrix, minX, minY, minZ).color(c.getRGB());
        buffer.vertex(posMatrix, minX, maxY, minZ).color(c1.getRGB());
        buffer.vertex(posMatrix, maxX, maxY, minZ).color(c1.getRGB());
        buffer.vertex(posMatrix, maxX, minY, minZ).color(c.getRGB());

        buffer.vertex(posMatrix, maxX, minY, minZ).color(c.getRGB());
        buffer.vertex(posMatrix, maxX, maxY, minZ).color(c1.getRGB());
        buffer.vertex(posMatrix, maxX, maxY, maxZ).color(c1.getRGB());
        buffer.vertex(posMatrix, maxX, minY, maxZ).color(c.getRGB());

        buffer.vertex(posMatrix, minX, minY, maxZ).color(c.getRGB());
        buffer.vertex(posMatrix, maxX, minY, maxZ).color(c.getRGB());
        buffer.vertex(posMatrix, maxX, maxY, maxZ).color(c1.getRGB());
        buffer.vertex(posMatrix, minX, maxY, maxZ).color(c1.getRGB());

        buffer.vertex(posMatrix, minX, minY, minZ).color(c.getRGB());
        buffer.vertex(posMatrix, minX, minY, maxZ).color(c.getRGB());
        buffer.vertex(posMatrix, minX, maxY, maxZ).color(c1.getRGB());
        buffer.vertex(posMatrix, minX, maxY, minZ).color(c1.getRGB());

        buffer.vertex(posMatrix, minX, maxY, minZ).color(c1.getRGB());
        buffer.vertex(posMatrix, minX, maxY, maxZ).color(c1.getRGB());
        buffer.vertex(posMatrix, maxX, maxY, maxZ).color(c1.getRGB());
        buffer.vertex(posMatrix, maxX, maxY, minZ).color(c1.getRGB());
    }

    public static void setFilledBoxVertexes(@NotNull BufferBuilder bufferBuilder, Mat4 m, @NotNull Box box, @NotNull Color c) {
        float minX = (float) (box.minX - mc.getEntityRenderManager().cameraX);
        float minY = (float) (box.minY - mc.getEntityRenderManager().cameraY);
        float minZ = (float) (box.minZ - mc.getEntityRenderManager().cameraZ);
        float maxX = (float) (box.maxX - mc.getEntityRenderManager().cameraX);
        float maxY = (float) (box.maxY - mc.getEntityRenderManager().cameraY);
        float maxZ = (float) (box.maxZ - mc.getEntityRenderManager().cameraZ);

        bufferBuilder.vertex(m, minX, minY, minZ).color(c.getRGB());
        bufferBuilder.vertex(m, maxX, minY, minZ).color(c.getRGB());
        bufferBuilder.vertex(m, maxX, minY, maxZ).color(c.getRGB());
        bufferBuilder.vertex(m, minX, minY, maxZ).color(c.getRGB());

        bufferBuilder.vertex(m, minX, minY, minZ).color(c.getRGB());
        bufferBuilder.vertex(m, minX, maxY, minZ).color(c.getRGB());
        bufferBuilder.vertex(m, maxX, maxY, minZ).color(c.getRGB());
        bufferBuilder.vertex(m, maxX, minY, minZ).color(c.getRGB());

        bufferBuilder.vertex(m, maxX, minY, minZ).color(c.getRGB());
        bufferBuilder.vertex(m, maxX, maxY, minZ).color(c.getRGB());
        bufferBuilder.vertex(m, maxX, maxY, maxZ).color(c.getRGB());
        bufferBuilder.vertex(m, maxX, minY, maxZ).color(c.getRGB());

        bufferBuilder.vertex(m, minX, minY, maxZ).color(c.getRGB());
        bufferBuilder.vertex(m, maxX, minY, maxZ).color(c.getRGB());
        bufferBuilder.vertex(m, maxX, maxY, maxZ).color(c.getRGB());
        bufferBuilder.vertex(m, minX, maxY, maxZ).color(c.getRGB());

        bufferBuilder.vertex(m, minX, minY, minZ).color(c.getRGB());
        bufferBuilder.vertex(m, minX, minY, maxZ).color(c.getRGB());
        bufferBuilder.vertex(m, minX, maxY, maxZ).color(c.getRGB());
        bufferBuilder.vertex(m, minX, maxY, minZ).color(c.getRGB());

        bufferBuilder.vertex(m, minX, maxY, minZ).color(c.getRGB());
        bufferBuilder.vertex(m, minX, maxY, maxZ).color(c.getRGB());
        bufferBuilder.vertex(m, maxX, maxY, maxZ).color(c.getRGB());
        bufferBuilder.vertex(m, maxX, maxY, minZ).color(c.getRGB());
    }

    public static void setupRender() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
    }

    public static void endRender() {
        RenderSystem.disableBlend();
    }

    public static void drawFilledBox(MatrixStack stack, Box box, Color c) {
        FILLED_QUEUE.add(new FillAction(box, c));
    }

    public static void drawBoxOutline(@NotNull Box box, Color color, float lineWidth) {
        OUTLINE_QUEUE.add(new OutlineAction(box, color, lineWidth));
    }

    public static void drawFilledFadeBox(@NotNull MatrixStack stack, @NotNull Box box, @NotNull Color c, @NotNull Color c1) {
        FADE_QUEUE.add(new FadeAction(box, c, c1));
    }

    public static void drawFilledSide(MatrixStack stack, @NotNull Box box, Color c, Direction dir) {
        FILLED_SIDE_QUEUE.add(new FillSideAction(box, c, dir));
    }

    public static void drawSideOutline(@NotNull Box box, Color color, float lineWidth, Direction dir) {
        OUTLINE_SIDE_QUEUE.add(new OutlineSideAction(box, color, lineWidth, dir));
    }

    public static void drawLineDebug(Vec3d start, Vec3d end, Color color) {
        DEBUG_LINE_QUEUE.add(new DebugLineAction(start, end, color));
    }

    public static void drawLine(@NotNull Vec3d start, @NotNull Vec3d end, @NotNull Color color) {
        LINE_QUEUE.add(new LineAction(start, end, color));
    }
public static final class FillAction {
    private final Box box;
    private final Color color;

    FillAction(Box box, Color color) {
        this.box = box;
        this.color = color;
    }

    public Box box() {
        return box;
    }

    public Color color() {
        return color;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FillAction)) return false;
        FillAction other = (FillAction) o;
        if (box == null ? other.box != null : !box.equals(other.box)) return false;
        if (color == null ? other.color != null : !color.equals(other.color)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + (box == null ? 0 : box.hashCode());
        result = 31 * result + (color == null ? 0 : color.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "FillAction[" + "box=" + box + ", " + "color=" + color + "]";
    }
    }
public static final class OutlineAction {
    private final Box box;
    private final Color color;
    private final float lineWidth;

    OutlineAction(Box box, Color color, float lineWidth) {
        this.box = box;
        this.color = color;
        this.lineWidth = lineWidth;
    }

    public Box box() {
        return box;
    }

    public Color color() {
        return color;
    }

    public float lineWidth() {
        return lineWidth;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OutlineAction)) return false;
        OutlineAction other = (OutlineAction) o;
        if (box == null ? other.box != null : !box.equals(other.box)) return false;
        if (color == null ? other.color != null : !color.equals(other.color)) return false;
        if (Float.compare(lineWidth, other.lineWidth) != 0) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + (box == null ? 0 : box.hashCode());
        result = 31 * result + (color == null ? 0 : color.hashCode());
        result = 31 * result + Float.floatToIntBits(lineWidth);
        return result;
    }

    @Override
    public String toString() {
        return "OutlineAction[" + "box=" + box + ", " + "color=" + color + ", " + "lineWidth=" + lineWidth + "]";
    }
    }
public static final class FadeAction {
    private final Box box;
    private final Color color;
    private final Color color2;

    FadeAction(Box box, Color color, Color color2) {
        this.box = box;
        this.color = color;
        this.color2 = color2;
    }

    public Box box() {
        return box;
    }

    public Color color() {
        return color;
    }

    public Color color2() {
        return color2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FadeAction)) return false;
        FadeAction other = (FadeAction) o;
        if (box == null ? other.box != null : !box.equals(other.box)) return false;
        if (color == null ? other.color != null : !color.equals(other.color)) return false;
        if (color2 == null ? other.color2 != null : !color2.equals(other.color2)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + (box == null ? 0 : box.hashCode());
        result = 31 * result + (color == null ? 0 : color.hashCode());
        result = 31 * result + (color2 == null ? 0 : color2.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "FadeAction[" + "box=" + box + ", " + "color=" + color + ", " + "color2=" + color2 + "]";
    }
    }
public static final class FillSideAction {
    private final Box box;
    private final Color color;
    private final Direction side;

    FillSideAction(Box box, Color color, Direction side) {
        this.box = box;
        this.color = color;
        this.side = side;
    }

    public Box box() {
        return box;
    }

    public Color color() {
        return color;
    }

    public Direction side() {
        return side;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FillSideAction)) return false;
        FillSideAction other = (FillSideAction) o;
        if (box == null ? other.box != null : !box.equals(other.box)) return false;
        if (color == null ? other.color != null : !color.equals(other.color)) return false;
        if (side == null ? other.side != null : !side.equals(other.side)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + (box == null ? 0 : box.hashCode());
        result = 31 * result + (color == null ? 0 : color.hashCode());
        result = 31 * result + (side == null ? 0 : side.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "FillSideAction[" + "box=" + box + ", " + "color=" + color + ", " + "side=" + side + "]";
    }
    }
public static final class OutlineSideAction {
    private final Box box;
    private final Color color;
    private final float lineWidth;
    private final Direction side;

    OutlineSideAction(Box box, Color color, float lineWidth, Direction side) {
        this.box = box;
        this.color = color;
        this.lineWidth = lineWidth;
        this.side = side;
    }

    public Box box() {
        return box;
    }

    public Color color() {
        return color;
    }

    public float lineWidth() {
        return lineWidth;
    }

    public Direction side() {
        return side;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OutlineSideAction)) return false;
        OutlineSideAction other = (OutlineSideAction) o;
        if (box == null ? other.box != null : !box.equals(other.box)) return false;
        if (color == null ? other.color != null : !color.equals(other.color)) return false;
        if (Float.compare(lineWidth, other.lineWidth) != 0) return false;
        if (side == null ? other.side != null : !side.equals(other.side)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + (box == null ? 0 : box.hashCode());
        result = 31 * result + (color == null ? 0 : color.hashCode());
        result = 31 * result + Float.floatToIntBits(lineWidth);
        result = 31 * result + (side == null ? 0 : side.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "OutlineSideAction[" + "box=" + box + ", " + "color=" + color + ", " + "lineWidth=" + lineWidth + ", " + "side=" + side + "]";
    }
    }
public static final class DebugLineAction {
    private final Vec3d start;
    private final Vec3d end;
    private final Color color;

    DebugLineAction(Vec3d start, Vec3d end, Color color) {
        this.start = start;
        this.end = end;
        this.color = color;
    }

    public Vec3d start() {
        return start;
    }

    public Vec3d end() {
        return end;
    }

    public Color color() {
        return color;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DebugLineAction)) return false;
        DebugLineAction other = (DebugLineAction) o;
        if (start == null ? other.start != null : !start.equals(other.start)) return false;
        if (end == null ? other.end != null : !end.equals(other.end)) return false;
        if (color == null ? other.color != null : !color.equals(other.color)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + (start == null ? 0 : start.hashCode());
        result = 31 * result + (end == null ? 0 : end.hashCode());
        result = 31 * result + (color == null ? 0 : color.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "DebugLineAction[" + "start=" + start + ", " + "end=" + end + ", " + "color=" + color + "]";
    }
    }
public static final class LineAction {
    private final Vec3d start;
    private final Vec3d end;
    private final Color color;

    LineAction(Vec3d start, Vec3d end, Color color) {
        this.start = start;
        this.end = end;
        this.color = color;
    }

    public Vec3d start() {
        return start;
    }

    public Vec3d end() {
        return end;
    }

    public Color color() {
        return color;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LineAction)) return false;
        LineAction other = (LineAction) o;
        if (start == null ? other.start != null : !start.equals(other.start)) return false;
        if (end == null ? other.end != null : !end.equals(other.end)) return false;
        if (color == null ? other.color != null : !color.equals(other.color)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + (start == null ? 0 : start.hashCode());
        result = 31 * result + (end == null ? 0 : end.hashCode());
        result = 31 * result + (color == null ? 0 : color.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "LineAction[" + "start=" + start + ", " + "end=" + end + ", " + "color=" + color + "]";
    }
    }
}
