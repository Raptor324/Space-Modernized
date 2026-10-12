package com.space_m.machine.dyson;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.interfaces.IHeatSource;
import com.space_m.machine.misc.R7MachineBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 NTM Space {@code TileEntityDysonConverterTU}: nimmt den Empfaengerstrahl an der Zelle {@code dir*6, y+1} an und
 * speichert ihn als Waerme (TU, bis {@code Integer.MAX_VALUE}, Ueberlauf -> Maximum); gibt sie als {@code IHeatSource}
 * ab ({@code useUpHeat}). Kein Tick.
 *
 * <p>HBM-Waermeverbraucher fragen das Blockentity direkt unter sich; die Waermezellen
 * ueber dem Kern sind HBM-Phantomteile mit Waermeweiterleitung ({@link #providesHeatThroughPart}, W-R7A-1, Original
 * {@code TileEntityProxyCombo.heatSource()}).
 * Original-Klasse: TileEntityDysonConverterTU
 */
public class TileEntityDysonConverterTU extends BaseMachineBlockEntity implements IDysonConverter, IHeatSource {

    public int heatEnergy;

    public TileEntityDysonConverterTU(BlockPos pos, BlockState state) {
        super(DysonBlockEntities.CONVERTER_TU.get(), pos, state, 0, 0L, 0L);
    }

    @Override
    public boolean provideEnergy(int x, int y, int z, long energy) {
        Direction dir = R7MachineBlock.dir(getBlockState());
        int rx = worldPosition.getX() + dir.getStepX() * 6;
        int ry = worldPosition.getY() + 1;
        int rz = worldPosition.getZ() + dir.getStepZ() * 6;

        if (x != rx || y != ry || z != rz) return false;

        if (energy > Integer.MAX_VALUE) {
            heatEnergy = Integer.MAX_VALUE;
            setChanged();
            return true;
        }
        heatEnergy += (int) energy;
        if (heatEnergy < 0) heatEnergy = Integer.MAX_VALUE; // prevent overflow

        setChanged();
        return true;
    }

    @Override
    public long maximumEnergy() {
        return Integer.MAX_VALUE;
    }

    @Override
    public int getHeatStored() {
        return heatEnergy;
    }

    /** HBM-Schnittstelle (Original ohne Obergrenze): Speichergrenze {@code Integer.MAX_VALUE}. */
    @Override
    public int getMaxHeatStored() {
        return Integer.MAX_VALUE;
    }

    /** Original {@code MachineDysonConverterTU}: {@code meta >= 6} (Zusatzzellen y+1/y+2 ueber dem Kern) = {@code heatSource()}. */
    @Override
    public boolean providesHeatThroughPart(@org.jetbrains.annotations.Nullable net.minecraft.core.BlockPos localOffset) {
        return localOffset != null && localOffset.getX() == 0 && localOffset.getZ() == 0 && localOffset.getY() > 0;
    }

    @Override
    public void useUpHeat(int heat) {
        heatEnergy = Math.max(0, heatEnergy - heat);
        setChanged();
    }

    @Override
    protected void writeNbtData(CompoundTag nbt, @Nullable net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(nbt, registries);
        nbt.putLong("heatEnergy", heatEnergy);
    }

    @Override
    protected void readNbtData(CompoundTag nbt, @Nullable net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(nbt, registries);
        this.heatEnergy = nbt.getInt("heatEnergy");
    }

    @Override
    public boolean canConnectEnergy(Direction side) {
        return false;
    }

    @Override protected boolean isItemValidForSlot(int slot, ItemStack stack) { return false; }

    /** Original {@code getName() = "container.machineDysonConverterTU"}. */
    @Override protected Component getDefaultName() { return Component.translatable("container.machineDysonConverterTU"); }
    @Override public Component getDisplayName() { return getDefaultName(); }
    @Nullable @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) { return null; }

    /** Original {@code getRenderBoundingBox}: x-6..x+7, y..y+6, z-6..z+7. */
    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.getX() - 6, worldPosition.getY(), worldPosition.getZ() - 6,
                worldPosition.getX() + 7, worldPosition.getY() + 6, worldPosition.getZ() + 7);
    }
}
