package com.space_m.dim.laythe;

import java.util.Random;

import com.space_m.dim.legacy.LegacyWorld;
import com.space_m.dim.legacy.WorldGenMinable;

import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * {@code space_m:vanilla_biome_ores} = 1:1 1.7.10 {@code BiomeDecorator.generateOres()}, das
 * {@code BiomeDecoratorCelestial.genDecorations} als Erstes aufruft. Wirkt nur dort, wo das Planetengestein
 * {@code stone} ist (Laythe): Erde, Kies, Eisen, Gold, Redstone, Diamant, Lapis in Stein. Kohle verbietet
 * {@code WorldGeneratorCelestial.onGenerateOre} auf allen Himmelskoerpern ({@code Result.DENY} - kein Aufruf).
 */
public class VanillaBiomeOres extends Feature<NoneFeatureConfiguration> {

    public VanillaBiomeOres() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel world = ctx.level();
        Random rand = LegacyWorld.random(ctx.random());
        int x = ctx.origin().getX() & ~15;
        int z = ctx.origin().getZ() & ~15;
        BlockState stone = Blocks.STONE.defaultBlockState();

        genStandardOre1(world, rand, x, z, 20, new WorldGenMinable(Blocks.DIRT.defaultBlockState(), 32, stone), 0, 256);
        genStandardOre1(world, rand, x, z, 10, new WorldGenMinable(Blocks.GRAVEL.defaultBlockState(), 32, stone), 0, 256);
        // coalGen: DENY (WorldGeneratorCelestial.onGenerateOre)
        genStandardOre1(world, rand, x, z, 20, new WorldGenMinable(Blocks.IRON_ORE.defaultBlockState(), 8, stone), 0, 64);
        genStandardOre1(world, rand, x, z, 2, new WorldGenMinable(Blocks.GOLD_ORE.defaultBlockState(), 8, stone), 0, 32);
        genStandardOre1(world, rand, x, z, 8, new WorldGenMinable(Blocks.REDSTONE_ORE.defaultBlockState(), 7, stone), 0, 16);
        genStandardOre1(world, rand, x, z, 1, new WorldGenMinable(Blocks.DIAMOND_ORE.defaultBlockState(), 7, stone), 0, 16);
        genStandardOre2(world, rand, x, z, 1, new WorldGenMinable(Blocks.LAPIS_ORE.defaultBlockState(), 6, stone), 16, 16);
        return true;
    }

    /** {@code genStandardOre1}. */
    private static void genStandardOre1(WorldGenLevel world, Random rand, int cx, int cz, int count, WorldGenMinable gen, int minY, int maxY) {
        for (int l = 0; l < count; ++l) {
            int i1 = cx + rand.nextInt(16);
            int j1 = rand.nextInt(maxY - minY) + minY;
            int k1 = cz + rand.nextInt(16);
            gen.generate(world, rand, i1, j1, k1);
        }
    }

    /** {@code genStandardOre2}. */
    private static void genStandardOre2(WorldGenLevel world, Random rand, int cx, int cz, int count, WorldGenMinable gen, int center, int spread) {
        for (int l = 0; l < count; ++l) {
            int i1 = cx + rand.nextInt(16);
            int j1 = rand.nextInt(spread) + rand.nextInt(spread) + (center - spread);
            int k1 = cz + rand.nextInt(16);
            gen.generate(world, rand, i1, j1, k1);
        }
    }
}
