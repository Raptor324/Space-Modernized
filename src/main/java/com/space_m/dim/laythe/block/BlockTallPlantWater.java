package com.space_m.dim.laythe.block;

import org.jetbrains.annotations.Nullable;

import com.space_m.dim.laythe.LaytheBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * 1:1 {@code com.hbm.blocks.generic.BlockTallPlantWater} ({@code plant_tall_laythe}, einzige Sorte
 * {@code EnumTallPlantWater.LAYTHE} "Saltgrass"): zweiteilige Unterwasserpflanze aus {@code Material.water},
 * Kreuzgitter-Darstellung (Render-Typ 6), unten auf Laythe-Schlick. Metadatum &lt; 8 = untere Haelfte, +8 = obere
 * ({@link #HALF}).
 *
 * <ul>
 *   <li>{@code checkAndDropBlock}: ohne Halt wird der Block zu Wasser ({@code detectCut} wirkt auf Schlick nicht).</li>
 *   <li>Beute: Original wirft beim Abbau einer Haelfte zweimal 0-3 Salzblatt (eigene Haelfte + {@code
 *       dropBlockAsItem} oberhalb); hier hat jede Haelfte eine Loot-Tabelle mit zwei Wuerfen, die andere Haelfte
 *       verschwindet ohne Beute.</li>
 *   <li>{@code canReplace}: setzbar, wenn ueber der Stelle Wasser ist und der Block halten kann.</li>
 *   <li>Wachstum ({@code IGrowable}): {@code func_149851_a} = false - Knochenmehl wirkt nicht.</li>
 * </ul>
 */
public class BlockTallPlantWater extends BlockWaterPlant {

    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;

    public BlockTallPlantWater(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF);
    }

    /** {@code canPlaceBlockOn}. */
    protected boolean canPlaceBlockOn(BlockState state) {
        return state.is(LaytheBlocks.LAYTHE_SILT.get());
    }

    /** {@code canBlockStay}. */
    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        if (state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            BlockState below = level.getBlockState(pos.below());
            return below.is(this) && below.getValue(HALF) == DoubleBlockHalf.LOWER;
        }
        return canPlaceBlockOn(level.getBlockState(pos.below()));
    }

    /** {@code canReplace}: Wasser darueber und haltbar. */
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockPos pos = ctx.getClickedPos();
        Level level = ctx.getLevel();
        if (pos.getY() >= level.getMaxBuildHeight() - 1) return null;
        if (!level.getBlockState(pos.above()).is(Blocks.WATER)) return null;
        BlockState state = defaultBlockState();
        return canSurvive(state, level, pos) ? state : null;
    }

    /** {@code onBlockPlacedBy}: obere Haelfte ({@code meta + 8}) darueber setzen. */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity entity, ItemStack stack) {
        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), 2);
    }

    /** {@code onNeighborBlockChange} -> {@code checkAndDropBlock} (inkl. {@code detectCut}). */
    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        super.updateShape(state, dir, neighbor, level, pos, neighborPos);
        if (!canSurvive(state, level, pos)) return Blocks.WATER.defaultBlockState();
        // detectCut greift nur, wenn plant_flower an der Stelle halten koennte (Gras/Erde darunter) - auf
        // Laythe-Schlick nie, die untere Haelfte bleibt also wie im Original stehen.
        return state;
    }
}
