package me.Padej_.soupapi.mixin.modifyReturnValue;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import me.Padej_.soupapi.compat.SoupLog;
import me.Padej_.soupapi.modules.Capes;
import me.Padej_.soupapi.utils.EntityUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Replaces a player's cape texture with the one chosen in the mod (or a custom URL).
 *
 * <p>Much simpler on 1.8.9. 1.21.4 returned a whole {@code SkinTextures} record from
 * {@code getSkinTextures}, so the mod had to rebuild the record with one field swapped. 1.8.9 asks
 * for the cape on its own, through {@code getCapeId()}, so only that return value is modified.
 * {@code canRenderCapeTexture()} is also forced true, since 1.8.9 gates cape rendering on it and
 * it is false for players whose account has no cape.
 *
 * <p>The custom-cape loader no longer round-trips the image through PNG bytes and a
 * {@code NativeImage}: 1.8.9's {@link NativeImageBackedTexture} takes the decoded
 * {@link BufferedImage} directly. The upload is still deferred to the client thread, because the
 * download runs on a worker and GL calls must not.
 */
@Mixin(value = AbstractClientPlayerEntity.class, priority = 20000)
public abstract class CapeTextureChanger {

    @Shadow
    protected abstract PlayerListEntry getPlayerListEntry();

    @Unique
    private static final Map<String, Boolean> soupapi$loadedCapes =
            new ConcurrentHashMap<String, Boolean>();

    /** Textures that finished downloading and still need uploading on the client thread. */
    @Unique
    private static final Map<String, BufferedImage> soupapi$pendingUploads =
            new ConcurrentHashMap<String, BufferedImage>();

    @ModifyReturnValue(method = "getCapeId", at = @At("RETURN"))
    private Identifier soupapi$useCapeIfPresent(Identifier original) {
        Identifier cape = soupapi$getDirCape();
        if (cape == null) {
            return original;
        }

        PlayerListEntry entry = getPlayerListEntry();
        String playerName = entry != null && entry.getProfile() != null
                ? entry.getProfile().getName() : null;
        if (playerName == null) {
            return original;
        }

        // Friends get the chosen cape, and so does the local player.
        if (EntityUtils.isFriend(playerName)) {
            return cape;
        }
        String currentUsername = MinecraftClient.getInstance().getSession().getUsername();
        return playerName.equals(currentUsername) ? cape : original;
    }

    /**
     * 1.8.9 only draws a cape when this returns true, and it is false for accounts without one.
     * A mod-supplied cape has to override that.
     */
    @ModifyReturnValue(method = "canRenderCapeTexture", at = @At("RETURN"))
    private boolean soupapi$allowCustomCape(boolean original) {
        return original || soupapi$getDirCape() != null;
    }

    @Unique
    private static Identifier soupapi$getDirCape() {
        MinecraftClient client = MinecraftClient.getInstance();

        String linkValue = Capes.link.getValue();
        if (Capes.useCustom.getValue() && linkValue != null && !linkValue.isEmpty()) {
            Identifier textureId = new Identifier("soupapi", "custom_cape_" + linkValue.hashCode());

            // Finish any download that completed since the last frame; GL uploads must happen on
            // the client thread, and this method is only ever called from rendering.
            BufferedImage pending = soupapi$pendingUploads.remove(linkValue);
            if (pending != null) {
                try {
                    NativeImageBackedTexture texture = new NativeImageBackedTexture(pending);
                    texture.upload();
                    client.getTextureManager().loadTexture(textureId, texture);
                    soupapi$loadedCapes.put(linkValue, Boolean.TRUE);
                } catch (Throwable t) {
                    SoupLog.error("Could not upload the custom cape texture", t);
                }
            }

            if (!Boolean.TRUE.equals(soupapi$loadedCapes.get(linkValue))) {
                if (!soupapi$loadedCapes.containsKey(linkValue)) {
                    soupapi$loadedCapes.put(linkValue, Boolean.FALSE);
                    soupapi$startCapeDownload(linkValue);
                }
                // Not ready yet; keep the original cape this frame.
                return null;
            }
            return textureId;
        }

        Identifier cape = Capes.cape.getValue().getTexturePath();
        if (Capes.enabled.getValue() && soupapi$resourceExists(cape)) {
            return cape;
        }
        return null;
    }

    @Unique
    private static void soupapi$startCapeDownload(final String linkValue) {
        Thread worker = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    BufferedImage image = ImageIO.read(new URL(linkValue));
                    if (image == null) {
                        SoupLog.warn("Custom cape URL decoded to no image: " + linkValue);
                        return;
                    }
                    soupapi$pendingUploads.put(linkValue, image);
                } catch (Throwable t) {
                    SoupLog.error("Could not download the custom cape", t);
                }
            }
        }, "SoupAPI-CapeLoader");
        worker.setDaemon(true);
        worker.start();
    }

    /**
     * 1.8.9's resource manager throws rather than returning an {@code Optional} when a resource is
     * missing, so presence is probed with a try/catch.
     */
    @Unique
    private static boolean soupapi$resourceExists(Identifier id) {
        try {
            return MinecraftClient.getInstance().getResourceManager().getResource(id) != null;
        } catch (Throwable t) {
            return false;
        }
    }
}
