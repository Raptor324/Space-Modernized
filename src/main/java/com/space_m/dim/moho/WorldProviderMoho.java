package com.space_m.dim.moho;

import com.space_m.dim.CelestialGenerators;
import com.space_m.dim.GenLayerDiversify;
import com.space_m.dim.WorldChunkManagerCelestial;
import com.space_m.dim.WorldChunkManagerCelestial.BiomeGenLayers;
import com.space_m.dim.legacy.GenLayer;
import com.space_m.dim.legacy.GenLayerFuzzyZoom;
import com.space_m.dim.legacy.GenLayerRiver;
import com.space_m.dim.legacy.GenLayerRiverMix;
import com.space_m.dim.legacy.GenLayerSmooth;
import com.space_m.dim.legacy.GenLayerVoronoiZoom;
import com.space_m.dim.legacy.GenLayerZoom;
import com.space_m.dim.legacy.LegacyBiomes;
import com.space_m.dim.moho.biome.BiomeGenBaseMoho;
import com.space_m.dim.moho.genlayer.GenLayerMohoBiomes;
import com.space_m.dim.moho.genlayer.GenLayerMohoPlateauExtend;

/**
 * 1:1 {@code com.hbm.dim.moho.WorldProviderMoho} (Dimension "Moho"):
 * <ul>
 *   <li>{@code registerWorldChunkManager} = GenLayer-Kette {@link #createBiomeGenerators} (Seeds/Reihenfolge 1:1).
 *       Die grobe Schicht mischt Vanilla-Fluesse ein (ID 7 = {@link LegacyBiomes#RIVER}, nur Rauschhoehe); die
 *       Detailschicht ignoriert sie.</li>
 *   <li>{@code createChunkGenerator} = {@link ChunkProviderMoho} (Meeresspiegel 63).</li>
 *   <li>{@code getStone} = {@code moho_stone}.</li>
 * </ul>
 * Kein eigenes Wetter (keine Atmosphaere). Pumpenboden {@code moho_regolith} ({@code TileEntityMachinePumpBase.validBlocks}).
 */
public final class WorldProviderMoho {

    private WorldProviderMoho() {}

    public static void init() {
        MohoBlocks.init();
        LegacyBiomes.init();
        BiomeGenBaseMoho.init();
        CelestialGenerators.register(new CelestialGenerators.BodyGen(MohoBlocks.BODY,
                seed -> new WorldChunkManagerCelestial(createBiomeGenerators(seed), seed, BiomeGenBaseMoho.mohoCrag),
                ChunkProviderMoho::new,
                () -> MohoBlocks.MOHO_STONE.get().defaultBlockState(),
                63));
        WorldGeneratorMoho.init();
        com.hbm_m.api.space.SpaceResources.registerPumpGround(() -> MohoBlocks.MOHO_REGOLITH.get());
    }

    /** Original {@code createBiomeGenerators(seed)}. */
    public static BiomeGenLayers createBiomeGenerators(long seed) {
        GenLayer biomes = new GenLayerMohoBiomes(seed);

        biomes = new GenLayerFuzzyZoom(2000L, biomes);
        biomes = new GenLayerDiversify(1234L, biomes, 6, BiomeGenBaseMoho.mohoBasalt);
        biomes = new GenLayerZoom(2001L, biomes);
        biomes = new GenLayerMohoPlateauExtend(9944L, biomes);
        biomes = new GenLayerZoom(1006L, biomes);
        biomes = new GenLayerSmooth(700L, biomes);
        biomes = new GenLayerZoom(1000L, biomes);
        biomes = new GenLayerZoom(107L, biomes);

        // biome detail layer should ignore rivers (for monster spawns and biome block replacement)
        GenLayer genlayerVoronoiZoom = new GenLayerVoronoiZoom(10L, biomes);

        GenLayer genlayerRiver = new GenLayerRiver(1004L, biomes);
        genlayerRiver = new GenLayerZoom(105L, genlayerRiver);
        genlayerRiver = new GenLayerZoom(106L, genlayerRiver); // Added extra zoom for more frequent rivers

        // apply the rivers to the biome map
        GenLayer genlayerRiverMix = new GenLayerRiverMix(100L, biomes, genlayerRiver);

        return new BiomeGenLayers(genlayerRiverMix, genlayerVoronoiZoom, seed);
    }
}
