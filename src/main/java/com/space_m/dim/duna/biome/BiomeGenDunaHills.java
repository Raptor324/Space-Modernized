package com.space_m.dim.duna.biome;

import java.util.Arrays;
import java.util.Random;

import com.space_m.dim.CelestialWorld;
import com.space_m.dim.duna.DunaBlocks;
import com.space_m.dim.legacy.Legacy;
import com.space_m.dim.legacy.NoiseGeneratorPerlin;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code com.hbm.dim.duna.biome.BiomeGenDunaHills} ("Weathered Dunaian Hills", ID 119, aus 1.7.10
 * {@code BiomeGenMesa}): Hoehe (0.525, 0.51), Gesteinsbaender aus gefaerbtem Ton nach dem Mesa-Muster
 * ({@code field_150621_aC}, Seed = Weltseed). Ersetzungen des Originals: Band "gelb" (4) -> {@code ferric_clay},
 * "weiss" (0) -> {@code stone_resource} CALCIUM, sonst {@code stained_hardened_clay} mit dem Bandmetadatum
 * (1.20: die gleichfarbige Terrakotta); oberhalb der Deckschicht immer Metadatum 14 (rot).
 * {@code MapGenPlateau} formt dieses Biom vorher zu Tafelbergen ({@code ChunkProviderDuna.genPlateau}).
 *
 * <p>Wie im Original sind {@code field_150626_aH}/{@code field_150620_aI} nie gesetzt (Mesa-Bryce/-Plateau aus):
 * {@code perlin1}/{@code perlin2} rechnen nicht mit. Die Bandtabelle ist hier threadsicher (einmal je Seed gebaut
 * und erst fertig veroeffentlicht) - das Biom-Objekt wird von allen Generator-Threads geteilt.
 */
public class BiomeGenDunaHills extends BiomeGenBaseDuna {

    public static final Height height = new Height(0.525F, 0.51F);

    /** 1.7.10 {@code stained_hardened_clay}-Metadatum -> Terrakotta. */
    private static final Block[] TERRACOTTA = {
            Blocks.WHITE_TERRACOTTA, Blocks.ORANGE_TERRACOTTA, Blocks.MAGENTA_TERRACOTTA, Blocks.LIGHT_BLUE_TERRACOTTA,
            Blocks.YELLOW_TERRACOTTA, Blocks.LIME_TERRACOTTA, Blocks.PINK_TERRACOTTA, Blocks.GRAY_TERRACOTTA,
            Blocks.LIGHT_GRAY_TERRACOTTA, Blocks.CYAN_TERRACOTTA, Blocks.PURPLE_TERRACOTTA, Blocks.BLUE_TERRACOTTA,
            Blocks.BROWN_TERRACOTTA, Blocks.GREEN_TERRACOTTA, Blocks.RED_TERRACOTTA, Blocks.BLACK_TERRACOTTA };

    /** Bandtabelle + Rauschen eines Seeds (Original: Felder {@code field_150621_aC}, {@code perlin3}, {@code seed}). */
    private static final class Bands {
        final long seed;
        final byte[] field_150621_aC;
        final NoiseGeneratorPerlin perlin3;
        final NoiseGeneratorPerlin perlin1;
        final NoiseGeneratorPerlin perlin2;

        Bands(long seed, byte[] bands, NoiseGeneratorPerlin perlin3, NoiseGeneratorPerlin perlin1, NoiseGeneratorPerlin perlin2) {
            this.seed = seed;
            this.field_150621_aC = bands;
            this.perlin3 = perlin3;
            this.perlin1 = perlin1;
            this.perlin2 = perlin2;
        }
    }

    private volatile Bands bands;
    private final boolean field_150626_aH = false;
    private final boolean field_150620_aI = false;

    public BiomeGenDunaHills(int id) {
        super(id, "duna_hills");
        this.setBiomeName("Weathered Dunaian Hills");
        this.setHeight(height);
    }

