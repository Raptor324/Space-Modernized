package com.space_m.dim.legacy;

import com.space_m.dim.BiomeGenBaseCelestial;

/**
 * 1:1 {@code net.minecraft.world.gen.layer.GenLayer} (1.7.10): Biom-Schicht mit LCG-Seeds
 * ({@code baseSeed}/{@code worldGenSeed}/{@code chunkSeed}). {@code IntCache} entfaellt - jede Abfrage bekommt ein
 * neues Feld (gleiche Werte, dafuer threadsicher). Die Instanzen haben trotzdem Zustand ({@code chunkSeed});
 * jeder Thread baut seine eigene Kette ({@code WorldChunkManagerCelestial}).
 */
public abstract class GenLayer {

    /** Vanilla-Biom-IDs (1.7.10), die die Vanilla-Schichten {@link GenLayerRiver}/{@link GenLayerRiverMix} benutzen. */
    public static final int OCEAN = 0, RIVER = 7, FROZEN_RIVER = 11, ICE_PLAINS = 12, MUSHROOM_ISLAND = 14,
            MUSHROOM_ISLAND_SHORE = 15, DEEP_OCEAN = 24, MESA_PLATEAU_F = 38, MESA_PLATEAU = 39;

    private long worldGenSeed;
    protected GenLayer parent;
    private long chunkSeed;
    protected long baseSeed;

    public GenLayer(long seed) {
        this.baseSeed = seed;
        this.baseSeed *= this.baseSeed * 6364136223846793005L + 1442695040888963407L;
        this.baseSeed += seed;
        this.baseSeed *= this.baseSeed * 6364136223846793005L + 1442695040888963407L;
        this.baseSeed += seed;
        this.baseSeed *= this.baseSeed * 6364136223846793005L + 1442695040888963407L;
        this.baseSeed += seed;
    }

    /** {@code initWorldGenSeed}: einmal mit dem Weltseed an der obersten Schicht aufrufen (geht die Kette hinab). */
    public void initWorldGenSeed(long seed) {
        this.worldGenSeed = seed;

        if (this.parent != null) {
            this.parent.initWorldGenSeed(seed);
        }

        this.worldGenSeed *= this.worldGenSeed * 6364136223846793005L + 1442695040888963407L;
        this.worldGenSeed += this.baseSeed;
        this.worldGenSeed *= this.worldGenSeed * 6364136223846793005L + 1442695040888963407L;
        this.worldGenSeed += this.baseSeed;
        this.worldGenSeed *= this.worldGenSeed * 6364136223846793005L + 1442695040888963407L;
        this.worldGenSeed += this.baseSeed;
    }

    public void initChunkSeed(long x, long z) {
        this.chunkSeed = this.worldGenSeed;
        this.chunkSeed *= this.chunkSeed * 6364136223846793005L + 1442695040888963407L;
        this.chunkSeed += x;
        this.chunkSeed *= this.chunkSeed * 6364136223846793005L + 1442695040888963407L;
        this.chunkSeed += z;
        this.chunkSeed *= this.chunkSeed * 6364136223846793005L + 1442695040888963407L;
        this.chunkSeed += x;
        this.chunkSeed *= this.chunkSeed * 6364136223846793005L + 1442695040888963407L;
        this.chunkSeed += z;
    }

    protected int nextInt(int bound) {
        int j = (int) ((this.chunkSeed >> 24) % (long) bound);

        if (j < 0) {
            j += bound;
        }

        this.chunkSeed *= this.chunkSeed * 6364136223846793005L + 1442695040888963407L;
        this.chunkSeed += this.worldGenSeed;
        return j;
    }

    /** Biom-IDs fuer das Rechteck {@code (x, z, width, depth)}, Index {@code dx + dz * width}. */
    public abstract int[] getInts(int x, int z, int width, int depth);

    /** {@code compareBiomesById}: gleiche ID oder gleiche Biomklasse (1.7.10 {@code isEqualTo}). */
    protected static boolean compareBiomesById(int a, int b) {
        if (a == b) return true;
        if (a != MESA_PLATEAU_F && a != MESA_PLATEAU) {
            BiomeGenBaseCelestial ba = BiomeGenBaseCelestial.getBiome(a);
            BiomeGenBaseCelestial bb = BiomeGenBaseCelestial.getBiome(b);
            return ba != null && bb != null && (ba == bb || ba.getClass() == bb.getClass());
        }
        return b == MESA_PLATEAU_F || b == MESA_PLATEAU;
    }

    protected static boolean isBiomeOceanic(int id) {
        return id == OCEAN || id == DEEP_OCEAN || id == 10;
    }

    protected int selectRandom(int... ints) {
        return ints[this.nextInt(ints.length)];
    }

    protected int selectModeOrRandom(int a, int b, int c, int d) {
        return b == c && c == d ? b : (a == b && a == c ? a : (a == b && a == d ? a : (a == c && a == d ? a
                : (a == b && c != d ? a : (a == c && b != d ? a : (a == d && b != c ? a : (b == c && a != d ? b
                : (b == d && a != c ? b : (c == d && a != b ? c : this.selectRandom(a, b, c, d))))))))));
    }
}
