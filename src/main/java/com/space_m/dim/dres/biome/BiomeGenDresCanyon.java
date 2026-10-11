package com.space_m.dim.dres.biome;

import java.util.Random;

import com.space_m.dim.CelestialWorld;
import com.space_m.dim.dres.DresBlocks;
import com.space_m.dim.legacy.Legacy;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code com.hbm.dim.dres.biome.BiomeGenDresCanyon} ("Dres Large Basins", ID 111): Hoehe (-1, 0.34), tiefe
 * Becken; unter y 62 Sellafit Metadatum 8 (60 %) oder 7 ({@code l1 < 68} ist dort immer wahr). Oberflaeche und
 * Fuellung Metadatum 0. Metadaten/Varianten siehe {@link BiomeGenBaseDres#slaked}; {@code Math.random()} wie im
 * Original.
 */
public class BiomeGenDresCanyon extends BiomeGenBaseDres {

    public BiomeGenDresCanyon(int id) {
        super(id, "dres_basins");
        this.setBiomeName("Dres Large Basins");

        this.setHeight(new Height(-1F, 0.34F));
    }

    @Override
    public void genTerrainBlocks(CelestialWorld world, Random rand, BlockState[] blocks, int x, int z, double noise) {
        Block dresRock = DresBlocks.DRES_ROCK.get();
        // block/block1 = Sellafit Metadatum 0 (topBlock/fillerBlock) oder null; der Zustand haengt an der Position
        boolean block = true;
        boolean block1 = true;
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
                    if (Legacy.is(block2, dresRock)) {
                        if (k == -1) {
                            if (l <= 0) {
                                block = false;
                                block1 = true;
                            } else if (l1 >= 59 && l1 <= 64) {
                                block = true;
                                block1 = true;
                            }

                            if (l1 < 63 && !block) {
                                if (this.getFloatTemperature(x, l1, z) < 0.15F) {
                                    block = true;
                                } else {
                                    block = true;
                                }
                            }

                            k = l;

                            if (l1 >= 62) {
                                blocks[i2] = block ? slaked(0, x, l1, z) : null;
                            } else if (l1 < 68) {
                                block = false;
                                block1 = true;
                                if (Math.random() > 0.4) {
                                    blocks[i2] = slaked(8, x, l1, z);
                                } else {
                                    blocks[i2] = slaked(7, x, l1, z);
                                }
                            } else {
                                blocks[i2] = block1 ? slaked(0, x, l1, z) : null;
                            }
                        } else if (k > 0) {
                            --k;
                            blocks[i2] = block1 ? slaked(0, x, l1, z) : null;
                            // Original: Sand -> Sandstein (block1 ist hier nie Sand)
                        }
                    }
                } else {
                    k = -1;
                }
            }
        }
    }
}
