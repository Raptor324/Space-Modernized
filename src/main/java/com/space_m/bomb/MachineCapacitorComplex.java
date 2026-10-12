package com.space_m.bomb;

import java.util.List;

import com.hbm_m.block.machines.MachineCapacitorBlock;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * NTM Space {@code MachineCapacitor(Material.iron, 100_000_000_000_000_000L, "complex")} als {@code capacitor_complex}
 * ("Wavefunction Capacitor (LEGACY)"): HBM-{@link MachineCapacitorBlock} mit eigener Kapazitaet, Texturen
 * {@code capacitor_complex_top/side/bottom}, Lade-/Entladerate wie alle Kondensatoren {@code power / 200} bzw.
 * {@code power / 600}, Drop mit Ladung ({@code IPersistentNBT}). Zusaetzlich die vier Original-Tooltipzeilen.
 * Original-Klasse: MachineCapacitor
 */
public class MachineCapacitorComplex extends MachineCapacitorBlock {

    public MachineCapacitorComplex(Properties properties, long capacity) {
        super(properties, capacity);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityCapacitorComplex(pos, state);
    }

    @Override
    //? if < 1.21.1 {
    public void appendHoverText(ItemStack stack, @org.jetbrains.annotations.Nullable net.minecraft.world.level.BlockGetter level,
                                List<Component> list, TooltipFlag flag) {
        super.appendHoverText(stack, level, list, flag);
    //?} else {
    /*public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext hbmTooltipCtx, List<Component> list, TooltipFlag flag) {
        super.appendHoverText(stack, hbmTooltipCtx, list, flag);
    *///?}
        // Original addInformation: if(this == ModBlocks.capacitor_complex) - fester englischer Text
        list.add(Component.literal("TaCdSa236-7 N-Boosted Anti Mass core surrounded by").withStyle(ChatFormatting.GRAY));
        list.add(Component.literal("Flashlead antimatter lattice in a BF Stabilization Matrix").withStyle(ChatFormatting.GRAY));
        list.add(Component.literal("subjected to the Ferric Osmiridium-Lutece").withStyle(ChatFormatting.GRAY));
        list.add(Component.literal("ψ(x,t)=Aeiℏ(px−Et) Wavefunction.").withStyle(ChatFormatting.GRAY));
    }

    //? if >1.20.1 {
    /*@Override
    protected com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() {
        return simpleCodec(p -> new MachineCapacitorComplex(p, getCapacity()));
    }
    *///?}
}
