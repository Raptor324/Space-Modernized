package com.space_m.dim.laythe.block;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

/**
 * 1:1 {@code com.hbm.blocks.BlockWaterPlant} ({@code laythe_short} "Laythe Seagrass", {@code laythe_glow}
 * "Laythe Glowgrass"): Pflanze aus {@code Material.water}, Kreuzgitter-Darstellung (Render-Typ 6 = {@code crop}),
 * keine Kollision, gesetzt nur mit Fluessigkeit darueber ({@code canPlaceBlockAt}). Beute 0-3 Salzblatt
 * ({@code quantityDropped = nextInt(4)}, Loot-Tabelle).
 *
 * <p>{@code Material.water} des Originals: der Block zaehlt als Wasser (Schwimmen, Wasserfluss) - hier ueber eine
 * dauerhafte Wasser-Fluessigkeit im Block ({@link #getFluidState}) wie Vanilla-Seegras.
 */
public class BlockWaterPlant extends Block {

    public BlockWaterPlant(Properties properties) {
        super(properties);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return Fluids.WATER.getSource(false);
    }

    /** {@code canPlaceBlockAt}: {@code super} (ersetzbar) {@code && canBlockStay && Fluessigkeit darueber}. */
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState above = ctx.getLevel().getBlockState(ctx.getClickedPos().above());
        if (above.getFluidState().isEmpty()) return null;
        return super.getStateForPlacement(ctx);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        return super.updateShape(state, dir, neighbor, level, pos, neighborPos);
    }
}
