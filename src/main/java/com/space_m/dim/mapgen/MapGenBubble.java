package com.space_m.dim.mapgen;

import java.util.function.Predicate;

import com.hbm_m.world.gen.legacy.MB;
import com.space_m.dim.BiomeGenBaseCelestial;
import com.space_m.dim.CelestialWorld;
import com.space_m.dim.legacy.Legacy;
import com.space_m.dim.legacy.MapGenBase;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 die Space-Fassung von {@code com.hbm.world.gen.terrain.MapGenBubble} (dort {@code MapGenBaseMeta} mit
 * Feldern {@code block}/{@code meta}/{@code replace}): abgeplattete Blasen (3 x 1 x 3) aus Oel/Gas/Sole im
 * Planetengestein, plus Oberflaechenfleck mit Bohrloch. Auf den Himmelskoerpern laeuft sie im ChunkProvider
 * (Mun: Sole, {@code munBrineSpawn}); die HBM-Fassung ({@code com.hbm_m.world.gen.terrain.MapGenBubble}) ist ein
 * Oberwelt-Carver mit festem Oel.
 *
 * <p>{@code block} + {@code meta} des Originals (Erz mit Koerper-Metadatum) sind hier ein {@link BlockState}
 * (die Koerper-Erzvariante, z.B. {@code space_m:ore_brine_mun}).
 */
public class MapGenBubble extends MapGenBase {

	/**
	 * Generates oil bubbles, which are generally wider than a chunk, in a safe + cascadeless manner
	 * Pretty much just an oblate sphere generator (dimensions: 3 x 1 x 3)
	 */

	private final int frequency;
	private int minSize = 8;
	private int maxSize = 64;

	public int minY = 15;
	public int rangeY = 25;

	public boolean fuzzy;

	public BlockState block;
	public BlockState replace = Blocks.STONE.defaultBlockState();

	public Predicate<BiomeGenBaseCelestial> canSpawn;

	public MapGenBubble(int frequency) {
		this.frequency = frequency;
	}

	public void setSize(int minSize, int maxSize) {
		this.minSize = minSize;
		this.maxSize = maxSize;

		this.range = (maxSize / 8) + 1;
	}

	@Override
	protected void func_151538_a(CelestialWorld world, int offsetX, int offsetZ, int chunkX, int chunkZ, BlockState[] blocks) {

		int effecFreq = frequency;
		BiomeGenBaseCelestial biome = world.getBiomeGenForCoords(offsetX * 16, offsetZ * 16);
		if(biome.temperature >= 2 && biome.rainfall < 0.1) effecFreq /= 3;
		if(effecFreq <= 0) effecFreq = 1;

		if(rand.nextInt(effecFreq) == effecFreq - 1 && (canSpawn == null || canSpawn.test(biome))) {
			int xCoord = (chunkX - offsetX) * 16 + rand.nextInt(16);
			int zCoord = (chunkZ - offsetZ) * 16 + rand.nextInt(16);

			int yCoord = rand.nextInt(rangeY) + minY;

			double radius = rand.nextInt(maxSize - minSize) + minSize;
			double radiusSqr = (radius * radius) / 2; // original OilBubble implementation divided the square by 2 for some reason

			int yMin = Math.max(1, Legacy.floor(yCoord - radius));
			int yMax = Math.min(127, Legacy.ceil(yCoord + radius));

			for(int bx = 15; bx >= 0; bx--) // bx, bz is the coordinate of the block we're modifying, relative to the generating chunk origin
			for(int bz = 15; bz >= 0; bz--)
			for(int by = yMin; by < yMax; by++) {
				int index = (bx * 16 + bz) * 256 + by;

				if(Legacy.is(blocks[index], replace)) {
					// x, z are the coordinates relative to the target virtual chunk origin
					int x = xCoord + bx;
					int z = zCoord + bz;
					int y = yCoord - by;

					double rSqr = x * x + z * z + y * y * 3;
					if(fuzzy) rSqr -= rand.nextDouble() * radiusSqr / 3;
					if(rSqr < radiusSqr) {
						blocks[index] = block;
					}
				}
			}

			if(rand.nextInt(1) == 0) {
				addSurfaceSpot(xCoord, zCoord, blocks);
			}
		}
	}

