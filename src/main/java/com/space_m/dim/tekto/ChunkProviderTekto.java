package com.space_m.dim.tekto;

import com.space_m.config.SpaceConfig;
import com.space_m.dim.CelestialWorld;
import com.space_m.dim.ChunkProviderCelestial;
import com.space_m.dim.mapgen.MapGenBubble;
import com.space_m.dim.mapgen.MapGenGreg;
import com.space_m.dim.mapgen.MapGenVolcano;
import com.space_m.dim.tekto.biome.BiomeGenBaseTekto;

import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code com.hbm.dim.tekto.ChunkProviderTekto}: Basalt-Gelaende ohne {@code reclamp} (zwei Rausch-Oktaven
 * uebersprungen), Meer aus Tetrachlorkohlenstoff ({@code ccl_block}) bis y 63, DCM-Geysir-Vulkane nur in der
 * Vinylwueste (1/12, r 8-16), GregGen-Hoehlen im Basalt, Tekto-Rohoelblasen ({@code tektoOilSpawn}, r 8-16) und
 * Grundgestein-Oel ({@code tektoBedrockOilSpawn}). {@code getFlatWorldBlock} (Superflach) entfaellt.
 */
public class ChunkProviderTekto extends ChunkProviderCelestial {

	private final MapGenGreg caveGenV3 = new MapGenGreg();
	private final MapGenVolcano volcano = new MapGenVolcano(12);

	private final MapGenBubble tektonic = new MapGenBubble(SpaceConfig.get().tektoOilSpawn);
	private final MapGenBedrockOil bedtonic = new MapGenBedrockOil(SpaceConfig.get().tektoBedrockOilSpawn);

	public ChunkProviderTekto(CelestialWorld world, long seed) {
		super(world, seed);
		BlockState basalt = com.hbm_m.block.ModBlocks.BASALT.get().defaultBlockState();
		reclamp = false; // please kill this, we spend more time calculating perlin noise than anything then skip two whole octaves of it!
		caveGenV3.stoneBlock = basalt;

		volcano.setSize(8, 16);
		volcano.setMaterial(TektoBlocks.GEYSIR_CHLORIC.get().defaultBlockState(), TektoBlocks.VINYL_SAND.get().defaultBlockState());

		tektonic.block = TektoBlocks.ORE_TEKTO.get().defaultBlockState(); // ore_tekto, meta = CelestialBody.getMeta(world) (Tekto)
		tektonic.replace = basalt;
		tektonic.setSize(8, 16);

		bedtonic.replace = basalt;
		bedtonic.block = TektoBlocks.ORE_BEDROCK_OIL.get().defaultBlockState(); // ore_bedrock_oil, meta = Body.TEKTO

		stoneBlock = basalt;
		seaBlock = TektoBlocks.CCL_BLOCK.get().defaultBlockState();
	}

	@Override
	public BlockMetaBuffer getChunkPrimer(int x, int z) {
		BlockMetaBuffer buffer = super.getChunkPrimer(x, z);

		if(biomesForGeneration[0] == BiomeGenBaseTekto.vinylsands) {
			volcano.func_151539_a(worldObj, x, z, buffer.blocks);
		}

		caveGenV3.func_151539_a(worldObj, x, z, buffer.blocks);

		tektonic.func_151539_a(worldObj, x, z, buffer.blocks);
		bedtonic.func_151539_a(worldObj, x, z, buffer.blocks);

		// how many times do I gotta say BEEEEG
		return buffer;
	}

}
