package com.space_m.dim.moho.biome;

import java.util.Random;

import com.space_m.dim.BiomeGenBaseCelestial;
import com.space_m.dim.CelestialWorld;
import com.space_m.dim.legacy.Legacy;
import com.space_m.dim.moho.MohoBlocks;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code com.hbm.dim.moho.biome.BiomeGenBaseMoho}: kein Regen, Temperatur 1.0, Niederschlag 0, Dekorator
 * {@code BiomeDecoratorCelestial(moho_stone)} mit {@code lavaCount = 50} (Biom-JSON {@code space_m:moho/decorator}),
 * Oberflaeche mit dickerer Schicht ({@code noise / 8 + 8}), tief Basalt.
 *
 * <p>Biom-IDs = Standardwerte aus {@code SpaceConfig} ({@code mohoBiome} 101, {@code mohoBasaltBiome} 100,
 * {@code mohoLavaBiome} 99, {@code mohoPlateauBiome} 98).
 */
public abstract class BiomeGenBaseMoho extends BiomeGenBaseCelestial {

    public static final int MOHO_BIOME = 101;
    public static final int MOHO_BASALT_BIOME = 100;
    public static final int MOHO_LAVA_BIOME = 99;
    public static final int MOHO_PLATEAU_BIOME = 98;

    public static final BiomeGenBaseCelestial mohoCrag = new BiomeGenMohoCrag(MOHO_BIOME);
    public static final BiomeGenBaseCelestial mohoBasalt = new BiomeGenMohoBasalt(MOHO_BASALT_BIOME);
    public static final BiomeGenBaseCelestial mohoLavaSea = new BiomeGenMohoLavaSea(MOHO_LAVA_BIOME);
    public static final BiomeGenBaseCelestial mohoPlateau = new BiomeGenMohoPlateau(MOHO_PLATEAU_BIOME);

    public BiomeGenBaseMoho(int id, String path) {
        super(id, path);
        this.setDisableRain();
        this.setTemperatureRainfall(1.0F, 0.0F);
    }

    /** Laedt die Biom-Objekte (Registrierung der IDs vor der ersten GenLayer-Abfrage). */
    public static void init() {
    }

    protected static BlockState mohoStone() {
        return MohoBlocks.MOHO_STONE.get().defaultBlockState();
    }

    protected static BlockState basalt() {
        return com.hbm_m.block.ModBlocks.BASALT.get().defaultBlockState();
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
                        } else if (k > 0) {
                            --k;
                            blocks[i] = block1;

                            if (k == 0 && Legacy.is(block1, Blocks.SAND)) {
                                k = rand.nextInt(4) + Math.max(0, by - 63);
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
