package me.Padej_.soupapi.melee;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.Padej_.soupapi.compat.SoupLog;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * A weapon pack on disk: a zip, a jar or a folder holding Bedrock models, animations and textures.
 *
 * <p>Packs are read from disk at runtime rather than bundled into the mod. That is partly a licence
 * question — a pack is someone else's artwork, and this mod's repository is public — and partly a
 * better deal: any pack built to the same layout loads, not just the one this was written against,
 * and the jar stays small.
 *
 * <p>Discovery looks in {@code soupapi/melee/}, {@code mods/}, {@code resourcepacks/} and
 * {@code tacz/} under the game directory, so a pack that is already installed as a mod or a
 * resource pack is found where it already is, with nothing to move.
 */
public final class MeleePack {

    private static final String[] SEARCH = {
            "soupapi/melee", "mods", "resourcepacks", "tacz", "tacz_packs", "gunpacks",
    };

    private static final List<MeleePack> PACKS = new ArrayList<MeleePack>();
    private static final List<MeleeWeapon> WEAPONS = new ArrayList<MeleeWeapon>();
    private static boolean scanned;
    private static String lastError;

    public final File source;
    public final String name;
    public final String namespace;
    private final boolean archive;
    private ZipFile zip;
    private final Map<String, String> lang = new HashMap<String, String>();
    private final Map<String, ResourceLocation> textures = new HashMap<String, ResourceLocation>();
    private final List<MeleeWeapon> weapons = new ArrayList<MeleeWeapon>();

    private MeleePack(File source, String namespace, boolean archive) {
        this.source = source;
        this.name = source.getName();
        this.namespace = namespace;
        this.archive = archive;
    }

    // ---- discovery -------------------------------------------------------------------------

    /** Every weapon found, across every pack. Scans once; call {@link #rescan()} to look again. */
    public static List<MeleeWeapon> weapons() {
        if (!scanned) scan();
        return Collections.unmodifiableList(WEAPONS);
    }

    public static List<MeleePack> packs() {
        if (!scanned) scan();
        return Collections.unmodifiableList(PACKS);
    }

    /** Why nothing was found, for the settings screen to show instead of an empty list. */
    public static String lastError() {
        return lastError;
    }

    public static void rescan() {
        for (MeleeWeapon weapon : WEAPONS) {
            weapon.unload();
        }
        for (MeleePack pack : PACKS) {
            pack.close();
        }
        PACKS.clear();
        WEAPONS.clear();
        scanned = false;
        scan();
    }

    private static synchronized void scan() {
        if (scanned) return;
        scanned = true;
        lastError = null;
        File root = gameDirectory();
        if (root == null) {
            lastError = "no game directory";
            return;
        }

        List<File> candidates = new ArrayList<File>();
        String configured = me.Padej_.soupapi.config.ConfigurableModule.CONFIG.meleePackPath;
        if (configured != null && !configured.trim().isEmpty()) {
            File explicit = new File(configured.trim());
            if (!explicit.isAbsolute()) explicit = new File(root, configured.trim());
            candidates.add(explicit);
        }
        for (String folder : SEARCH) {
            File directory = new File(root, folder);
            File[] children = directory.listFiles();
            if (children == null) continue;
            Collections.addAll(candidates, children);
        }

        for (File candidate : candidates) {
            try {
                MeleePack pack = open(candidate);
                if (pack == null) continue;
                pack.index();
                if (pack.weapons.isEmpty()) {
                    pack.close();
                    continue;
                }
                PACKS.add(pack);
                WEAPONS.addAll(pack.weapons);
            } catch (Throwable t) {
                SoupLog.warn("[SoupAPI] could not read the weapon pack " + candidate.getName(), t);
            }
        }
        if (WEAPONS.isEmpty() && lastError == null) {
            lastError = "no pack found in " + String.join(", ", SEARCH);
        }
        SoupLog.info("[SoupAPI] melee packs: " + PACKS.size() + ", weapons: " + WEAPONS.size());
    }

