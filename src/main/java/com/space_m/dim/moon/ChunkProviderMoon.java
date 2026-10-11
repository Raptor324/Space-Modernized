package com.space_m.dim.moon;

import com.space_m.config.SpaceConfig;
import com.space_m.dim.CelestialWorld;
import com.space_m.dim.ChunkProviderCelestial;
import com.space_m.dim.mapgen.MapGenBubble;
import com.space_m.dim.mapgen.MapGenCrater;
import com.space_m.dim.mapgen.MapGenGreg;
import com.space_m.dim.mapgen.MapgenRavineButBased;

import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code com.hbm.dim.moon.ChunkProviderMoon}: Mondgestein mit Basalt-"Meeren" bis y 64, GregGen-Hoehlen,
 * Schluchten, kleine (r 8-32, 1/6) und grosse (r 96-128, 1/64) Krater mit Basalt-Kern, Sole-Blasen
 * ({@code munBrineSpawn}, r 8-16). Reihenfolge der MapGens wie im Original.
 *
 * <p>{@code getFlatWorldBlock} (Superflach) entfaellt (siehe {@link ChunkProviderCelestial}).
 */
public class ChunkProviderMoon extends ChunkProviderCelestial {

	private final MapGenGreg caveGenV3 = new MapGenGreg();
	private final MapgenRavineButBased rgen = new MapgenRavineButBased();

	private final MapGenCrater smallCrater = new MapGenCrater(6);
	private final MapGenCrater largeCrater = new MapGenCrater(64);

	private final MapGenBubble brine = new MapGenBubble(SpaceConfig.get().munBrineSpawn);

	public ChunkProviderMoon(CelestialWorld world, long seed) {
		super(world, seed);
		BlockState moonRock = MunBlocks.MOON_ROCK.get().defaultBlockState();
		BlockState basalt = com.hbm_m.block.ModBlocks.BASALT.get().defaultBlockState();

		caveGenV3.stoneBlock = moonRock;
		rgen.stoneBlock = moonRock;

		smallCrater.setSize(8, 32);
		largeCrater.setSize(96, 128);

		smallCrater.regolith = largeCrater.regolith = basalt;
		smallCrater.rock = largeCrater.rock = moonRock;

		brine.block = MunBlocks.ORE_BRINE.get().defaultBlockState(); // ore_brine, meta = CelestialBody.getMeta(world) (Mun)
		brine.replace = moonRock;
		brine.setSize(8, 16);

		stoneBlock = moonRock;
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
		largeCrater.func_151539_a(worldObj, x, z, buffer.blocks);
		brine.func_151539_a(worldObj, x, z, buffer.blocks);

		return buffer;
	}

}
