package com.space_m.dim.mapgen;

import java.util.Random;

import com.space_m.dim.BiomeGenBaseCelestial;
import com.space_m.dim.CelestialWorld;
import com.space_m.dim.legacy.Legacy;
import com.space_m.dim.legacy.MapGenBase;
import com.space_m.dim.legacy.NoiseGeneratorPerlin;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code com.hbm.dim.mapgen.MapGenPlateau}: gestufte Hochebenen aus 2D-Rauschen.
 *
 * <p>Abweichung: Das Original seedet sein Rauschen mit {@code world.rand} (bei jedem Start anders); ohne Welt im
 * 1.20-Generator und mit mehreren Generator-Threads wird stattdessen der Weltseed benutzt.
 */
public class MapGenPlateau extends MapGenBase {

	public BlockState stoneBlock;
	public BlockState surfrock;
	public BlockState fillblock;

	public NoiseGeneratorPerlin plateauNoise;
	public double noiseScale = 0.05;
	public int maxPlateauAddition = 12;
	public int stepHeight = 6;
	public int topsoilThickness = 2;

	public BiomeGenBaseCelestial applyToBiome;

	public MapGenPlateau(CelestialWorld world) {
		this.plateauNoise = new NoiseGeneratorPerlin(new Random(world.getSeed()), 4);
	}

	@Override
	public void func_151539_a(CelestialWorld world, int chunkX, int chunkZ, BlockState[] blocks) {
		int[][] plateauTops = new int[16][16];
		int[][] baseHeights = new int[16][16];
		for(int localX = 0; localX < 16; localX++) {
			for(int localZ = 0; localZ < 16; localZ++) {
				int baseHeight = getSurfaceHeight(blocks, localX, localZ);
				baseHeights[localX][localZ] = baseHeight;
				double noiseVal = plateauNoise.func_151601_a((chunkX * 16 + localX) * noiseScale, (chunkZ * 16 + localZ) * noiseScale);
				int plateauAddition = (int)(((noiseVal + 1) / 2.0) * maxPlateauAddition);
				plateauAddition = (plateauAddition / stepHeight) * stepHeight;
				plateauTops[localX][localZ] = baseHeight + plateauAddition;
			}
		}

		for(int localX = 0; localX < 16; localX++) {
			for(int localZ = 0; localZ < 16; localZ++) {
				if(applyToBiome != null) {
					BiomeGenBaseCelestial biome = world.getBiomeGenForCoords(localX + chunkX * 16, localZ + chunkZ * 16);
					if(biome != applyToBiome) continue;
				}
				int baseHeight = baseHeights[localX][localZ];
				int plateauTop = plateauTops[localX][localZ];
				for(int y = baseHeight + 1; y < 256; y++) {
					int index = (localX * 16 + localZ) * 256 + y;
					if(y < plateauTop - topsoilThickness) {
						blocks[index] = stoneBlock;
					} else if(y < plateauTop) {
						boolean sameLeft  = (localX - 1 < 0)    || (plateauTops[localX - 1][localZ] == plateauTop);
						boolean sameRight = (localX + 1 >= 16)  || (plateauTops[localX + 1][localZ] == plateauTop);
						boolean sameFront = (localZ - 1 < 0)    || (plateauTops[localX][localZ - 1] == plateauTop);
						boolean sameBack  = (localZ + 1 >= 16)  || (plateauTops[localX][localZ + 1] == plateauTop);

						if(y == plateauTop - 1 && sameLeft && sameRight && sameFront && sameBack) {
							blocks[index] = fillblock;
						} else {
							blocks[index] = surfrock;
						}
					} else {
						blocks[index] = Blocks.AIR.defaultBlockState();
					}
				}
			}
		}
	}

	private int getSurfaceHeight(BlockState[] blocks, int localX, int localZ) {
		int baseHeight = 0;
		for(int y = 255; y >= 0; y--) {
			int index = (localX * 16 + localZ) * 256 + y;
			if(!Legacy.is(blocks[index], Blocks.AIR)) {
				baseHeight = y;
				break;
			}
		}
		return baseHeight;
	}

}
