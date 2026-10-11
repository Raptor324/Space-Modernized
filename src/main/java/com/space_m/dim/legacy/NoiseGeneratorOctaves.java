package com.space_m.dim.legacy;

import java.util.Random;

/** 1:1 {@code net.minecraft.world.gen.NoiseGeneratorOctaves} (1.7.10): Summe von {@link NoiseGeneratorImproved}-Oktaven. */
public class NoiseGeneratorOctaves {

    private final NoiseGeneratorImproved[] generatorCollection;
    private final int octaves;

    public NoiseGeneratorOctaves(Random rand, int octaves) {
        this.octaves = octaves;
        this.generatorCollection = new NoiseGeneratorImproved[octaves];

        for (int j = 0; j < octaves; ++j) {
            this.generatorCollection[j] = new NoiseGeneratorImproved(rand);
        }
    }

    /** 3D-Feld {@code xSize * ySize * zSize}, Index {@code (x * zSize + z) * ySize + y}. */
    public double[] generateNoiseOctaves(double[] arr, int xOff, int yOff, int zOff, int xSize, int ySize, int zSize,
                                         double xScale, double yScale, double zScale) {
        if (arr == null) {
            arr = new double[xSize * ySize * zSize];
        } else {
            for (int k1 = 0; k1 < arr.length; ++k1) {
                arr[k1] = 0.0D;
            }
        }

        double d6 = 1.0D;

        for (int l1 = 0; l1 < this.octaves; ++l1) {
            double d3 = (double) xOff * d6 * xScale;
            double d4 = (double) yOff * d6 * yScale;
            double d5 = (double) zOff * d6 * zScale;
            long i2 = Legacy.floorLong(d3);
            long j2 = Legacy.floorLong(d5);
            d3 -= (double) i2;
            d5 -= (double) j2;
            i2 %= 16777216L;
            j2 %= 16777216L;
            d3 += (double) i2;
            d5 += (double) j2;
            this.generatorCollection[l1].populateNoiseArray(arr, d3, d4, d5, xSize, ySize, zSize, xScale * d6, yScale * d6, zScale * d6, d6);
            d6 /= 2.0D;
        }

        return arr;
    }

    /** 2D-Variante (y = 10, Hoehe 1). */
    public double[] generateNoiseOctaves(double[] arr, int x, int z, int xSize, int zSize, double xScale, double zScale, double unused) {
        return this.generateNoiseOctaves(arr, x, 10, z, xSize, 1, zSize, xScale, 1.0D, zScale);
    }
}
