package me.Padej_.soupapi.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.common.reflect.TypeToken;
import me.Padej_.soupapi.compat.SoupLog;
import net.minecraft.client.Minecraft;

import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Small side-car file for things that are not config options.
 *
 * <p>It used to hold a second copy of five modules' settings, which is why changing those options
 * in the config file did nothing: the modules read this file instead. Those modules now read the
 * config like everything else, so all that is left here is interface state — which settings page was
 * open last — kept out of the config proper so it never shows up as an option.
 */
public class ConfigManager {

    private static final Path CONFIG_PATH = Paths.get(
            Minecraft.getMinecraft().mcDataDir.getAbsolutePath(), "config", "soupapi_gui.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Map<String, String> metadata = new HashMap<String, String>();
    private static boolean loaded;

    public static void saveConfig() {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("meta", metadata);
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.write(CONFIG_PATH, GSON.toJson(data).getBytes("UTF-8"));
        } catch (Exception e) {
            SoupLog.warn("[SoupAPI] could not save the interface state", e);
        }
    }

    public static void loadConfig() {
        if (loaded) return;
        loaded = true;
        if (!Files.exists(CONFIG_PATH)) return;
        try {
            String json = new String(Files.readAllBytes(CONFIG_PATH), "UTF-8");
            if (json.trim().isEmpty()) return;
            JsonObject root = new JsonParser().parse(json).getAsJsonObject();
            JsonObject meta = root.getAsJsonObject("meta");
            if (meta != null) {
                Type type = new TypeToken<Map<String, String>>() {
                }.getType();
                Map<String, String> read = GSON.fromJson(meta, type);
                if (read != null) {
                    metadata = read;
                }
            }
        } catch (Exception e) {
            SoupLog.warn("[SoupAPI] could not read the interface state", e);
        }
    }

    public static void setMetadata(String key, String value) {
        if (value == null) {
            metadata.remove(key);
        } else {
            metadata.put(key, value);
        }
    }

    public static String getMetadata(String key) {
        return metadata.get(key);
    }
}
