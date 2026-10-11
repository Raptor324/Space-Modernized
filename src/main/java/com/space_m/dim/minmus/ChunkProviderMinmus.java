package com.space_m.dim.minmus;

import com.space_m.config.SpaceConfig;
import com.space_m.dim.CelestialWorld;
import com.space_m.dim.ChunkProviderCelestial;
import com.space_m.dim.mapgen.MapGenBubble;
import com.space_m.dim.mapgen.MapGenCrater;
import com.space_m.dim.mapgen.MapGenVanillaCaves;
import com.space_m.dim.legacy.MapGenBase;

import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code com.hbm.dim.minmus.ChunkProviderMinmus}: Minmus-Gestein mit "Meeren" aus {@code minmus_smooth} bis
 * y 63, Vanilla-Hoehlen im Planetengestein (unter y 10 mit {@code minmus_smooth} statt Lava gefuellt), kleine
 * (r 6-24, Chance 1/5 quadriert) und grosse (r 64-128, 1/96) Krater mit Regolith-Kern, Sole-Blasen
 * ({@code minmusBrineSpawn}, r 8-16). Reihenfolge der MapGens wie im Original.
 *
 * <p>{@code getFlatWorldBlock} (Superflach) entfaellt (siehe {@link ChunkProviderCelestial}).
 */
public class ChunkProviderMinmus extends ChunkProviderCelestial {

	private final MapGenBase caveGenerator;

	private final MapGenCrater smallCrater = new MapGenCrater(5);
	private final MapGenCrater largeCrater = new MapGenCrater(96);

	private final MapGenBubble brine = new MapGenBubble(SpaceConfig.get().minmusBrineSpawn);

	public ChunkProviderMinmus(CelestialWorld world, long seed) {
		super(world, seed);
		BlockState stone = MinmusBlocks.MINMUS_STONE.get().defaultBlockState();
		BlockState smooth = MinmusBlocks.MINMUS_SMOOTH.get().defaultBlockState();
		BlockState regolith = MinmusBlocks.MINMUS_REGOLITH.get().defaultBlockState();

		caveGenerator = new MapGenVanillaCaves(stone).withLava(smooth);

		smallCrater.setSize(6, 24);
		largeCrater.setSize(64, 128);

		smallCrater.regolith = largeCrater.regolith = regolith;
		smallCrater.rock = largeCrater.rock = stone;

		brine.block = MinmusBlocks.ORE_BRINE.get().defaultBlockState(); // ore_brine, meta = CelestialBody.getMeta(world) (Minmus)
		brine.replace = stone;
		brine.setSize(8, 16);

		stoneBlock = stone;
		seaBlock = smooth;
		seaLevel = 63;
	}

	@Override
	public BlockMetaBuffer getChunkPrimer(int x, int z) {
		BlockMetaBuffer buffer = super.getChunkPrimer(x, z);

		caveGenerator.func_151539_a(worldObj, x, z, buffer.blocks);
		smallCrater.func_151539_a(worldObj, x, z, buffer.blocks);
		largeCrater.func_151539_a(worldObj, x, z, buffer.blocks);
		brine.func_151539_a(worldObj, x, z, buffer.blocks);

		return buffer;
	}

}
