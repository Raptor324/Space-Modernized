package com.space_m.machine.chem;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.interfaces.IItemFluidIdentifier;
import com.hbm_m.interfaces.ILookOverlay;
import com.hbm_m.multiblock.DummyableStructureBuilder;
import com.hbm_m.multiblock.MultiblockStructureHelper;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1:1 NTM Space {@code MachineAlkylation} ({@code machine_alkylation}, "Alkylation Unit"): {@code getDimensions
 * {3, 0, 2, 2, 1, 1}}, Offset 2, sechs Anschlusszellen ({@code makeExtra}) bei {@code +-rot} und {@code +-rot +- dir*2}
 * (Strom + Fluid). Rechtsklick mit Fluid-Identifikator setzt den Eingangstank ("Changed type to ...!"), Overlay mit
 * Strom und den belegten Tanks.
 * Original-Klasse: MachineAlkylation
 */
public class MachineAlkylation extends DummyableMachineBlock implements ILookOverlay {

    public MachineAlkylation(Properties properties) {
        super(properties);
    }

    @Override
    protected MultiblockStructureHelper defineStructure() {
        return DummyableStructureBuilder.create()
                .box(3, 0, 2, 2, 1, 1)
                .extra(0, 0, 1)
                .extra(2, 0, 1)
                .extra(-2, 0, 1)
                .extra(0, 0, -1)
                .extra(2, 0, -1)
                .extra(-2, 0, -1)
                .placementOffset(2)
                .build(() -> ModBlocks.UNIVERSAL_MACHINE_PART.get().defaultBlockState());
    }

    /** Original {@code getRenderType() = -1} (TESR). */
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineAlkylation(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ChemMachines.ALKYLATION_BE.get(), TileEntityMachineAlkylation::tick);
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return onActivated(level, pos, player, player.getItemInHand(hand));
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return onActivated(level, pos, player, player.getItemInHand(InteractionHand.MAIN_HAND));
    }
    *///?}

    /** Original {@code onBlockActivated}: Server, nicht geschlichen, Fluid-Identifikator -> Tank 0. */
    private InteractionResult onActivated(Level world, BlockPos pos, Player player, ItemStack held) {
        if (!world.isClientSide && !player.isShiftKeyDown()) {
            if (!held.isEmpty() && held.getItem() instanceof IItemFluidIdentifier identifier) {
                if (!(world.getBlockEntity(pos) instanceof TileEntityMachineAlkylation alkylation)) return InteractionResult.PASS;

                Fluid type = identifier.getType(world, pos, held);
                alkylation.tanks[0].setTankType(type);
                alkylation.setChanged();
                player.sendSystemMessage(Component.literal("Changed type to ").withStyle(ChatFormatting.YELLOW)
                        .append(ChemUtil.fluidName(type).copy().withStyle(ChatFormatting.YELLOW))
                        .append(Component.literal("!").withStyle(ChatFormatting.YELLOW)));
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        } else {
            return InteractionResult.SUCCESS;
        }
    }

    @Override
    public void printHook(GuiGraphics g, Level world, BlockPos pos) {
        if (!(world.getBlockEntity(pos) instanceof TileEntityMachineAlkylation alkylation)) return;

        List<Component> text = ChemUtil.lines();
        text.add(ChemUtil.powerLine(alkylation.getEnergyStored(), alkylation.getEnergyStored() < alkylation.getMaxEnergyStored() / 20));

        for (int i = 0; i < alkylation.tanks.length; i++) {
            if (ChemUtil.isNone(alkylation.tanks[i].getTankType())) continue;
            text.add(ChemUtil.tankLine(alkylation.tanks[i], i < 2));
        }

        ILookOverlay.printGeneric(g, Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }

    //? if >1.20.1 {
    /*public static final com.mojang.serialization.MapCodec<MachineAlkylation> CODEC = simpleCodec(MachineAlkylation::new);
    @Override protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    *///?}
}
