package com.space_m.dim.laythe.block;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

/**
 * 1:1 {@code com.hbm.blocks.BlockCoral} ({@code laythe_coral}, {@code EnumCoral} TUBE/BRAIN/BUBBLE/FIRE/HORN):
 * Koralle aus {@code Material.water}, keine Kollision, setzbar nur mit Fluessigkeit darueber. Je Sorte ein Block
 * {@code laythe_coral_<sorte>} (1.20 ohne Metadaten; Name wie im Original fuer alle {@code tile.laythe_coral.name}).
 *
 * <p>{@code RenderCoral}: gekreuzte Flaechen, ausgerichtet am ersten festen Nachbarn in der Reihenfolge
 * N, S, W, O, oben, unten (sonst unten) -> Eigenschaft {@link #ATTACH} + gedrehte Kreuzmodelle.
 * Beute {@code powder_calcium} = {@code hbm_m:calcium_powder} (Loot-Tabelle, R4-B).
 */
// Original-Klasse: RenderCoral
// Original-Block: laythe_coral (je Sorte laythe_coral_<sorte>, LaytheBlocks)
// Original-Block: laythe_coral_block (je Sorte laythe_coral_block_<sorte>, LaytheBlocks)
public class BlockCoral extends BlockWaterPlant {

    public static final DirectionProperty ATTACH = BlockStateProperties.FACING;

    private static final Direction[] DIRECTIONS = { Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.UP, Direction.DOWN };

    public BlockCoral(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ATTACH, Direction.DOWN));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ATTACH);
    }

    /** {@code RenderCoral.getAttachment}. */
    public static Direction attachment(BlockGetter level, BlockPos pos) {
        for (Direction dir : DIRECTIONS) {
            BlockPos n = pos.relative(dir);
            if (level.getBlockState(n).isFaceSturdy(level, n, dir.getOpposite())) return dir;
        }
        return Direction.DOWN;
    }

    public BlockState attached(BlockGetter level, BlockPos pos) {
        return defaultBlockState().setValue(ATTACH, attachment(level, pos));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState s = super.getStateForPlacement(ctx);
        return s == null ? null : attached(ctx.getLevel(), ctx.getClickedPos());
    }

    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        super.updateShape(state, dir, neighbor, level, pos, neighborPos);
        return state.setValue(ATTACH, attachment(level, pos));
    }
}
