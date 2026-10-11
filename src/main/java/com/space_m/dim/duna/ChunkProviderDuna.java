package com.space_m.dim.duna;

import com.space_m.config.SpaceConfig;
import com.space_m.dim.CelestialWorld;
import com.space_m.dim.ChunkProviderCelestial;
import com.space_m.dim.duna.biome.BiomeGenBaseDuna;
import com.space_m.dim.mapgen.ExperimentalCaveGenerator;
import com.space_m.dim.mapgen.MapGenBubble;
import com.space_m.dim.mapgen.MapGenPlateau;

import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code com.hbm.dim.duna.ChunkProviderDuna}: Duna-Gestein, Meeresspiegel 63 (Luft); Ablauf wie im Original
 * <em>ohne</em> {@code super.getChunkPrimer}: Rauschen -> Tafelberge ({@code MapGenPlateau}, nur Huegelland, vor der
 * Oberflaeche) -> Oberflaeche je Biom -> Basalt-Hoehlen nur im Tiefland ({@code caveGenV2}, Lava = Basalt, Waende
 * {@code duna_cobble}), feine Hoehlen ausserhalb ({@code caveGenSmall}, "Lava" = {@code duna_sands}) -> Oel-Blasen
 * ({@code dunaOilSpawn}, r 8-16, {@code ore_oil} Metadatum Duna).
 *
 * <p>{@code getFlatWorldBlock} (Superflach) entfaellt (siehe {@link ChunkProviderCelestial}).
 */
public class ChunkProviderDuna extends ChunkProviderCelestial {

	private final ExperimentalCaveGenerator caveGenSmall = new ExperimentalCaveGenerator(2, 12, 0.12F);
	private final ExperimentalCaveGenerator caveGenV2 = new ExperimentalCaveGenerator(2, 40, 3.0F);

	private final MapGenPlateau genPlateau = new MapGenPlateau(worldObj);

	private final MapGenBubble oil = new MapGenBubble(SpaceConfig.get().dunaOilSpawn);

	public ChunkProviderDuna(CelestialWorld world, long seed) {
		super(world, seed);
		BlockState dunaRock = DunaBlocks.DUNA_ROCK.get().defaultBlockState();
		BlockState dunaCobble = DunaBlocks.DUNA_COBBLE.get().defaultBlockState();
		BlockState dunaSands = DunaBlocks.DUNA_SANDS.get().defaultBlockState();
		stoneBlock = dunaRock;

		caveGenV2.lavaBlock = com.hbm_m.block.ModBlocks.BASALT.get().defaultBlockState();
		caveGenV2.stoneBlock = dunaCobble;
		caveGenSmall.lavaBlock = dunaSands;
		caveGenSmall.stoneBlock = dunaRock;

		caveGenSmall.smallCaveSize = 0.1F;

		caveGenV2.onlyBiome = BiomeGenBaseDuna.dunaLowlands;
		caveGenSmall.ignoreBiome = BiomeGenBaseDuna.dunaLowlands;

		genPlateau.surfrock = dunaSands;
		genPlateau.stoneBlock = dunaRock;
		genPlateau.fillblock = dunaSands;
		genPlateau.applyToBiome = BiomeGenBaseDuna.dunaHills;

		oil.block = DunaBlocks.ORE_OIL.get().defaultBlockState(); // ore_oil, meta = CelestialBody.getMeta(world) (Duna)
		oil.replace = dunaRock;
		oil.setSize(8, 16);
	}

	@Override
	public BlockMetaBuffer getChunkPrimer(int x, int z) {
		// Instead of calling super, we want to do some generation before _and_ after block replacement, for shaping!
		BlockMetaBuffer buffer = new BlockMetaBuffer();
		generateBlocks(x, z, buffer.blocks);
		biomesForGeneration = this.worldObj.loadBlockGeneratorData(biomesForGeneration, x * 16, z * 16, 16, 16);

		boolean hasLowlands = false;
		boolean hasNotLowlands = false;
		boolean hasPlateau = false;

		for(int i = 0; i < biomesForGeneration.length; i++) {
			if(biomesForGeneration[i] == BiomeGenBaseDuna.dunaLowlands) hasLowlands = true; else hasNotLowlands = true;
			if(biomesForGeneration[i] == BiomeGenBaseDuna.dunaHills) hasPlateau = true;
			if(hasLowlands && hasNotLowlands && hasPlateau) break;
		}

		// Pre-biome blocks
		if(hasPlateau) genPlateau.func_151539_a(worldObj, x, z, buffer.blocks);

		replaceBlocksForBiome(x, z, buffer.blocks, biomesForGeneration);

		// Post-biome blocks
		if(hasLowlands) caveGenV2.func_151539_a(worldObj, x, z, buffer.blocks);
		if(hasNotLowlands) caveGenSmall.func_151539_a(worldObj, x, z, buffer.blocks);

		oil.func_151539_a(worldObj, x, z, buffer.blocks);

		return buffer;
	}

}
