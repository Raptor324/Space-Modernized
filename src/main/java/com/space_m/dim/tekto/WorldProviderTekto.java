package com.space_m.dim.tekto;

import com.space_m.dim.CelestialGenerators;
import com.space_m.dim.WorldChunkManagerCelestial;
import com.space_m.dim.WorldChunkManagerCelestial.BiomeGenLayers;
import com.space_m.dim.legacy.GenLayer;
import com.space_m.dim.legacy.GenLayerRiver;
import com.space_m.dim.legacy.GenLayerSmooth;
import com.space_m.dim.legacy.GenLayerVoronoiZoom;
import com.space_m.dim.legacy.GenLayerZoom;
import com.space_m.dim.tekto.biome.BiomeGenBaseTekto;
import com.space_m.dim.tekto.genlayer.GenLayerTektoRiverMix;
import com.space_m.dim.tekto.genlayer.GenlayerTektoBiomes;

/**
 * 1:1 {@code com.hbm.dim.tekto.WorldProviderTekto} (Dimension "Tekto", {@code space_m:tekto}):
 * {@code registerWorldChunkManager} = GenLayer-Kette unten, {@code createChunkGenerator} = {@link ChunkProviderTekto},
 * {@code getStone} = {@code basalt}, Meeresspiegel 63. {@code getSunBrightness * 0.7} liegt bei R1-C
 * ({@code effects space_m:tekto}), {@code hasIce = true} bei den Grundgestein-Erzen (R8).
 */
public final class WorldProviderTekto {

	private WorldProviderTekto() {}

	public static void init() {
		TektoBlocks.init();
		BiomeGenBaseTekto.init();
		CelestialGenerators.register(new CelestialGenerators.BodyGen(TektoBlocks.BODY,
				seed -> new WorldChunkManagerCelestial(createBiomeGenerators(seed), seed, BiomeGenBaseTekto.polyvinylPlains),
				ChunkProviderTekto::new,
				() -> com.hbm_m.block.ModBlocks.BASALT.get().defaultBlockState(),
				63));
		TektoFeatures.init();
		WorldGeneratorTekto.init();
	}

	public static BiomeGenLayers createBiomeGenerators(long seed) {
		GenLayer genlayerBiomes = new GenlayerTektoBiomes(seed); // Your custom biome layer

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
		GenLayerTektoRiverMix genlayerrivermix = new GenLayerTektoRiverMix(100L, genlayersmooth1, genlayersmooth);
		GenLayerVoronoiZoom genlayervoronoizoom = new GenLayerVoronoiZoom(10L, genlayerrivermix);

		return new BiomeGenLayers(genlayerrivermix, genlayervoronoizoom, seed);
	}
}