	protected void addSurfaceSpot(int xCoord, int zCoord, BlockState[] blocks) {

		int deadMetaCount = 5; // EnumDeadPlantType.values().length
		int spotCount = 150;
		int spotWidth = 7;

		// Add oil spot damage
		for(int i = 0; i < spotCount; i++) {
			int offX = (int)(rand.nextGaussian() * spotWidth);
			int offZ = (int)(rand.nextGaussian() * spotWidth);
			int rx = offX - xCoord;
			int rz = offZ - zCoord;

			if(rx >= 0 && rx < 16 && rz >= 0 && rz < 16) {
				// find ground level
				for(int y = 127; y >= 4; y--) {
					int index = (rx * 16 + rz) * 256 + y;

					if(blocks[index] != null && Legacy.isOpaqueCube(blocks[index])) {
						for(int oy = 1; oy > -3; oy--) {
							int subIndex = index + oy;

							int distSq = offX * offX + offZ * offZ;
							boolean inner = distSq < (spotWidth / 2) * (spotWidth / 2);

							if(Legacy.is(blocks[subIndex], Blocks.GRASS_BLOCK) || Legacy.is(blocks[subIndex], Blocks.DIRT)) {
								blocks[subIndex] = inner ? MB.dirt_oily.state(0) : MB.dirt_dead.state(0);

								// this generation occurs BEFORE decoration, so we have no plants to modify
								// so we'll instead just add some new ones right now
								if(!inner && oy == 0 && rand.nextInt(20) == 0) {
									blocks[subIndex + 1] = MB.plant_dead.state(rand.nextInt(deadMetaCount));
								}

								break;
							} else if(Legacy.is(blocks[subIndex], Blocks.SAND) || Legacy.is(blocks[subIndex], Blocks.RED_SAND) || Legacy.is(blocks[subIndex], MB.ore_oil_sand.state(0))) {
								if(Legacy.is(blocks[subIndex], Blocks.RED_SAND)) { // Original: metas[subIndex] == 1
									blocks[subIndex] = MB.sand_dirty_red.state(0);
								} else {
									blocks[subIndex] = MB.sand_dirty.state(0);
								}
								break;
							} else if(Legacy.is(blocks[subIndex], Blocks.STONE)) {
								blocks[subIndex] = MB.stone_cracked.state(0);
								break;
							}
						}

						break;
					}
				}
			}
		}

		// and now for the hole(tm)
		for(int i = 1; i < 6; i++) {
			// ForgeDirection.getOrientation(i): UP, NORTH, SOUTH, WEST, EAST
			int dx = i == 4 ? -1 : i == 5 ? 1 : 0;
			int dz = i == 2 ? -1 : i == 3 ? 1 : 0;
			int x = dx - xCoord;
			int z = dz - zCoord;

			if(x >= 0 && x < 16 && z >= 0 && z < 16) {
				int solids = 0;

				for(int y = 127; y >= 0; y--) {
					int index = (x * 16 + z) * 256 + y;
					if(blocks[index] == null) continue;

					if(Legacy.isLiquid(blocks[index])) break;

					if(Legacy.isOpaqueCube(blocks[index])) {
						solids++;

						// this approach might break a little when the surface has holes and uneveness but i don't care lmao
						if(i > 1) {
							blocks[index] = MB.stone_cracked.state(0);
							if(solids >= 4) break;
						} else {
							if(solids < 3) blocks[index] = Blocks.AIR.defaultBlockState();
							if(solids == 3) blocks[index] = MB.oil_spill.state(0);
							if(solids > 3 && solids < 7) blocks[index] = MB.stone_cracked.state(0);
							if(solids >= 7) break;
						}
					}
				}
			}
		}
	}
}
