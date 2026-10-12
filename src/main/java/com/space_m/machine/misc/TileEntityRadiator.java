package com.space_m.machine.misc;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.api.space.SpaceApi;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 NTM Space {@code TileEntityRadiator} ({@code extends TileEntityCondenser}, {@code vacuumOptimised = true}):
 * verbrauchter Dampf -> Wasser, je Tick so viel wie in beide Tanks passt (Tanks je 500 mB, Config "radiator"
 * {@code I:inputTankSize}/{@code I:outputTankSize}). Im Vakuum funktioniert er (anders als der Kondensator); nur im
 * Tom-Feuersturm verdampft das Wasser ({@code SpaceApi.condenserEvaporates(level, pos, true)}). Dampf-Eingang und
 * Wasser-Ausgang an den vier Eckzellen ({@code +-rot}, y+-1), Rohre jeweils hinter ihnen ({@code -dir}).
 * NBT wie Original ({@code water} = Eingang, {@code steam} = Ausgang).
 * Original-Klasse: TileEntityRadiator
 */
public class TileEntityRadiator extends BaseMachineBlockEntity implements IFluidStandardTransceiverMK2 {

    public static int inputTankSize = 500;
    public static int outputTankSize = 500;

    public int age = 0;
    public FluidTank[] tanks;

    public int waterTimer = 0;
    protected int throughput;

    public boolean vacuumOptimised = false;

    public TileEntityRadiator(BlockPos pos, BlockState state) {
        super(MiscBlockEntities.RADIATOR.get(), pos, state, 0, 0L, 0L);
        tanks = new FluidTank[2];
        tanks[0] = new FluidTank(ModFluids.SPENTSTEAM.getSource(), inputTankSize);
        tanks[1] = new FluidTank(Fluids.WATER, outputTankSize);
        vacuumOptimised = true;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileEntityRadiator be) {
        if (level.isClientSide) return;

        be.age++;
        if (be.age >= 2) {
            be.age = 0;
        }

        if (be.waterTimer > 0)
            be.waterTimer--;

        int convert = Math.min(be.tanks[0].getFill(), be.tanks[1].getMaxFill() - be.tanks[1].getFill());
        be.throughput = convert;

        be.tanks[0].setFill(be.tanks[0].getFill() - convert);

        if (convert > 0)
            be.waterTimer = 20;

        if (SpaceApi.condenserEvaporates(level, pos, be.vacuumOptimised)) { // Make both steam and water evaporate during firestorms and in vacuums
            be.tanks[1].setFill(Math.max(0, be.tanks[1].getFill() - convert));
        } else {
            be.tanks[1].setFill(be.tanks[1].getFill() + convert);
        }

        // subscribeToAllAround / sendFluidToAll
        Direction dir = R7MachineBlock.dir(state);
        Direction rot = R7MachineBlock.rot(dir);
        for (int side = -1; side <= 1; side += 2) {
            for (int up = -1; up <= 1; up += 2) {
                BlockPos pipe = pos.offset(-dir.getStepX() + rot.getStepX() * side, up, -dir.getStepZ() + rot.getStepZ() * side);
                be.trySubscribe(be.tanks[0].getTankType(), level, pipe, dir.getOpposite());
            }
        }
        for (int side = -1; side <= 1; side += 2) {
            for (int up = -1; up <= 1; up += 2) {
                BlockPos pipe = pos.offset(-dir.getStepX() + rot.getStepX() * side, up, -dir.getStepZ() + rot.getStepZ() * side);
                be.tryProvide(be.tanks[1], level, pipe, dir.getOpposite());
            }
        }

        be.setChanged();
        be.sendUpdateToClient(); // networkPackNT(150)
    }

    @Override public FluidTank[] getSendingTanks() { return new FluidTank[] { tanks[1] }; }
    @Override public FluidTank[] getReceivingTanks() { return new FluidTank[] { tanks[0] }; }
    @Override public FluidTank[] getAllTanks() { return tanks; }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    protected void writeNbtData(CompoundTag nbt, @Nullable net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(nbt, registries);
        tanks[0].writeToNBT(nbt, "water");
        tanks[1].writeToNBT(nbt, "steam");
        nbt.putByte("s_waterTimer", (byte) waterTimer);
    }

    @Override
    protected void readNbtData(CompoundTag nbt, @Nullable net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(nbt, registries);
        tanks[0].readFromNBT(nbt, "water");
        tanks[1].readFromNBT(nbt, "steam");
    }

    @Override
    protected void applyClientUpdate(CompoundTag nbt) {
        super.applyClientUpdate(nbt);
        waterTimer = nbt.getByte("s_waterTimer");
    }

    @Override
    public boolean canConnectEnergy(Direction side) {
        return false;
    }

    @Override protected boolean isItemValidForSlot(int slot, ItemStack stack) { return false; }
    @Override protected Component getDefaultName() { return Component.translatable("block.space_m.machine_radiator"); }
    @Override public Component getDisplayName() { return getDefaultName(); }
    @Nullable @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) { return null; }

    /** Original {@code getRenderBoundingBox}: x-20..x+21, y-1..y+1, z-20..z+21. */
    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.getX() - 20, worldPosition.getY() - 1, worldPosition.getZ() - 20,
                worldPosition.getX() + 21, worldPosition.getY() + 1, worldPosition.getZ() + 21);
    }
}
