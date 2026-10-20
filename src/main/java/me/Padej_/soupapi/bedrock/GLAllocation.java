package me.Padej_.soupapi.bedrock;

import org.lwjgl.opengl.GL11;

/**
 * Display list handles.
 *
 * <p>Thin on purpose: 1.8.9's own {@code GLAllocation} tracks lists for the whole game and frees
 * them on resource reload, which would pull a weapon's compiled bones out from under it. These are
 * allocated and freed with the weapon that owns them.
 */
final class GLAllocation {

    private GLAllocation() {
    }

    static int generate() {
        try {
            return GL11.glGenLists(1);
        } catch (Throwable t) {
            return 0;
        }
    }

    static void free(int list) {
        try {
            GL11.glDeleteLists(list, 1);
        } catch (Throwable ignored) {
            // Losing a list on shutdown or a context loss is not worth a crash.
        }
    }
}
