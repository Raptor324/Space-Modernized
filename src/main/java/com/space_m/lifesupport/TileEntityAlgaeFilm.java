package com.space_m.lifesupport;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.api.space.SpaceApi;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.space_m.blockentity.SpaceBlockEntities;

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

/**
 * 1:1 NTM Space {@code TileEntityAlgaeFilm} (Block {@code algae_film}): nur im Orbit (Schwerelosigkeit) wandelt der
 * Algenfilm in jedem zweiten Tick (Zufall) 1 mB CO2 in 1 mB O2 um (je 8 000 mB Tank). Anschluesse nur an den beiden
 * Stirnseiten quer zur Blickrichtung (Original {@code getRotation(UP)} und Gegenseite).
 * Original-Klasse: TileEntityAlgaeFilm
 */
public class TileEntityAlgaeFilm extends BaseMachineBlockEntity implements IFluidStandardTransceiverMK2 {

    public final FluidTank[] tanks;
    public boolean canOperate;

    public TileEntityAlgaeFilm(BlockPos pos, BlockState state) {
        super(SpaceBlockEntities.ALGAE_FILM.get(), pos, state, 0, 0L, 0L, 0L);
        tanks = new FluidTank[2];
        tanks[0] = new FluidTank(ModFluids.CARBONDIOXIDE.getSource(), 8_000);
        tanks[1] = new FluidTank(ModFluids.OXYGEN.getSource(), 8_000);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileEntityAlgaeFilm be) {
        if (!level.isClientSide) be.updateEntity();
    }

    public void updateEntity() {
        canOperate = SpaceApi.inOrbit(level);

        if (canOperate && level.random.nextBoolean()) {
            if (tanks[0].getFill() > 0 && tanks[1].getFill() < tanks[1].getMaxFill()) {
                tanks[0].setFill(tanks[0].getFill() - 1);
                tanks[1].setFill(tanks[1].getFill() + 1);
            }
        }

        // Original: ForgeDirection.getOrientation(meta).getRotation(UP) = Drehung gegen den Uhrzeigersinn
        Direction d = getBlockState().getValue(BlockAlgaeFilm.FACING).getCounterClockWise();
        Direction[] dirs = new Direction[] { d, d.getOpposite() };

        for (Direction dir : dirs) {
            trySubscribe(tanks[0].getTankType(), level, worldPosition.relative(dir), dir);
            tryProvide(tanks[1], level, worldPosition.relative(dir), dir);
        }

        // Original networkPackNT(20)
        setChanged();
        sendUpdateToClient();
    }

    @Override
    protected void writeNbtData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(nbt, registries);
        for (int i = 0; i < tanks.length; i++) tanks[i].writeToNBT(nbt, "t" + i);
        nbt.putBoolean("canOperate", canOperate); // nur Anzeige (Original serialize)
    }

    @Override
    protected void readNbtData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(nbt, registries);
        for (int i = 0; i < tanks.length; i++) tanks[i].readFromNBT(nbt, "t" + i);
        canOperate = nbt.getBoolean("canOperate");
    }

    @Override
    public FluidTank[] getAllTanks() {
        return tanks;
    }

    @Override
    public FluidTank[] getSendingTanks() {
        return new FluidTank[] { tanks[1] };
    }

    @Override
    public FluidTank[] getReceivingTanks() {
        return new FluidTank[] { tanks[0] };
    }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    // ── BaseMachineBlockEntity (kein GUI, Original getName "container.algaeFilm") ──

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.algaeFilm");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return false;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return null;
    }
}
