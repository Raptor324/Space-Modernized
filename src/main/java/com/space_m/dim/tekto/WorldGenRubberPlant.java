package com.space_m.dim.tekto;

import java.util.Random;

import com.space_m.dim.BiomeGenBaseCelestial;
import com.space_m.dim.laythe.WorldGenWaterPlant;
import com.space_m.dim.legacy.LegacyWorld;
import com.space_m.dim.tekto.biome.BiomeGenBaseTekto;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/**
 * {@code space_m:rubber_plants} = 1:1 {@code com.hbm.dim.WorldGenRubberPlant} samt Aufrufschleife aus
 * {@code BiomeDecoratorCelestial.genDecorations} ({@code rubberPlantsPerChunk}-mal bei {@code chunk + rand(16) + 8},
 * {@code y = rand(80)}): je 64 Versuche Gummipflanzen (Gras/Farn/Strauch) auf Gummigras, 1/64 Stachelkaktus auf
 * Vinylsand (an der obersten festen Stelle), im Tekto-Wald zusaetzlich Buesche/Blumen/Kakteen.
 */
public class WorldGenRubberPlant extends Feature<WorldGenWaterPlant.Config> {

	public WorldGenRubberPlant() {
		super(WorldGenWaterPlant.Config.CODEC);
	}

	@Override
	public boolean place(FeaturePlaceContext<WorldGenWaterPlant.Config> ctx) {
		WorldGenLevel world = ctx.level();
		Random rand = LegacyWorld.random(ctx.random());
		int chunk_X = ctx.origin().getX() & ~15;
		int chunk_Z = ctx.origin().getZ() & ~15;
		BlockPos origin = new BlockPos(chunk_X + 8, 0, chunk_Z + 8);

		for (int i = 0; i < ctx.config().count; ++i) {
			int x = chunk_X + rand.nextInt(16) + 8;
			int z = chunk_Z + rand.nextInt(16) + 8;
			int y = rand.nextInt(80);
			generate(world, origin, rand, x, y, z);
		}
		return true;
	}

	private static boolean below(WorldGenLevel world, BlockPos origin, int x, int y, int z, net.minecraft.world.level.block.Block b) {
		return exists(world, origin, x, y, z) && world.getBlockState(new BlockPos(x, y, z)).is(b);
	}

	private static boolean exists(WorldGenLevel world, BlockPos origin, int x, int y, int z) {
		return Math.abs((x >> 4) - (origin.getX() >> 4)) <= 1 && Math.abs((z >> 4) - (origin.getZ() >> 4)) <= 1
				&& y >= world.getMinBuildHeight() && y < world.getMaxBuildHeight();
	}

	private static void set(WorldGenLevel world, BlockPos origin, int x, int y, int z, BlockState state) {
		if (exists(world, origin, x, y, z)) world.setBlock(new BlockPos(x, y, z), state, 2);
	}

	private static BlockState plant(int type) {
		return TektoBlocks.RUBBER_PLANT.get(TektoBlocks.RUBBER_PLANTS[type]).get().defaultBlockState();
	}

	private static BlockState cacti(int type) {
		return TektoBlocks.SPIKE_CACTI.get(TektoBlocks.RUBBER_CACTI[type]).get().defaultBlockState();
	}

	public boolean generate(WorldGenLevel world, BlockPos origin, Random rand, int x, int y, int z) {
		boolean flag = false;
		BiomeGenBaseCelestial currentBiome = exists(world, origin, x, 64, z)
				? BiomeGenBaseCelestial.byKey(world.getBiome(new BlockPos(x, 64, z)).unwrapKey().orElse(null)) : null;

		for (int l = 0; l < 64; ++l) {
			int px = x + rand.nextInt(8) - rand.nextInt(8);
			int py = y + rand.nextInt(4) - rand.nextInt(4);
			int pz = z + rand.nextInt(8) - rand.nextInt(8);

			if (py < 254 && below(world, origin, px, py - 1, pz, TektoBlocks.RUBBER_GRASS.get())) {

				int plantType = rand.nextInt(3);  // 0, 1, or 2 -> GRASS, FERN, SHRUB

				set(world, origin, px, py, pz, plant(plantType));
				flag = true;

			} else if (py < 254 && below(world, origin, px, py - 1, pz, TektoBlocks.VINYL_SAND.get())) {
				if(rand.nextInt(64) == 0) {
					if (exists(world, origin, px, 0, pz)) {
						int top = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, px, pz);
						set(world, origin, px, top, pz, cacti(0)); // EnumBushType.CACT
					}
				}

			}
			if (currentBiome == BiomeGenBaseTekto.forest) {
				if (py < 254 && below(world, origin, px, py - 1, pz, TektoBlocks.RUBBER_GRASS.get())) {
					int plantType = rand.nextInt(2) + 1; // 0 -> BUSH, 1 -> FLOWER

					int OtherplantType = rand.nextInt(3);

					if(rand.nextInt(4) == 0) {
						set(world, origin, px, py, pz, cacti(plantType));
					} else {
						set(world, origin, px, py, pz, plant(OtherplantType));

					}
					if(rand.nextInt(10) == 0) {
						set(world, origin, px, py, pz, cacti(0));

					}
					flag = true;

				}

			}
		}

		return flag;
	}

}
