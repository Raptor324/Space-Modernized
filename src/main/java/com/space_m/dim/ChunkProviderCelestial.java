package com.space_m.dim;

import java.util.Random;

import com.space_m.dim.legacy.Legacy;
import com.space_m.dim.legacy.NoiseGeneratorOctaves;
import com.space_m.dim.legacy.NoiseGeneratorPerlin;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code com.hbm.dim.ChunkProviderCelestial}: fuellt einen Chunk-Puffer aus 3D-Rauschen (Werte, Frequenzen,
 * Interpolation und Biom-Gewichtung unveraendert), dann Oberflaeche je Biom ({@code genTerrainBlocks}); die
 * Unterklassen je Koerper haengen Hoehlen/Krater/Blasen an ({@link #getChunkPrimer}).
 *
 * <p><b>Hoehen:</b> Das Original rechnet in 0..255 (33 Rauschstufen a 8 Bloecke). Die Space-Dimensionen haben
 * deshalb {@code min_y 0, height 256} ({@code dimension_type/<koerper>.json}); jeder Block liegt auf derselben
 * y-Hoehe wie im Original - keine Umrechnung, Meeresspiegel/Kraterboeden/Hoehlenhoehen 1:1.
 *
 * <p><b>Entfallen:</b> {@code WorldType.FLAT} (Superflach-Zweig mit {@code getFlatWorldBlock}) - die Dimensionen
 * haben einen festen Generator, Superflach gibt es nur fuer die Oberwelt; {@code TerrainGen.getModdedNoiseGenerators}
 * (Forge-1.7.10-Ereignis ohne Gegenstueck). {@code BlockFalling.fallInstantly} ist unnoetig (Puffer, keine Welt).
 * Die {@code IChunkProvider}-Verwaltung (provide/load/save/populate) uebernimmt {@link CelestialChunkGenerator};
 * {@code populate} -> Biom-Features des Datapacks, {@code performWorldGenSpawning} -> {@code spawnOriginalMobs}.
 */
public abstract class ChunkProviderCelestial {

    // Default block fills
    protected BlockState stoneBlock;
    protected BlockState seaBlock; // Doesn't have to be liquid, if you want like, basalt seas
    protected int seaLevel;

    // Noise frequency (Vec3 x/y/z)
    protected double[] firstOrderFreq = {684.412D, 684.412D, 684.412D};
    protected double[] secondOrderFreq = {684.412D, 684.412D, 684.412D};
    protected double[] thirdOrderFreq = {8.555150000000001D, 4.277575000000001D, 8.555150000000001D};
    protected double[] heightOrderFreq = {200.0D, 200.0D, 0.5D};

    // Embiggenify
    protected boolean amplified = false;

    //round
    protected boolean reclamp = true;

    // Now for the regular stuff, changing these won't change gen, just break things
    protected final CelestialWorld worldObj;
    protected final Random rand;

    // Generation buffers and the like, no need to modify or have visibility to these
    private final NoiseGeneratorOctaves firstOrder;
    private final NoiseGeneratorOctaves secondOrder;
    private final NoiseGeneratorOctaves thirdOrder;
    private final NoiseGeneratorPerlin perlin;
    private final NoiseGeneratorOctaves heightOrder;

    protected BiomeGenBaseCelestial[] biomesForGeneration;
    private final double[] terrainBuffer = new double[825];
    private final float[] parabolicField = new float[25];
    private double[] stoneNoise = new double[256];

    private double[] firstOrderBuffer;
    private double[] secondOrderBuffer;
    private double[] thirdOrderBuffer;
    private double[] heightOrderBuffer;

    public ChunkProviderCelestial(CelestialWorld world, long seed) {
        this.worldObj = world;
        this.rand = new Random(seed);

        this.stoneBlock = Blocks.STONE.defaultBlockState();
        this.seaBlock = Blocks.AIR.defaultBlockState();
        this.seaLevel = 63;

        this.firstOrder = new NoiseGeneratorOctaves(this.rand, 16);
        this.secondOrder = new NoiseGeneratorOctaves(this.rand, 16);
        this.thirdOrder = new NoiseGeneratorOctaves(this.rand, 8);
        this.perlin = new NoiseGeneratorPerlin(this.rand, 4);
        this.heightOrder = new NoiseGeneratorOctaves(this.rand, 16);

        for (int j = -2; j <= 2; ++j) {
            for (int k = -2; k <= 2; ++k) {
                float f = 10.0F / Legacy.sqrt((float) (j * j + k * k) + 0.2F);
                this.parabolicField[j + 2 + (k + 2) * 5] = f;
            }
        }
    }

    public int getSeaLevel() {
        return seaLevel;
    }

    public BlockState getStoneBlock() {
        return stoneBlock;
    }

    /**
     * Will return back a chunk, if it doesn't exist and its not a MP client it will generates all the blocks for the
     * specified chunk from the map seed and chunk seed
     */
    protected BlockMetaBuffer getChunkPrimer(int x, int z) {
        BlockMetaBuffer buffer = new BlockMetaBuffer();
        generateBlocks(x, z, buffer.blocks);
        biomesForGeneration = this.worldObj.loadBlockGeneratorData(biomesForGeneration, x * 16, z * 16, 16, 16);
        replaceBlocksForBiome(x, z, buffer.blocks, biomesForGeneration);
        return buffer;
    }

    // Fills in the chunk with stone, water, and air
    protected void generateBlocks(int x, int z, BlockState[] blocks) {
        biomesForGeneration = worldObj.getBiomesForGeneration(biomesForGeneration, x * 4 - 2, z * 4 - 2, 10, 10);
        generateNoiseField(x * 4, 0, z * 4);
        BlockState air = Blocks.AIR.defaultBlockState();

        for (int k = 0; k < 4; ++k) {
            int l = k * 5;
            int i1 = (k + 1) * 5;

            for (int j1 = 0; j1 < 4; ++j1) {
                int k1 = (l + j1) * 33;
                int l1 = (l + j1 + 1) * 33;
                int i2 = (i1 + j1) * 33;
                int j2 = (i1 + j1 + 1) * 33;

                for (int k2 = 0; k2 < 32; ++k2) {
                    double d0 = 0.125D;
                    double d1 = terrainBuffer[k1 + k2];
                    double d2 = terrainBuffer[l1 + k2];
                    double d3 = terrainBuffer[i2 + k2];
                    double d4 = terrainBuffer[j2 + k2];
                    double d5 = (terrainBuffer[k1 + k2 + 1] - d1) * d0;
                    double d6 = (terrainBuffer[l1 + k2 + 1] - d2) * d0;
                    double d7 = (terrainBuffer[i2 + k2 + 1] - d3) * d0;
                    double d8 = (terrainBuffer[j2 + k2 + 1] - d4) * d0;

                    for (int l2 = 0; l2 < 8; ++l2) {
                        double d9 = 0.25D;
                        double d10 = d1;
                        double d11 = d2;
                        double d12 = (d3 - d1) * d9;
                        double d13 = (d4 - d2) * d9;

                        for (int i3 = 0; i3 < 4; ++i3) {
                            int j3 = i3 + k * 4 << 12 | 0 + j1 * 4 << 8 | k2 * 8 + l2;
                            short short1 = 256;
                            j3 -= short1;
                            double d14 = 0.25D;
                            double d16 = (d11 - d10) * d14;
                            double d15 = d10 - d16;

                            for (int k3 = 0; k3 < 4; ++k3) {
                                if ((d15 += d16) > 0.0D) {
                                    blocks[j3 += short1] = stoneBlock;
                                } else if (k2 * 8 + l2 < seaLevel) {
                                    blocks[j3 += short1] = seaBlock;
                                } else {
                                    blocks[j3 += short1] = air;
                                }
                            }

                            d10 += d12;
                            d11 += d13;
                        }

                        d1 += d5;
                        d2 += d6;
                        d3 += d7;
                        d4 += d8;
                    }
                }
            }
        }
    }

    // Fills up a buffer with "chances for this block to be stone" using 3D noise and biome specific information
    protected void generateNoiseField(int x, int y, int z) {
        firstOrderBuffer = firstOrder.generateNoiseOctaves(firstOrderBuffer, x, y, z, 5, 33, 5, firstOrderFreq[0], firstOrderFreq[1], firstOrderFreq[2]);
        secondOrderBuffer = secondOrder.generateNoiseOctaves(secondOrderBuffer, x, y, z, 5, 33, 5, secondOrderFreq[0], secondOrderFreq[1], secondOrderFreq[2]);
        thirdOrderBuffer = thirdOrder.generateNoiseOctaves(thirdOrderBuffer, x, y, z, 5, 33, 5, thirdOrderFreq[0], thirdOrderFreq[1], thirdOrderFreq[2]);
        heightOrderBuffer = heightOrder.generateNoiseOctaves(heightOrderBuffer, x, z, 5, 5, heightOrderFreq[0], heightOrderFreq[1], heightOrderFreq[2]);

        int l = 0;
        int i1 = 0;

        for (int j1 = 0; j1 < 5; ++j1) {
            for (int k1 = 0; k1 < 5; ++k1) {
                float f = 0.0F;
                float f1 = 0.0F;
                float f2 = 0.0F;
                byte b0 = 2;
                BiomeGenBaseCelestial biomegenbase = biomesForGeneration[j1 + 2 + (k1 + 2) * 10];

                for (int l1 = -b0; l1 <= b0; ++l1) {
                    for (int i2 = -b0; i2 <= b0; ++i2) {
                        BiomeGenBaseCelestial biomegenbase1 = biomesForGeneration[j1 + l1 + 2 + (k1 + i2 + 2) * 10];
                        float f3 = biomegenbase1.rootHeight;
                        float f4 = biomegenbase1.heightVariation;

                        if (amplified && f3 > 0.0F) {
                            f3 = 1.0F + f3 * 2.0F;
                            f4 = 1.0F + f4 * 4.0F;
                        }

                        float f5 = parabolicField[l1 + 2 + (i2 + 2) * 5] / (f3 + 2.0F);

                        if (biomegenbase1.rootHeight > biomegenbase.rootHeight) {
                            f5 /= 2.0F;
                        }

                        f += f4 * f5;
                        f1 += f3 * f5;
                        f2 += f5;
                    }
                }

                f /= f2;
                f1 /= f2;
                f = f * 0.9F + 0.1F;
                f1 = (f1 * 4.0F - 1.0F) / 8.0F;
                double d12 = heightOrderBuffer[i1] / 8000.0D;

                if (d12 < 0.0D) {
                    d12 = -d12 * 0.3D;
                }

                d12 = d12 * 3.0D - 2.0D;

                if (d12 < 0.0D) {
                    d12 /= 2.0D;

                    if (d12 < -1.0D) {
                        d12 = -1.0D;
                    }

                    d12 /= 1.4D;
                    d12 /= 2.0D;
                } else {
                    if (d12 > 1.0D) {
                        d12 = 1.0D;
                    }

                    d12 /= 8.0D;
                }

                ++i1;
                double d13 = (double) f1;
                double d14 = (double) f;
                d13 += d12 * 0.2D;
                d13 = d13 * 8.5D / 8.0D;
                double d5 = 8.5D + d13 * 4.0D;

                for (int j2 = 0; j2 < 33; ++j2) {
                    double d6 = ((double) j2 - d5) * 12.0D * 128.0D / 256.0D / d14;

                    if (d6 < 0.0D) {
                        d6 *= 4.0D;
                    }

                    double d7 = firstOrderBuffer[l] / 512.0D;
                    double d8 = secondOrderBuffer[l] / 512.0D;
                    double d9 = (thirdOrderBuffer[l] / 10.0D + 1.0D) / 2.0D;
                    //srry, there has to be a better way to smooth out things, we got the perlin tools to do so but i have no idea how to invoke those tools here.
                    //maybe sometime soon...?
                    double d10 = reclamp ? Legacy.denormalizeClamp(d7, d8, d9) - d6 : d8 - d6;
                    if (j2 > 29) {
                        double d11 = (double) ((float) (j2 - 29) / 3.0F);
                        d10 = d10 * (1.0D - d11) + -10.0D * d11;
                    }

                    terrainBuffer[l] = d10;
                    ++l;
                }
            }
        }
    }

    protected void replaceBlocksForBiome(int x, int z, BlockState[] blocks, BiomeGenBaseCelestial[] biomes) {
        double d0 = 0.03125D;
        stoneNoise = perlin.func_151599_a(stoneNoise, (double) (x * 16), (double) (z * 16), 16, 16, d0 * 2.0D, d0 * 2.0D, 1.0D);

        for (int k = 0; k < 16; ++k) {
            for (int l = 0; l < 16; ++l) {
                BiomeGenBaseCelestial biomegenbase = biomes[l + k * 16];
                biomegenbase.genTerrainBlocks(worldObj, rand, blocks, x * 16 + k, z * 16 + l, stoneNoise[l + k * 16]);
            }
        }
    }

    /**
     * {@code provideChunk} ohne {@code Chunk}: Puffer + Biome der 256 Spalten ({@code biomesForGeneration},
     * Index {@code z * 16 + x}).
     */
    public BlockMetaBuffer provideChunk(int x, int z) {
        rand.setSeed((long) x * 341873128712L + (long) z * 132897987541L);

        BlockMetaBuffer buffer = getChunkPrimer(x, z);
        buffer.biomes = biomesForGeneration.clone();
        return buffer;
    }

    /** Original {@code BlockMetaBuffer}; Metadaten stecken im {@code BlockState}. */
    public static class BlockMetaBuffer {
        public BlockState[] blocks = new BlockState[Legacy.CHUNK_SIZE];
        /** Spaltenbiome ({@code biomesForGeneration} nach {@code provideChunk}), Index {@code z * 16 + x}. */
        public BiomeGenBaseCelestial[] biomes;
    }
}
