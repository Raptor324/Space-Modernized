package com.space_m.dim.legacy;

import java.util.Random;

/**
 * 1:1 {@code net.minecraft.world.gen.NoiseGeneratorPerlin} (1.7.10): Oktaven aus {@link NoiseGeneratorSimplex}
 * (Name aus MCP, eigentlich Simplex). Liefert u.a. das {@code stoneNoise} der Oberflaechen-Ersetzung.
 */
public class NoiseGeneratorPerlin {

    private final NoiseGeneratorSimplex[] octaveList;
    private final int octaves;

    public NoiseGeneratorPerlin(Random rand, int octaves) {
        this.octaves = octaves;
        this.octaveList = new NoiseGeneratorSimplex[octaves];

        for (int j = 0; j < octaves; ++j) {
            this.octaveList[j] = new NoiseGeneratorSimplex(rand);
        }
    }

    /** {@code func_151601_a}. */
    public double func_151601_a(double x, double z) {
        double d2 = 0.0D;
        double d3 = 1.0D;

        for (int i = 0; i < this.octaves; ++i) {
            d2 += this.octaveList[i].getValue(x * d3, z * d3) / d3;
            d3 /= 2.0D;
        }

        return d2;
    }

    /** {@code func_151599_a}: Feld mit Persistenz 0.5. */
    public double[] func_151599_a(double[] arr, double x, double z, int w, int h, double sx, double sz, double lacunarity) {
        return this.func_151600_a(arr, x, z, w, h, sx, sz, lacunarity, 0.5D);
    }

    /** {@code func_151600_a}. */
    public double[] func_151600_a(double[] arr, double x, double z, int w, int h, double sx, double sz, double lacunarity, double persistence) {
        if (arr != null && arr.length >= w * h) {
            for (int k = 0; k < arr.length; ++k) {
                arr[k] = 0.0D;
            }
        } else {
            arr = new double[w * h];
        }

        double d6 = 1.0D;
        double d7 = 1.0D;

        for (int l = 0; l < this.octaves; ++l) {
            this.octaveList[l].add(arr, x, z, w, h, sx * d7, sz * d7, 0.55D / d6);
            d7 *= lacunarity;
            d6 *= persistence;
        }

        return arr;
    }
}
