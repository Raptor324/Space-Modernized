package com.space_m.dim;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import dev.architectury.event.events.common.LifecycleEvent;
import net.minecraft.server.MinecraftServer;

/**
 * Weltseed fuer die Space-Generatoren (Original: {@code world.getSeed()} in ChunkProvider, MapGen, GenLayer).
 *
 * <p>In 1.20 bekommen {@code BiomeSource} und {@code ChunkGenerator.fillFromNoise} keinen Seed (Vanilla-Generatoren
 * leiten alles aus {@code RandomState} ab). Die 1:1-portierten GenLayer/Rausch-Generatoren brauchen aber den echten
 * Weltseed - er wird vor dem Laden der Welten gesetzt ({@code SERVER_BEFORE_START}, beide Loader), also bevor der
 * erste Chunk generiert wird. Alle Dimensionen teilen wie im Original denselben Seed.
 */
public final class CelestialSeed {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static volatile long seed;
    private static volatile boolean set;
    private static volatile boolean warned;

    private CelestialSeed() {}

    public static void init() {
        LifecycleEvent.SERVER_BEFORE_START.register(CelestialSeed::onServerStarting);
        LifecycleEvent.SERVER_STOPPED.register(server -> set = false);
    }

    private static void onServerStarting(MinecraftServer server) {
        set(server.getWorldData().worldGenOptions().seed());
    }

    public static void set(long newSeed) {
        if (!set || seed != newSeed) {
            seed = newSeed;
            set = true;
            CelestialGenerators.invalidate();
            LOGGER.debug("[space_m] Celestial world seed set");
        }
    }

    public static long get() {
        if (!set && !warned) {
            warned = true;
            LOGGER.warn("[space_m] Celestial generator used before the server set the world seed, using 0");
        }
        return seed;
    }
}
