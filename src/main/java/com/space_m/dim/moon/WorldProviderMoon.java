package com.space_m.dim.moon;

import com.space_m.dim.CelestialGenerators;
import com.space_m.dim.WorldChunkManagerCelestial;

/**
 * 1:1 {@code com.hbm.dim.moon.WorldProviderMoon} (Dimension "Mun"):
 * <ul>
 *   <li>{@code registerWorldChunkManager} = {@code WorldChunkManagerHell(BiomeGenMoon.biome, 0.0F)} -> ein Biom.</li>
 *   <li>{@code createChunkGenerator} = {@link ChunkProviderMoon}.</li>
 *   <li>{@code getStone} = {@code moon_rock}.</li>
 *   <li>{@code getDimensionName} "Mun" = Dimension {@code space_m:mun} ({@code data/space_m/dimension/mun.json}).</li>
 * </ul>
 * Licht, Hoehe, Himmel ({@code effects space_m:mun}) stehen in {@code dimension_type/mun.json}; Himmel, Zeit und
 * Atmosphaere (Rest von {@code WorldProviderCelestial}) gehoeren zu R1-C/R1-D/R2.
 */
public final class WorldProviderMoon {

    private WorldProviderMoon() {}

    public static void init() {
        MunBlocks.init();
        CelestialGenerators.register(new CelestialGenerators.BodyGen(MunBlocks.BODY,
                seed -> WorldChunkManagerCelestial.fixed(BiomeGenMoon.biome, seed),
                ChunkProviderMoon::new,
                () -> MunBlocks.MOON_ROCK.get().defaultBlockState(),
                64));
        WorldGeneratorMoon.init();
    }
}
