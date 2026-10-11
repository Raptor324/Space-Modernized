package com.space_m.dim.laythe;

import com.space_m.dim.CelestialGenerators;
import com.space_m.dim.WorldChunkManagerCelestial;
import com.space_m.dim.WorldChunkManagerCelestial.BiomeGenLayers;
import com.space_m.dim.laythe.biome.BiomeGenBaseLaythe;
import com.space_m.dim.laythe.genlayer.GenLayerDiversifyLaythe;
import com.space_m.dim.laythe.genlayer.GenLayerLaytheBiomes;
import com.space_m.dim.laythe.genlayer.GenLayerLaytheIslands;
import com.space_m.dim.laythe.genlayer.GenLayerLaytheOceans;
import com.space_m.dim.laythe.genlayer.GenLayerLaythePolar;
import com.space_m.dim.legacy.GenLayer;
import com.space_m.dim.legacy.GenLayerSmooth;
import com.space_m.dim.legacy.GenLayerVoronoiZoom;
import com.space_m.dim.legacy.GenLayerZoom;

import net.minecraft.world.level.block.Blocks;

/**
 * 1:1 {@code com.hbm.dim.laythe.WorldProviderLaythe} (Dimension "Laythe", {@code space_m:laythe}):
 * {@code registerWorldChunkManager} = GenLayer-Kette unten, {@code createChunkGenerator} = {@link ChunkProviderLaythe},
 * {@code getStone} = {@code stone} (Standard von {@code WorldProviderCelestial}), Meeresspiegel 63.
 *
 * <p>Client-Teile ({@code getSkyRenderer} = Sonnenuntergang, {@code updateLightmap} Jool-Gruen, Wasser-Nebel 0.03)
 * liegen bei R1-C ({@code effects space_m:laythe}); {@code hasLife} = Atmosphaere/R2; {@code getWaterOpacity} 1 =
 * Vanilla-Wasser in 1.20 (Lichtdaempfung 1).
 */
public final class WorldProviderLaythe {

	private WorldProviderLaythe() {}

	public static void init() {
		LaytheBlocks.init();
		BiomeGenBaseLaythe.init();
		CelestialGenerators.register(new CelestialGenerators.BodyGen(LaytheBlocks.BODY,
				seed -> new WorldChunkManagerCelestial(createBiomeGenerators(seed), seed, BiomeGenBaseLaythe.laytheOcean),
				ChunkProviderLaythe::new,
				Blocks.STONE::defaultBlockState,
				63));
		LaytheFeatures.init();
		WorldGeneratorLaythe.init();
	}

	public static BiomeGenLayers createBiomeGenerators(long seed) {
		GenLayer biomes = new GenLayerLaytheBiomes(seed);
		biomes = new GenLayerLaythePolar(1000L, biomes);

		biomes = new GenLayerZoom(1040L, biomes);
		biomes = GenLayerZoom.magnify(1000L, biomes, 1);
		biomes = new GenLayerLaythePolar(1300L, biomes);

		biomes = new GenLayerDiversifyLaythe(1000L, biomes);
		biomes = new GenLayerLaytheOceans(56000L, biomes);
		biomes = new GenLayerLaytheIslands(200L, biomes);

		biomes = new GenLayerZoom(1200L, biomes);
		biomes = new GenLayerZoom(1001L, biomes);

		biomes = new GenLayerLaytheOceans(4000L, biomes);
		biomes = new GenLayerLaytheOceans(4001L, biomes, BiomeGenBaseLaythe.laytheCoast.biomeID);

		biomes = new GenLayerZoom(1003L, biomes);
		biomes = new GenLayerLaytheOceans(4342L, biomes, BiomeGenBaseLaythe.laytheCoast.biomeID);
		biomes = new GenLayerSmooth(700L, biomes);

		biomes = new GenLayerZoom(1006L, biomes);

		GenLayer genLayerVoronoiZoom = new GenLayerVoronoiZoom(10L, biomes);

		return new BiomeGenLayers(biomes, genLayerVoronoiZoom, seed);
	}
}
