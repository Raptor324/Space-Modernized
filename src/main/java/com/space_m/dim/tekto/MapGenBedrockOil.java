package com.space_m.dim.tekto;

import com.hbm_m.world.gen.legacy.MB;
import com.space_m.dim.CelestialWorld;
import com.space_m.dim.legacy.Legacy;
import com.space_m.dim.legacy.MapGenBase;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 die Space-Fassung von {@code com.hbm.world.gen.terrain.MapGenBedrockOil} (auf Tekto: {@code bedtonic},
 * {@code tektoBedrockOilSpawn}): Grundgestein-Oellager (Raute |x|+|y|+|z| &lt;= 6 in y 0-4) und 50 Oelflecken an der
 * Oberflaeche; die Space-Fassung macht aus Tekto-Schlick/-Gras/Vinylsand {@code sellafield_slaked}.
 * {@code block} + {@code meta} = Koerper-Variante ({@code ore_bedrock_oil_tekto}). Die Oberwelt-Fassung lebt in
 * HBM-Modernized ({@code com.hbm_m.world.gen.terrain.MapGenBedrockOil}).
 */
public class MapGenBedrockOil extends MapGenBase {

	/**
	 * Similar to oil bubbles, but with a few more behaviours, like adding oily dirt
	 * no porous stone don't @ me
	 */

	private final int frequency;

	public BlockState block;
	public BlockState replace = Blocks.STONE.defaultBlockState();

	public int spotWidth = 5;
	public int spotCount = 50;
	public boolean addWillows = true;

	public MapGenBedrockOil(int frequency) {
		this.frequency = frequency;
		this.range = 4;
	}

	@Override
	protected void func_151538_a(CelestialWorld world, int offsetX, int offsetZ, int chunkX, int chunkZ, BlockState[] blocks) {
		if(rand.nextInt(frequency) == frequency - 2) {
			int xCoord = (chunkX - offsetX) * 16 + rand.nextInt(16);
			int zCoord = (chunkZ - offsetZ) * 16 + rand.nextInt(16);

			// Add the bedrock oil spot
			for(int bx = 15; bx >= 0; bx--)
			for(int bz = 15; bz >= 0; bz--)
			for(int y = 0; y < 5; y++) {
				int index = (bx * 16 + bz) * 256 + y;

				if(Legacy.is(blocks[index], replace) || Legacy.is(blocks[index], Blocks.BEDROCK)) {
					// x, z are the coordinates relative to the target virtual chunk origin
					int x = xCoord + bx;
					int z = zCoord + bz;

					if(Math.abs(x) < 5 && Math.abs(z) < 5 && Math.abs(x) + Math.abs(y) + Math.abs(z) <= 6) {
						blocks[index] = block;
					}
				}
			}

			int deadMetaCount = 5; // EnumDeadPlantType.values().length

			// Add oil spot damage
			for(int i = 0; i < spotCount; i++) {
				int rx = (int)(rand.nextGaussian() * spotWidth) - xCoord;
				int rz = (int)(rand.nextGaussian() * spotWidth) - zCoord;

				if(rx >= 0 && rx < 16 && rz >= 0 && rz < 16) {
					// find ground level
					for(int y = 127; y >= 4; y--) {
						int index = (rx * 16 + rz) * 256 + y;

						if(blocks[index] != null && Legacy.isOpaqueCube(blocks[index])) {
							for(int oy = 1; oy > -3; oy--) {
								int subIndex = index + oy;

								if(Legacy.is(blocks[subIndex], Blocks.GRASS_BLOCK) || Legacy.is(blocks[subIndex], Blocks.DIRT)) {
									blocks[subIndex] = rand.nextInt(10) == 0 ? MB.dirt_oily.state() : MB.dirt_dead.state();

									if(addWillows && oy == 0 && rand.nextInt(50) == 0) {
										blocks[subIndex + 1] = MB.plant_flower.state(4); // EnumFlowerType.CD0
									}

									// this generation occurs BEFORE decoration, so we have no plants to modify
									// so we'll instead just add some new ones right now
									if(oy == 0 && rand.nextInt(20) == 0) {
										blocks[subIndex + 1] = MB.plant_dead.state(rand.nextInt(deadMetaCount));
									}

									break;
								} else if(Legacy.is(blocks[subIndex], Blocks.SAND) || Legacy.is(blocks[subIndex], Blocks.RED_SAND) || Legacy.is(blocks[subIndex], MB.ore_oil_sand.state())) {
									if(Legacy.is(blocks[subIndex], Blocks.RED_SAND)) { // Original: metas[subIndex] == 1
										blocks[subIndex] = MB.sand_dirty_red.state();
									} else {
										blocks[subIndex] = MB.sand_dirty.state();
									}
									break;
								} else if(Legacy.is(blocks[subIndex], Blocks.STONE)) {
									blocks[subIndex] = MB.stone_cracked.state();
									break;
								} else if(Legacy.is(blocks[subIndex], TektoBlocks.RUBBER_SILT.get()) || Legacy.is(blocks[subIndex], TektoBlocks.RUBBER_GRASS.get())
										|| Legacy.is(blocks[subIndex], TektoBlocks.VINYL_SAND.get())) {
									blocks[subIndex] = com.hbm_m.block.ModBlocks.SELLAFIELD_SLAKED.get().defaultBlockState();
									break;
								}
							}

							break;
						}
					}
				}
			}
		}
	}


}
