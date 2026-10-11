package com.space_m.dim.eve;

import com.space_m.dim.CelestialGenerators;
import com.space_m.dim.WorldChunkManagerCelestial;
import com.space_m.dim.WorldChunkManagerCelestial.BiomeGenLayers;
import com.space_m.dim.eve.biome.BiomeGenBaseEve;
import com.space_m.dim.eve.genlayer.GenLayerEveBiomes;
import com.space_m.dim.eve.genlayer.GenLayerEveRiverMix;
import com.space_m.dim.legacy.GenLayer;
import com.space_m.dim.legacy.GenLayerRiver;
import com.space_m.dim.legacy.GenLayerSmooth;
import com.space_m.dim.legacy.GenLayerVoronoiZoom;
import com.space_m.dim.legacy.GenLayerZoom;

/**
 * 1:1 {@code com.hbm.dim.eve.WorldProviderEve} (Dimension "Eve"):
 * <ul>
 *   <li>{@code registerWorldChunkManager} = GenLayer-Kette {@link #createBiomeGenerators} (Seeds/Reihenfolge 1:1).</li>
 *   <li>{@code createChunkGenerator} = {@link ChunkProviderEve} (Meeresspiegel 63).</li>
 *   <li>{@code getStone} = {@code eve_rock}; {@code getBedrockAcid} = KMnO4 500 mB (Bedrock-Erze, R8).</li>
 *   <li>{@code updateWeather}/{@code getSkyColor}/{@code getSunBrightness} (Blitzschein + {@code misc.rumble}) sind
 *       clientseitig und liegen im Himmel (R1-C, {@code CelestialSkyProvider}); das Wetter selbst ist das
 *       allgemeine Koerperwetter ({@code CelestialWeather}, CBT_Weather) - die Eve ueberschreibt serverseitig nichts.</li>
 * </ul>
 * Zusaetzlich aus anderen Original-Stellen: Pumpenboden {@code eve_silt}/{@code eve_rock}
 * ({@code TileEntityMachinePumpBase.validBlocks}).
 */
public final class WorldProviderEve {

    private WorldProviderEve() {}

    public static void init() {
        EveBlocks.init();
        BiomeGenBaseEve.init();
        CelestialGenerators.register(new CelestialGenerators.BodyGen(EveBlocks.BODY,
                seed -> new WorldChunkManagerCelestial(createBiomeGenerators(seed), seed, BiomeGenBaseEve.evePlains),
                ChunkProviderEve::new,
                () -> EveBlocks.EVE_ROCK.get().defaultBlockState(),
                63));
        WorldGeneratorEve.init();
        com.hbm_m.api.space.SpaceResources.registerPumpGround(() -> EveBlocks.EVE_SILT.get());
        com.hbm_m.api.space.SpaceResources.registerPumpGround(() -> EveBlocks.EVE_ROCK.get());
    }

    /** Original {@code createBiomeGenerators(seed)}. */
    public static BiomeGenLayers createBiomeGenerators(long seed) {
        GenLayer genlayerBiomes = new GenLayerEveBiomes(seed); // Your custom biome layer

        genlayerBiomes = new GenLayerZoom(1000L, genlayerBiomes);
        genlayerBiomes = new GenLayerZoom(1001L, genlayerBiomes);
        genlayerBiomes = new GenLayerZoom(1002L, genlayerBiomes);
        genlayerBiomes = new GenLayerZoom(1003L, genlayerBiomes);
        genlayerBiomes = new GenLayerZoom(1004L, genlayerBiomes);
        genlayerBiomes = new GenLayerZoom(1005L, genlayerBiomes);

        GenLayer genlayerRiverZoom = new GenLayerZoom(1000L, genlayerBiomes);
        GenLayer genlayerRiver = new GenLayerRiver(1001L, genlayerRiverZoom); // Your custom river layer
        GenLayerSmooth genlayersmooth = new GenLayerSmooth(1000L, genlayerRiver);

        GenLayerSmooth genlayersmooth1 = new GenLayerSmooth(1000L, genlayerBiomes);
        GenLayerEveRiverMix genlayerrivermix = new GenLayerEveRiverMix(100L, genlayersmooth1, genlayersmooth);
        GenLayerVoronoiZoom genlayervoronoizoom = new GenLayerVoronoiZoom(10L, genlayerrivermix);

        return new BiomeGenLayers(genlayerrivermix, genlayervoronoizoom, seed);
    }
}
