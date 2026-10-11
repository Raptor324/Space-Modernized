package com.space_m.dim.legacy;

import java.util.Random;

/** 1:1 {@code net.minecraft.world.gen.NoiseGeneratorSimplex} (1.7.10): 2D-Simplex-Rauschen. */
public class NoiseGeneratorSimplex {

    private static final int[][] GRAD = new int[][] {{1, 1, 0}, {-1, 1, 0}, {1, -1, 0}, {-1, -1, 0}, {1, 0, 1}, {-1, 0, 1}, {1, 0, -1}, {-1, 0, -1}, {0, 1, 1}, {0, -1, 1}, {0, 1, -1}, {0, -1, -1}};
    public static final double SQRT_3 = Math.sqrt(3.0D);
    private final int[] perm;
    public double xo;
    public double yo;
    public double zo;
    private static final double F2 = 0.5D * (SQRT_3 - 1.0D);
    private static final double G2 = (3.0D - SQRT_3) / 6.0D;

    public NoiseGeneratorSimplex() {
        this(new Random());
    }

    public NoiseGeneratorSimplex(Random rand) {
        this.perm = new int[512];
        this.xo = rand.nextDouble() * 256.0D;
        this.yo = rand.nextDouble() * 256.0D;
        this.zo = rand.nextDouble() * 256.0D;
        int i;

        for (i = 0; i < 256; this.perm[i] = i++) {
        }

        for (i = 0; i < 256; ++i) {
            int j = rand.nextInt(256 - i) + i;
            int k = this.perm[i];
            this.perm[i] = this.perm[j];
            this.perm[j] = k;
            this.perm[i + 256] = this.perm[i];
        }
    }

    private static int fastFloor(double d) {
        return d > 0.0D ? (int) d : (int) d - 1;
    }

    private static double dot(int[] g, double x, double y) {
        return (double) g[0] * x + (double) g[1] * y;
    }

    /** {@code func_151605_a}: Einzelwert ohne Ursprungsversatz. */
    public double getValue(double x, double y) {
        double d11 = 0.5D * (SQRT_3 - 1.0D);
        double d2 = (x + y) * d11;
        int i = fastFloor(x + d2);
        int j = fastFloor(y + d2);
        double d3 = (3.0D - SQRT_3) / 6.0D;
        double d4 = (double) (i + j) * d3;
        double d5 = (double) i - d4;
        double d6 = (double) j - d4;
        double d7 = x - d5;
        double d8 = y - d6;
        byte b0;
        byte b1;

        if (d7 > d8) {
            b0 = 1;
            b1 = 0;
        } else {
            b0 = 0;
            b1 = 1;
        }

        double d9 = d7 - (double) b0 + d3;
        double d10 = d8 - (double) b1 + d3;
        double d12 = d7 - 1.0D + 2.0D * d3;
        double d13 = d8 - 1.0D + 2.0D * d3;
        int k = i & 255;
        int l = j & 255;
        int i1 = this.perm[k + this.perm[l]] % 12;
        int j1 = this.perm[k + b0 + this.perm[l + b1]] % 12;
        int k1 = this.perm[k + 1 + this.perm[l + 1]] % 12;
        double d14 = 0.5D - d7 * d7 - d8 * d8;
        double d0;

        if (d14 < 0.0D) {
            d0 = 0.0D;
        } else {
            d14 *= d14;
            d0 = d14 * d14 * dot(GRAD[i1], d7, d8);
        }

        double d15 = 0.5D - d9 * d9 - d10 * d10;
        double d1;

        if (d15 < 0.0D) {
            d1 = 0.0D;
        } else {
            d15 *= d15;
            d1 = d15 * d15 * dot(GRAD[j1], d9, d10);
        }

        double d16 = 0.5D - d12 * d12 - d13 * d13;
        double d17;

        if (d16 < 0.0D) {
            d17 = 0.0D;
        } else {
            d16 *= d16;
            d17 = d16 * d16 * dot(GRAD[k1], d12, d13);
        }

        return 70.0D * (d0 + d1 + d17);
    }

    /** {@code func_151606_a}: Feld (Zeilen = zweite Koordinate) mit Ursprungsversatz, addiert {@code amp}-fach. */
    public void add(double[] arr, double xOff, double yOff, int w, int h, double sx, double sy, double amp) {
        int k = 0;

        for (int l = 0; l < h; ++l) {
            double d4 = (yOff + (double) l) * sy + this.yo;

            for (int i1 = 0; i1 < w; ++i1) {
                double d5 = (xOff + (double) i1) * sx + this.xo;
                double d9 = (d5 + d4) * F2;
                int j1 = fastFloor(d5 + d9);
                int k1 = fastFloor(d4 + d9);
                double d10 = (double) (j1 + k1) * G2;
                double d11 = (double) j1 - d10;
                double d12 = (double) k1 - d10;
                double d13 = d5 - d11;
                double d14 = d4 - d12;
                byte b0;
                byte b1;

                if (d13 > d14) {
                    b0 = 1;
                    b1 = 0;
                } else {
                    b0 = 0;
                    b1 = 1;
                }

                double d15 = d13 - (double) b0 + G2;
                double d16 = d14 - (double) b1 + G2;
                double d17 = d13 - 1.0D + 2.0D * G2;
                double d18 = d14 - 1.0D + 2.0D * G2;
                int l1 = j1 & 255;
                int i2 = k1 & 255;
                int j2 = this.perm[l1 + this.perm[i2]] % 12;
                int k2 = this.perm[l1 + b0 + this.perm[i2 + b1]] % 12;
                int l2 = this.perm[l1 + 1 + this.perm[i2 + 1]] % 12;
                double d19 = 0.5D - d13 * d13 - d14 * d14;
                double d6;

                if (d19 < 0.0D) {
                    d6 = 0.0D;
                } else {
                    d19 *= d19;
                    d6 = d19 * d19 * dot(GRAD[j2], d13, d14);
                }

                double d20 = 0.5D - d15 * d15 - d16 * d16;
                double d7;

                if (d20 < 0.0D) {
                    d7 = 0.0D;
                } else {
                    d20 *= d20;
                    d7 = d20 * d20 * dot(GRAD[k2], d15, d16);
                }

                double d21 = 0.5D - d17 * d17 - d18 * d18;
                double d8;

                if (d21 < 0.0D) {
                    d8 = 0.0D;
                } else {
                    d21 *= d21;
                    d8 = d21 * d21 * dot(GRAD[l2], d17, d18);
                }

                int i3 = k++;
                arr[i3] += 70.0D * (d6 + d7 + d8) * amp;
            }
        }
    }
}
