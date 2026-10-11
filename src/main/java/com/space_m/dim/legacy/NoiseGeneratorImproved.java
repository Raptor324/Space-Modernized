package com.space_m.dim.legacy;

import java.util.Random;

/**
 * 1:1 {@code net.minecraft.world.gen.NoiseGeneratorImproved} (1.7.10): klassisches Perlin-Rauschen mit
 * Gradiententabellen und dem Feld-Befueller {@code populateNoiseArray}, wie ihn {@link NoiseGeneratorOctaves} nutzt.
 * Eigene Kopie (statt 1.20 {@code ImprovedNoise}), damit Zugfolge des {@link Random} und Rechenweg exakt dem
 * Original entsprechen.
 */
public class NoiseGeneratorImproved {

    private final int[] permutations;
    public double xCoord;
    public double yCoord;
    public double zCoord;

    private static final double[] GRAD_X = new double[] {1.0D, -1.0D, 1.0D, -1.0D, 1.0D, -1.0D, 1.0D, -1.0D, 0.0D, 0.0D, 0.0D, 0.0D, 1.0D, 0.0D, -1.0D, 0.0D};
    private static final double[] GRAD_Y = new double[] {1.0D, 1.0D, -1.0D, -1.0D, 0.0D, 0.0D, 0.0D, 0.0D, 1.0D, -1.0D, 1.0D, -1.0D, 1.0D, -1.0D, 1.0D, -1.0D};
    private static final double[] GRAD_Z = new double[] {0.0D, 0.0D, 0.0D, 0.0D, 1.0D, 1.0D, -1.0D, -1.0D, 1.0D, 1.0D, -1.0D, -1.0D, 0.0D, 1.0D, 0.0D, -1.0D};
    private static final double[] GRAD2_X = new double[] {1.0D, -1.0D, 1.0D, -1.0D, 1.0D, -1.0D, 1.0D, -1.0D, 0.0D, 0.0D, 0.0D, 0.0D, 1.0D, 0.0D, -1.0D, 0.0D};
    private static final double[] GRAD2_Z = new double[] {0.0D, 0.0D, 0.0D, 0.0D, 1.0D, 1.0D, -1.0D, -1.0D, 1.0D, 1.0D, -1.0D, -1.0D, 0.0D, 1.0D, 0.0D, -1.0D};

    public NoiseGeneratorImproved() {
        this(new Random());
    }

    public NoiseGeneratorImproved(Random rand) {
        this.permutations = new int[512];
        this.xCoord = rand.nextDouble() * 256.0D;
        this.yCoord = rand.nextDouble() * 256.0D;
        this.zCoord = rand.nextDouble() * 256.0D;
        int i;

        for (i = 0; i < 256; this.permutations[i] = i++) {
        }

        for (i = 0; i < 256; ++i) {
            int j = rand.nextInt(256 - i) + i;
            int k = this.permutations[i];
            this.permutations[i] = this.permutations[j];
            this.permutations[j] = k;
            this.permutations[i + 256] = this.permutations[i];
        }
    }

