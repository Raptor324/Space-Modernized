package com.space_m.dim.moho.biome;

import java.util.Random;

import com.space_m.dim.CelestialWorld;
import com.space_m.dim.legacy.Legacy;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code com.hbm.dim.moho.biome.BiomeGenMohoBasalt} ("Moho Basalt Deltas", Biom {@code space_m:moho_basalt}):
 * Hoehe (0, 0.224), Basaltoberflaeche mit Hoehenlinien - ausserhalb der Rauschbaender werden Basaltsaeulen
 * (bis 8 hoch, {@code rand^4 * 8}) auf die Oberflaeche gesetzt.
 */
public class BiomeGenMohoBasalt extends BiomeGenBaseMoho {

    public BiomeGenMohoBasalt(int id) {
        super(id, "moho_basalt");
        this.setBiomeName("Moho Basalt Deltas");

        this.setHeight(new Height(0, 0.224F));

        this.setTopBlock(() -> com.hbm_m.block.ModBlocks.BASALT.get().defaultBlockState());
        this.setFillerBlock(() -> com.hbm_m.block.ModBlocks.BASALT.get().defaultBlockState());
    }

    @Override
    public void genTerrainBlocks(CelestialWorld world, Random rand, BlockState[] blocks, int x, int z, double noise) {
        BlockState stone = mohoStone();
        BlockState block = this.topBlock();
        BlockState block1 = this.fillerBlock();
        int k = -1;
        int l = (int) (noise / 8.0D + 8.0D + rand.nextDouble() * 0.50D);
        int bx = x & 15;
        int bz = z & 15;
        int s = blocks.length / 256;

        for (int by = 255; by >= 0; --by) {
            int i = (bz * 16 + bx) * s + by;

            boolean contour = (noise > -0.25D && noise < 1.0D) || (noise > 1.5D && noise < 2.0D) || (noise > 2.5D && noise < 3.0D);
            double offset = contour ? 0 : Math.pow(rand.nextDouble(), 4) * 8;

            if (by <= 0 + rand.nextInt(5)) {
                blocks[i] = Blocks.BEDROCK.defaultBlockState();
            } else {
                BlockState block2 = blocks[i];

                if (block2 != null && !block2.isAir()) {
                    if (Legacy.is(block2, stone)) {
                        if (k == -1) {
                            if (l <= 0) {
                                block = null;
                                block1 = stone;
                            } else if (by >= 59 && by <= 64) {
                                block = this.topBlock();
                                block1 = this.fillerBlock();
                            }

                            if (by < 63 && (block == null || block.isAir())) {
                                if (this.getFloatTemperature(x, by, z) < 0.15F) {
                                    block = this.topBlock();
                                } else {
                                    block = this.topBlock();
                                }
                            }

                            k = l;

                            if (by >= 62) {
                                blocks[i] = block;
                            } else if (by < 56 - l) {
                                block = null;
                                block1 = stone;
                                blocks[i] = basalt();
                            } else {
                                blocks[i] = block1;
                            }

                            // Original ohne Grenzpruefung; oberhalb der Spalte (by + oy > 255) gaebe es dort einen Absturz
                            for (int oy = 0; oy < offset && by + oy < 256; oy++) {
                                blocks[i + oy] = this.topBlock();
                            }
                        } else if (k > 0) {
                            --k;
                            blocks[i] = block1;
                        }
                    }
                } else {
                    k = -1;
                }
            }
        }
    }
}
