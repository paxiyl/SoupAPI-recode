package me.Padej_.soupapi.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import me.Padej_.soupapi.compat.SoupLog;
import me.Padej_.soupapi.config.annotation.Config;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;

/**
 * JSON-backed config serialiser, replacing Cloth Config's {@code GsonConfigSerializer}.
 *
 * <p>Writes {@code config/<name>.json} under the game directory, the same path and format Cloth
 * used, so an existing config file carries over.
 *
 * <p>1.8.9 ships Gson 2.2.4, which predates {@code JsonObject.keySet()} and the newer builder
 * options, so this sticks to the long-standing reflective {@code toJson}/{@code fromJson} API that
 * version supports.
 */
public final class GsonConfigSerializer<T extends ConfigData> implements ConfigSerializer<T> {

    // Charset.forName rather than StandardCharsets so the intent is explicit about matching
    // whatever Cloth wrote previously.
    private static final Charset UTF8 = Charset.forName("UTF-8");

    private final Class<T> type;
    private final Gson gson;
    private final File file;

    public GsonConfigSerializer(Class<T> type) {
        this.type = type;
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .excludeFieldsWithModifiers(java.lang.reflect.Modifier.TRANSIENT,
                        java.lang.reflect.Modifier.STATIC)
                .serializeNulls()
                .create();

        Config annotation = type.getAnnotation(Config.class);
        String name = annotation != null ? annotation.name() : type.getSimpleName().toLowerCase();
        // Forge has no config-dir API; the game directory's config/ folder is the convention.
        this.file = new File(new File(Minecraft.getMinecraft().mcDataDir, "config"), name + ".json");
    }

    public File getFile() {
        return file;
    }

    @Override
    public void serialize(T config) throws Exception {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new java.io.IOException("Could not create config directory " + parent);
        }
        Writer writer = new OutputStreamWriter(new FileOutputStream(file), UTF8);
        try {
            gson.toJson(config, type, writer);
        } finally {
            writer.close();
        }
    }

    @Override
    public T deserialize() throws Exception {
        if (!file.exists()) {
            return createDefault();
        }
        Reader reader = new InputStreamReader(new FileInputStream(file), UTF8);
        try {
            T loaded = gson.fromJson(reader, type);
            if (loaded == null) {
                // Empty or all-whitespace file.
                return createDefault();
            }
            return loaded;
        } catch (Exception e) {
            // A corrupt or incompatible file must not stop the game from starting; fall back to
            // defaults and keep the broken file around so it can be inspected.
            SoupLog.warn("Could not read " + file + ", falling back to defaults", e);
            return createDefault();
        } finally {
            reader.close();
        }
    }

    @Override
    public T createDefault() {
        try {
            return type.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Config class " + type.getName()
                    + " needs a public no-argument constructor", e);
        }
    }
}
