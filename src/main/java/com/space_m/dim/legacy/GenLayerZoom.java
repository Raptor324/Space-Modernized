package com.space_m.dim.legacy;

/** 1:1 {@code net.minecraft.world.gen.layer.GenLayerZoom} (1.7.10): verdoppelt die Aufloesung. */
public class GenLayerZoom extends GenLayer {

    public GenLayerZoom(long seed, GenLayer parent) {
        super(seed);
        this.parent = parent;
    }

    @Override
    public int[] getInts(int x, int z, int w, int h) {
        int i1 = x >> 1;
        int j1 = z >> 1;
        int k1 = (w >> 1) + 2;
        int l1 = (h >> 1) + 2;
        int[] aint = this.parent.getInts(i1, j1, k1, l1);
        int i2 = k1 - 1 << 1;
        int j2 = l1 - 1 << 1;
        int[] aint1 = new int[i2 * j2];
        int l2;

        for (int k2 = 0; k2 < l1 - 1; ++k2) {
            l2 = (k2 << 1) * i2;
            int i3 = 0;
            int j3 = aint[i3 + 0 + (k2 + 0) * k1];

            for (int k3 = aint[i3 + 0 + (k2 + 1) * k1]; i3 < k1 - 1; ++i3) {
                this.initChunkSeed((long) (i3 + i1 << 1), (long) (k2 + j1 << 1));
                int l3 = aint[i3 + 1 + (k2 + 0) * k1];
                int i4 = aint[i3 + 1 + (k2 + 1) * k1];
                aint1[l2] = j3;
                aint1[l2++ + i2] = this.selectRandom(j3, k3);
                aint1[l2] = this.selectRandom(j3, l3);
                aint1[l2++ + i2] = this.selectModeOrRandom(j3, l3, k3, i4);
                j3 = l3;
                k3 = i4;
            }
        }

        int[] aint2 = new int[w * h];

        for (l2 = 0; l2 < h; ++l2) {
            System.arraycopy(aint1, (l2 + (z & 1)) * i2 + (x & 1), aint2, l2 * w, w);
        }

        return aint2;
    }

    /** {@code GenLayerZoom.magnify}: {@code times}-fach zoomen (Seeds {@code seed + k}). */
    public static GenLayer magnify(long seed, GenLayer layer, int times) {
        GenLayer object = layer;

        for (int k = 0; k < times; ++k) {
            object = new GenLayerZoom(seed + (long) k, object);
        }

        return object;
    }
}
