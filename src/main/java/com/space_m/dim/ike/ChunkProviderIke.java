package com.space_m.dim.ike;

import com.hbm_m.config.WorldConfig;
import com.space_m.config.SpaceConfig;
import com.space_m.dim.CelestialWorld;
import com.space_m.dim.ChunkProviderCelestial;
import com.space_m.dim.legacy.MapGenBase;
import com.space_m.dim.legacy.MapGenCaves;
import com.space_m.dim.mapgen.MapGenBubble;
import com.space_m.dim.mapgen.MapGenTiltedSpires;

import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code com.hbm.dim.Ike.ChunkProviderIke}: Ike-Gestein ohne Meer (Luft unter y 63), schraege Felsnadeln
 * ({@code MapGenTiltedSpires(6, 6, 0)}, Mitte y 86), Vanilla-Hoehlen ({@code MapGenCaves} - hoehlt wie im Original nur
 * {@code Blocks.stone} und den Regolith aus, nicht das Ike-Gestein), Sole-Blasen ({@code ikeBrineSpawn}, r 8-16) und
 * Sellafield-Strahlungskrater ({@code radfreq / 10}, {@link MapGenCraterSellafield}). Reihenfolge wie im Original.
 *
 * <p>{@code getPossibleCreatures} liefert im Original {@code null} ("man fuck Ike") - keine Spawns; im Biom-JSON
 * {@code ike.json} sind die Spawner leer. {@code getFlatWorldBlock} (Superflach) entfaellt.
 */
public class ChunkProviderIke extends ChunkProviderCelestial {

	private final MapGenBase caveGenerator = new MapGenCaves();
	private final MapGenTiltedSpires spires = new MapGenTiltedSpires(6, 6, 0F);

	private final MapGenBubble brine = new MapGenBubble(SpaceConfig.get().ikeBrineSpawn);
	private final MapGenCraterSellafield sellafield = new MapGenCraterSellafield(WorldConfig.radfreq / 10);

	public ChunkProviderIke(CelestialWorld world, long seed) {
		super(world, seed);
		BlockState stone = IkeBlocks.IKE_STONE.get().defaultBlockState();

		spires.rock = stone;
		spires.regolith = IkeBlocks.IKE_REGOLITH.get().defaultBlockState();
		spires.mid = 86;

		brine.block = IkeBlocks.ORE_BRINE.get().defaultBlockState(); // ore_brine, meta = CelestialBody.getMeta(world) (Ike)
		brine.replace = stone;
		brine.setSize(8, 16);

		sellafield.regolith = com.hbm_m.block.ModBlocks.SELLAFIELD_0.get().defaultBlockState();
		sellafield.rock = com.hbm_m.block.ModBlocks.SELLAFIELD_SLAKED.get().defaultBlockState();

		stoneBlock = stone;
	}

	@Override
	public BlockMetaBuffer getChunkPrimer(int x, int z) {
		BlockMetaBuffer buffer = super.getChunkPrimer(x, z);

		spires.func_151539_a(worldObj, x, z, buffer.blocks);
		caveGenerator.func_151539_a(worldObj, x, z, buffer.blocks);
		brine.func_151539_a(worldObj, x, z, buffer.blocks);
		sellafield.func_151539_a(worldObj, x, z, buffer.blocks);

		return buffer;
	}

}
