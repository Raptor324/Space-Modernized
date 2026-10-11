package com.space_m.dim.laythe.biome;

import java.util.Random;

import com.space_m.dim.BiomeGenBaseCelestial;
import com.space_m.dim.CelestialWorld;
import com.space_m.dim.laythe.LaytheBlocks;
import com.space_m.dim.legacy.Legacy;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code com.hbm.dim.laythe.biome.BiomeGenBaseLaythe} (Copyright 2015 SteveKunG - More Planets Mod, CC BY-NC-ND 4.0
 * wie im Original vermerkt).
 *
 * <p>Biom-JSON ({@code data/space_m/worldgen/biome/laythe_*.json}): {@code waterColorMultiplier 0x5b009a} als
 * {@code water_color}, Wasserkreaturen Scutterfish (10/4/4) und Sifter-Aal (10/1/3) folgen mit den Mobs (R8),
 * {@code BiomeDictionary} COLD/WET/DENSE/SPOOKY entfaellt (keine 1.20-Entsprechung, nur Mod-Kompatibilitaet).
 * Dekorator {@code BiomeDecoratorCelestial(Blocks.stone)} mit {@code waterPlantsPerChunk 32},
 * {@code generateLakes false}: Features {@code space_m:laythe_vanilla_ores} (geerbtes {@code generateOres()}),
 * {@code space_m:water_plants} (32) im Biom-JSON.
 */
public abstract class BiomeGenBaseLaythe extends BiomeGenBaseCelestial {

    public static final BiomeGenBaseLaythe laytheIsland = new BiomeGenLaytheIslands(BiomeGenLaytheIslands.ID);
    public static final BiomeGenBaseLaythe laytheOcean = new BiomeGenLaytheOcean(BiomeGenLaytheOcean.ID);
    public static final BiomeGenBaseLaythe laythePolar = new BiomeGenLaythePolar(BiomeGenLaythePolar.ID);
    public static final BiomeGenBaseLaythe laytheCoast = new BiomeGenLaytheCoast(BiomeGenLaytheCoast.ID);

    /** {@code waterColorMultiplier} (Biom-JSON {@code water_color}). */
    public int waterColorMultiplier;
    /** {@code BiomeDecoratorCelestial.waterPlantsPerChunk}. */
    public int waterPlantsPerChunk;
    /** {@code BiomeDecoratorCelestial.coralPerChunk}. */
    public int coralPerChunk;

    protected BiomeGenBaseLaythe(int id, String path) {
        super(id, path);
        this.waterColorMultiplier = 0x5b009a;

        // BiomeDecoratorCelestial decorator = new BiomeDecoratorCelestial(Blocks.stone);
        this.waterPlantsPerChunk = 32;
        // decorator.seaLevel = 96;
        // theBiomeDecorator.generateLakes = false;

        this.setTopBlock(() -> LaytheBlocks.LAYTHE_SILT.get().defaultBlockState());
        this.setFillerBlock(() -> LaytheBlocks.LAYTHE_SILT.get().defaultBlockState());
    }

    /** Laythe-Biome initialisieren (Klassenladen in der Original-Reihenfolge). */
    public static void init() {
    }

    // Same as vanilla but without the gravel
    @Override
    public void genTerrainBlocks(CelestialWorld world, Random rand, BlockState[] blocks, int x, int z, double noise) {
        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockState silt = LaytheBlocks.LAYTHE_SILT.get().defaultBlockState();
        BlockState block = this.topBlock();
        BlockState block1 = this.fillerBlock();
        int k = -1;
        int l = (int) (noise / 3.0D + 3.0D + rand.nextDouble() * 0.25D);
        int i1 = x & 15;
        int j1 = z & 15;
        int k1 = blocks.length / 256;

        for (int l1 = 255; l1 >= 0; --l1) {
            int i2 = (j1 * 16 + i1) * k1 + l1;

            if (l1 <= 0 + rand.nextInt(5)) {
                blocks[i2] = Blocks.BEDROCK.defaultBlockState();
            } else {
                BlockState block2 = blocks[i2];

                if (block2 != null && !block2.isAir()) {
                    if (Legacy.is(block2, stone)) {
                        if (k == -1) {
                            if (l <= 0) {
                                block = null;
                                block1 = stone;
                            } else if (l1 >= 59 && l1 <= 64) {
                                block = this.topBlock();
                                block1 = this.fillerBlock();
                            }

                            if (l1 < 63 && (block == null || block.isAir())) {
                                if (this.getFloatTemperature(x, l1, z) < 0.15F) {
                                    block = Blocks.ICE.defaultBlockState();
                                } else {
                                    block = Blocks.WATER.defaultBlockState();
                                }
                            }

                            k = l;

                            if (l1 >= 62) {
                                blocks[i2] = block;
                            } else if (l1 < 56 - l) {
                                block = null;
                                block1 = stone;
                                blocks[i2] = silt;
                            } else {
                                blocks[i2] = block1;
                            }
                        } else if (k > 0) {
                            --k;
                            blocks[i2] = block1;

                            if (k == 0 && Legacy.is(block1, Blocks.SAND)) {
                                k = rand.nextInt(4) + Math.max(0, l1 - 63);
                                block1 = Blocks.SANDSTONE.defaultBlockState();
                            }
                        }
                    }
                } else {
                    k = -1;
                }
            }
        }
    }
}
