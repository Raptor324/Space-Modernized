package com.space_m.dim.laythe.block;

import org.jetbrains.annotations.Nullable;

import com.space_m.dim.laythe.LaytheBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * 1:1 {@code com.hbm.blocks.BlockKelp} ({@code laythe_kelp}): Seetang-Saeule aus {@code Material.water},
 * Kreuz-Darstellung (Render-Typ 1). Steht auf sich selbst oder Laythe-Schlick, sonst wird er zu Wasser
 * ({@code onNeighborBlockChange}). Metadatum 0 = Kelp darueber (Textur {@code laythe_kelp}), 8 = Spitze
 * ({@code laythe_kelp_top}) -> Eigenschaft {@link #TOP}. Beute 0-3 Salzblatt.
 */
public class BlockKelp extends BlockWaterPlant {

    /** Original Metadatum 8 (Spitze, nichts Gleiches darueber). */
    public static final BooleanProperty TOP = BooleanProperty.create("top");

    public BlockKelp(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(TOP, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TOP);
    }

    /** {@code canBlockStay}/{@code canPlaceBlockAt}: darunter Kelp oder Laythe-Schlick. */
    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        return below.is(this) || below.is(LaytheBlocks.LAYTHE_SILT.get());
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        if (!canSurvive(defaultBlockState(), ctx.getLevel(), ctx.getClickedPos())) return null;
        return withTop(defaultBlockState(), ctx.getLevel(), ctx.getClickedPos());
    }

    private BlockState withTop(BlockState state, BlockGetter level, BlockPos pos) {
        return state.setValue(TOP, !level.getBlockState(pos.above()).is(this));
    }

    /** {@code onNeighborBlockChange}: ohne Halt zu Wasser, sonst Metadatum nachfuehren. */
    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (!canSurvive(state, level, pos)) return Blocks.WATER.defaultBlockState();
        super.updateShape(state, dir, neighbor, level, pos, neighborPos);
        return withTop(state, level, pos);
    }
}
