package com.space_m.dim.thatmo;

import java.util.Random;

import com.space_m.dim.BiomeGenBaseCelestial;
import com.space_m.dim.CelestialWorld;
import com.space_m.dim.legacy.Legacy;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code com.hbm.dim.thatmo.BiomeGenThatmo} ("Thatmo", {@code thatmoBiome} 87): Temperatur -1, kein Regen,
 * Hoehe (0.125, 0.05), Schnee ueber Basalt; unter y 62 zufaellig Basalt/geloeschtes Sellafit ({@code Math.random()}
 * wie im Original). Spawns Tankbot 6/1/1, War-Behemoth 2/1/1, Mun-Kuh 10/1/1 folgen mit den Mobs (R8).
 * Dekorator: Vanilla-{@code BiomeDecorator} mit {@code generateLakes = false} - auf Thatmo bleiben davon nur Pilze
 * in dunklen Hoehlen ({@code space_m:vanilla_mushrooms}).
 */
public class BiomeGenThatmo extends BiomeGenBaseCelestial {

    /** {@code SpaceConfig.thatmoBiome}. */
    public static final int ID = 87;

    public static final Height height = new Height(0.125F, 0.05F);

    public static final BiomeGenThatmo biome = new BiomeGenThatmo(ID);

    public BiomeGenThatmo(int id) {
        super(id, "thatmo");
        this.setBiomeName("Thatmo");
        this.setDisableRain();
        this.setTemperatureRainfall(-1.0F, 0.0F);

        this.setHeight(height);

        this.setTopBlock(Blocks.SNOW_BLOCK::defaultBlockState);
        this.setFillerBlock(Blocks.SNOW_BLOCK::defaultBlockState);
    }

    @Override
    public void genTerrainBlocks(CelestialWorld world, Random rand, BlockState[] blocks, int x, int z, double noise) {
        BlockState basalt = com.hbm_m.block.ModBlocks.BASALT.get().defaultBlockState();
        BlockState slaked = com.hbm_m.block.ModBlocks.SELLAFIELD_SLAKED.get().defaultBlockState();
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
                    if (Legacy.is(block2, basalt)) {
                        if (k == -1) {
                            if (l <= 0) {
                                block = null;
                                block1 = basalt;
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
                                block1 = basalt;
                                if (Math.random() > 0.4) {
                                    blocks[i2] = basalt;
                                } else {
                                    blocks[i2] = slaked;
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
