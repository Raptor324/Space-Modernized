package com.space_m.rocket;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Die von der VAB dynamisch gesetzten Arbeitsbuehnen und Stuetzsaeulen. Im Original sind das {@code machine_rocket_assembly}-
 * Dummybloecke mit Richtungs-Meta ({@code addPlatform}: WEST/EAST/NORTH/SOUTH zur Mitte hin, Saeulen UP), unsichtbar
 * ({@code getRenderType -1}); der BER zeichnet "Level"/"Support". {@code BlockDummyable.findCore} laeuft entgegen der Meta-
 * Richtung bis zum Kern.
 *
 * <p>Abweichung (technisch noetig): HBMs Dummy-Teil ({@code universal_machine_part}) leitet Form und Kern aus der festen
 * Struktur ab und kennt keine nachtraeglich gesetzten Zellen; deshalb ein eigener unsichtbarer Vollblock mit Richtung
 * ({@link #FACING} = Original-Meta). Klick oeffnet die VAB, Abbau bricht wie im Original den Kern (und damit alles) ab,
 * verwaiste Teile (kein Kern erreichbar) entfernen sich selbst.
 */
public class RocketAssemblyPartBlock extends Block {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public RocketAssemblyPartBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    /** Original {@code BlockDummyable.findCore}: entgegen der Richtung laufen, bis der Kern erreicht ist. */
    @Nullable
    public static BlockPos findCore(BlockGetter level, BlockPos pos) {
        BlockPos cur = pos;
        for (int i = 0; i < 300; i++) {
            BlockState state = level.getBlockState(cur);
            if (state.getBlock() instanceof MachineRocketAssemblyBlock) return cur;
            // Eckssaeulen enden auf der Deckplatte (HBM-Dummy mit Kernzeiger)
            if (level.getBlockEntity(cur) instanceof com.hbm_m.interfaces.IMultiblockPart part) {
                BlockPos controller = part.getControllerPos();
                return controller != null && level.getBlockState(controller).getBlock() instanceof MachineRocketAssemblyBlock ? controller : null;
            }
            if (!(state.getBlock() instanceof RocketAssemblyPartBlock)) return null;
            cur = cur.relative(state.getValue(FACING).getOpposite());
        }
        return null;
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return useCore(level, pos, player);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return useCore(level, pos, player);
    }
    *///?}

    private static InteractionResult useCore(Level level, BlockPos pos, Player player) {
        BlockPos core = findCore(level, pos);
        if (core == null) return InteractionResult.PASS;
        return MachineRocketAssemblyBlock.openMenu(level, core, player);
    }

    /** Original {@code BlockDummyable.breakBlock} eines Dummys: den Kern abbauen (sofern nicht {@code safeRem}). */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide() && !MachineRocketAssemblyBlockEntity.safeRem) {
            BlockPos core = findCoreFrom(level, pos, state.getValue(FACING));
            if (core != null) level.destroyBlock(core, true);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Nullable
    private static BlockPos findCoreFrom(Level level, BlockPos pos, Direction facing) {
        return findCore(level, pos.relative(facing.getOpposite()));
    }

    /** Verwaiste Teile (Original {@code onNeighborBlockChange} -> Kern fehlt) entfernen sich. */
    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean moving) {
        super.neighborChanged(state, level, pos, block, fromPos, moving);
        if (!level.isClientSide()) level.scheduleTick(pos, this, 1);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (findCore(level, pos) == null) {
            MachineRocketAssemblyBlockEntity.runSafe(() -> level.removeBlock(pos, false));
        }
    }

    //? if < 1.21.1 {
    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return new ItemStack(SpaceRocketBlocks.MACHINE_ROCKET_ASSEMBLY.get());
    }
    //?} else {
    /*@Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(SpaceRocketBlocks.MACHINE_ROCKET_ASSEMBLY.get());
    }
    *///?}

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<RocketAssemblyPartBlock> CODEC = simpleCodec(RocketAssemblyPartBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends Block> codec() {
        return CODEC;
    }
    *///?}
}
