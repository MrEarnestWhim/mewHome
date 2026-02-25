package com.mew.mewhome;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side i18n. Resolves translation keys using the player's client language.
 * Falls back to en_us if the player's language has no translation file.
 */
public final class ServerI18n {
    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String, String>>() {}.getType();
    private static final Map<String, Map<String, String>> LANG_CACHE = new ConcurrentHashMap<>();
    private static final String FALLBACK = "en_us";

    private ServerI18n() {}

    public static void init() {
        // Pre-load known languages at startup
        loadLang(FALLBACK);
        loadLang("ru_ru");
    }

    private static Map<String, String> loadLang(String langCode) {
        return LANG_CACHE.computeIfAbsent(langCode, code -> {
            String path = "/assets/" + MewHome.MODID + "/lang/" + code + ".json";
            try (InputStream is = ServerI18n.class.getResourceAsStream(path)) {
                if (is == null) return Map.of();
                return GSON.fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), MAP_TYPE);
            } catch (Exception e) {
                MewHome.LOGGER.warn("Failed to load lang file: {}", path, e);
                return Map.of();
            }
        });
    }

    /**
     * Resolve a translation key for the given player's language.
     * Falls back to en_us, then to the raw key.
     */
    public static String get(ServerPlayer player, String key) {
        String lang = player.clientInformation().language();
        Map<String, String> translations = loadLang(lang);
        String result = translations.get(key);
        if (result != null) return result;

        // Fallback to en_us
        Map<String, String> fallback = loadLang(FALLBACK);
        return fallback.getOrDefault(key, key);
    }

    /**
     * Create a styled Component resolved on the server side.
     */
    public static MutableComponent translate(ServerPlayer player, String key, ChatFormatting... styles) {
        MutableComponent component = Component.literal(get(player, key));
        for (ChatFormatting style : styles) {
            component = component.withStyle(style);
        }
        return component;
    }
}
