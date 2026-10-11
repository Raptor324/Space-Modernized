package com.space_m.dim.thatmo;

import com.space_m.dim.CelestialWorld;
import com.space_m.dim.ChunkProviderCelestial;
import com.space_m.dim.mapgen.MapGenCrater;
import com.space_m.dim.mapgen.MapGenGreg;
import com.space_m.dim.mapgen.MapgenRavineButBased;

import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code com.hbm.dim.thatmo.ChunkProviderThatmo}: Basalt mit Basalt-"Meer" bis y 64, GregGen-Hoehlen und
 * Schluchten im geloeschten Sellafit (das als Gestein selten vorkommt - wie im Original), kleine Krater (r 8-32,
 * 1/6) aus Sellafit. {@code getFlatWorldBlock} (Superflach) entfaellt.
 */
public class ChunkProviderThatmo extends ChunkProviderCelestial {

	private final MapGenGreg caveGenV3 = new MapGenGreg();
	private final MapgenRavineButBased rgen = new MapgenRavineButBased();

	private final MapGenCrater smallCrater = new MapGenCrater(6);


	public ChunkProviderThatmo(CelestialWorld world, long seed) {
		super(world, seed);
		BlockState slaked = com.hbm_m.block.ModBlocks.SELLAFIELD_SLAKED.get().defaultBlockState();
		BlockState basalt = com.hbm_m.block.ModBlocks.BASALT.get().defaultBlockState();
		caveGenV3.stoneBlock = slaked;
		rgen.stoneBlock = slaked;

		smallCrater.setSize(8, 32);

		smallCrater.regolith = slaked;
		smallCrater.rock = slaked;

		stoneBlock = basalt;
		seaBlock = basalt;
		seaLevel = 64;
	}

	@Override
	public BlockMetaBuffer getChunkPrimer(int x, int z) {
		BlockMetaBuffer buffer = super.getChunkPrimer(x, z);

		// NEW CAVES
		caveGenV3.func_151539_a(worldObj, x, z, buffer.blocks);
		rgen.func_151539_a(worldObj, x, z, buffer.blocks);
		smallCrater.func_151539_a(worldObj, x, z, buffer.blocks);

		return buffer;
	}

}
