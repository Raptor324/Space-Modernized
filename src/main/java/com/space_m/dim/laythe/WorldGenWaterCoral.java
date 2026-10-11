package com.space_m.dim.laythe;

import java.util.Random;

import com.space_m.dim.laythe.block.BlockCoral;
import com.space_m.dim.legacy.LegacyWorld;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/**
 * {@code space_m:water_coral} = 1:1 {@code com.hbm.dim.WorldGenWaterCoral} samt Aufrufschleife aus
 * {@code BiomeDecoratorCelestial.genDecorations} ({@code coralPerChunk}-mal, {@code y = rand(seaLevel)}): 16 Versuche,
 * auf Laythe-Schlick unter Wasser ein Korallenblock und darauf eine bis zu 8 hohe, seitlich wandernde Saeule aus
 * Korallenbloecken mit Korallen an den Seiten. Sorte {@code meta = rand(5) + 8} -> Textur {@code meta % 5}.
 * Wasser-Test {@code getMaterial() == Material.water} = Block mit Wasser-Fluessigkeit (auch Laythe-Pflanzen).
 */
public class WorldGenWaterCoral extends Feature<WorldGenWaterPlant.Config> {

	public WorldGenWaterCoral() {
		super(WorldGenWaterPlant.Config.CODEC);
	}

	@Override
	public boolean place(FeaturePlaceContext<WorldGenWaterPlant.Config> ctx) {
		WorldGenWaterPlant.Config c = ctx.config();
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

	private static boolean exists(WorldGenLevel world, BlockPos origin, int x, int y, int z) {
		return WorldGenWaterPlant.exists(world, origin, x, y, z);
	}

	private static void set(WorldGenLevel world, BlockPos origin, int x, int y, int z, BlockState state) {
		BlockPos pos = new BlockPos(x, y, z);
		if (state.getBlock() instanceof BlockCoral) {
			// RenderCoral.getAttachment, nur innerhalb des schreibbaren Bereichs
			net.minecraft.core.Direction att = net.minecraft.core.Direction.DOWN;
			for (net.minecraft.core.Direction d : new net.minecraft.core.Direction[] {net.minecraft.core.Direction.NORTH, net.minecraft.core.Direction.SOUTH,
					net.minecraft.core.Direction.WEST, net.minecraft.core.Direction.EAST, net.minecraft.core.Direction.UP, net.minecraft.core.Direction.DOWN}) {
				BlockPos n = pos.relative(d);
				if (exists(world, origin, n.getX(), n.getY(), n.getZ()) && world.getBlockState(n).isFaceSturdy(world, n, d.getOpposite())) {
					att = d;
					break;
				}
			}
			state = state.setValue(BlockCoral.ATTACH, att);
		}
		world.setBlock(pos, state, 2);
	}

	public boolean generate(WorldGenLevel world, BlockPos origin, Random rand, int x, int y, int z, int seaLevel) {
		boolean flag = false;

		for(int l = 0; l < 16; ++l) {
			int px = x + rand.nextInt(8) - rand.nextInt(8);
			int py = y + rand.nextInt(4) - rand.nextInt(4);
			int pz = z + rand.nextInt(8) - rand.nextInt(8);

			if(!exists(world, origin, px, py, pz) || !exists(world, origin, px, py - 1, pz)) continue;

			if(py < seaLevel - 1 && world.getBlockState(new BlockPos(px, py, pz)).getFluidState().is(FluidTags.WATER)
					&& world.getBlockState(new BlockPos(px, py - 1, pz)).is(LaytheBlocks.LAYTHE_SILT.get())) {

				int meta = rand.nextInt(LaytheBlocks.CORALS.length) + 8;
				String type = LaytheBlocks.CORALS[meta % LaytheBlocks.CORALS.length];
				BlockState block = LaytheBlocks.LAYTHE_CORAL_BLOCK.get(type).get().defaultBlockState();
				BlockState coral = LaytheBlocks.LAYTHE_CORAL.get(type).get().defaultBlockState();

				set(world, origin, px, py - 1, pz, block);

				int oy = 0;
				int ox = 0;
				int oz = 0;
				while(oy < 8) {
					if(py + oy > seaLevel - 3) break;
					if(rand.nextBoolean()) break;

					if(rand.nextBoolean()) {
						if(rand.nextBoolean()) {
							ox += rand.nextBoolean() ? 1 : -1;
						} else {
							oz += rand.nextBoolean() ? 1 : -1;
						}
					}

					if(exists(world, origin, px + ox, py + oy, pz + oz)) set(world, origin, px + ox, py + oy, pz + oz, block);
					if(rand.nextBoolean() && exists(world, origin, px + ox + 1, py + oy, pz + oz)) set(world, origin, px + ox + 1, py + oy, pz + oz, coral);
					if(rand.nextBoolean() && exists(world, origin, px + ox - 1, py + oy, pz + oz)) set(world, origin, px + ox - 1, py + oy, pz + oz, coral);
					if(rand.nextBoolean() && exists(world, origin, px + ox, py + oy, pz + oz + 1)) set(world, origin, px + ox, py + oy, pz + oz + 1, coral);
					if(rand.nextBoolean() && exists(world, origin, px + ox, py + oy, pz + oz - 1)) set(world, origin, px + ox, py + oy, pz + oz - 1, coral);

					oy++;
				}

				if(exists(world, origin, px + ox, py + oy, pz + oz)) set(world, origin, px + ox, py + oy, pz + oz, coral);

				flag = true;
			}
		}

		return flag;
	}

}
