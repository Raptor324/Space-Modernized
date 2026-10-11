package com.space_m.dim.tekto.biome;

import java.util.Random;
import java.util.function.Supplier;

import com.space_m.dim.BiomeGenBaseCelestial;
import com.space_m.dim.CelestialWorld;
import com.space_m.dim.legacy.Legacy;
import com.space_m.dim.tekto.TektoBlocks;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code com.hbm.dim.tekto.biome.BiomeGenBaseTekto}: Temperatur 1.0, Niederschlag 0.5, Vinylsand, Wasserfarbe
 * {@code 0x5b009a}; Dekorator {@code BiomeDecoratorCelestial(basalt)} mit {@code rubberPlantsPerChunk 64},
 * {@code generateLakes false} (Feature {@code space_m:rubber_plants} im Biom-JSON). {@code BiomeDictionary}
 * HOT/DRY/DEAD/SPOOKY entfaellt.
 *
 * <p>Die fuenf Unterklassen haben im Original dieselbe Oberflaechenmethode ({@link #genTerrainBlocks}) mit
 * eigenem Gestein/Zweitblock - hier als Parameter {@link #rock}/{@link #alt}.
 */
public abstract class BiomeGenBaseTekto extends BiomeGenBaseCelestial {

    public static final BiomeGenBaseTekto polyvinylPlains = new BiomeGenPolyvinylPlains(BiomeGenPolyvinylPlains.ID);
    public static final BiomeGenBaseTekto halogenHills = new BiomeGenHalogenHills(BiomeGenHalogenHills.ID);
    public static final BiomeGenBaseTekto tetrachloricRiver = new BiomeGenTetrachloricRiver(BiomeGenTetrachloricRiver.ID);
    public static final BiomeGenBaseTekto forest = new BiomeGenForest(BiomeGenForest.ID);
    public static final BiomeGenBaseTekto vinylsands = new BiomeGenVinylSands(BiomeGenVinylSands.ID);

    /** {@code waterColorMultiplier}. */
    public int waterColorMultiplier;
    /** {@code BiomeDecoratorCelestial.rubberPlantsPerChunk}. */
    public int rubberPlantsPerChunk;

    /** Gestein, das {@code genTerrainBlocks} ersetzt ({@code ModBlocks.basalt}, Fluss: {@code duna_rock}). */
    protected Supplier<BlockState> rock = () -> com.hbm_m.block.ModBlocks.BASALT.get().defaultBlockState();
    /** Zweitblock unter y 62 (40 %: {@code basalt_smooth}, Fluss: {@code duna_sands}). */
    protected Supplier<BlockState> alt = () -> com.hbm_m.block.ModBlocks.BASALT_SMOOTH.get().defaultBlockState();

    protected BiomeGenBaseTekto(int id, String path) {
        super(id, path);
        this.waterColorMultiplier = 0x5b009a;

        setTemperatureRainfall(1.0F, 0.5F);

        // BiomeDecoratorCelestial decorator = new BiomeDecoratorCelestial(ModBlocks.basalt);
        this.rubberPlantsPerChunk = 64;
        // theBiomeDecorator.generateLakes = false;

        this.setTopBlock(() -> TektoBlocks.VINYL_SAND.get().defaultBlockState());
        this.setFillerBlock(() -> TektoBlocks.VINYL_SAND.get().defaultBlockState());
    }

    /** Block aus einer anderen Koerper-Runde (z.B. {@code space_m:duna_rock}); fehlt er, Luft (passt nie). */
    protected static BlockState lookup(String id) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.tryParse(id)).defaultBlockState();
    }

    public static void init() {
    }

    @Override
    public void genTerrainBlocks(CelestialWorld world, Random rand, BlockState[] blocks, int x, int z, double noise) {
        BlockState stone = rock.get();
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
                    if (!stone.isAir() && Legacy.is(block2, stone)) {
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
                            } else if (l1 < 62) {
                                block = null;
                                block1 = stone;
                                if (Math.random() > 0.4) {
                                    blocks[i2] = stone;
                                } else {
                                    blocks[i2] = alt.get();
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
