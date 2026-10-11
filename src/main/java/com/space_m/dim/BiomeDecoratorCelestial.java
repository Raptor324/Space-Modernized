package com.space_m.dim;

import java.util.Random;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.space_m.dim.legacy.LegacyWorld;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.LakeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

/**
 * 1:1 {@code com.hbm.dim.BiomeDecoratorCelestial.genDecorations} als Feature {@code space_m:celestial_decorator}:
 * Lava-Quellen ({@code lavaCount}, {@link WorldGenLiquidsCelestial} im Planetengestein, nur bei
 * {@code generateLakes}) und echte Seen ({@code lakeChancePerChunk}, {@code WorldGenLakes} = Vanilla
 * {@link LakeFeature}, y 0-255).
 *
 * <p>{@code generateOres()} (Vanilla-Erze, ersetzen nur {@code stone}) ist auf den Koerpern wirkungslos - die
 * Vanilla-Erze im Planetengestein kommen aus {@code WorldGeneratorCelestial.generateVanillaOres}
 * ({@code space_m:celestial_ore}, {@code "distribution": "vanilla"}). Wasserpflanzen/Gummibaeume/Korallen
 * ({@code waterPlantsPerChunk}/{@code rubberPlantsPerChunk}/{@code coralPerChunk}) gehoeren zu Laythe/Tekto und
 * kommen mit deren Bloecken (R3) als eigene Features in die jeweiligen Biome.
 */
public class BiomeDecoratorCelestial extends Feature<BiomeDecoratorCelestial.Config> {

    public BiomeDecoratorCelestial() {
        super(Config.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> ctx) {
        Config c = ctx.config();
        WorldGenLevel world = ctx.level();
        Random rand = LegacyWorld.random(ctx.random());
        int chunk_X = ctx.origin().getX() & ~15;
        int chunk_Z = ctx.origin().getZ() & ~15;

        if (c.generateLakes) {
            WorldGenLiquidsCelestial liquids = new WorldGenLiquidsCelestial(Blocks.LAVA.defaultBlockState(), c.stoneBlock);
            for (int i = 0; i < c.lavaCount; ++i) {
                int x = chunk_X + rand.nextInt(16) + 8;
                int y = rand.nextInt(rand.nextInt(rand.nextInt(240) + 8) + 8);
                int z = chunk_Z + rand.nextInt(16) + 8;
                liquids.generate(world, rand, x, y, z);
            }
        }

        if (c.lakeChancePerChunk > 0) {
            if (rand.nextInt(c.lakeChancePerChunk) == 0) {
                int x = chunk_X + rand.nextInt(16) + 8;
                int y = rand.nextInt(256);
                int z = chunk_Z + rand.nextInt(16) + 8;
                // R3-1: 1.7.10 WorldGenLakes.generate: Ecke x-8/z-8, durch Luft bis zum Boden absteigen (y > 5),
                // Waende nur bei Lava (und dann Blocks.stone); LakeFeature selbst geht danach 4 Bloecke tiefer.
                x -= 8;
                z -= 8;
                while (y > 5 && world.isEmptyBlock(new BlockPos(x, y, z))) --y;
                if (y > 4) {
                    BlockState barrier = c.lakeBlock.getFluidState().is(net.minecraft.tags.FluidTags.LAVA) ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState();
                    @SuppressWarnings("deprecation")
                    LakeFeature.Configuration lake = new LakeFeature.Configuration(BlockStateProvider.simple(c.lakeBlock), BlockStateProvider.simple(barrier));
                    Feature.LAKE.place(lake, world, ctx.chunkGenerator(), ctx.random(), new BlockPos(x, y, z));
                }
            }
        }

        return true;
    }

    public static final class Config implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockState.CODEC.fieldOf("stone").forGetter(c -> c.stoneBlock),
                Codec.BOOL.optionalFieldOf("generate_lakes", true).forGetter(c -> c.generateLakes),
                Codec.INT.optionalFieldOf("lava_count", 20).forGetter(c -> c.lavaCount),
                Codec.INT.optionalFieldOf("lake_chance", 0).forGetter(c -> c.lakeChancePerChunk),
                BlockState.CODEC.optionalFieldOf("lake_block", Blocks.WATER.defaultBlockState()).forGetter(c -> c.lakeBlock)
        ).apply(i, Config::new));

        /** {@code BiomeDecoratorCelestial(stoneBlock)}. */
        public final BlockState stoneBlock;
        /** {@code BiomeDecorator.generateLakes} (Vanilla-Standard true). */
        public final boolean generateLakes;
        /** {@code lavaCount} (20). */
        public final int lavaCount;
        /** {@code lakeChancePerChunk} (0 = keine). */
        public final int lakeChancePerChunk;
        /** {@code lakeBlock} (Wasser). */
        public final BlockState lakeBlock;

        public Config(BlockState stoneBlock, boolean generateLakes, int lavaCount, int lakeChancePerChunk, BlockState lakeBlock) {
            this.stoneBlock = stoneBlock;
            this.generateLakes = generateLakes;
            this.lavaCount = lavaCount;
            this.lakeChancePerChunk = lakeChancePerChunk;
            this.lakeBlock = lakeBlock;
        }
    }
}
