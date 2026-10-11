package com.space_m.dim.feature;

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
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * {@code space_m:depth_deposit} = 1:1 {@code DepthDeposit.generateCondition(world, x, yMin, yDev, z, size, fill,
 * block, rand, chance, genTarget, filler)} wie in {@code WorldGeneratorCelestial.generateNTMOres}: kugelige
 * Tiefenerz-Lagerstaette in der Grundgesteinsschicht, Huelle aus {@code filler} (Tiefengestein); ersetzt
 * Planetengestein und Grundgestein. Hoehen wie im Original absolut (Space-Dimensionen beginnen bei y 0).
 */
public class DepthDepositFeature extends Feature<DepthDepositFeature.Config> {

    public DepthDepositFeature() {
        super(Config.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> ctx) {
        Config c = ctx.config();
        Random rand = LegacyWorld.random(ctx.random());
        int x = ctx.origin().getX() & ~15;
        int z = ctx.origin().getZ() & ~15;

        if (rand.nextInt(c.chance) == 0) {
            generate(ctx.level(), x + rand.nextInt(16) + 8, c.minY + rand.nextInt(c.yDev), z + rand.nextInt(16) + 8, c.size, c.fill, c.ore, rand, c.target, c.filler, 0);
            return true;
        }
        return false;
    }

    /** {@code DepthDeposit.generate}. */
    public static void generate(WorldGenLevel world, int x, int y, int z, int size, double fill, BlockState block, Random rand, BlockState genTarget, BlockState filler, int bottom) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockState bedrock = Blocks.BEDROCK.defaultBlockState();

        for (int i = x - size; i <= x + size; i++) {
            for (int j = y - size; j <= y + size; j++) {
                for (int k = z - size; k <= z + size; k++) {

                    if (j < bottom + 1 || j > bottom + 126)
                        continue;

                    double dx = x - i, dy = y - j, dz = z - k;
                    double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    pos.set(i, j, k);

                    if (LegacyWorld.isReplaceableOreGen(world, pos, genTarget) || LegacyWorld.isReplaceableOreGen(world, pos, bedrock)) { //yes you've heard right, bedrock

                        if (len + rand.nextInt(2) < size * fill) {
                            world.setBlock(pos, block, 3);

                        } else if (len + rand.nextInt(2) <= size) {
                            world.setBlock(pos, filler, 3);
                        }
                    }
                }
            }
        }
    }

    public static final class Config implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockState.CODEC.fieldOf("ore").forGetter(c -> c.ore),
                BlockState.CODEC.fieldOf("target").forGetter(c -> c.target),
                BlockState.CODEC.fieldOf("filler").forGetter(c -> c.filler),
                Codec.INT.fieldOf("min_y").forGetter(c -> c.minY),
                Codec.INT.fieldOf("y_dev").forGetter(c -> c.yDev),
                Codec.INT.fieldOf("size").forGetter(c -> c.size),
                Codec.DOUBLE.fieldOf("fill").forGetter(c -> c.fill),
                Codec.INT.fieldOf("chance").forGetter(c -> c.chance)
        ).apply(i, Config::new));

        public final BlockState ore, target, filler;
        public final int minY, yDev, size, chance;
        public final double fill;

        public Config(BlockState ore, BlockState target, BlockState filler, int minY, int yDev, int size, double fill, int chance) {
            this.ore = ore;
            this.target = target;
            this.filler = filler;
            this.minY = minY;
            this.yDev = yDev;
            this.size = size;
            this.fill = fill;
            this.chance = chance;
        }
    }
}
