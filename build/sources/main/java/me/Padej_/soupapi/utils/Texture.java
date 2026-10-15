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
        // 1.8.9 ResourceLocation exposes no validators, so the path is always normalised.
        StringBuilder ret = new StringBuilder();
        for (char c : path.toLowerCase().toCharArray()) {
            if (c == '_' || c == '-' || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
                    || c == '.' || c == '/') {
                ret.append(c);
            }
        }
        return ret.toString();
    }

    public ResourceLocation getId() {
        return id;
    }
}
