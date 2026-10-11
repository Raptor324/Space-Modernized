package com.space_m.dim;

import java.util.Random;

import com.space_m.dim.legacy.LegacyWorld;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code com.hbm.dim.WorldGenLiquidsCelestial} (= Vanilla {@code WorldGenLiquids} mit waehlbarem Gestein):
 * Quelle in einer Wand aus drei Gesteinsseiten mit genau einer offenen Seite. Statt
 * {@code scheduledUpdatesAreImmediate + updateTick} wird ein Fluid-Tick geplant (1.20-Weltgen).
 */
public class WorldGenLiquidsCelestial {

    // Identical to WorldGenLiquids except you can specify the stone block to replace

    private final BlockState liquidBlock;
    private final Block targetBlock;

    public WorldGenLiquidsCelestial(BlockState liquidBlock, BlockState targetBlock) {
        this.liquidBlock = liquidBlock;
        this.targetBlock = targetBlock.getBlock();
    }

    private boolean is(WorldGenLevel world, int x, int y, int z, Block b) {
        BlockPos p = new BlockPos(x, y, z);
        return LegacyWorld.inBounds(world, p) && world.getBlockState(p).is(b);
    }

    private boolean isAir(WorldGenLevel world, int x, int y, int z) {
        BlockPos p = new BlockPos(x, y, z);
        return LegacyWorld.inBounds(world, p) && world.getBlockState(p).isAir();
    }

    public boolean generate(WorldGenLevel world, Random rand, int x, int y, int z) {
        if(!is(world, x, y + 1, z, targetBlock)) {
            return false;
        } else if(!is(world, x, y - 1, z, targetBlock)) {
            return false;
        } else if(!isAir(world, x, y, z) && !is(world, x, y, z, targetBlock)) {
            return false;
        } else {
            int l = 0;

            if(is(world, x - 1, y, z, targetBlock)) {
                ++l;
            }

            if(is(world, x + 1, y, z, targetBlock)) {
                ++l;
            }

            if(is(world, x, y, z - 1, targetBlock)) {
                ++l;
            }

            if(is(world, x, y, z + 1, targetBlock)) {
                ++l;
            }

            int i1 = 0;

            if(isAir(world, x - 1, y, z)) {
                ++i1;
            }

            if(isAir(world, x + 1, y, z)) {
                ++i1;
            }

            if(isAir(world, x, y, z - 1)) {
                ++i1;
            }

            if(isAir(world, x, y, z + 1)) {
                ++i1;
            }

            if(l == 3 && i1 == 1) {
                BlockPos pos = new BlockPos(x, y, z);
                world.setBlock(pos, this.liquidBlock, 2);
                if(!this.liquidBlock.getFluidState().isEmpty()) {
                    world.scheduleTick(pos, this.liquidBlock.getFluidState().getType(), 0);
                }
            }

            return true;
        }
    }
}
