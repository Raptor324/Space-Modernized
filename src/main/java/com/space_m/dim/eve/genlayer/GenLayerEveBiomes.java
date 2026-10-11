package com.space_m.dim.eve.genlayer;

import com.space_m.dim.BiomeGenBaseCelestial;
import com.space_m.dim.eve.biome.BiomeGenBaseEve;
import com.space_m.dim.legacy.GenLayer;
import com.space_m.dim.legacy.IntCache;

/** 1:1 {@code com.hbm.dim.eve.genlayer.GenLayerEveBiomes}: Ebene/Ozean/Berge gleichverteilt, 1/20 Seismische Ebene. */
public class GenLayerEveBiomes extends GenLayer {

    private static final BiomeGenBaseCelestial[] biomes = new BiomeGenBaseCelestial[] { BiomeGenBaseEve.evePlains, BiomeGenBaseEve.eveOcean, BiomeGenBaseEve.eveMountains };
    protected BiomeGenBaseCelestial[] rareBiomes = { BiomeGenBaseEve.eveSeismicPlains };

    public GenLayerEveBiomes(long l) {
        super(l);
    }

    @Override
    public int[] getInts(int x, int z, int width, int depth) {
        int[] dest = IntCache.getIntCache(width * depth);

        for (int dz = 0; dz < depth; dz++) {
            for (int dx = 0; dx < width; dx++) {
                this.initChunkSeed(dx + x, dz + z);

                if (this.nextInt(20) == 0) {
                    dest[dx + dz * width] = this.rareBiomes[this.nextInt(this.rareBiomes.length)].biomeID;
                } else {
                    dest[dx + dz * width] = biomes[this.nextInt(biomes.length)].biomeID;
                }
            }
        }
        return dest;
    }
}
