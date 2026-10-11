package com.space_m.dim.legacy;

import java.util.Random;

import com.space_m.dim.BiomeGenBaseCelestial;
import com.space_m.dim.CelestialWorld;

import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code net.minecraft.world.gen.MapGenCaves} (1.7.10 mit den Forge-Haken {@code isOceanBlock},
 * {@code isTopBlock}, {@code digBlock}). Die Forge-Ausnahmebiome (Pilzinsel, Strand, Wueste) gibt es auf
 * Himmelskoerpern nicht und entfallen daher.
 */
public class MapGenCaves extends MapGenBase {

    /** {@code func_151542_a}: grosse Hoehle (Raum). */
    protected void func_151542_a(long seed, int chunkX, int chunkZ, BlockState[] blocks, double x, double y, double z) {
        this.func_151541_a(seed, chunkX, chunkZ, blocks, x, y, z, 1.0F + this.rand.nextFloat() * 6.0F, 0.0F, 0.0F, -1, -1, 0.5D);
    }

    /** {@code func_151541_a}: ein Hoehlenast. */
    protected void func_151541_a(long seed, int chunkX, int chunkZ, BlockState[] blocks, double x, double y, double z,
                                 float size, float yaw, float pitch, int step, int maxSteps, double heightRatio) {
        double d4 = (double) (chunkX * 16 + 8);
        double d5 = (double) (chunkZ * 16 + 8);
        float f3 = 0.0F;
        float f4 = 0.0F;
        Random random = new Random(seed);

        if (maxSteps <= 0) {
            int j1 = this.range * 16 - 16;
            maxSteps = j1 - random.nextInt(j1 / 4);
        }

        boolean flag2 = false;

        if (step == -1) {
            step = maxSteps / 2;
            flag2 = true;
        }

        int k1 = random.nextInt(maxSteps / 2) + maxSteps / 4;

        for (boolean flag = random.nextInt(6) == 0; step < maxSteps; ++step) {
            double d6 = 1.5D + (double) (Mth.sin((float) step * (float) Math.PI / (float) maxSteps) * size * 1.0F);
            double d7 = d6 * heightRatio;
            float f5 = Mth.cos(pitch);
            float f6 = Mth.sin(pitch);
            x += (double) (Mth.cos(yaw) * f5);
            y += (double) f6;
            z += (double) (Mth.sin(yaw) * f5);

            if (flag) {
                pitch *= 0.92F;
            } else {
                pitch *= 0.7F;
            }

            pitch += f4 * 0.1F;
            yaw += f3 * 0.1F;
            f4 *= 0.9F;
            f3 *= 0.75F;
            f4 += (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 2.0F;
            f3 += (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 4.0F;

            if (!flag2 && step == k1 && size > 1.0F && maxSteps > 0) {
                this.func_151541_a(random.nextLong(), chunkX, chunkZ, blocks, x, y, z, random.nextFloat() * 0.5F + 0.5F, yaw - ((float) Math.PI / 2F), pitch / 3.0F, step, maxSteps, 1.0D);
                this.func_151541_a(random.nextLong(), chunkX, chunkZ, blocks, x, y, z, random.nextFloat() * 0.5F + 0.5F, yaw + ((float) Math.PI / 2F), pitch / 3.0F, step, maxSteps, 1.0D);
                return;
            }

            if (flag2 || random.nextInt(4) != 0) {
                double d8 = x - d4;
                double d9 = z - d5;
                double d10 = (double) (maxSteps - step);
                double d11 = (double) (size + 2.0F + 16.0F);

                if (d8 * d8 + d9 * d9 - d10 * d10 > d11 * d11) {
                    return;
                }

                if (x >= d4 - 16.0D - d6 * 2.0D && z >= d5 - 16.0D - d6 * 2.0D && x <= d4 + 16.0D + d6 * 2.0D && z <= d5 + 16.0D + d6 * 2.0D) {
                    int i4 = Legacy.floor(x - d6) - chunkX * 16 - 1;
                    int l1 = Legacy.floor(x + d6) - chunkX * 16 + 1;
                    int j4 = Legacy.floor(y - d7) - 1;
                    int i2 = Legacy.floor(y + d7) + 1;
                    int k4 = Legacy.floor(z - d6) - chunkZ * 16 - 1;
                    int j2 = Legacy.floor(z + d6) - chunkZ * 16 + 1;

                    if (i4 < 0) i4 = 0;
                    if (l1 > 16) l1 = 16;
                    if (j4 < 1) j4 = 1;
                    if (i2 > 248) i2 = 248;
                    if (k4 < 0) k4 = 0;
                    if (j2 > 16) j2 = 16;

                    boolean flag3 = false;
                    int k2;
                    int j3;

                    for (k2 = i4; !flag3 && k2 < l1; ++k2) {
                        for (int l2 = k4; !flag3 && l2 < j2; ++l2) {
                            for (int i3 = i2 + 1; !flag3 && i3 >= j4 - 1; --i3) {
                                j3 = (k2 * 16 + l2) * 256 + i3;

                                if (i3 >= 0 && i3 < 256) {
                                    if (this.isOceanBlock(blocks, j3, k2, i3, l2, chunkX, chunkZ)) {
                                        flag3 = true;
                                    }

                                    if (i3 != j4 - 1 && k2 != i4 && k2 != l1 - 1 && l2 != k4 && l2 != j2 - 1) {
                                        i3 = j4;
                                    }
                                }
                            }
                        }
                    }

                    if (!flag3) {
                        for (k2 = i4; k2 < l1; ++k2) {
                            double d13 = ((double) (k2 + chunkX * 16) + 0.5D - x) / d6;

                            for (j3 = k4; j3 < j2; ++j3) {
                                double d14 = ((double) (j3 + chunkZ * 16) + 0.5D - z) / d6;
                                int k3 = (k2 * 16 + j3) * 256 + i2;
                                boolean flag1 = false;

                                if (d13 * d13 + d14 * d14 < 1.0D) {
                                    for (int l3 = i2 - 1; l3 >= j4; --l3) {
                                        double d12 = ((double) l3 + 0.5D - y) / d7;

                                        if (d12 > -0.7D && d13 * d13 + d12 * d12 + d14 * d14 < 1.0D) {
                                            if (this.isTopBlock(blocks, k3, k2, l3, j3, chunkX, chunkZ)) {
                                                flag1 = true;
                                            }

                                            this.digBlock(blocks, k3, k2, l3, j3, chunkX, chunkZ, flag1);
                                        }

                                        --k3;
                                    }
                                }
                            }
                        }

                        if (flag2) {
                            break;
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void func_151538_a(CelestialWorld world, int offsetX, int offsetZ, int chunkX, int chunkZ, BlockState[] blocks) {
        int i1 = this.rand.nextInt(this.rand.nextInt(this.rand.nextInt(15) + 1) + 1);

        if (this.rand.nextInt(7) != 0) {
            i1 = 0;
        }

        for (int j1 = 0; j1 < i1; ++j1) {
            double d0 = (double) (offsetX * 16 + this.rand.nextInt(16));
            double d1 = (double) this.rand.nextInt(this.rand.nextInt(120) + 8);
            double d2 = (double) (offsetZ * 16 + this.rand.nextInt(16));
            int k1 = 1;

            if (this.rand.nextInt(4) == 0) {
                this.func_151542_a(this.rand.nextLong(), chunkX, chunkZ, blocks, d0, d1, d2);
                k1 += this.rand.nextInt(4);
            }

            for (int l1 = 0; l1 < k1; ++l1) {
                float f = this.rand.nextFloat() * (float) Math.PI * 2.0F;
                float f1 = (this.rand.nextFloat() - 0.5F) * 2.0F / 8.0F;
                float f2 = this.rand.nextFloat() * 2.0F + this.rand.nextFloat();

                if (this.rand.nextInt(10) == 0) {
                    f2 *= this.rand.nextFloat() * this.rand.nextFloat() * 3.0F + 1.0F;
                }

                this.func_151541_a(this.rand.nextLong(), chunkX, chunkZ, blocks, d0, d1, d2, f2, f, f1, 0, 0, 1.0D);
            }
        }
    }

    /** Forge-Haken: Wasser im Weg -> Hoehle abbrechen. */
    protected boolean isOceanBlock(BlockState[] data, int index, int x, int y, int z, int chunkX, int chunkZ) {
        return Legacy.is(data[index], Blocks.WATER);
    }

    /** Forge-Haken: ist der Block die Oberflaeche des Bioms? */
    protected boolean isTopBlock(BlockState[] data, int index, int x, int y, int z, int chunkX, int chunkZ) {
        BiomeGenBaseCelestial biome = this.worldObj.getBiomeGenForCoords(x + chunkX * 16, z + chunkZ * 16);
        return Legacy.is(data[index], biome.topBlock());
    }

    /** Forge-Haken: Block aushoehlen (unter y 10 Lava). */
    protected void digBlock(BlockState[] data, int index, int x, int y, int z, int chunkX, int chunkZ, boolean foundTop) {
        BiomeGenBaseCelestial biome = this.worldObj.getBiomeGenForCoords(x + chunkX * 16, z + chunkZ * 16);
        BlockState top = biome.topBlock();
        BlockState filler = biome.fillerBlock();
        BlockState block = data[index];

        if (Legacy.is(block, Blocks.STONE) || Legacy.is(block, filler) || Legacy.is(block, top)) {
            if (y < 10) {
                data[index] = Blocks.LAVA.defaultBlockState();
            } else {
                data[index] = null;

                if (foundTop && Legacy.is(data[index - 1], filler)) {
                    data[index - 1] = top;
                }
            }
        }
    }
}
