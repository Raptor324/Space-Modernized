package com.space_m.dim.eve.genlayer;

import com.space_m.dim.BiomeGenBaseCelestial;
import com.space_m.dim.eve.biome.BiomeGenBaseEve;
import com.space_m.dim.legacy.GenLayer;
import com.space_m.dim.legacy.IntCache;

/**
 * 1:1 {@code com.hbm.dim.eve.genlayer.GenLayerDiversifyEve}: jedes zweite Feld ein zufaelliges Eve-Biom. Im Original
 * ungenutzt ({@code WorldProviderEve.createBiomeGenerators} verwendet es nicht) - mitportiert.
 */
public class GenLayerDiversifyEve extends GenLayer {

    private static final BiomeGenBaseCelestial[] biomes = new BiomeGenBaseCelestial[] { BiomeGenBaseEve.evePlains, BiomeGenBaseEve.eveOcean, BiomeGenBaseEve.eveMountains, BiomeGenBaseEve.eveSeismicPlains, BiomeGenBaseEve.eveRiver };

    public GenLayerDiversifyEve(long l, GenLayer parent) {
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
                } else output[i] = center;
            }
        }
        return output;
    }
}
