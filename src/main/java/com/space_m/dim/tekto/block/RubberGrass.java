package com.space_m.dim.tekto.block;

import com.space_m.dim.tekto.TektoBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code com.hbm.blocks.RubberGrass} ({@code rubber_grass}): Oberseite {@code rubber_grass_top}, Seiten
 * {@code rubber_grass_side}, unten {@code rubber_silt}; droppt sich selbst. Wird Fluessigkeit oder ein normaler
 * Wuerfel darauf gesetzt, wird er zu Tekto-Schlick ({@code onNeighborBlockChange}). Kein Zufallsticken
 * ({@code tick = false}), {@code canSustainPlant = false}.
 */
public class RubberGrass extends Block {

    public RubberGrass(Properties properties) {
        super(properties);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (dir == Direction.UP) {
            BlockState b = level.getBlockState(pos.above());
            if (!b.getFluidState().isEmpty() || b.isRedstoneConductor(level, pos.above())) {
                return TektoBlocks.RUBBER_SILT.get().defaultBlockState();
            }
        }
        return super.updateShape(state, dir, neighbor, level, pos, neighborPos);
    }
}
