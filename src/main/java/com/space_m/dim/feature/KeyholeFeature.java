package com.space_m.dim.feature;

import java.util.Random;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.space_m.dim.legacy.LegacyWorld;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * {@code space_m:keyhole} = Ende von {@code WorldGeneratorCelestial.generateNTMOres}: in jedem 4. Chunk ein
 * Schluesselloch-Stein ({@code stone_keyhole}) bei y 6-18, wenn dort Planetengestein liegt.
 */
public class KeyholeFeature extends Feature<KeyholeFeature.Config> {

    public KeyholeFeature() {
        super(Config.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> ctx) {
        Config c = ctx.config();
        Random rand = LegacyWorld.random(ctx.random());
        int x = ctx.origin().getX() & ~15;
        int z = ctx.origin().getZ() & ~15;

        if (rand.nextInt(4) == 0) {
            int rx = x + rand.nextInt(16) + 8;
            int ry = 6 + rand.nextInt(13);
            int rz = z + rand.nextInt(16) + 8;
            BlockPos pos = new BlockPos(rx, ry, rz);

            if (LegacyWorld.isReplaceableOreGen(ctx.level(), pos, c.target)) {
                ctx.level().setBlock(pos, c.state, 3);
                return true;
            }
        }
        return false;
    }

    public static final class Config implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockState.CODEC.fieldOf("state").forGetter(c -> c.state),
                BlockState.CODEC.fieldOf("target").forGetter(c -> c.target)
        ).apply(i, Config::new));

        public final BlockState state;
        public final BlockState target;

        public Config(BlockState state, BlockState target) {
            this.state = state;
            this.target = target;
        }
    }
}
