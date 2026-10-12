package com.space_m.machine.misc;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.interfaces.ILookOverlay;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Gemeinsamer Teil der R7-A-Mehrblockmaschinen (Original {@code BlockDummyable}, TESR {@code getRenderType() = -1}):
 * Rechtsklick (auch ueber die HBM-Phantomteile, die an den Kern weiterreichen) als {@link #onActivated}, Blick-Overlay
 * {@link #printHook} (Standard: keines) und {@code addStandardInfo}-Tooltip ({@link #standardInfo()}).
 */
public abstract class R7MachineBlock extends DummyableMachineBlock implements ILookOverlay {

    protected R7MachineBlock(Properties properties) {
        super(properties);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    /** Original {@code ForgeDirection.getOrientation(getBlockMetadata() - BlockDummyable.offset)}. */
    public static Direction dir(BlockState state) {
        return state.hasProperty(FACING) ? state.getValue(FACING) : Direction.NORTH;
    }

    /** Original {@code dir.getRotation(ForgeDirection.UP)}. */
    public static Direction rot(Direction dir) {
        return dir.getClockWise();
    }

    //? if < 1.21.1 {
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return onActivated(level, pos, state, player, player.getItemInHand(hand), hand);
    }
    //?} else {
    /*@Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return onActivated(level, pos, state, player, player.getItemInHand(InteractionHand.MAIN_HAND), InteractionHand.MAIN_HAND);
    }
    *///?}

    /** Original {@code onBlockActivated} (pos = Kern). Standard: nichts. */
    protected InteractionResult onActivated(Level level, BlockPos pos, BlockState state, Player player, ItemStack held, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    @Override
    public void printHook(GuiGraphics g, Level level, BlockPos pos) { }

    /** Original {@code ILookOverlay.printGeneric(event, I18nUtil.resolveKey(getUnlocalizedName() + ".name"), 0xffff00, 0x404000, text)}. */
    protected void print(GuiGraphics g, List<Component> text) {
        ILookOverlay.printGeneric(g, Component.translatable(getDescriptionId()), 0xffff00, 0x404000, text);
    }

    /** true = Original {@code addInformation -> addStandardInfo}. */
    protected boolean standardInfo() {
        return false;
    }

    @Override
    //? if < 1.21.1 {
    public void appendHoverText(ItemStack stack, @Nullable net.minecraft.world.level.BlockGetter level,
                                List<Component> list, net.minecraft.world.item.TooltipFlag flag) {
    //?} else {
    /*public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext hbmTooltipCtx, List<Component> list, net.minecraft.world.item.TooltipFlag flag) {
    *///?}
        if (standardInfo()) com.hbm_m.util.StandardInfo.add(list, getDescriptionId() + ".desc");
    }
}
