package com.space_m.dim.minmus;

import java.util.Random;

import com.space_m.dim.CelestialWorld;
import com.space_m.dim.legacy.Legacy;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code com.hbm.dim.minmus.biome.BiomeGenMinmusHills} ("Minmus Hills", Biom {@code space_m:minmus_hills}):
 * Hoehe (0.325, 0.08), Regolith-Decke ueber {@code minmus_stone} (Oberflaechenfenster y 59-74).
 */
public class BiomeGenMinmusHills extends BiomeGenBaseMinmus {

    public BiomeGenMinmusHills(int id) {
        super(id, "minmus_hills");
        this.setBiomeName("Minmus Hills");

        this.setHeight(new Height(0.325F, 0.08F));
        this.setTopBlock(() -> MinmusBlocks.MINMUS_REGOLITH.get().defaultBlockState());
        this.setFillerBlock(() -> MinmusBlocks.MINMUS_REGOLITH.get().defaultBlockState());
    }

    @Override
    public void genTerrainBlocks(CelestialWorld world, Random rand, BlockState[] blocks, int x, int z, double noise) {
        BlockState stone = MinmusBlocks.MINMUS_STONE.get().defaultBlockState();
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
                            } else if (l1 >= 59 && l1 <= 74) {
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

                            if (l1 >= 62) {
                                blocks[i2] = block;
                            } else if (l1 < 56 - l) {
                                block = null;
                                block1 = stone;
                                blocks[i2] = Blocks.GRAVEL.defaultBlockState();
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
