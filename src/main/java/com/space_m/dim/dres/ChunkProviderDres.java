package com.space_m.dim.dres;

import com.space_m.dim.CelestialWorld;
import com.space_m.dim.ChunkProviderCelestial;
import com.space_m.dim.legacy.MapGenBase;
import com.space_m.dim.mapgen.MapGenCrater;
import com.space_m.dim.mapgen.MapGenVanillaCaves;

import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code com.hbm.dim.dres.ChunkProviderDres}: Dres-Gestein (Meeresspiegel 63, Luft), Vanilla-Hoehlen im
 * Dres-Gestein, kleine (r 8-32, 1/6) und grosse (r 96-128, 1/64) Krater ganz aus {@code dres_rock}.
 *
 * <p>{@code getFlatWorldBlock} (Superflach) entfaellt (siehe {@link ChunkProviderCelestial}).
 */
public class ChunkProviderDres extends ChunkProviderCelestial {

	private final MapGenBase caveGenerator = new MapGenVanillaCaves(DresBlocks.DRES_ROCK.get().defaultBlockState());

	private final MapGenCrater smallCrater = new MapGenCrater(6);
	private final MapGenCrater largeCrater = new MapGenCrater(64);

	public ChunkProviderDres(CelestialWorld world, long seed) {
		super(world, seed);
		BlockState dresRock = DresBlocks.DRES_ROCK.get().defaultBlockState();

		smallCrater.setSize(8, 32);
		largeCrater.setSize(96, 128);

		smallCrater.regolith = largeCrater.regolith = dresRock;
		smallCrater.rock = largeCrater.rock = dresRock;

		stoneBlock = dresRock;
	}

	@Override
	public BlockMetaBuffer getChunkPrimer(int x, int z) {
		BlockMetaBuffer buffer = super.getChunkPrimer(x, z);

		caveGenerator.func_151539_a(worldObj, x, z, buffer.blocks);
		smallCrater.func_151539_a(worldObj, x, z, buffer.blocks);
		largeCrater.func_151539_a(worldObj, x, z, buffer.blocks);

		return buffer;
	}

}
