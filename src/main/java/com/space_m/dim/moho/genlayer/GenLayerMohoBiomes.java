package com.space_m.dim.moho.genlayer;

import com.space_m.dim.BiomeGenBaseCelestial;
import com.space_m.dim.legacy.GenLayer;
import com.space_m.dim.legacy.IntCache;
import com.space_m.dim.moho.biome.BiomeGenBaseMoho;

/** 1:1 {@code com.hbm.dim.moho.genlayer.GenLayerMohoBiomes}: 2/3 Krater-Hochland, 1/3 Lavameer. */
public class GenLayerMohoBiomes extends GenLayer {

    private static final BiomeGenBaseCelestial[] biomes = new BiomeGenBaseCelestial[] { BiomeGenBaseMoho.mohoCrag, BiomeGenBaseMoho.mohoCrag, BiomeGenBaseMoho.mohoLavaSea };

    public GenLayerMohoBiomes(long l) {
        super(l);
    }

    @Override
    public int[] getInts(int x, int z, int width, int depth) {
        int[] dest = IntCache.getIntCache(width * depth);

        for (int k = 0; k < depth; ++k) {
            for (int i = 0; i < width; ++i) {
                initChunkSeed(x + i, z + k);
                dest[i + k * width] = biomes[nextInt(biomes.length)].biomeID;
            }
        }

        return dest;
    }
}
