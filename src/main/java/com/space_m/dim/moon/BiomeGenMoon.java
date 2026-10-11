package com.space_m.dim.moon;

import java.util.Random;

import com.space_m.dim.BiomeGenBaseCelestial;
import com.space_m.dim.CelestialWorld;
import com.space_m.dim.legacy.Legacy;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code com.hbm.dim.moon.BiomeGenMoon} ("Mun", ID {@code moonBiome} 125): Temperatur -1, Niederschlag 0, kein
 * Regen, Hoehe (0.125, 0.05), Oberflaeche {@code moon_turf} ueber {@code moon_rock}.
 *
 * <p>Im Biom-JSON {@code data/space_m/worldgen/biome/mun.json}: Temperatur/Niederschlag, {@code getSpawningChance}
 * 0.008 = {@code creature_spawn_probability}, Mun-Kuh ({@code EntityMoonCow} 10/4/4, {@code enableMunCows}) folgt
 * mit dem Mob (R8). {@code theBiomeDecorator} ist der Vanilla-Dekorator mit {@code generateLakes = false} - er
 * erzeugt auf Mondgestein nichts (Vanilla-Erze/Pflanzen brauchen Stein/Gras/Wasser), daher keine Dekorator-Features.
 */
public class BiomeGenMoon extends BiomeGenBaseCelestial {

    /** {@code SpaceConfig.moonBiome} (Standard 125). */
    public static final int MOON_BIOME_ID = 125;

    public static final BiomeGenMoon biome = new BiomeGenMoon(MOON_BIOME_ID);

    public BiomeGenMoon(int id) {
        super(id, "mun");
        this.setBiomeName("Mun");
        this.setDisableRain();
        this.setTemperatureRainfall(-1.0F, 0.0F);

        this.setHeight(new Height(0.125F, 0.05F));

        this.setTopBlock(() -> BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("hbm_m", "moon_turf")).defaultBlockState());
        this.setFillerBlock(() -> MunBlocks.MOON_ROCK.get().defaultBlockState());
    }

    @Override
    public void genTerrainBlocks(CelestialWorld world, Random rand, BlockState[] blocks, int x, int z, double noise) {
        // boolean flag = true;
        BlockState moonRock = MunBlocks.MOON_ROCK.get().defaultBlockState();
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
                    if (Legacy.is(block2, moonRock)) {
                        if (k == -1) {
                            if (l <= 0) {
                                block = null;
                                block1 = moonRock;
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
                                block1 = moonRock;
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
