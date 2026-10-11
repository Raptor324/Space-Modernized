package com.space_m.dim.legacy;

/** 1:1 {@code net.minecraft.world.gen.layer.GenLayerRiverMix} (1.7.10): legt die Fluesse ueber die Biome. */
public class GenLayerRiverMix extends GenLayer {

    private final GenLayer biomePatternGeneratorChain;
    private final GenLayer riverPatternGeneratorChain;

    public GenLayerRiverMix(long seed, GenLayer biomes, GenLayer rivers) {
        super(seed);
        this.biomePatternGeneratorChain = biomes;
        this.riverPatternGeneratorChain = rivers;
    }

    @Override
    public void initWorldGenSeed(long seed) {
        this.biomePatternGeneratorChain.initWorldGenSeed(seed);
        this.riverPatternGeneratorChain.initWorldGenSeed(seed);
        super.initWorldGenSeed(seed);
    }

    @Override
    public int[] getInts(int x, int z, int w, int h) {
        int[] aint = this.biomePatternGeneratorChain.getInts(x, z, w, h);
        int[] aint1 = this.riverPatternGeneratorChain.getInts(x, z, w, h);
        int[] aint2 = new int[w * h];

        for (int i1 = 0; i1 < w * h; ++i1) {
            if (aint[i1] != OCEAN && aint[i1] != DEEP_OCEAN) {
                if (aint1[i1] == RIVER) {
                    if (aint[i1] == ICE_PLAINS) {
                        aint2[i1] = FROZEN_RIVER;
                    } else if (aint[i1] != MUSHROOM_ISLAND && aint[i1] != MUSHROOM_ISLAND_SHORE) {
                        aint2[i1] = aint1[i1] & 255;
                    } else {
                        aint2[i1] = MUSHROOM_ISLAND_SHORE;
                    }
                } else {
                    aint2[i1] = aint[i1];
                }
            } else {
                aint2[i1] = aint[i1];
            }
        }

        return aint2;
    }
}
