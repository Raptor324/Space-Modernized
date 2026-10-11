package com.space_m.dim.ike;

import java.util.Random;

import com.space_m.dim.BiomeGenBaseCelestial;
import com.space_m.dim.CelestialWorld;
import com.space_m.dim.legacy.Legacy;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code com.hbm.dim.Ike.BiomeGenIke} ("Ike", ID {@code ikeBiome} 93): Hoehe (0.325, 0.05), dicke
 * Regolith-Decke ({@code noise / 8 + 8}) ueber {@code ike_stone}. Temperatur/Niederschlag bleiben die
 * 1.7.10-Standardwerte 0.5/0.5 (Regen erlaubt).
 *
 * <p>{@code theBiomeDecorator = BiomeDecoratorCelestial(ike_stone)} mit {@code lakeChancePerChunk = 8},
 * {@code lakeBlock = bromine_block}, {@code generateLakes = false}: keine Lava-Quellen, aber Brom-Seen (1 in 8
 * Chunks) - im Biom-JSON {@code data/space_m/worldgen/biome/ike.json} als {@code space_m:celestial_decorator}.
 */
public class BiomeGenIke extends BiomeGenBaseCelestial {

    /** {@code SpaceConfig.ikeBiome} (Standard 93). */
    public static final int IKE_BIOME_ID = 93;

    public static final BiomeGenIke biome = new BiomeGenIke(IKE_BIOME_ID);

    public BiomeGenIke(int id) {
        super(id, "ike");
        this.setBiomeName("Ike");

        this.setHeight(new Height(0.325F, 0.05F));

        this.setTopBlock(() -> IkeBlocks.IKE_REGOLITH.get().defaultBlockState());
        this.setFillerBlock(() -> IkeBlocks.IKE_REGOLITH.get().defaultBlockState()); // thiccer regolith due to uhhhhhh...................
    }

    @Override
    public void genTerrainBlocks(CelestialWorld world, Random rand, BlockState[] blocks, int x, int z, double noise) {
        BlockState stone = IkeBlocks.IKE_STONE.get().defaultBlockState();
        BlockState block = this.topBlock();
        BlockState block1 = this.fillerBlock();
        int k = -1;
        int l = (int) (noise / 8.0D + 8.0D + rand.nextDouble() * 0.50D);
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
