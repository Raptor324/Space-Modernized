package com.space_m.dim.laythe.genlayer;

import com.space_m.dim.BiomeGenBaseCelestial;
import com.space_m.dim.laythe.biome.BiomeGenBaseLaythe;
import com.space_m.dim.legacy.GenLayer;

/** 1:1 {@code com.hbm.dim.laythe.genlayer.GenLayerLaytheBiomes}: Startschicht (Inseln/Ozean/Riff gleichverteilt). */
public class GenLayerLaytheBiomes extends GenLayer {

	private static final BiomeGenBaseCelestial[] biomes = new BiomeGenBaseCelestial[] { BiomeGenBaseLaythe.laytheIsland, BiomeGenBaseLaythe.laytheOcean, BiomeGenBaseLaythe.laytheCoast };

	public GenLayerLaytheBiomes(long l) {
		super(l);
	}

	@Override
	public int[] getInts(int x, int z, int width, int depth) {
		int[] dest = new int[width * depth];

		for(int k = 0; k < depth; ++k) {
			for(int i = 0; i < width; ++i) {
				initChunkSeed(x + i, z + k);
				dest[i + k * width] = biomes[nextInt(biomes.length)].biomeID;
			}
		}

		return dest;
	}
}
