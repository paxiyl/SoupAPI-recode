package me.Padej_.soupapi.utils;

import net.minecraft.entity.Entity;

public class CaptureArmoredEntity {

    public static final CaptureArmoredEntity INSTANCE = new CaptureArmoredEntity();

    private static final ThreadLocal<Entity> entity = new ThreadLocal<>();

    public void setEntity(Entity e) {
        entity.set(e);
    }

    public void clear() {
        entity.remove();
    }

    public static Entity get() {
        return entity.get();
    }
}



