package com.space_m.dim.feature;

import java.lang.reflect.Field;

import org.slf4j.Logger;

import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.space_m.config.SpaceConfig;

/**
 * Ganzzahl fuer Feature-Konfigurationen: fester Wert ({@code 7}) oder Verweis auf die Original-Konfiguration
 * ({@code "hbm_m:uraniumSpawn"} = {@code com.hbm_m.config.WorldConfig.uraniumSpawn}, {@code "space_m:nickelSpawn"} =
 * {@code SpaceConfig.get().nickelSpawn}). So bleiben die Raten wie im Original ({@code WorldConfig.xSpawn})
 * einstellbar, obwohl die Erze als Datapack-Features definiert sind. Gelesen wird bei jeder Platzierung.
 */
public final class ConfigInt {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final Codec<ConfigInt> CODEC = Codec.either(Codec.INT, Codec.STRING).xmap(
            e -> e.map(ConfigInt::new, ConfigInt::new),
            c -> c.reference == null ? Either.left(c.value) : Either.right(c.reference));

    private final int value;
    private final String reference;
    private volatile Field field;
    private volatile boolean failed;

    public ConfigInt(int value) {
        this.value = value;
        this.reference = null;
    }

    public ConfigInt(String reference) {
        this.value = 0;
        this.reference = reference;
    }

    public int get() {
        if (reference == null) return value;
        if (failed) return 0;
        try {
            Field f = field;
            int colon = reference.indexOf(':');
            String ns = colon < 0 ? "hbm_m" : reference.substring(0, colon);
            String name = reference.substring(colon + 1);
            // R3-1: optionaler Faktor "name*3" (z.B. WorldGeneratorMinmus: WorldConfig.copperSpawn * 3)
            int mult = 1;
            int star = name.indexOf('*');
            if (star >= 0) {
                mult = Integer.parseInt(name.substring(star + 1).trim());
                name = name.substring(0, star).trim();
            }
            if ("space_m".equals(ns)) {
                if (f == null) field = f = SpaceConfig.class.getField(name);
                return f.getInt(SpaceConfig.get()) * mult;
            }
            // R3-2: "hbm_cloth:coltanRate" = HBM ModClothConfig.get().coltanRate (Original GeneralConfig.coltanRate)
            if ("hbm_cloth".equals(ns)) {
                if (f == null) field = f = com.hbm_m.config.ModClothConfig.class.getField(name);
                return f.getInt(com.hbm_m.config.ModClothConfig.get()) * mult;
            }
            if (f == null) field = f = Class.forName("com.hbm_m.config.WorldConfig").getField(name);
            return f.getInt(null) * mult;
        } catch (ReflectiveOperationException | RuntimeException e) {
            failed = true;
            LOGGER.error("[space_m] Unknown config value '{}' in a celestial feature, using 0", reference, e);
            return 0;
        }
    }

    @Override
    public String toString() {
        return reference == null ? Integer.toString(value) : reference;
    }
}
