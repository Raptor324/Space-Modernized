package com.space_m.dim.minmus;

import com.space_m.dim.CelestialGenerators;
import com.space_m.dim.GenLayerDiversify;
import com.space_m.dim.WorldChunkManagerCelestial;
import com.space_m.dim.WorldChunkManagerCelestial.BiomeGenLayers;
import com.space_m.dim.legacy.GenLayer;
import com.space_m.dim.legacy.GenLayerFuzzyZoom;
import com.space_m.dim.legacy.GenLayerSmooth;
import com.space_m.dim.legacy.GenLayerVoronoiZoom;
import com.space_m.dim.legacy.GenLayerZoom;

/**
 * 1:1 {@code com.hbm.dim.minmus.WorldProviderMinmus} (Dimension "Minmus"):
 * <ul>
 *   <li>{@code registerWorldChunkManager} = {@code WorldChunkManagerCelestial(createBiomeGenerators(seed))}:
 *       GenLayer-Kette Becken/Huegel ({@link #createBiomeGenerators}, Seeds und Reihenfolge 1:1).</li>
 *   <li>{@code createChunkGenerator} = {@link ChunkProviderMinmus}.</li>
 *   <li>{@code getStone} = {@code minmus_stone}.</li>
 *   <li>{@code getDimensionName} "Minmus" = Dimension {@code space_m:minmus} ({@code data/space_m/dimension/minmus.json}).</li>
 * </ul>
 * Licht, Hoehe, Himmel ({@code effects space_m:minmus}) stehen in {@code dimension_type/minmus.json}; Himmel, Zeit
 * und Atmosphaere (Rest von {@code WorldProviderCelestial}) gehoeren zu R1-C/R1-D/R2.
 *
 * <p>Zusaetzlich aus anderen Original-Stellen: Pumpenboden {@code minmus_smooth}
 * ({@code TileEntityMachinePumpBase.validBlocks}, ueber {@code SpaceResources.registerPumpGround}).
 */
public final class WorldProviderMinmus {

    private WorldProviderMinmus() {}

    public static void init() {
        MinmusBlocks.init();
        CelestialGenerators.register(new CelestialGenerators.BodyGen(MinmusBlocks.BODY,
                seed -> new WorldChunkManagerCelestial(createBiomeGenerators(seed), seed, BiomeGenBaseMinmus.minmusPlains),
                ChunkProviderMinmus::new,
                () -> MinmusBlocks.MINMUS_STONE.get().defaultBlockState(),
                63));
        WorldGeneratorMinmus.init();
        com.hbm_m.api.space.SpaceResources.registerPumpGround(() -> MinmusBlocks.MINMUS_SMOOTH.get());
    }

    /** Original {@code createBiomeGenerators(seed)}. */
    public static BiomeGenLayers createBiomeGenerators(long seed) {
        GenLayer biomes = new GenLayerMinmusBiomes(seed);

        biomes = new GenLayerFuzzyZoom(2000L, biomes);
        biomes = new GenLayerZoom(2001L, biomes);
        biomes = new GenLayerDiversify(1000L, biomes, 2, BiomeGenBaseMinmus.minmusCanyon, BiomeGenBaseMinmus.minmusPlains);
        biomes = new GenLayerZoom(1000L, biomes);
        biomes = new GenLayerDiversify(1001L, biomes, 2, BiomeGenBaseMinmus.minmusCanyon, BiomeGenBaseMinmus.minmusPlains);
        biomes = new GenLayerZoom(1001L, biomes);
        biomes = new GenLayerMinmusBasins(3000L, biomes);
        biomes = new GenLayerZoom(1003L, biomes);
        biomes = new GenLayerSmooth(700L, biomes);
        biomes = new GenLayerMinmusPlains(200L, biomes);
        biomes = new GenLayerZoom(1006L, biomes);

        GenLayer genLayerVoronoiZoom = new GenLayerVoronoiZoom(10L, biomes);

        return new BiomeGenLayers(biomes, genLayerVoronoiZoom, seed);
    }

}
