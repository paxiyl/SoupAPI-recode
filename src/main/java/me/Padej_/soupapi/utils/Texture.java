package me.Padej_.soupapi.utils;

import net.minecraft.util.ResourceLocation;

public class Texture {
    final ResourceLocation id;

    public Texture(String path) {
        id = new ResourceLocation("maillib", validatePath(path));
    }

    public Texture(ResourceLocation i) {
        id = new ResourceLocation(i.getResourceDomain(), i.getResourcePath());
    }

    String validatePath(String path) {
        if (ResourceLocation.isPathValid(path)) {
            return path;
        }
        StringBuilder ret = new StringBuilder();
        for (char c : path.toLowerCase().toCharArray()) {
            if (ResourceLocation.isPathCharacterValid(c)) {
                ret.append(c);
            }
        }
        return ret.toString();
    }

    public ResourceLocation getId() {
        return id;
    }
}
