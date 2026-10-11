package com.space_m.dim.dres;

import com.space_m.dim.CelestialGenerators;
import com.space_m.dim.WorldChunkManagerCelestial;
import com.space_m.dim.WorldChunkManagerCelestial.BiomeGenLayers;
import com.space_m.dim.dres.biome.BiomeGenBaseDres;
import com.space_m.dim.dres.genlayer.GenLayerDiversifyDres;
import com.space_m.dim.dres.genlayer.GenLayerDresBasins;
import com.space_m.dim.dres.genlayer.GenLayerDresBiomes;
import com.space_m.dim.dres.genlayer.GenLayerDresPlains;
import com.space_m.dim.legacy.GenLayer;
import com.space_m.dim.legacy.GenLayerFuzzyZoom;
import com.space_m.dim.legacy.GenLayerSmooth;
import com.space_m.dim.legacy.GenLayerVoronoiZoom;
import com.space_m.dim.legacy.GenLayerZoom;

/**
 * 1:1 {@code com.hbm.dim.dres.WorldProviderDres} (Dimension "Dres", {@code space_m:dres}):
 * {@code registerWorldChunkManager} = GenLayer-Kette {@link #createBiomeGenerators} (Ebenen + Becken),
 * {@code createChunkGenerator} = {@link ChunkProviderDres}, {@code getStone} = {@code dres_rock}.
 * {@code getBedrockAcid} (Salpetersaeure 500 mB) -> Bedrock-Erze (R8). Himmel/Zeit/Atmosphaere: R1-C/R1-D/R2.
 */
public final class WorldProviderDres {

    private WorldProviderDres() {}

    public static void init() {
        DresBlocks.init();
        BiomeGenBaseDres.init();
        CelestialGenerators.register(new CelestialGenerators.BodyGen(DresBlocks.BODY,
                seed -> new WorldChunkManagerCelestial(createBiomeGenerators(seed), seed, BiomeGenBaseDres.dresPlains),
                ChunkProviderDres::new,
                () -> DresBlocks.DRES_ROCK.get().defaultBlockState(),
                63));
        WorldGeneratorDres.init();
    }

    public static BiomeGenLayers createBiomeGenerators(long seed) {
        GenLayer biomes = new GenLayerDresBiomes(seed);

        biomes = new GenLayerFuzzyZoom(2000L, biomes);
        biomes = new GenLayerZoom(2001L, biomes);
        biomes = new GenLayerDiversifyDres(1000L, biomes);
        biomes = new GenLayerZoom(1000L, biomes);
        biomes = new GenLayerDiversifyDres(1001L, biomes);
        biomes = new GenLayerZoom(1001L, biomes);
        biomes = new GenLayerDresBasins(3000L, biomes);
        biomes = new GenLayerZoom(1003L, biomes);
        biomes = new GenLayerSmooth(700L, biomes);
        biomes = new GenLayerDresPlains(200L, biomes);

        biomes = new GenLayerZoom(1006L, biomes);

        GenLayer genLayerVeronoiZoom = new GenLayerVoronoiZoom(10L, biomes);

        return new BiomeGenLayers(biomes, genLayerVeronoiZoom, seed);
    }
}
