package com.space_m.dim.tekto;

import java.util.Random;

import com.hbm_m.world.gen.nbt.NBTStructure;
import com.space_m.dim.BiomeGenBaseCelestial;
import com.space_m.dim.legacy.LegacyWorld;
import com.space_m.dim.legacy.WorldGenMinable;
import com.space_m.dim.tekto.biome.BiomeGenBaseTekto;

import dev.architectury.event.events.common.LifecycleEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * 1:1 {@code com.hbm.dim.tekto.WorldGeneratorTekto} als Feature {@code space_m:tekto_decorator} (Biom-JSON aller
 * Tekto-Biome, Schritt {@code TOP_LAYER_MODIFICATION} wie die {@code IWorldGenerator} nach der Dekoration):
 * <ol>
 *   <li>Kobalt {@code cobaltSpawn * 2} Adern x6, y 4-11 im Basalt.</li>
 *   <li>DCM-Geysire der Vulkane (y 32-127) neu setzen - erzeugt deren Block-Entity.</li>
 *   <li>Baeume ({@link TTree}) nach dem Biom bei {@code chunk + 16}: Polyvinyl-Ebene 2x (1/10 PVC-Baum, 1/8
 *       Vinyl-Stumpf), Halogen-Huegel 1/12 vier PVC-Baeume, Wald 8 Baeume (halb hoch mit Ranken, halb Busch).</li>
 * </ol>
 * {@code NBTStructure.registerNullWeight(tektoDimension, 24)} wird angemeldet; {@code BlockOre.addValidBody}
 * (Tooltip) folgt mit R8.
 */
public class WorldGeneratorTekto extends Feature<NoneFeatureConfiguration> {

	/** Original {@code SpaceConfig.tektoDimension}. */
	public static final int TEKTO_DIMENSION_ID = 413024;

	public WorldGeneratorTekto() {
		super(NoneFeatureConfiguration.CODEC);
	}

	public static void init() {
		LifecycleEvent.SETUP.register(() -> NBTStructure.registerNullWeight(TEKTO_DIMENSION_ID, 24));
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
		Random rand = LegacyWorld.random(ctx.random());
		generateTekto(ctx.level(), rand, ctx.origin().getX() & ~15, ctx.origin().getZ() & ~15);
		return true;
	}

	private static int height(WorldGenLevel world, int x, int z) {
		return world.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
	}

	private void generateTekto(WorldGenLevel world, Random rand, int i, int j) {
		BlockState stone = com.hbm_m.block.ModBlocks.BASALT.get().defaultBlockState();

		// DungeonToolbox.generateOre(world, rand, i, j, WorldConfig.cobaltSpawn * 2, 6, 4, 8, ModBlocks.ore_cobalt, meta, stone);
		WorldGenMinable cobalt = new WorldGenMinable(TektoBlocks.ORES.get("ore_cobalt").get().defaultBlockState(), 6, stone);
		int count = com.hbm_m.config.WorldConfig.cobaltSpawn * 2;
		for (int c = 0; c < count; c++) {
			cobalt.generate(world, rand, i + rand.nextInt(16), 4 + rand.nextInt(8), j + rand.nextInt(16));
		}

		BiomeGenBaseCelestial biome = BiomeGenBaseCelestial.byKey(world.getBiome(new BlockPos(i + 16, 64, j + 16)).unwrapKey().orElse(null));

		BlockState geysir = TektoBlocks.GEYSIR_CHLORIC.get().defaultBlockState();
		for(int x = 0; x < 16; x++) {
			for(int z = 0; z < 16; z++) {
				for(int y = 32; y < 128; y++) {
					BlockPos p = new BlockPos(i + x + 8, y, j + z + 8);
					if(world.getBlockState(p).is(geysir.getBlock())) {
						world.setBlock(p, geysir, 3);
					}
				}
			}
		}

		if(biome == BiomeGenBaseTekto.polyvinylPlains) {
			for(int o = 0; o < 2; o++) {
				if(rand.nextInt(10) == 0) {
					int x = i + rand.nextInt(16) + 8;
					int z = j + rand.nextInt(16) + 8;
					int y = height(world, x, z);

					TTree customTreeGen = new TTree(false, 4, 2, 10, 2, 4, false, TektoBlocks.PVC_LOG.get(), TektoBlocks.RUBBER_LEAVES.get());
					customTreeGen.generate(world, rand, x, y, z);
				}

				if(rand.nextInt(8) == 0) {
					int x = i + rand.nextInt(16) + 8;
					int z = j + rand.nextInt(16) + 8;
					int y = height(world, x, z);

					TTree chopped = new TTree(false, 2, 4, 5, 3, 2, false, TektoBlocks.VINYL_LOG.get(), TektoBlocks.PET_LEAVES.get());
					chopped.generate(world, rand, x, y, z);
				}
			}

		}

		if(biome == BiomeGenBaseTekto.halogenHills) {
			if(rand.nextInt(12) == 0) {
				for(int o = 0; o < 4; o++) {
					int x = i + rand.nextInt(16) + 8;
					int z = j + rand.nextInt(16) + 8;
					int y = height(world, x, z);

					TTree customTreeGen = new TTree(false, 3, 2, 14, 3, 3, false, TektoBlocks.PVC_LOG.get(), TektoBlocks.RUBBER_LEAVES.get());
					customTreeGen.generate(world, rand, x, y, z);
				}
			}
		}

		if(biome == BiomeGenBaseTekto.forest) {
			for(int o = 0; o < 8; o++) {
				int x = i + rand.nextInt(16) + 8;
				int z = j + rand.nextInt(16) + 8;
				int y = height(world, x, z);

				if(rand.nextInt(2) == 0) {
					TTree customTreeGen = new TTree(false, 3, 2, 20, 3, 5, true, TektoBlocks.PVC_LOG.get(), TektoBlocks.RUBBER_LEAVES.get());
					customTreeGen.generate(world, rand, x, y, z);
				} else {
					TTree tustomTreeGen = new TTree(false, 3, 1, 1, 3, 5, false, TektoBlocks.PVC_LOG.get(), TektoBlocks.RUBBER_LEAVES.get());
					tustomTreeGen.generate(world, rand, x, y, z);
				}
			}
		}
	}

}
