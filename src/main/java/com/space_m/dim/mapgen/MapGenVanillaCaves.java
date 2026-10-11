package com.space_m.dim.mapgen;

import com.space_m.dim.BiomeGenBaseCelestial;
import com.space_m.dim.legacy.Legacy;
import com.space_m.dim.legacy.MapGenCaves;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** 1:1 {@code com.hbm.dim.mapgen.MapGenVanillaCaves}: Vanilla-Hoehlen, die das Planetengestein aushoehlen. */
public class MapGenVanillaCaves extends MapGenCaves {

	// Same as vanilla cavegen but supports celestial stone
	private final BlockState stoneBlock;

	private BlockState lavaBlock = Blocks.LAVA.defaultBlockState();

	public MapGenVanillaCaves(BlockState stoneBlock) {
		this.stoneBlock = stoneBlock;
	}

	public MapGenVanillaCaves withLava(BlockState lavaBlock) {
		this.lavaBlock = lavaBlock;
		return this;
	}

	@Override
	protected void digBlock(BlockState[] data, int index, int x, int y, int z, int chunkX, int chunkZ, boolean foundTop) {
		BiomeGenBaseCelestial biome = worldObj.getBiomeGenForCoords(x + chunkX * 16, z + chunkZ * 16);
		BlockState block = data[index];

		if(Legacy.is(block, stoneBlock) || Legacy.is(block, biome.fillerBlock()) || Legacy.is(block, biome.topBlock())) {
			if(y < 10) {
				data[index] = lavaBlock;
			} else {
				data[index] = null;

				if(foundTop && Legacy.is(data[index - 1], biome.fillerBlock())) {
					data[index - 1] = biome.topBlock();
				}
			}
		}
	}

}
