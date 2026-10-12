package com.space_m.rocket.pad;

import org.jetbrains.annotations.Nullable;

import com.space_m.block.SpaceBlocks;
import com.space_m.blockentity.SpaceBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Turm- und Plattformzellen der Raketen-Startrampe. Im Original setzt {@code TileEntityLaunchPadRocket} zur Laufzeit
 * weitere {@code launch_pad_rocket}-Dummies (Meta UP fuer den Turm, dir/rot fuer die Plattform), die ueber
 * {@code findCore} zum Kern fuehren.
 *
 * <p><b>Technisch noetige Abweichung:</b> eigener Block {@code launch_pad_rocket_tower} statt HBM-Phantomteilen, weil
 * HBM-Teile ihre Kollisionsform aus der festen Strukturkarte des Kerns beziehen - Zellen ausserhalb der Karte waeren
 * nicht begehbar (Plattform/Leiter). Verhalten wie ein Dummy: voller Block, unsichtbar (der Kern-Renderer zeichnet
 * den Turm), Rechtsklick oeffnet die Rampe, Abbau reisst die ganze Rampe ab ({@code BlockDummyable.breakBlock}).
 */
public class LaunchPadRocketTowerBlock extends BaseEntityBlock {

    /** Original Meta-Richtung der Dummy-Zelle. */
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    /** Original {@code BlockDummyable.safeRem}: beim Auf-/Abbau durch die Rampe selbst keine Kettenreaktion. */
    public static boolean safeRem = false;

    public LaunchPadRocketTowerBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP));
    }

    public static BlockState state(Direction dir) {
        return SpaceBlocks.LAUNCH_PAD_ROCKET_TOWER.get().defaultBlockState().setValue(FACING, dir);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    /** Original: Dummies sind nicht lichtundurchlaessig (Himmelspruefung der Rampe). */
    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TowerBlockEntity(pos, state);
    }

    @Nullable
    public static BlockPos findCore(BlockGetter level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof TowerBlockEntity tower) return tower.core;
        return null;
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        BlockPos core = findCore(level, pos);
        if (core == null) return InteractionResult.PASS;
        BlockState coreState = level.getBlockState(core);
        if (!(coreState.getBlock() instanceof LaunchPadRocket)) return InteractionResult.PASS;
        return coreState.use(level, player, hand, hit.withPosition(core));
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        BlockPos core = findCore(level, pos);
        if (core == null) return InteractionResult.PASS;
        BlockState coreState = level.getBlockState(core);
        if (!(coreState.getBlock() instanceof LaunchPadRocket)) return InteractionResult.PASS;
        return coreState.useWithoutItem(level, player, hit.withPosition(core));
    }
    *///?}

    /** Original {@code BlockDummyable.breakBlock}: ein abgebauter Dummy reisst den Kern (und damit die Rampe) mit. */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && !level.isClientSide && !safeRem) {
            BlockPos core = findCore(level, pos);
            if (core != null && level.getBlockState(core).getBlock() instanceof LaunchPadRocket) {
                level.destroyBlock(core, true);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<LaunchPadRocketTowerBlock> CODEC = simpleCodec(LaunchPadRocketTowerBlock::new);

    @Override
    protected com.mojang.serialization.MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }
    *///?}

    /** Merkt sich den Kern der Rampe (Ersatz fuer {@code findCore} ueber die Meta-Kette). */
    public static class TowerBlockEntity extends BlockEntity {

        @Nullable BlockPos core;

        public TowerBlockEntity(BlockPos pos, BlockState state) {
            super(SpaceBlockEntities.LAUNCH_PAD_ROCKET_TOWER.get(), pos, state);
        }

        public void setCore(BlockPos core) {
            this.core = core;
            setChanged();
        }

        //? if < 1.21.1 {
        @Override
        protected void saveAdditional(CompoundTag tag) {
            super.saveAdditional(tag);
            if (core != null) tag.putLong("core", core.asLong());
        }

        @Override
        public void load(CompoundTag tag) {
            super.load(tag);
            core = tag.contains("core") ? BlockPos.of(tag.getLong("core")) : null;
        }
        //?} else {
        /*@Override
        protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
            super.saveAdditional(tag, registries);
            if (core != null) tag.putLong("core", core.asLong());
        }

        @Override
        protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
            super.loadAdditional(tag, registries);
            core = tag.contains("core") ? BlockPos.of(tag.getLong("core")) : null;
        }
        *///?}
    }
}
