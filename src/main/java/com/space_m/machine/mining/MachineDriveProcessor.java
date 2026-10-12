package com.space_m.machine.mining;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1:1 NTM Space {@code MachineDriveProcessor} ({@code machine_drive_processor}, "Drive Processor"): {@code getDimensions
 * {0, 0, 0, 0, 0, 1}} (zwei Bloecke), Offset 0; die Nebenzelle ist {@code TileEntityProxyCombo(true, true, false)}
 * (Inventar + Strom, hier Anschlusszelle). Rechtsklick oeffnet das GUI ({@code standardOpenBehavior}).
 * Original-Klasse: MachineDriveProcessor
 */
public class MachineDriveProcessor extends DummyableMachineBlock {

    public MachineDriveProcessor(Properties properties) {
        super(properties);
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        // {up, down, north, south, west, east} = {0, 0, 0, 0, 0, 1}; Nebenzelle (Osten kanonisch = -rot) als Anschluss
        return DummyableStructureBuilder.create().box(0, 0, 0, 0, 0, 1).extra(0, 0, -1)
                .placementOffset(0).build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    /** TESR ({@code RenderDriveProcessor}). */
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineDriveProcessor(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, MiningMachines.DRIVE_PROCESSOR_BE.get(), TileEntityMachineDriveProcessor::tick);
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return onActivated(level, pos, player);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return onActivated(level, pos, player);
    }
    *///?}

    /** Original {@code standardOpenBehavior(world, x, y, z, player, 0)}: Client SUCCESS, geschlichen nichts, sonst GUI. */
    private InteractionResult onActivated(Level world, BlockPos pos, Player player) {
        if (world.isClientSide) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        if (!(world.getBlockEntity(pos) instanceof TileEntityMachineDriveProcessor processor)) return InteractionResult.PASS;

        MenuRegistry.openExtendedMenu((ServerPlayer) player, processor, buf -> buf.writeBlockPos(pos));
        return InteractionResult.SUCCESS;
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineDriveProcessor> CODEC = simpleCodec(MachineDriveProcessor::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
