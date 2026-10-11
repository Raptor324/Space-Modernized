package com.space_m.dim.dres.genlayer;

import com.space_m.dim.BiomeGenBaseCelestial;
import com.space_m.dim.dres.biome.BiomeGenBaseDres;
import com.space_m.dim.legacy.GenLayer;
import com.space_m.dim.legacy.IntCache;

/** 1:1 {@code com.hbm.dim.dres.genlayer.GenLayerDiversifyDres}: ersetzt jedes zweite Feld (1/2) durch ein Zufallsbiom. */
public class GenLayerDiversifyDres extends GenLayer {

    private static final BiomeGenBaseCelestial[] biomes = new BiomeGenBaseCelestial[] { BiomeGenBaseDres.dresCanyon, BiomeGenBaseDres.dresPlains };

    public GenLayerDiversifyDres(long l, GenLayer parent) {
        super(l);
        this.parent = parent;
    }

    @Override
    public int[] getInts(int x, int z, int width, int depth) {
        return diversify(x, z, width, depth);
    }

    private int[] diversify(int x, int z, int width, int height) {
        int[] input = this.parent.getInts(x, z, width, height);
        int[] output = IntCache.getIntCache(width * height);
        for (int zOut = 0; zOut < height; zOut++) {
            for (int xOut = 0; xOut < width; xOut++) {
                int i = xOut + zOut * width;
                int center = input[i];
                initChunkSeed(xOut + x, zOut + z);
                if (nextInt(2) == 0) {
                    output[i] = biomes[nextInt(biomes.length)].biomeID;
                } else
                    output[i] = center;
            }
        }
        return output;
    }
}
