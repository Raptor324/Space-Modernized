package com.space_m.dim.laythe.biome;

import java.util.Random;

import com.space_m.dim.CelestialWorld;
import com.space_m.dim.legacy.Legacy;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code com.hbm.dim.laythe.biome.BiomeGenLaythePolar} ("Laythe Poles", {@code laythePolarBiome} 95):
 * Schnee ueber Eis; unter y 65 zufaellig Schnee/Packeis ({@code Math.random()} wie im Original - nicht vom
 * Weltseed abhaengig).
 */
public class BiomeGenLaythePolar extends BiomeGenBaseLaythe {

    /** {@code SpaceConfig.laythePolarBiome}. */
    public static final int ID = 95;

    public BiomeGenLaythePolar(int id) {
        super(id, "laythe_polar");
        this.setBiomeName("Laythe Poles");
        this.waterColorMultiplier = 0xC1F4FF;

        this.setHeight(new Height(-0.1F, 0.05F));
        this.setTemperatureRainfall(0.2F, 0.2F);

        this.setTopBlock(Blocks.SNOW_BLOCK::defaultBlockState);
        this.setFillerBlock(Blocks.ICE::defaultBlockState);
    }

    @Override
    public void genTerrainBlocks(CelestialWorld world, Random rand, BlockState[] blocks, int x, int z, double noise) {
        BlockState stone = Blocks.STONE.defaultBlockState();
        BlockState block = this.topBlock();
        BlockState block1 = this.fillerBlock();
        int k = -1;
        int l = (int) (noise / 6.0D + 6.0D + rand.nextDouble() * 0.85D);
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
                                    block = this.topBlock();
                                } else {
                                    block = this.topBlock();
                                }
                            }

                            k = l;

                            if (l1 >= 65) {
                                blocks[i2] = block;
                            } else if (l1 < 65) {
                                block = null;
                                block1 = stone;
                                if (Math.random() > 0.4) {
                                    blocks[i2] = Blocks.SNOW_BLOCK.defaultBlockState();
                                } else {
                                    blocks[i2] = Blocks.PACKED_ICE.defaultBlockState();
                                }
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
