package com.space_m.dim.legacy;

/** 1:1 {@code net.minecraft.world.gen.layer.GenLayerSmooth} (1.7.10): glaettet einzelne Ausreisser. */
public class GenLayerSmooth extends GenLayer {

    public GenLayerSmooth(long seed, GenLayer parent) {
        super(seed);
        this.parent = parent;
    }

    @Override
    public int[] getInts(int x, int z, int w, int h) {
        int i1 = x - 1;
        int j1 = z - 1;
        int k1 = w + 2;
        int l1 = h + 2;
        int[] aint = this.parent.getInts(i1, j1, k1, l1);
        int[] aint1 = new int[w * h];

        for (int i2 = 0; i2 < h; ++i2) {
            for (int j2 = 0; j2 < w; ++j2) {
                int k2 = aint[j2 + 0 + (i2 + 1) * k1];
                int l2 = aint[j2 + 2 + (i2 + 1) * k1];
                int i3 = aint[j2 + 1 + (i2 + 0) * k1];
                int j3 = aint[j2 + 1 + (i2 + 2) * k1];
                int k3 = aint[j2 + 1 + (i2 + 1) * k1];

                if (k2 == l2 && i3 == j3) {
                    this.initChunkSeed((long) (j2 + x), (long) (i2 + z));

                    if (this.nextInt(2) == 0) {
                        k3 = k2;
                    } else {
                        k3 = i3;
                    }
                } else {
                    if (k2 == l2) {
                        k3 = k2;
                    }

                    if (i3 == j3) {
                        k3 = i3;
                    }
                }

                aint1[j2 + i2 * w] = k3;
            }
        }

        return aint1;
    }
}