    private static File gameDirectory() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc != null && mc.mcDataDir != null) return mc.mcDataDir;
        } catch (Throwable ignored) {
            // Headless, or called before the client exists.
        }
        return new File(".");
    }

    /** Opens a candidate file as a pack, or returns {@code null} when it is not one. */
    private static MeleePack open(File file) throws IOException {
        if (file == null || !file.exists()) return null;
        if (file.isDirectory()) {
            File meta = new File(file, "gunpack.meta.json");
            if (!meta.isFile()) return null;
            String namespace = namespaceOf(readJson(new FileInputStream(meta)));
            if (namespace == null) return null;
            return new MeleePack(file, namespace, false);
        }
        String lower = file.getName().toLowerCase(java.util.Locale.ROOT);
        if (!lower.endsWith(".zip") && !lower.endsWith(".jar")) return null;
        ZipFile archive = new ZipFile(file);
        ZipEntry meta = archive.getEntry("gunpack.meta.json");
        if (meta == null) {
            archive.close();
            return null;
        }
        String namespace = namespaceOf(readJson(archive.getInputStream(meta)));
        if (namespace == null) {
            archive.close();
            return null;
        }
        MeleePack pack = new MeleePack(file, namespace, true);
        pack.zip = archive;
        return pack;
    }

    private static String namespaceOf(JsonObject meta) {
        if (meta == null || !meta.has("namespace")) return null;
        return meta.get("namespace").getAsString();
    }

    /** Finds the weapons this pack declares and reads their names. */
    private void index() {
        readLang();
        String prefix = "data/" + namespace + "/index/melee/";
        for (String path : list(prefix, ".json")) {
            String id = path.substring(prefix.length(), path.length() - 5);
            if (id.isEmpty() || id.contains("/")) continue;
            JsonObject display = readJson("assets/" + namespace + "/display/melee/" + id + ".json");
            if (display == null) continue;
            JsonObject index = readJson(path);
            String key = index != null && index.has("name") ? index.get("name").getAsString() : null;
            String label = key != null && lang.containsKey(key) ? lang.get(key) : prettify(id);
            weapons.add(new MeleeWeapon(this, id, label, display));
        }
        Collections.sort(weapons, new java.util.Comparator<MeleeWeapon>() {
            @Override
            public int compare(MeleeWeapon a, MeleeWeapon b) {
                return a.id.compareTo(b.id);
            }
        });
    }

    private void readLang() {
        JsonObject json = readJson("assets/" + namespace + "/lang/en_us.json");
        if (json == null) return;
        for (Map.Entry<String, com.google.gson.JsonElement> entry : json.entrySet()) {
            if (entry.getValue().isJsonPrimitive()) {
                lang.put(entry.getKey(), entry.getValue().getAsString());
            }
        }
    }

    private static String prettify(String id) {
        String spaced = id.replace('_', ' ');
        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }

    // ---- reading ---------------------------------------------------------------------------

    /** Paths in this pack that start with {@code prefix} and end with {@code suffix}. */
    private List<String> list(String prefix, String suffix) {
        List<String> out = new ArrayList<String>();
        if (archive) {
            java.util.Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (name.startsWith(prefix) && name.endsWith(suffix)) out.add(name);
            }
        } else {
            File directory = new File(source, prefix);
            File[] children = directory.listFiles();
            if (children != null) {
                for (File child : children) {
                    if (child.isFile() && child.getName().endsWith(suffix)) {
                        out.add(prefix + child.getName());
                    }
                }
            }
        }
        Collections.sort(out);
        return out;
    }

    InputStream open(String path) throws IOException {
        if (path == null) return null;
        if (archive) {
            ZipEntry entry = zip.getEntry(path);
            return entry == null ? null : zip.getInputStream(entry);
        }
        File file = new File(source, path);
        return file.isFile() ? new FileInputStream(file) : null;
    }

    JsonObject readJson(String path) {
        try {
            InputStream stream = open(path);
            return stream == null ? null : readJson(stream);
        } catch (Throwable t) {
            SoupLog.warn("[SoupAPI] could not read " + path + " from " + name, t);
            return null;
        }
    }

    private static JsonObject readJson(InputStream stream) {
        Reader reader = null;
        try {
            reader = new InputStreamReader(stream, "UTF-8");
            com.google.gson.JsonElement parsed = new JsonParser().parse(reader);
            return parsed != null && parsed.isJsonObject() ? parsed.getAsJsonObject() : null;
        } catch (Throwable t) {
            return null;
        } finally {
            close(reader);
            close(stream);
        }
    }

    private static void close(java.io.Closeable closeable) {
        if (closeable == null) return;
        try {
            closeable.close();
        } catch (IOException ignored) {
            // Nothing useful to do with a failed close on a read-only stream.
        }
    }

    // ---- textures --------------------------------------------------------------------------

    /**
     * Registers a texture from the pack with the game, decoding it on first use.
     *
     * <p>Returns {@code null} while nothing has been decoded, which the renderer reads as "skip the
     * pass that needs it" rather than drawing an untextured white model.
     */
    ResourceLocation texture(String path) {
        if (path == null) return null;
        ResourceLocation existing = textures.get(path);
        if (existing != null) return existing;
        try {
            InputStream stream = open(path);
            if (stream == null) {
                textures.put(path, null);
                return null;
            }
            BufferedImage image;
            try {
                image = ImageIO.read(stream);
            } finally {
                close(stream);
            }
            if (image == null) {
                textures.put(path, null);
                return null;
            }
            ResourceLocation location = new ResourceLocation("soupapi",
                    "melee/" + namespace + "/" + path.hashCode() + "/" + sanitise(path));
            Minecraft.getMinecraft().getTextureManager()
                    .loadTexture(location, new DynamicTexture(image));
            textures.put(path, location);
            return location;
        } catch (Throwable t) {
            SoupLog.warn("[SoupAPI] could not load the texture " + path + " from " + name, t);
            textures.put(path, null);
            return null;
        }
    }

    void releaseTexture(String path) {
        if (path == null) return;
        ResourceLocation location = textures.remove(path);
        if (location == null) return;
        try {
            Minecraft.getMinecraft().getTextureManager().deleteTexture(location);
        } catch (Throwable ignored) {
            // Deleting a texture after the context is gone is not worth a crash.
        }
    }

    private static String sanitise(String path) {
        StringBuilder out = new StringBuilder(path.length());
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            out.append((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '/' || c == '_'
                    || c == '.' || c == '-' ? c : '_');
        }
        return out.toString();
    }

    void close() {
        for (String path : new ArrayList<String>(textures.keySet())) {
            releaseTexture(path);
        }
        if (zip != null) {
            try {
                zip.close();
            } catch (IOException ignored) {
                // Already gone, or the file was replaced underneath us.
            }
            zip = null;
        }
    }

    /** The weapons this pack declares, in id order. */
    public List<MeleeWeapon> contents() {
        return Collections.unmodifiableList(weapons);
    }

    /** Looks a weapon up by {@code pack:id} or by bare id. */
    public static MeleeWeapon find(String id) {
        if (id == null || id.trim().isEmpty()) return null;
        String wanted = id.trim();
        for (MeleeWeapon weapon : weapons()) {
            if (weapon.id.equals(wanted)) return weapon;
        }
        for (MeleeWeapon weapon : weapons()) {
            if ((weapon.pack.namespace + ":" + weapon.id).equals(wanted)) return weapon;
        }
        return null;
    }

    /** The weapons the player has not switched off, in order. */
    public static List<MeleeWeapon> enabled() {
        List<MeleeWeapon> out = new ArrayList<MeleeWeapon>();
        Map<String, Boolean> disabled = disabledSet();
        for (MeleeWeapon weapon : weapons()) {
            if (!disabled.containsKey(weapon.id)) out.add(weapon);
        }
        return out;
    }

    private static Map<String, Boolean> disabledSet() {
        Map<String, Boolean> out = new LinkedHashMap<String, Boolean>();
        String[] list = me.Padej_.soupapi.config.ConfigurableModule.CONFIG.meleeDisabled;
        if (list != null) {
            for (String id : list) {
                if (id != null && !id.trim().isEmpty()) out.put(id.trim(), Boolean.TRUE);
            }
        }
        return out;
    }

    public static boolean isDisabled(MeleeWeapon weapon) {
        return weapon != null && disabledSet().containsKey(weapon.id);
    }

    /** Switches one weapon on or off; the choice lives in the config as a list of ids. */
    public static void setDisabled(MeleeWeapon weapon, boolean disabled) {
        if (weapon == null) return;
        Map<String, Boolean> set = disabledSet();
        if (disabled) {
            set.put(weapon.id, Boolean.TRUE);
        } else {
            set.remove(weapon.id);
        }
        me.Padej_.soupapi.config.ConfigurableModule.CONFIG.meleeDisabled =
                set.keySet().toArray(new String[0]);
    }
}