    private Bands bands(long seed) {
        Bands b = bands;
        if (b == null || b.seed != seed) {
            synchronized (this) {
                b = bands;
                if (b == null || b.seed != seed) {
                    // Original: generateBuffers(seed), danach perlin1/perlin2 aus new Random(this.seed) (vorheriger Seed)
                    byte[] buf = new byte[64];
                    NoiseGeneratorPerlin p3 = generateBuffers(seed, buf);
                    long previous = b == null ? 0L : b.seed;
                    Random random1 = new Random(previous);
                    b = new Bands(seed, buf, p3, new NoiseGeneratorPerlin(random1, 4), new NoiseGeneratorPerlin(random1, 1));
                    bands = b;
                }
            }
        }
        return b;
    }

    // Ripped from BiomeGenMesa
    @Override
    public void genTerrainBlocks(CelestialWorld world, Random rand, BlockState[] blocks, int x, int z, double noise) {
        Bands b = bands(world.getSeed());
        Block dunaRock = DunaBlocks.DUNA_ROCK.get();
        BlockState rock = dunaRock.defaultBlockState();
        BlockState ferricClay = DunaBlocks.FERRIC_CLAY.get().defaultBlockState();
        BlockState calcium = DunaBlocks.STONE_RESOURCE_CALCIUM.get().defaultBlockState();
        BlockState sands = DunaBlocks.DUNA_SANDS.get().defaultBlockState();

        double d5 = 0.0D;
        int k;
        int l;

        if (this.field_150626_aH) {
            k = (x & -16) + (z & 15);
            l = (z & -16) + (x & 15);
            double d1 = Math.min(Math.abs(noise), b.perlin1.func_151601_a((double) k * 0.25D, (double) l * 0.25D));

            if (d1 > 0.0D) {
                double d2 = 0.001953125D;
                double d3 = Math.abs(b.perlin2.func_151601_a((double) k * d2, (double) l * d2));
                d5 = d1 * d1 * 2.5D;
                double d4 = Math.ceil(d3 * 50.0D) + 14.0D;

                if (d5 > d4) {
                    d5 = d4;
                }

                d5 += 64.0D;
            }
        }

        k = x & 15;
        l = z & 15;
        BlockState block = ferricClay;
        BlockState block2 = this.fillerBlock();
        int i1 = (int) (noise / 3.0D + 3.0D + rand.nextDouble() * 0.25D);
        boolean flag1 = Math.cos(noise / 3.0D * Math.PI) > 0.0D;
        int j1 = -1;
        boolean flag2 = false;
        int k1 = blocks.length / 256;

        for (int l1 = 255; l1 >= 0; --l1) {
            int i2 = (l * 16 + k) * k1 + l1;

            if ((blocks[i2] == null || blocks[i2].isAir()) && l1 < (int) d5) {
                blocks[i2] = rock;
            }

            if (l1 <= 0 + rand.nextInt(5)) {
                blocks[i2] = Blocks.BEDROCK.defaultBlockState();
            } else {
                BlockState block1 = blocks[i2];

                if (block1 != null && !block1.isAir()) {
                    if (Legacy.is(block1, dunaRock)) {
                        byte b0;

                        if (j1 == -1) {
                            flag2 = false;

                            if (i1 <= 0) {
                                block = null;
                                block2 = rock;
                            } else if (l1 >= 59 && l1 <= 64) {
                                block = ferricClay;
                                block2 = this.fillerBlock();
                            }

                            if (l1 < 63 && (block == null || block.isAir())) {
                                block = Blocks.WATER.defaultBlockState();
                            }

                            j1 = i1 + Math.max(0, l1 - 63);

                            if (l1 >= 62) {
                                if (this.field_150620_aI && l1 > 86 + i1 * 2) {
                                    if (flag1) {
                                        blocks[i2] = sands;
                                    } else {
                                        blocks[i2] = sands;
                                    }
                                } else if (l1 > 66 + i1) {
                                    b0 = 16;

                                    if (l1 >= 64 && l1 <= 127) {
                                        if (!flag1) {
                                            b0 = this.func_150618_d(b, x, l1, z);
                                        }
                                    } else {
                                        b0 = 1;
                                    }

                                    if (b0 < 16) {
                                        blocks[i2] = TERRACOTTA[14].defaultBlockState();
                                    } else {
                                        blocks[i2] = rock;
                                    }
                                } else {
                                    blocks[i2] = this.topBlock();
                                    flag2 = true;
                                }
                            } else {
                                blocks[i2] = block2;
                            }
                        } else if (j1 > 0) {
                            --j1;

                            if (flag2) {
                                blocks[i2] = ferricClay;
                            } else {
                                b0 = this.func_150618_d(b, x, l1, z);

                                if (b0 == 4) { // yellow is ugly
                                    blocks[i2] = ferricClay;
                                } else if (b0 == 0) { // white is good for yer teeth
                                    blocks[i2] = calcium;
                                } else if (b0 < 16) {
                                    blocks[i2] = TERRACOTTA[b0].defaultBlockState();
                                } else {
                                    blocks[i2] = rock;
                                }
                            }
                        }
                    }
                } else {
                    j1 = -1;
                }
            }
        }
    }

