package com.space_m.dim.tekto.genlayer;

import com.space_m.dim.BiomeGenBaseCelestial;
import com.space_m.dim.legacy.GenLayer;
import com.space_m.dim.tekto.biome.BiomeGenBaseTekto;

/**
 * 1:1 {@code com.hbm.dim.tekto.GenLayerTekto.GenLayerTektoRiverMix}: Flusskanten (Vanilla-ID {@code river} 7) werden
 * ausser in den Polyvinyl-Ebenen zum Tetrachlorid-Fluss. {@code BiomeGenBaseEve.getBiomeGenArray()[id] != null}
 * (globale Biomliste) = bekanntes Himmelskoerper-Biom ({@link BiomeGenBaseCelestial#getBiome}); die
 * Pilzinsel-Zweige sind wie im Original unerreichbar, bleiben aber erhalten.
 */
public class GenLayerTektoRiverMix extends GenLayer {

	private final GenLayer biomePatternGeneratorChain;
	private final GenLayer riverPatternGeneratorChain;

	public GenLayerTektoRiverMix(long seed, GenLayer biomePatternGeneratorChain, GenLayer riverPatternGeneratorChain) {
		super(seed);

		this.biomePatternGeneratorChain = biomePatternGeneratorChain;
		this.riverPatternGeneratorChain = riverPatternGeneratorChain;
	}

	@Override
	public void initWorldGenSeed(long seed) {
		this.biomePatternGeneratorChain.initWorldGenSeed(seed);
		this.riverPatternGeneratorChain.initWorldGenSeed(seed);
		super.initWorldGenSeed(seed);
	}

	@Override
	public int[] getInts(int x, int z, int width, int length) {
		int[] inputBiomeIds = this.biomePatternGeneratorChain.getInts(x, z, width, length);
		int[] riverBiomeIds = this.riverPatternGeneratorChain.getInts(x, z, width, length);
		int[] outputBiomeIds = new int[width * length];
		//what if instead of constantly copying like this we just set the ids in the constructor?????
		//later
		for(int i1 = 0; i1 < width * length; ++i1) {
			if(inputBiomeIds[i1] != BiomeGenBaseTekto.polyvinylPlains.biomeID) {
				if(riverBiomeIds[i1] == RIVER) {
					if(BiomeGenBaseCelestial.getBiome(inputBiomeIds[i1]) != null) {
						outputBiomeIds[i1] = BiomeGenBaseTekto.tetrachloricRiver.biomeID;
					} else if(inputBiomeIds[i1] != MUSHROOM_ISLAND && inputBiomeIds[i1] != MUSHROOM_ISLAND_SHORE) {
						outputBiomeIds[i1] = riverBiomeIds[i1] & 255;
					} else {
						outputBiomeIds[i1] = MUSHROOM_ISLAND_SHORE;
					}
				} else {
					outputBiomeIds[i1] = inputBiomeIds[i1];
				}
			} else {
				outputBiomeIds[i1] = inputBiomeIds[i1];
			}
		}

		return outputBiomeIds;
	}
}
