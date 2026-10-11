package com.space_m.dim;

import com.space_m.dim.legacy.GenLayer;
import com.space_m.dim.legacy.IntCache;

/** 1:1 {@code com.hbm.dim.GenLayerDiversify}: ersetzt mit Wahrscheinlichkeit 1/{@code chance} durch ein Zufallsbiom. */
public class GenLayerDiversify extends GenLayer {

    private final BiomeGenBaseCelestial[] biomes;
    private final int chance;

    public GenLayerDiversify(long l, GenLayer parent, int chance, BiomeGenBaseCelestial... biomes) {
        super(l);
        this.parent = parent;
        this.biomes = biomes;
        this.chance = chance;
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
                if (nextInt(chance) == 0) {
                    output[i] = biomes[nextInt(biomes.length)].biomeID;
                } else
                    output[i] = center;
            }
        }
        return output;
    }
}
