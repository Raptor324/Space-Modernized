package com.space_m.dim.eve.genlayer;

import com.space_m.dim.BiomeGenBaseCelestial;
import com.space_m.dim.eve.biome.BiomeGenBaseEve;
import com.space_m.dim.legacy.GenLayer;
import com.space_m.dim.legacy.GenLayerRiverMix;
import com.space_m.dim.legacy.IntCache;

/**
 * 1:1 {@code com.hbm.dim.eve.genlayer.GenLayerEveRiverMix}: Vanilla-Flusskanten ({@code BiomeGenBase.river}, ID 7)
 * werden ausserhalb des Ozeans zum Explodium-Fluss. {@code BiomeGenBase.getBiomeGenArray()[id] != null} = das
 * Eingangsbiom ist registriert (hier: ein Himmelskoerper-Biom - die Eve-Kette liefert nur Eve-IDs).
 */
public class GenLayerEveRiverMix extends GenLayerRiverMix {

    private final GenLayer biomePatternGeneratorChain;
    private final GenLayer riverPatternGeneratorChain;

    public GenLayerEveRiverMix(long seed, GenLayer biomePatternGeneratorChain, GenLayer riverPatternGeneratorChain) {
        super(seed, biomePatternGeneratorChain, riverPatternGeneratorChain);

        this.biomePatternGeneratorChain = biomePatternGeneratorChain;
        this.riverPatternGeneratorChain = riverPatternGeneratorChain;
    }

    @Override
    public void initWorldGenSeed(long seed) {
        this.biomePatternGeneratorChain.initWorldGenSeed(seed);
        this.riverPatternGeneratorChain.initWorldGenSeed(seed);
        super.initWorldGenSeed(seed);
    }

    @Override
    public int[] getInts(int x, int z, int width, int length) {
        int[] inputBiomeIds = this.biomePatternGeneratorChain.getInts(x, z, width, length);
        int[] riverBiomeIds = this.riverPatternGeneratorChain.getInts(x, z, width, length);
        int[] outputBiomeIds = IntCache.getIntCache(width * length);

        for (int i1 = 0; i1 < width * length; ++i1) {
            if (inputBiomeIds[i1] != BiomeGenBaseEve.eveOcean.biomeID) {
                if (riverBiomeIds[i1] == RIVER) {
                    if (BiomeGenBaseCelestial.getBiome(inputBiomeIds[i1]) != null) {
                        outputBiomeIds[i1] = BiomeGenBaseEve.eveRiver.biomeID;
                    } else if (inputBiomeIds[i1] != MUSHROOM_ISLAND && inputBiomeIds[i1] != MUSHROOM_ISLAND_SHORE) {
                        outputBiomeIds[i1] = riverBiomeIds[i1] & 255;
                    } else {
                        outputBiomeIds[i1] = MUSHROOM_ISLAND_SHORE;
                    }
                } else {
                    outputBiomeIds[i1] = inputBiomeIds[i1];
                }
            } else {
                outputBiomeIds[i1] = inputBiomeIds[i1];
            }
        }

        return outputBiomeIds;
    }
}