    public final double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }

    public final double grad2(int hash, double x, double z) {
        int j = hash & 15;
        return GRAD2_X[j] * x + GRAD2_Z[j] * z;
    }

    public final double grad(int hash, double x, double y, double z) {
        int j = hash & 15;
        return GRAD_X[j] * x + GRAD_Y[j] * y + GRAD_Z[j] * z;
    }

    /** {@code populateNoiseArray}: addiert das Rauschen dieser Oktave (geteilt durch {@code noiseScale}) auf {@code arr}. */
    public void populateNoiseArray(double[] arr, double xOff, double yOff, double zOff, int xSize, int ySize, int zSize,
                                   double xScale, double yScale, double zScale, double noiseScale) {
        int l;
        int i1;
        double d10;
        double d12;
        int l1;
        double d13;
        int i2;
        int j2;
        double d14;
        double d15;

        if (ySize == 1) {
            double d21;
            double d22;
            int j3 = 0;
            double d23 = 1.0D / noiseScale;

            for (int j1 = 0; j1 < xSize; ++j1) {
                d10 = xOff + (double) j1 * xScale + this.xCoord;
                int k1 = (int) d10;

                if (d10 < (double) k1) {
                    --k1;
                }

                l1 = k1 & 255;
                d10 -= (double) k1;
                d13 = d10 * d10 * d10 * (d10 * (d10 * 6.0D - 15.0D) + 10.0D);

                for (i2 = 0; i2 < zSize; ++i2) {
                    d12 = zOff + (double) i2 * zScale + this.zCoord;
                    j2 = (int) d12;

                    if (d12 < (double) j2) {
                        --j2;
                    }

                    int k2 = j2 & 255;
                    d12 -= (double) j2;
                    d14 = d12 * d12 * d12 * (d12 * (d12 * 6.0D - 15.0D) + 10.0D);
                    l = this.permutations[l1] + 0;
                    int l2 = this.permutations[l] + k2;
                    int i3 = this.permutations[l1 + 1] + 0;
                    i1 = this.permutations[i3] + k2;
                    d21 = this.lerp(d13, this.grad2(this.permutations[l2], d10, d12), this.grad(this.permutations[i1], d10 - 1.0D, 0.0D, d12));
                    d22 = this.lerp(d13, this.grad(this.permutations[l2 + 1], d10, 0.0D, d12 - 1.0D), this.grad(this.permutations[i1 + 1], d10 - 1.0D, 0.0D, d12 - 1.0D));
                    double d24 = this.lerp(d14, d21, d22);
                    int idx = j3++;
                    arr[idx] += d24 * d23;
                }
            }
        } else {
            l = 0;
            double d7 = 1.0D / noiseScale;
            i1 = -1;
            double d8 = 0.0D;
            double d9 = 0.0D;
            double d16 = 0.0D;
            double d17 = 0.0D;

            for (l1 = 0; l1 < xSize; ++l1) {
                d10 = xOff + (double) l1 * xScale + this.xCoord;
                i2 = (int) d10;

                if (d10 < (double) i2) {
                    --i2;
                }

                j2 = i2 & 255;
                d10 -= (double) i2;
                d13 = d10 * d10 * d10 * (d10 * (d10 * 6.0D - 15.0D) + 10.0D);

                for (int k3 = 0; k3 < zSize; ++k3) {
                    d12 = zOff + (double) k3 * zScale + this.zCoord;
                    int l3 = (int) d12;

                    if (d12 < (double) l3) {
                        --l3;
                    }

                    int i4 = l3 & 255;
                    d12 -= (double) l3;
                    d14 = d12 * d12 * d12 * (d12 * (d12 * 6.0D - 15.0D) + 10.0D);

                    for (int j4 = 0; j4 < ySize; ++j4) {
                        double d18 = yOff + (double) j4 * yScale + this.yCoord;
                        int k4 = (int) d18;

                        if (d18 < (double) k4) {
                            --k4;
                        }

                        int l4 = k4 & 255;
                        d18 -= (double) k4;
                        double d19 = d18 * d18 * d18 * (d18 * (d18 * 6.0D - 15.0D) + 10.0D);

                        if (j4 == 0 || l4 != i1) {
                            i1 = l4;
                            int k2 = this.permutations[j2] + l4;
                            int l2 = this.permutations[k2] + i4;
                            int i3 = this.permutations[k2 + 1] + i4;
                            int j3 = this.permutations[j2 + 1] + l4;
                            int k5 = this.permutations[j3] + i4;
                            int l5 = this.permutations[j3 + 1] + i4;
                            d8 = this.lerp(d13, this.grad(this.permutations[l2], d10, d18, d12), this.grad(this.permutations[k5], d10 - 1.0D, d18, d12));
                            d9 = this.lerp(d13, this.grad(this.permutations[i3], d10, d18 - 1.0D, d12), this.grad(this.permutations[l5], d10 - 1.0D, d18 - 1.0D, d12));
                            d16 = this.lerp(d13, this.grad(this.permutations[l2 + 1], d10, d18, d12 - 1.0D), this.grad(this.permutations[k5 + 1], d10 - 1.0D, d18, d12 - 1.0D));
                            d17 = this.lerp(d13, this.grad(this.permutations[i3 + 1], d10, d18 - 1.0D, d12 - 1.0D), this.grad(this.permutations[l5 + 1], d10 - 1.0D, d18 - 1.0D, d12 - 1.0D));
                        }

                        double d20 = this.lerp(d19, d8, d9);
                        d15 = this.lerp(d19, d16, d17);
                        double d24 = this.lerp(d14, d20, d15);
                        int idx = l++;
                        arr[idx] += d24 * d7;
                    }
                }
            }
        }
    }
}
