package com.space_m.dim.eve.genlayer;

import com.space_m.dim.eve.biome.BiomeGenBaseEve;
import com.space_m.dim.legacy.GenLayer;
import com.space_m.dim.legacy.IntCache;

/**
 * 1:1 {@code com.hbm.dim.eve.genlayer.GenLayerEveRiver}: wie Vanilla {@code GenLayerRiver}, Kanten werden direkt
 * Explodium-Fluss. Im Original ungenutzt ({@code WorldProviderEve} nimmt den Vanilla-Fluss + {@link GenLayerEveRiverMix}).
 */
public class GenLayerEveRiver extends GenLayer {

    public GenLayerEveRiver(long p_i2128_1_, GenLayer p_i2128_3_) {
        super(p_i2128_1_);
        super.parent = p_i2128_3_;
    }

    @Override
    public int[] getInts(int p_75904_1_, int p_75904_2_, int p_75904_3_, int p_75904_4_) {
        int i1 = p_75904_1_ - 1;
        int j1 = p_75904_2_ - 1;
        int k1 = p_75904_3_ + 2;
        int l1 = p_75904_4_ + 2;
        int[] aint = this.parent.getInts(i1, j1, k1, l1);
        int[] aint1 = IntCache.getIntCache(p_75904_3_ * p_75904_4_);

        for (int i2 = 0; i2 < p_75904_4_; ++i2) {
            for (int j2 = 0; j2 < p_75904_3_; ++j2) {
                int k2 = this.func_151630_c(aint[j2 + 0 + (i2 + 1) * k1]);
                int l2 = this.func_151630_c(aint[j2 + 2 + (i2 + 1) * k1]);
                int i3 = this.func_151630_c(aint[j2 + 1 + (i2 + 0) * k1]);
                int j3 = this.func_151630_c(aint[j2 + 1 + (i2 + 2) * k1]);
                int k3 = this.func_151630_c(aint[j2 + 1 + (i2 + 1) * k1]);

                if (k3 == k2 && k3 == i3 && k3 == l2 && k3 == j3) {
                    aint1[j2 + i2 * p_75904_3_] = -1;
                } else {
                    aint1[j2 + i2 * p_75904_3_] = BiomeGenBaseEve.eveRiver.biomeID;
                }
            }
        }

        return aint1;
    }

    private int func_151630_c(int p_151630_1_) {
        return p_151630_1_ >= 2 ? 2 + (p_151630_1_ & 1) : p_151630_1_;
    }
}
