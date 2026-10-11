package com.space_m.dim.tekto.genlayer;

import com.space_m.dim.BiomeGenBaseCelestial;
import com.space_m.dim.legacy.GenLayer;
import com.space_m.dim.tekto.biome.BiomeGenBaseTekto;

/** 1:1 {@code com.hbm.dim.tekto.GenLayerTekto.GenlayerTektoBiomes} (IntCache -> neues Feld). */



public class GenlayerTektoBiomes extends GenLayer {

	private static final BiomeGenBaseCelestial[] biomes = new BiomeGenBaseCelestial[] { BiomeGenBaseTekto.polyvinylPlains, BiomeGenBaseTekto.halogenHills, BiomeGenBaseTekto.forest, BiomeGenBaseTekto.vinylsands};

	public GenlayerTektoBiomes(long l) {
		super(l);
	}

	@Override
	public int[] getInts(int x, int z, int width, int depth) {
		int[] dest = new int[width * depth];

		for(int dz = 0; dz < depth; dz++) {
			for(int dx = 0; dx < width; dx++) {
				this.initChunkSeed(dx + x, dz + z);

				dest[dx + dz * width] = biomes[this.nextInt(biomes.length)].biomeID;
			}
		}
		return dest;
	}
}
