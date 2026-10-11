package com.space_m.dim.eve.biome;

import java.util.Random;

import com.space_m.dim.CelestialWorld;
import com.space_m.dim.legacy.Legacy;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code com.hbm.dim.eve.biome.BiomeGenEveRiver} ("Explodium River", Biom {@code space_m:eve_river}): Hoehe (-0.7, 0.0); vergleicht wie der Ozean 1:1 mit {@code duna_rock} (nur Grundgestein).
 */
public class BiomeGenEveRiver extends BiomeGenBaseEve {

    public BiomeGenEveRiver(int id) {
        super(id, "eve_river");
        this.setBiomeName("Explodium River");

        this.setHeight(new Height(-0.7F, 0.0F));
        this.setTopBlock(() -> com.space_m.dim.eve.EveBlocks.EVE_SILT.get().defaultBlockState());
        this.setFillerBlock(() -> com.space_m.dim.eve.EveBlocks.EVE_SILT.get().defaultBlockState());
    }

    @Override
    public void genTerrainBlocks(CelestialWorld world, Random rand, BlockState[] blocks, int x, int z, double noise) {
        BlockState rock = dunaRock();
        Random math = columnRandom(world, x, z); // Math.random()
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
                    if (Legacy.is(block2, rock)) {
                        if (k == -1) {
                            if (l <= 0) {
                                block = null;
                                block1 = rock;
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

                            if (l1 >= 62) {
                                blocks[i2] = block;
                            } else if (l1 < 62) {
                                block = null;
                                block1 = rock;
                                if (math.nextDouble() > 0.4) {
                                    blocks[i2] = rock;
                                } else {
                                    blocks[i2] = dunaSands();
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
