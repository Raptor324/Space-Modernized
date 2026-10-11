package com.space_m.dim.duna.genlayer;

import com.space_m.dim.BiomeGenBaseCelestial;
import com.space_m.dim.duna.biome.BiomeGenBaseDuna;
import com.space_m.dim.legacy.GenLayer;
import com.space_m.dim.legacy.IntCache;

/** 1:1 {@code com.hbm.dim.duna.genlayer.GenLayerDunaBiomes}: Startschicht, je Feld ein Zufallsbiom. */
public class GenLayerDunaBiomes extends GenLayer {

    private static final BiomeGenBaseCelestial[] biomes = new BiomeGenBaseCelestial[] { BiomeGenBaseDuna.dunaPlains, BiomeGenBaseDuna.dunaLowlands, BiomeGenBaseDuna.dunaPolar, BiomeGenBaseDuna.dunaHills, BiomeGenBaseDuna.dunaPolarHills };

    public GenLayerDunaBiomes(long l) {
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
