package com.space_m.dim.noise;

import java.util.Random;

import com.space_m.dim.BiomeGenBaseCelestial;
import com.space_m.dim.CelestialWorld;
import com.space_m.dim.legacy.Legacy;
import com.space_m.dim.legacy.MapGenBase;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** 1:1 {@code com.hbm.dim.noise.MapGenVNoise}: Voronoi-Platten (Risse zwischen Zellen) ueber einem Fluid. */
public class MapGenVNoise extends MapGenBase {

	//"absurd" MY ASS
	public BlockState fluidBlock;
	public BlockState surfBlock;
	public BlockState rockBlock;
	public int cellSize = 32;
	public double crackSize = 2.0;
	public int plateStartY = 55;
	public int plateThickness = 35;
	public double shapeExponent = 2.0;

	public BiomeGenBaseCelestial applyToBiome;

	@Override
	public void func_151539_a(CelestialWorld world, int chunkX, int chunkZ, BlockState[] blocks) {
		long seed = world.getSeed();
		int startX = chunkX * 16;
		int startZ = chunkZ * 16;

		for(int lx = 0; lx < 16; lx++) {
			for(int lz = 0; lz < 16; lz++) {

				if(applyToBiome != null) {
					BiomeGenBaseCelestial biome = world.getBiomeGenForCoords(lx + chunkX * 16, lz + chunkZ * 16);
					if(biome != applyToBiome) continue;
				}

				int gx = startX + lx;
				int gz = startZ + lz;
				double d0 = Double.MAX_VALUE, d1 = Double.MAX_VALUE;
				int cellX = gx / cellSize;
				int cellZ = gz / cellSize;

				for(int dx = -2; dx <= 2; dx++) {
					for(int dz = -2; dz <= 2; dz++) {
						double[] center = getCellCenter(cellX + dx, cellZ + dz, seed);
						double dist = distance(gx, gz, center[0], center[1]);
						if(dist < d0) {
							d1 = d0;
							d0 = dist;
						} else if(dist < d1) {
							d1 = dist;
						}
					}
				}

				double edge = d1 - d0;
				if(edge > crackSize) {
					double alpha = (edge - crackSize) / (cellSize - crackSize);
					alpha = Math.max(0, Math.min(1, alpha));
					alpha = Math.pow(alpha, shapeExponent);
					int topY = plateStartY + (int)(plateThickness * alpha);

					for(int y = plateStartY; y < topY && y < 256; y++) {
						int index = (lx * 16 + lz) * 256 + y;

						if(Legacy.is(blocks[index], Blocks.AIR) || Legacy.is(blocks[index], fluidBlock)) {
							blocks[index] = rockBlock;
						}
					}
					if(topY > plateStartY) {
						int topIndex = (lx * 16 + lz) * 256 + (topY - 1);
						blocks[topIndex] = surfBlock;
					}
				}
			}
		}
	}


	//i cant math for shit to be honest
	private double[] getCellCenter(int cellX, int cellZ, long worldSeed) {
		long hash = (long) cellX * 341873128712L + (long) cellZ * 132897987541L + worldSeed;
		Random rand = new Random(hash);
		double offsetX = (rand.nextDouble() - 0.5) * cellSize;
		double offsetZ = (rand.nextDouble() - 0.5) * cellSize;
		double centerX = cellX * (double) cellSize + cellSize / 2.0 + offsetX;
		double centerZ = cellZ * (double) cellSize + cellSize / 2.0 + offsetZ;
		return new double[]{centerX, centerZ};
	}

	private double distance(double x1, double z1, double x2, double z2) {
		double dx = x1 - x2;
		double dz = z1 - z2;
		return Math.sqrt(dx * dx + dz * dz);
	}

}
