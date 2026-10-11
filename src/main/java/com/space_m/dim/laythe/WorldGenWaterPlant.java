package com.space_m.dim.laythe;

import java.util.Random;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.space_m.dim.laythe.block.BlockKelp;
import com.space_m.dim.legacy.LegacyWorld;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * {@code space_m:water_plants} = 1:1 {@code com.hbm.dim.WorldGenWaterPlant} samt Aufrufschleife aus
 * {@code BiomeDecoratorCelestial.genDecorations} ({@code waterPlantsPerChunk}-mal bei {@code chunk + rand(16) + 8},
 * {@code y = rand(seaLevel)}): je 128 Versuche Seegras (3/7), Leuchtgras (1/7), Salzgras (2/7, zweiteilig) oder
 * Seetang (1/7, 2-9 hoch) auf Laythe-Schlick unter Wasser.
 */
public class WorldGenWaterPlant extends Feature<WorldGenWaterPlant.Config> {

	public WorldGenWaterPlant() {
		super(Config.CODEC);
	}

	/** Liegt die Position im schreibbaren 3x3-Chunkbereich der Dekoration ({@code world.blockExists}). */
	static boolean exists(WorldGenLevel world, BlockPos origin, int x, int y, int z) {
		if (y < world.getMinBuildHeight() || y >= world.getMaxBuildHeight()) return false;
		return Math.abs((x >> 4) - (origin.getX() >> 4)) <= 1 && Math.abs((z >> 4) - (origin.getZ() >> 4)) <= 1;
	}

	@Override
	public boolean place(FeaturePlaceContext<Config> ctx) {
		Config c = ctx.config();
		WorldGenLevel world = ctx.level();
		Random rand = LegacyWorld.random(ctx.random());
		int chunk_X = ctx.origin().getX() & ~15;
		int chunk_Z = ctx.origin().getZ() & ~15;
		BlockPos origin = new BlockPos(chunk_X + 8, 0, chunk_Z + 8);

		for (int i = 0; i < c.count; ++i) {
			int x = chunk_X + rand.nextInt(16) + 8;
			int z = chunk_Z + rand.nextInt(16) + 8;
			int y = rand.nextInt(c.seaLevel);
			generate(world, origin, rand, x, y, z, c.seaLevel);
		}
		return true;
	}

	private static boolean is(WorldGenLevel world, BlockPos origin, int x, int y, int z, net.minecraft.world.level.block.Block b) {
		return exists(world, origin, x, y, z) && world.getBlockState(new BlockPos(x, y, z)).is(b);
	}

	private static void set(WorldGenLevel world, BlockPos origin, int x, int y, int z, BlockState state) {
		if (exists(world, origin, x, y, z)) world.setBlock(new BlockPos(x, y, z), state, 2);
	}

	public boolean generate(WorldGenLevel world, BlockPos origin, Random rand, int x, int y, int z, int seaLevel) {
		boolean flag = false;

		for(int l = 0; l < 128; ++l) {
			int px = x + rand.nextInt(8) - rand.nextInt(8);
			int py = y + rand.nextInt(4) - rand.nextInt(4);
			int pz = z + rand.nextInt(8) - rand.nextInt(8);

			if(py < seaLevel - 1 && is(world, origin, px, py, pz, Blocks.WATER) && is(world, origin, px, py - 1, pz, LaytheBlocks.LAYTHE_SILT.get())) {
				int type = rand.nextInt(7);

				switch(type) {
				case 0:
				case 1:
				case 2:
					set(world, origin, px, py, pz, LaytheBlocks.LAYTHE_SHORT.get().defaultBlockState());
					break;
				case 3:
					set(world, origin, px, py, pz, LaytheBlocks.LAYTHE_GLOW.get().defaultBlockState());
					break;
				case 4:
				case 5:
					if(py < seaLevel - 2) {
						BlockState tall = LaytheBlocks.PLANT_TALL_LAYTHE.get().defaultBlockState();
						set(world, origin, px, py, pz, tall);
						set(world, origin, px, py + 1, pz, tall.setValue(com.space_m.dim.laythe.block.BlockTallPlantWater.HALF, DoubleBlockHalf.UPPER));
					}
					break;
				case 6:
					if(py < seaLevel - 4) {
						int height = 2 + rand.nextInt(Math.min(8, seaLevel - py - 2));
						BlockState kelp = LaytheBlocks.LAYTHE_KELP.get().defaultBlockState();
						for(int h = 0; h < height; ++h) {
							// flag 1 im Original: die untere Saeule wird per Nachbar-Update zu Metadatum 0, die Spitze bleibt 8
							set(world, origin, px, py + h, pz, kelp.setValue(BlockKelp.TOP, h == height - 1));
						}
					}
					break;
				}

				flag = true;
			}
		}

		return flag;
	}

	public static final class Config implements FeatureConfiguration {
		public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.INT.fieldOf("count").forGetter(c -> c.count),
				Codec.INT.optionalFieldOf("sea_level", 63).forGetter(c -> c.seaLevel)
		).apply(i, Config::new));

		/** {@code waterPlantsPerChunk}. */
		public final int count;
		/** {@code BiomeDecoratorCelestial.seaLevel} (63). */
		public final int seaLevel;

		public Config(int count, int seaLevel) {
			this.count = count;
			this.seaLevel = seaLevel;
		}
	}
}
