package com.space_m.machine.chem;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.interfaces.ILookOverlay;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
 * 1:1 NTM Space {@code MachineVacuumCircuit} ({@code machine_vacuum_circuit}, "Vacuum Solderer"): {@code getDimensions
 * {0, 0, 1, 1, 1, 1}}, Offset 0, alle acht Nebenzellen sind Anschluesse ({@code TileEntityProxyCombo().inventory().power()
 * .fluid()}). Overlay blinkt "REQUIRES VACUUM" ({@code atmosphere.noAir}), wenn Luftdruck &gt; 0.001.
 * Kein Tooltip (Original ohne {@code ITooltipProvider}; der Lang-Schluessel {@code .desc} existiert, wird aber nie gezeigt).
 * Original-Klasse: MachineVacuumCircuit
 */
public class MachineVacuumCircuit extends DummyableMachineBlock implements ILookOverlay {

    public MachineVacuumCircuit(Properties properties) {
        super(properties);
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        DummyableStructureBuilder b = DummyableStructureBuilder.create().box(0, 0, 1, 1, 1, 1);
        for (int f = -1; f <= 1; f++) for (int s = -1; s <= 1; s++) if (f != 0 || s != 0) b.extra(f, 0, s);
        return b.placementOffset(0).build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineVacuumCircuit(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ChemMachines.VACUUM_CIRCUIT_BE.get(), TileEntityMachineVacuumCircuit::tick);
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return ChemMachines.standardOpen(level, pos, player);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return ChemMachines.standardOpen(level, pos, player);
    }
    *///?}

    @Override
    public void printHook(GuiGraphics g, Level world, BlockPos pos) {
        if (!(world.getBlockEntity(pos) instanceof TileEntityMachineVacuumCircuit machine)) return;

        List<Component> text = ChemUtil.lines();
        if (!machine.canOperate) text.add(ChemUtil.blinkWarning("atmosphere.noAir"));
        if (text.isEmpty()) return;

        ILookOverlay.printGeneric(g, Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineVacuumCircuit> CODEC = simpleCodec(MachineVacuumCircuit::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
