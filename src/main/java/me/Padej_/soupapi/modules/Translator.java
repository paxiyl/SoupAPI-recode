package me.Padej_.soupapi.modules;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.Padej_.soupapi.config.ConfigurableModule;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Translator extends ConfigurableModule {

    public static final Map<String, String> translationCache = new HashMap<>();
    private static final Queue<String> translationQueue = new LinkedList<>();
    private static final Set<String> processing = new HashSet<>();
    private static final File cacheFile = new File("translator_cache.json");
    private static final Gson gson = new Gson();

    private static long lastRequestTime = 0;
    private static int failCounter = 0;

    public static void onTick() {
        // Was hard-disabled with `if (true) return;`, so the option could never do anything. It is
        // gated on its own setting now, and nothing is sent anywhere while that setting is off.
        if (!ConfigurableModule.CONFIG.translatorBruhEnabled) return;

        long currentTime = System.currentTimeMillis();

        if (currentTime - lastRequestTime < 10000) {
            return;
        }

        if (!translationQueue.isEmpty() && processing.isEmpty()) {
            String textToTranslate = translationQueue.poll();
            if (textToTranslate != null) {
                processing.add(textToTranslate);
                lastRequestTime = currentTime;

                new Thread(() -> {
                    try {
                        String translated = translateText(textToTranslate);
                        synchronized (translationCache) {
                            translationCache.put(textToTranslate, translated);
                        }
                        saveCache();
                        failCounter = 0; // Успех, сбрасываем счётчик
                    } catch (IOException e) {
                        if (e.getMessage().contains("Server returned HTTP response code: 429")) {
                            System.out.println("[Translator] Получено 429. Перезаписываем в очередь: " + textToTranslate);
                            synchronized (translationQueue) {
                                translationQueue.add(textToTranslate); // Вернуть текст обратно
                            }
                            failCounter++;
                            if (failCounter >= 5) {
                                System.out.println("[Translator] Слишком много 429 ошибок подряд. Очистка очереди...");
                                translationQueue.clear();
                            }
                        } else {
                            e.printStackTrace();
                        }
                    } finally {
                        processing.remove(textToTranslate);
                    }
                }, "Translator-Thread").start();
            }
        }
    }

    public static boolean containsCyrillic(String text) {
        for (char c : text.toCharArray()) {
            if (c >= 'А' && c <= 'я' || c == 'ё' || c == 'Ё') {
                return true;
            }
        }
        return false;
    }

    /** A finished translation for {@code text}, or {@code null} while it is still in flight. */
    public static String cachedTranslation(String text) {
        synchronized (translationCache) {
            Object value = translationCache.get(text);
            return value == null ? null : String.valueOf(value);
        }
    }

    public static void queueTranslation(String text) {
        if (!translationCache.containsKey(text) && !translationQueue.contains(text) && !processing.contains(text)) {
            translationQueue.add(text);
        }
    }

    private static String translateText(String text) throws IOException {
        String apiUrl = "https://api.mymemory.translated.net/get?q=" + URLEncoder.encode(text, "UTF-8") + "&langpair=ru|en";

        HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        Scanner scanner = new Scanner(connection.getInputStream());
        StringBuilder json = new StringBuilder();
        while (scanner.hasNext()) {
            json.append(scanner.nextLine());
        }
        scanner.close();
        connection.disconnect();

        JsonObject jsonObject = new JsonParser().parse(json.toString()).getAsJsonObject();
        String translated = jsonObject.getAsJsonObject("responseData").get("translatedText").getAsString();

        // ПРОВЕРКА: перевод не должен содержать подозрительные вещи
        if (translated.contains("http") || translated.contains("//") || translated.length() > text.length() * 2) {
            System.out.println("[Translator] Подозрительный перевод, оригинал сохранён: " + text);
            return text; // Вернуть оригинал вместо плохого перевода
        }

        return translated;
    }

    public static void loadCache() {
        if (cacheFile.exists()) {
            try (Reader reader = new java.io.InputStreamReader(
                    new java.io.FileInputStream(cacheFile), StandardCharsets.UTF_8)) {
                Map<?, ?> loaded = gson.fromJson(reader, Map.class);
                for (Map.Entry<?, ?> entry : loaded.entrySet()) {
                    translationCache.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void saveCache() {
        try (Writer writer = new java.io.OutputStreamWriter(
                new java.io.FileOutputStream(cacheFile), StandardCharsets.UTF_8)) {
            gson.toJson(translationCache, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

