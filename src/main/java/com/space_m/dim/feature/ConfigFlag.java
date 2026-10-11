package com.space_m.dim.feature;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.space_m.config.SpaceConfig;

/**
 * Schalter fuer Feature-Konfigurationen (Gegenstueck zu {@link ConfigInt} fuer {@code boolean}):
 * {@code "hbm_m:enableSulfurCave"} = {@code com.hbm_m.config.WorldConfig.enableSulfurCave}, {@code "space_m:x"} =
 * {@code SpaceConfig.get().x}. So bleiben die Original-{@code if(WorldConfig.enableX)}-Abfragen einstellbar.
 * Unbekannte Namen gelten als {@code false} (einmal geloggt).
 */
public final class ConfigFlag {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, Field> FIELDS = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> FAILED = new ConcurrentHashMap<>();

    private ConfigFlag() {}

    public static boolean get(String reference) {
        if (FAILED.containsKey(reference)) return false;
        try {
            int colon = reference.indexOf(':');
            String ns = colon < 0 ? "hbm_m" : reference.substring(0, colon);
            String name = reference.substring(colon + 1);
            if ("space_m".equals(ns)) {
                Field f = FIELDS.computeIfAbsent(reference, r -> field(SpaceConfig.class, name));
                return f.getBoolean(SpaceConfig.get());
            }
            Field f = FIELDS.computeIfAbsent(reference, r -> field(forName("com.hbm_m.config.WorldConfig"), name));
            return f.getBoolean(null);
        } catch (ReflectiveOperationException | RuntimeException e) {
            FAILED.put(reference, Boolean.TRUE);
            LOGGER.error("[space_m] Unknown config flag '{}' in a celestial feature, using false", reference, e);
            return false;
        }
    }

    private static Class<?> forName(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Field field(Class<?> owner, String name) {
        try {
            return owner.getField(name);
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }
}
