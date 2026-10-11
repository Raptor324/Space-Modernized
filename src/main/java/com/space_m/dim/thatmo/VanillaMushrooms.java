package com.space_m.dim.thatmo;

import java.util.Random;

import com.space_m.dim.legacy.LegacyWorld;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * {@code space_m:vanilla_mushrooms} = der Teil des 1.7.10-{@code BiomeDecorator.genDecorations}, der auf Thatmo
 * (Schnee/Basalt/Sellafit, kein Wasser, kein Gras) etwas erzeugt: 1/4 braune Pilze, 1/8 rote Pilze bei
 * {@code y = rand(getHeightValue * 2)}, je 64 Versuche ({@code WorldGenFlowers}); halten nur auf fester Oberseite
 * bei Licht &lt; 13 ({@code BlockMushroom.canBlockStay}, Pflanzentyp Cave) - also in Hoehlen/Schluchten.
 */
public class VanillaMushrooms extends Feature<NoneFeatureConfiguration> {

    public VanillaMushrooms() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel world = ctx.level();
        Random rand = LegacyWorld.random(ctx.random());
        int chunk_X = ctx.origin().getX() & ~15;
        int chunk_Z = ctx.origin().getZ() & ~15;

        if (rand.nextInt(4) == 0) {
            int k = chunk_X + rand.nextInt(16) + 8;
            int l = chunk_Z + rand.nextInt(16) + 8;
            int h = world.getHeight(Heightmap.Types.MOTION_BLOCKING, k, l) * 2;
            if (h > 0) generate(world, rand, Blocks.BROWN_MUSHROOM, k, rand.nextInt(h), l, chunk_X, chunk_Z);
        }

        if (rand.nextInt(8) == 0) {
            int k = chunk_X + rand.nextInt(16) + 8;
            int l = chunk_Z + rand.nextInt(16) + 8;
            int h = world.getHeight(Heightmap.Types.MOTION_BLOCKING, k, l) * 2;
            if (h > 0) generate(world, rand, Blocks.RED_MUSHROOM, k, rand.nextInt(h), l, chunk_X, chunk_Z);
        }
        return true;
    }

    /** {@code WorldGenFlowers.generate}. */
    private static void generate(WorldGenLevel world, Random rand, Block block, int x, int y, int z, int cx, int cz) {
        for (int l = 0; l < 64; ++l) {
            int i1 = x + rand.nextInt(8) - rand.nextInt(8);
            int j1 = y + rand.nextInt(4) - rand.nextInt(4);
            int k1 = z + rand.nextInt(8) - rand.nextInt(8);
            if (j1 < 1 || j1 >= 255 || Math.abs((i1 >> 4) - (cx >> 4)) > 1 || Math.abs((k1 >> 4) - (cz >> 4)) > 1) continue;
            BlockPos pos = new BlockPos(i1, j1, k1);

            if (world.getBlockState(pos).isAir() && canBlockStay(world, pos)) {
                world.setBlock(pos, block.defaultBlockState(), 2);
            }
        }
    }

    /** {@code BlockMushroom.canBlockStay}: Myzel/Podsol oder Licht &lt; 13 auf fester Oberseite. */
    private static boolean canBlockStay(WorldGenLevel world, BlockPos pos) {
        BlockState below = world.getBlockState(pos.below());
        if (below.is(Blocks.MYCELIUM) || below.is(Blocks.PODZOL)) return true;
        // Licht ist waehrend der Dekoration noch nicht berechnet: Himmelslicht 15 ueber der Hoehenkarte, darunter dunkel
        boolean skylit = pos.getY() >= world.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());
        return !skylit && below.isFaceSturdy(world, pos.below(), Direction.UP);
    }
}
