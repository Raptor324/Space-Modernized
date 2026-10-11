package com.space_m.dim.duna.biome;

import java.util.Random;

import com.space_m.dim.CelestialWorld;
import com.space_m.dim.duna.DunaBlocks;
import com.space_m.dim.legacy.Legacy;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code com.hbm.dim.duna.biome.BiomeGenDunaLowlands} ("Dunaian Lowland Plains", ID 121): Hoehe (-0.6, 0.01),
 * unter y 62 ein Gemisch aus {@code duna_cobble} (tiefer haeufiger), {@code duna_rock} und {@code duna_sands}
 * (aus dem Chunk-Zufall). Nur hier graben die Basalt-Hoehlen ({@code ChunkProviderDuna.caveGenV2}).
 */
public class BiomeGenDunaLowlands extends BiomeGenBaseDuna {

    public static final Height height = new Height(-0.6F, 0.01F);

    public BiomeGenDunaLowlands(int id) {
        super(id, "duna_lowlands");
        this.setBiomeName("Dunaian Lowland Plains");

        this.setHeight(height);
    }

    @Override
    public void genTerrainBlocks(CelestialWorld world, Random rand, BlockState[] blocks, int x, int z, double noise) {
        Block dunaRock = DunaBlocks.DUNA_ROCK.get();
        BlockState rock = dunaRock.defaultBlockState();
        BlockState cobble = DunaBlocks.DUNA_COBBLE.get().defaultBlockState();
        BlockState sands = DunaBlocks.DUNA_SANDS.get().defaultBlockState();
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
                    if (Legacy.is(block2, dunaRock)) {
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
                                block1 = this.topBlock();
                                float randValue = rand.nextFloat();
                                if (l1 < 55 && randValue > 0.75) {
                                    blocks[i2] = cobble;
                                } else if (l1 < 54 && randValue > 0.5) {
                                    blocks[i2] = cobble;
                                } else if (randValue > 0.4) {
                                    blocks[i2] = rock;
                                } else {
                                    blocks[i2] = sands;
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
