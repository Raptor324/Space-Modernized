package com.space_m.dim.eve.genlayer;

import java.util.Random;

import com.space_m.dim.eve.biome.BiomeGenBaseEve;
import com.space_m.dim.legacy.GenLayer;
import com.space_m.dim.legacy.IntCache;

/** 1:1 {@code com.hbm.dim.eve.genlayer.GenLayerEveSeismicPlains}: Ebene mit Ebenen-Nachbar wird mit 1/10 (ungeseedetes {@code new Random()}) zum Vanilla-Fluss (ID 7, {@code BiomeGenBaseEve.river}). Im Original ungenutzt - mitportiert. */
public class GenLayerEveSeismicPlains extends GenLayer {

    public GenLayerEveSeismicPlains(long seed, GenLayer genLayer) {
        super(seed);
        this.parent = genLayer;
    }

    @Override
    public int[] getInts(int areaX, int areaY, int areaWidth, int areaHeight) {
        return this.getIntsOcean(areaX, areaY, areaWidth, areaHeight);
    }

    private int[] getIntsOcean(int p_151626_1_, int p_151626_2_, int p_151626_3_, int p_151626_4_) {
        Random rand = new Random();
        int i = p_151626_1_ - 1;
        int j = p_151626_2_ - 1;
        int k = 1 + p_151626_3_ + 1;
        int l = 1 + p_151626_4_ + 1;
        int[] aint = this.parent.getInts(i, j, k, l);
        int[] aint1 = IntCache.getIntCache(p_151626_3_ * p_151626_4_);

        for (int i1 = 0; i1 < p_151626_4_; ++i1) {
            for (int j1 = 0; j1 < p_151626_3_; ++j1) {
                this.initChunkSeed((long) (j1 + p_151626_1_), (long) (i1 + p_151626_2_));
                int k1 = aint[j1 + 1 + (i1 + 1) * k];

                if (k1 == BiomeGenBaseEve.evePlains.biomeID) {
                    int l1 = aint[j1 + 1 + (i1 + 1 - 1) * k];
                    int i2 = aint[j1 + 1 + 1 + (i1 + 1) * k];
                    int j2 = aint[j1 + 1 - 1 + (i1 + 1) * k];
                    int k2 = aint[j1 + 1 + (i1 + 1 + 1) * k];
                    boolean flag = ((l1 == BiomeGenBaseEve.evePlains.biomeID)
                            || (i2 == BiomeGenBaseEve.evePlains.biomeID)
                            || (j2 == BiomeGenBaseEve.evePlains.biomeID)
                            || (k2 == BiomeGenBaseEve.evePlains.biomeID));
                    if (flag && rand.nextInt(10) == 0) {
                        k1 = RIVER;
                    }
                }

                aint1[j1 + i1 * p_151626_3_] = k1;
            }
        }

        return aint1;
    }
}
