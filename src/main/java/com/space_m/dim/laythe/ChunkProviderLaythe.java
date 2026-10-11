package com.space_m.dim.laythe;

import com.space_m.config.SpaceConfig;
import com.space_m.dim.CelestialWorld;
import com.space_m.dim.ChunkProviderCelestial;
import com.space_m.dim.laythe.biome.BiomeGenBaseLaythe;
import com.space_m.dim.mapgen.MapGenBubble;
import com.space_m.dim.mapgen.MapGenGreg;
import com.space_m.dim.mapgen.MapGenRoots;
import com.space_m.dim.mapgen.MapGenTiltedSpires;
import com.space_m.dim.mapgen.MapgenRavineButBased;

import net.minecraft.world.level.block.Blocks;

/**
 * 1:1 {@code com.hbm.dim.laythe.ChunkProviderLaythe}: Wasserwelt (Meer bis y 63 aus Wasser), schraege Felsnadeln
 * (Polar: Packeis/Schnee, sonst Stein/Laythe-Schlick), Wurzelgaenge ({@code laythe_root}), GregGen-Hoehlen,
 * breite Unterwasser-Schluchten, Oelblasen ({@code laytheOilSpawn}, r 8-16).
 *
 * <p>Entfallen bzw. verschoben: {@code getFlatWorldBlock} (Superflach); {@code getPossibleCreatures} (Fleisch-Creeper
 * auf {@code tumor}) folgt mit dem Mob {@code EntityCreeperFlesh} (R8).
 */
public class ChunkProviderLaythe extends ChunkProviderCelestial {

	private final MapGenGreg caveGenV3 = new MapGenGreg();
	private final MapgenRavineButBased rgen = new MapgenRavineButBased();
	private final MapGenRoots roots = new MapGenRoots();

	private final MapGenTiltedSpires spires = new MapGenTiltedSpires(2, 14, 0.75F);
	private final MapGenTiltedSpires snowires = new MapGenTiltedSpires(2, 14, 0.75F);

	private final MapGenBubble oil = new MapGenBubble(SpaceConfig.get().laytheOilSpawn);

	public ChunkProviderLaythe(CelestialWorld world, long seed) {
		super(world, seed);

		rgen.yMin = 32;
		rgen.frequency = 20;
		rgen.strataFreq = 3;
		rgen.strataScale = 8.0F;
		rgen.width = 16.0D;
		rgen.taper = 8.0D;
		rgen.allowUnderwater = true;
		rgen.height = 5.0D;

		roots.rootBlock = LaytheBlocks.LAYTHE_ROOT.get().defaultBlockState();

		snowires.rock = Blocks.PACKED_ICE.defaultBlockState();
		snowires.regolith = Blocks.SNOW_BLOCK.defaultBlockState();
		spires.rock = Blocks.STONE.defaultBlockState();
		spires.regolith = LaytheBlocks.LAYTHE_SILT.get().defaultBlockState();

		spires.curve = snowires.curve = true;
		spires.maxPoint = snowires.maxPoint = 6.0F;
		spires.maxTilt = snowires.maxTilt = 3.5F;

		oil.block = LaytheBlocks.ORE_OIL.get().defaultBlockState(); // ore_oil, meta = CelestialBody.getMeta(world) (Laythe)
		oil.replace = Blocks.STONE.defaultBlockState();
		oil.setSize(8, 16);

		seaBlock = Blocks.WATER.defaultBlockState();
		// seaLevel = 96;
	}

	@Override
	public BlockMetaBuffer getChunkPrimer(int x, int z) {
		BlockMetaBuffer buffer = super.getChunkPrimer(x, z);

		if(biomesForGeneration[0] == BiomeGenBaseLaythe.laythePolar) {
			snowires.func_151539_a(worldObj, x, z, buffer.blocks);
		} else {
			spires.func_151539_a(worldObj, x, z, buffer.blocks);
		}
		roots.func_151539_a(worldObj, x, z, buffer.blocks);
		caveGenV3.func_151539_a(worldObj, x, z, buffer.blocks);
		rgen.func_151539_a(worldObj, x, z, buffer.blocks);
		oil.func_151539_a(worldObj, x, z, buffer.blocks);

		return buffer;
	}

}
