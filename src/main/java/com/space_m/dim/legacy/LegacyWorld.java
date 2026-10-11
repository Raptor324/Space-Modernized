package com.space_m.dim.legacy;

import java.util.Random;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;

/** Welt-Zugriffe der 1.7.10-Dekoratoren auf einer Dekorations-Region ({@link WorldGenLevel}). */
public final class LegacyWorld {

    private LegacyWorld() {}

    /** Forge {@code Block.isReplaceableOreGen(world, x, y, z, target)}: gleicher Block wie {@code target}. */
    public static boolean isReplaceableOreGen(WorldGenLevel world, BlockPos pos, BlockState target) {
        if (!inBounds(world, pos)) return false;
        return world.getBlockState(pos).getBlock() == target.getBlock();
    }

    /** Liegt die Position in der Bauhoehe (1.7.10 hat dort einfach nichts gesetzt). */
    public static boolean inBounds(WorldGenLevel world, BlockPos pos) {
        return pos.getY() >= world.getMinBuildHeight() && pos.getY() < world.getMaxBuildHeight();
    }

    /** Ein {@link Random} aus dem Feature-Zufall (die Originale arbeiten mit {@code java.util.Random}). */
    public static Random random(RandomSource source) {
        return new Random(source.nextLong());
    }
}
