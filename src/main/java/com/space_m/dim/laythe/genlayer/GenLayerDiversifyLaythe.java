package com.space_m.dim.laythe.genlayer;

import com.space_m.dim.BiomeGenBaseCelestial;
import com.space_m.dim.laythe.biome.BiomeGenBaseLaythe;
import com.space_m.dim.legacy.GenLayer;

/** 1:1 {@code com.hbm.dim.laythe.genlayer.GenLayerDiversifyLaythe}: jede zweite Zelle ein zufaelliges Laythe-Biom. */
public class GenLayerDiversifyLaythe extends GenLayer {

	private static final BiomeGenBaseCelestial[] biomes = new BiomeGenBaseCelestial[] { BiomeGenBaseLaythe.laytheIsland, BiomeGenBaseLaythe.laytheOcean, BiomeGenBaseLaythe.laytheCoast };

	public GenLayerDiversifyLaythe(long l, GenLayer parent) {
		super(l);
		this.parent = parent;
	}

	@Override
	public int[] getInts(int x, int z, int width, int depth) {
		return diversify(x, z, width, depth);
	}

	private int[] diversify(int x, int z, int width, int height) {
		int[] input = this.parent.getInts(x, z, width, height);
		int[] output = new int[width * height];
		// EnumBiomeTypeDevonian type;
		for(int zOut = 0; zOut < height; zOut++) {
			for(int xOut = 0; xOut < width; xOut++) {
				int i = xOut + zOut * width;
				int center = input[i];
				initChunkSeed(xOut + x, zOut + z);
				if(nextInt(2) == 0) {
					output[i] = biomes[nextInt(biomes.length)].biomeID;
				} else
					output[i] = center;
			}
		}
		return output;
	}
}
