package com.space_m.dim.dres.genlayer;

import com.space_m.dim.BiomeGenBaseCelestial;
import com.space_m.dim.dres.biome.BiomeGenBaseDres;
import com.space_m.dim.legacy.GenLayer;
import com.space_m.dim.legacy.IntCache;

/** 1:1 {@code com.hbm.dim.dres.genlayer.GenLayerDresBiomes}: Startschicht, je Feld ein Zufallsbiom. */
public class GenLayerDresBiomes extends GenLayer {

    private static final BiomeGenBaseCelestial[] biomes = new BiomeGenBaseCelestial[] { BiomeGenBaseDres.dresCanyon, BiomeGenBaseDres.dresPlains };

    public GenLayerDresBiomes(long l) {
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
