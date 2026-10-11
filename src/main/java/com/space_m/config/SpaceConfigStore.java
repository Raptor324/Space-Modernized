package com.space_m.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.space_m.platform.SpacePlatform;

/**
 * JSON-Speicher fuer {@link SpaceConfig} - gleiches Schema wie HBM-Modernized ({@code HbmConfigStore}):
 * flaches Objekt {@code "<Originalschluessel>": "<Wert>"} plus {@code "_desc_<Schluessel>"}-Kommentare.
 * Unbekannte Schluessel werden ignoriert, kaputte Dateien fuehren zu Standardwerten (Datei bleibt unangetastet),
 * Zahlen werden auf {@link ConfigEntry#min()}/{@link ConfigEntry#max()} begrenzt.
 */
public final class SpaceConfigStore {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private SpaceConfigStore() {}

    public static Path file(boolean client) {
        return SpacePlatform.configDir().resolve("space_m").resolve(client ? "client.json" : "common.json");
    }

    /** Alle annotierten Felder einer Seite in Deklarationsreihenfolge. */
    public static List<Field> fields(boolean client) {
        List<Field> out = new ArrayList<>();
        for (Field f : SpaceConfig.class.getDeclaredFields()) {
            if (Modifier.isStatic(f.getModifiers())) continue;
            ConfigEntry e = f.getAnnotation(ConfigEntry.class);
            if (e != null && e.client() == client) out.add(f);
        }
        return out;
    }

    public static void load(SpaceConfig cfg, boolean client) {
        Path file = file(client);
        if (!Files.exists(file)) {
            save(cfg, client);
            return;
        }
        try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject obj = GSON.fromJson(r, JsonObject.class);
            if (obj == null) return;
            for (Field f : fields(client)) {
                ConfigEntry e = f.getAnnotation(ConfigEntry.class);
                JsonElement v = obj.get(e.key());
                if (v == null || !v.isJsonPrimitive()) continue;
                try {
                    apply(cfg, f, e, v.getAsString().trim());
                } catch (RuntimeException ex) {
                    LOGGER.warn("[space_m] Ungueltiger Config-Wert {}={} ({}), Standard bleibt", e.key(), v, ex.toString());
                }
            }
        } catch (Exception ex) {
            LOGGER.error("[space_m] Config {} nicht lesbar: {}", file, ex.toString());
        }
    }

    public static void save(SpaceConfig cfg, boolean client) {
        Path file = file(client);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("_comment", client
                ? "Space Modernized client config (NTM Space ClientConfig additions)."
                : "Space Modernized common config. Keys are the original NTM Space hbm.cfg keys.");
        try {
            for (Field f : fields(client)) {
                ConfigEntry e = f.getAnnotation(ConfigEntry.class);
                if (!e.comment().isEmpty()) out.put("_desc_" + e.key(), "[" + e.category() + "] " + e.comment());
                out.put(e.key(), String.valueOf(f.get(cfg)));
            }
            Files.createDirectories(file.getParent());
            try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(out, w);
            }
        } catch (IOException | IllegalAccessException ex) {
            LOGGER.error("[space_m] Config {} nicht schreibbar: {}", file, ex.toString());
        }
    }

    private static void apply(SpaceConfig cfg, Field f, ConfigEntry e, String s) {
        try {
            Class<?> t = f.getType();
            if (t == boolean.class) {
                f.setBoolean(cfg, Boolean.parseBoolean(s));
            } else if (t == int.class) {
                f.setInt(cfg, (int) clamp(Long.parseLong(s), e));
            } else if (t == double.class) {
                f.setDouble(cfg, clamp(Double.parseDouble(s), e));
            } else if (t == float.class) {
                f.setFloat(cfg, (float) clamp(Double.parseDouble(s), e));
            } else if (t == String.class) {
                f.set(cfg, s);
            }
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static double clamp(double v, ConfigEntry e) {
        return Math.max(e.min(), Math.min(e.max(), v));
    }
}
