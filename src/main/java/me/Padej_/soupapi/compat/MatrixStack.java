package me.Padej_.soupapi.compat;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Replacement for {@code net.minecraft.client.util.math.MatrixStack}.
 *
 * <p>1.8.9 has no matrix stack of its own: transforms go straight to the fixed-function GL stack via
 * {@code GlStateManager}. Rather than rewrite every call site to push/pop GL state, this keeps a
 * CPU-side stack with the 1.21.4 API, and vertices get transformed as they are submitted (see
 * {@link Buf#vertex(Mat4, float, float, float)}).
 *
 * <p>Two reasons that is the better trade here:
 * <ul>
 *   <li>It preserves the ported render math exactly — the 1.21.4 code already assumed
 *       "transform the vertex by {@code peek().getPositionMatrix()}".</li>
 *   <li>It keeps GL matrix traffic down, which matters on gl4es/PojavLauncher where each
 *       fixed-function matrix op is translated rather than executed natively.</li>
 * </ul>
 *
 * <p>Where GL state genuinely must change (vanilla rendering called into from a mixin, item
 * rendering, the hand renderer), {@link #applyToGl()} pushes the current entry onto the real GL
 * stack; {@link #undoGl()} pops it.
 */
public final class MatrixStack {

    /** Mirrors {@code MatrixStack.Entry}; only the position matrix is ever read by the mod. */
    public static final class Entry {
        private final Mat4 position;

        private Entry(Mat4 position) {
            this.position = position;
        }

        public Mat4 getPositionMatrix() {
            return position;
        }

        Entry copy() {
            return new Entry(position.copy());
        }
    }

    private final Deque<Entry> stack = new ArrayDeque<Entry>();

    public MatrixStack() {
        stack.addLast(new Entry(new Mat4()));
    }

    public Entry peek() {
        return stack.peekLast();
    }

    public void push() {
        stack.addLast(stack.peekLast().copy());
    }

    public void pop() {
        // Guard rather than throw: a mixin that bails out early mid-render must not take the whole
        // frame down with a NoSuchElementException.
        if (stack.size() > 1) {
            stack.removeLast();
        }
    }

    public boolean isEmpty() {
        return stack.size() == 1;
    }

    public void loadIdentity() {
        stack.peekLast().getPositionMatrix().identity();
    }

    public void translate(float x, float y, float z) {
        stack.peekLast().getPositionMatrix().translate(x, y, z);
    }

    public void translate(double x, double y, double z) {
        translate((float) x, (float) y, (float) z);
    }

    public void scale(float x, float y, float z) {
        stack.peekLast().getPositionMatrix().scale(x, y, z);
    }

    public void scale(double x, double y, double z) {
        scale((float) x, (float) y, (float) z);
    }

    public void multiply(Quat rotation) {
        rotation.apply(stack.peekLast().getPositionMatrix());
    }

    public void multiply(Mat4 matrix) {
        stack.peekLast().getPositionMatrix().mul(matrix);
    }

    /** Replaces the top entry's matrix wholesale. */
    public void set(Mat4 matrix) {
        stack.peekLast().getPositionMatrix().set(matrix);
    }

    /**
     * Pushes the current entry onto the real GL matrix stack, so that vanilla code which renders
     * through fixed-function transforms (item rendering, entity models) lands where this stack says
     * it should. Always pair with {@link #undoGl()}.
     */
    public void applyToGl() {
        GlCompat.pushMatrix();
        GlCompat.multMatrix(stack.peekLast().getPositionMatrix());
    }

    public void undoGl() {
        GlCompat.popMatrix();
    }
}
