package com.space_m.dim.ike;

import com.space_m.dim.CelestialGenerators;
import com.space_m.dim.WorldChunkManagerCelestial;

/**
 * 1:1 {@code com.hbm.dim.Ike.WorldProviderIke} (Dimension "Ike"):
 * <ul>
 *   <li>{@code registerWorldChunkManager} = {@code WorldChunkManagerHell(BiomeGenIke.biome, 0.0F)} -> ein Biom.</li>
 *   <li>{@code createChunkGenerator} = {@link ChunkProviderIke}; {@code getStone} = {@code ike_stone}.</li>
 *   <li>{@code updateLightmap} (rote Naechte) ist Teil des Himmels (R1-C, {@code CelestialSkyProvider.Ike}).</li>
 *   <li>{@code getFish/getJunk/getTreasure} (nur Pluesch-Tier) haengt am Angel-Rahmen von {@code WorldProviderCelestial}
 *       ({@code ModEventHandler.updateFish}), den es im Port noch nicht gibt - offen.</li>
 * </ul>
 * Zusaetzlich: Pumpenboden {@code ike_regolith}/{@code ike_stone} ({@code TileEntityMachinePumpBase.validBlocks}).
 */
public final class WorldProviderIke {

    private WorldProviderIke() {}

    public static void init() {
        IkeBlocks.init();
        CelestialGenerators.register(new CelestialGenerators.BodyGen(IkeBlocks.BODY,
                seed -> WorldChunkManagerCelestial.fixed(BiomeGenIke.biome, seed),
                ChunkProviderIke::new,
                () -> IkeBlocks.IKE_STONE.get().defaultBlockState(),
                63));
        WorldGeneratorIke.init();
        com.hbm_m.api.space.SpaceResources.registerPumpGround(() -> IkeBlocks.IKE_REGOLITH.get());
        com.hbm_m.api.space.SpaceResources.registerPumpGround(() -> IkeBlocks.IKE_STONE.get());
    }
}