    /** Original {@code generateBuffers(seed)}: fuellt die Bandtabelle, liefert {@code perlin3}. */
    private static NoiseGeneratorPerlin generateBuffers(long seed, byte[] field_150621_aC) {
        Arrays.fill(field_150621_aC, (byte) 16);
        Random random = new Random(seed);
        NoiseGeneratorPerlin perlin3 = new NoiseGeneratorPerlin(random, 1);
        int j;

        for (j = 0; j < 64; ++j) {
            j += random.nextInt(5) + 1;

            if (j < 64) {
                field_150621_aC[j] = 1;
            }
        }

        j = random.nextInt(4) + 2;
        int k;
        int l;
        int i1;
        int j1;

        for (k = 0; k < j; ++k) {
            l = random.nextInt(3) + 1;
            i1 = random.nextInt(64);

            for (j1 = 0; i1 + j1 < 64 && j1 < l; ++j1) {
                field_150621_aC[i1 + j1] = 4;
            }
        }

        k = random.nextInt(4) + 2;
        int k1;

        for (l = 0; l < k; ++l) {
            i1 = random.nextInt(3) + 2;
            j1 = random.nextInt(64);

            for (k1 = 0; j1 + k1 < 64 && k1 < i1; ++k1) {
                field_150621_aC[j1 + k1] = 12;
            }
        }

        l = random.nextInt(4) + 2;

        for (i1 = 0; i1 < l; ++i1) {
            j1 = random.nextInt(3) + 1;
            k1 = random.nextInt(64);

            for (int l1 = 0; k1 + l1 < 64 && l1 < j1; ++l1) {
                field_150621_aC[k1 + l1] = 14;
            }
        }

        i1 = random.nextInt(3) + 3;
        j1 = 0;

        for (k1 = 0; k1 < i1; ++k1) {
            byte b0 = 1;
            j1 += random.nextInt(16) + 4;

            for (int i2 = 0; j1 + i2 < 64 && i2 < b0; ++i2) {
                field_150621_aC[j1 + i2] = 0;

                if (j1 + i2 > 1 && random.nextBoolean()) {
                    field_150621_aC[j1 + i2 - 1] = 8;
                }

                if (j1 + i2 < 63 && random.nextBoolean()) {
                    field_150621_aC[j1 + i2 + 1] = 8;
                }
            }
        }
        return perlin3;
    }

    private byte func_150618_d(Bands b, int p_150618_1_, int p_150618_2_, int p_150618_3_) {
        int l = (int) Math.round(b.perlin3.func_151601_a((double) p_150618_1_ * 1.0D / 512.0D, (double) p_150618_1_ * 1.0D / 512.0D) * 2.0D);
        return b.field_150621_aC[(p_150618_2_ + l + 64) % 64];
    }
}
