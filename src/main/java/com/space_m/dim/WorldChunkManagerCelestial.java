package com.space_m.dim;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.space_m.dim.legacy.GenLayer;
import com.space_m.dim.legacy.IntCache;

/**
 * 1:1 {@code com.hbm.dim.WorldChunkManagerCelestial} (Biomverteilung aus zwei GenLayer-Ketten) samt der benutzten
 * Teile von 1.7.10 {@code WorldChunkManager}/{@code BiomeCache}; dazu {@link #fixed} = 1.7.10
 * {@code WorldChunkManagerHell} (ein einziges Biom, z.B. Mun).
 *
 * <ul>
 *   <li>{@code biomeLayer} (1/4 Aufloesung): {@link #getBiomesForGeneration} - Rauschhoehen im ChunkProvider.</li>
 *   <li>{@code biomeDetailLayer} (Blockaufloesung): {@link #getBiomeGenForCoords}/{@link #loadBlockGeneratorData} -
 *       Oberflaeche, Hoehlen, gespeicherte Chunk-Biome ({@code CelestialBiomeSource}).</li>
 * </ul>
 * Instanzen haben Zustand (GenLayer-{@code chunkSeed}, Cache) und gehoeren je einem Thread.
 */
public class WorldChunkManagerCelestial implements CelestialWorld {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static volatile boolean warnedUnknown;

    private final long seed;
    private final GenLayer biomeLayer;
    private final GenLayer biomeDetailLayer;
    private final BiomeGenBaseCelestial fallback;

    /** 1.7.10 {@code BiomeCache}: 16x16-Detailfelder je Chunk. */
    private final Map<Long, BiomeGenBaseCelestial[]> biomeCache = new HashMap<>();

    public WorldChunkManagerCelestial(BiomeGenLayers layers, long seed, BiomeGenBaseCelestial fallback) {
        this.seed = seed;
        this.biomeLayer = layers == null ? null : layers.biomeLayer;
        this.biomeDetailLayer = layers == null ? null : layers.biomeDetailLayer;
        this.fallback = fallback;
    }

    /** 1.7.10 {@code WorldChunkManagerHell(biome, rainfall)}: ueberall dasselbe Biom. */
    public static WorldChunkManagerCelestial fixed(BiomeGenBaseCelestial biome, long seed) {
        return new WorldChunkManagerCelestial(null, seed, biome);
    }

    public boolean isFixed() {
        return biomeLayer == null;
    }

    @Override
    public long getSeed() {
        return seed;
    }

    private BiomeGenBaseCelestial biome(int id) {
        BiomeGenBaseCelestial b = BiomeGenBaseCelestial.getBiome(id);
        if (b == null) {
            if (!warnedUnknown) {
                warnedUnknown = true;
                LOGGER.warn("[space_m] GenLayer returned unknown celestial biome id {}, using {}", id, fallback);
            }
            return fallback;
        }
        return b;
    }

    @Override
    public BiomeGenBaseCelestial getBiomeGenForCoords(int x, int z) {
        if (isFixed()) return fallback;
        long key = ((long) (x >> 4) & 0xFFFFFFFFL) | (((long) (z >> 4) & 0xFFFFFFFFL) << 32);
        BiomeGenBaseCelestial[] cached = biomeCache.get(key);
        if (cached == null) {
            if (biomeCache.size() > 1024) biomeCache.clear(); // 1.7.10 cleanupCache
            cached = this.loadBlockGeneratorData(null, x & ~15, z & ~15, 16, 16);
            biomeCache.put(key, cached);
        }
        return cached[(x & 15) | (z & 15) << 4];
    }

    @Override
    public BiomeGenBaseCelestial[] getBiomesForGeneration(BiomeGenBaseCelestial[] biomes, int x, int z, int width, int length) {
        IntCache.resetIntCache();

        if (biomes == null || biomes.length < width * length) {
            biomes = new BiomeGenBaseCelestial[width * length];
        }

        if (isFixed()) {
            java.util.Arrays.fill(biomes, 0, width * length, fallback);
            return biomes;
        }

        int[] biomeIds = biomeLayer.getInts(x, z, width, length);

        for (int i = 0; i < width * length; ++i) {
            biomes[i] = biome(biomeIds[i]);
        }

        return biomes;
    }

    @Override
    public BiomeGenBaseCelestial[] loadBlockGeneratorData(BiomeGenBaseCelestial[] biomes, int x, int z, int width, int length) {
        IntCache.resetIntCache();

        if (biomes == null || biomes.length < width * length) {
            biomes = new BiomeGenBaseCelestial[width * length];
        }

        if (isFixed()) {
            java.util.Arrays.fill(biomes, 0, width * length, fallback);
            return biomes;
        }

        int[] biomeIds = biomeDetailLayer.getInts(x, z, width, length);

        for (int i = 0; i < width * length; ++i) {
            biomes[i] = biome(biomeIds[i]);
        }

        return biomes;
    }

    /** {@code getRainfall}: Niederschlag der Detailschicht (fuer Wetter/Schnee-Abfragen). */
    public float[] getRainfall(float[] values, int x, int z, int width, int length) {
        if (values == null || values.length < width * length) {
            values = new float[width * length];
        }

        BiomeGenBaseCelestial[] b = loadBlockGeneratorData(null, x, z, width, length);

        for (int i = 0; i < width * length; ++i) {
            // 1.7.10: getIntRainfall() / 65536 = (int)(rainfall * 65536) / 65536
            float f = (float) ((int) (b[i].rainfall * 65536.0F)) / 65536.0F;
            if (f > 1.0F) f = 1.0F;
            values[i] = f;
        }

        return values;
    }

    /** Original {@code WorldChunkManagerCelestial.BiomeGenLayers}: beide Ketten, mit dem Weltseed initialisiert. */
    public static class BiomeGenLayers {

        private final GenLayer biomeLayer;
        private final GenLayer biomeDetailLayer;

        public BiomeGenLayers(GenLayer biomeLayer, GenLayer biomeDetailLayer, long seed) {
            this.biomeLayer = biomeLayer;
            this.biomeDetailLayer = biomeDetailLayer;

            biomeLayer.initWorldGenSeed(seed);
            biomeDetailLayer.initWorldGenSeed(seed);
        }
    }
}
