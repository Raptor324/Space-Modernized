package com.space_m.dim.duna;

import com.space_m.dim.CelestialGenerators;
import com.space_m.dim.WorldChunkManagerCelestial;
import com.space_m.dim.WorldChunkManagerCelestial.BiomeGenLayers;
import com.space_m.dim.duna.biome.BiomeGenBaseDuna;
import com.space_m.dim.duna.genlayer.GenLayerDiversifyDuna;
import com.space_m.dim.duna.genlayer.GenLayerDunaBiomes;
import com.space_m.dim.duna.genlayer.GenLayerDunaLowlands;
import com.space_m.dim.legacy.GenLayer;
import com.space_m.dim.legacy.GenLayerFuzzyZoom;
import com.space_m.dim.legacy.GenLayerRiver;
import com.space_m.dim.legacy.GenLayerRiverMix;
import com.space_m.dim.legacy.GenLayerSmooth;
import com.space_m.dim.legacy.GenLayerVoronoiZoom;
import com.space_m.dim.legacy.GenLayerZoom;
import com.space_m.dim.legacy.LegacyBiomes;

/**
 * 1:1 {@code com.hbm.dim.duna.WorldProviderDuna} (Dimension "Duna", {@code space_m:duna}):
 * <ul>
 *   <li>{@code registerWorldChunkManager}: GenLayer-Kette {@link #createBiomeGenerators} (5 Biome, Flussmischung
 *       nur in der groben Schicht -> Rinnen im Gelaende).</li>
 *   <li>{@code createChunkGenerator} = {@link ChunkProviderDuna}, {@code getStone} = {@code duna_rock}.</li>
 *   <li>Staubsturm ({@code updateWeather}, NBT, {@code isDaytime}, {@code fogDensity}): {@link DunaDustStorm},
 *       {@code DunaDayMixin}, {@code CelestialSkyProvider.Duna}; {@code getHorizon} 52 im Himmel.</li>
 *   <li>{@code getRespawnDimension} (Weltart "The Martian"): {@link MartianWorld}.</li>
 * </ul>
 */
public final class WorldProviderDuna {

    private WorldProviderDuna() {}

    public static void init() {
        DunaBlocks.init();
        LegacyBiomes.init();
        BiomeGenBaseDuna.init();
        CelestialGenerators.register(new CelestialGenerators.BodyGen(DunaBlocks.BODY,
                seed -> new WorldChunkManagerCelestial(createBiomeGenerators(seed), seed, BiomeGenBaseDuna.dunaPlains),
                ChunkProviderDuna::new,
                () -> DunaBlocks.DUNA_ROCK.get().defaultBlockState(),
                63));
        WorldGeneratorDuna.init();
        DunaDustStorm.init();
        MartianWorld.init();
        // TileEntityMachinePumpBase.validBlocks.add(duna_sands)
        com.hbm_m.api.space.SpaceResources.registerPumpGround(DunaBlocks.DUNA_SANDS);
    }

    public static BiomeGenLayers createBiomeGenerators(long seed) {
        GenLayer biomes = new GenLayerDunaBiomes(seed);

        biomes = new GenLayerFuzzyZoom(2000L, biomes);
        biomes = new GenLayerZoom(2001L, biomes);
        biomes = new GenLayerDiversifyDuna(1000L, biomes);
        biomes = new GenLayerZoom(1000L, biomes);
        biomes = new GenLayerDiversifyDuna(1001L, biomes);
        biomes = new GenLayerZoom(1001L, biomes);
        biomes = new GenLayerDunaLowlands(1300L, biomes);
        biomes = new GenLayerZoom(1003L, biomes);
        biomes = new GenLayerSmooth(700L, biomes);
        biomes = new GenLayerZoom(1005L, biomes);
        biomes = new GenLayerSmooth(703L, biomes);
        biomes = new GenLayerFuzzyZoom(1000L, biomes);
        biomes = new GenLayerSmooth(705L, biomes);
        biomes = new GenLayerFuzzyZoom(1001L, biomes);
        biomes = new GenLayerZoom(1006L, biomes);

        GenLayer genlayerVoronoiZoom = new GenLayerVoronoiZoom(10L, biomes);

        GenLayer genlayerRiverZoom = new GenLayerZoom(1000L, biomes);
        GenLayer genlayerRiver = new GenLayerRiver(1004L, genlayerRiverZoom); // Your custom river layer
        genlayerRiver = new GenLayerZoom(105L, genlayerRiver);

        GenLayer genlayerRiverMix = new GenLayerRiverMix(100L, biomes, genlayerRiver);

        return new BiomeGenLayers(genlayerRiverMix, genlayerVoronoiZoom, seed);
    }
}
