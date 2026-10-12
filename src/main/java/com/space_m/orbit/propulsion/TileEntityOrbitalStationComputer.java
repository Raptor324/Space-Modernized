package com.space_m.orbit.propulsion;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.tile.IControlReceiver;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.space_m.celestial.CelestialBody;
import com.space_m.orbit.OrbitalStation;
import com.space_m.orbit.OrbitalStation.StationState;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 NTM Space {@code TileEntityOrbitalStationComputer}: ein Slot fuer das Ziel-Laufwerk. {@link #travelTo} startet die
 * Reise der Station unter dem Computer ({@code OrbitalStation.travelTo}: Delta-v, Treibstoffpruefung aller Triebwerke,
 * Brennzeiten, Transferdauer) und behaelt das Laufwerk. GUI ueber {@code NBTControlPacket}: Stationsname
 * ({@code name}) und kuenstliche Schwerkraft ({@code gravity} -> {@code gravityMultiplier} 1/0). Sync {@code hasDrive}
 * (Renderer zeigt das Laufwerk).
 * Original-Klasse: TileEntityOrbitalStationComputer
 */
public class TileEntityOrbitalStationComputer extends BaseMachineBlockEntity implements IControlReceiver {

    public boolean hasDrive;

    public TileEntityOrbitalStationComputer(BlockPos pos, BlockState state) {
        super(PropulsionBlockEntities.ORBITAL_STATION_COMPUTER.get(), pos, state, 1, 0, 0);
    }

    public ItemStack getDrive() {
        return inventory.getStackInSlot(0);
    }

    public void setDrive(ItemStack stack) {
        inventory.setStackInSlot(0, stack);
        setChanged();
    }

    public boolean travelTo(CelestialBody body, ItemStack drive) {
        OrbitalStation station = OrbitalStation.getStationFromPosition(worldPosition.getX(), worldPosition.getZ());

        if (station.orbiting == body) return false;

        station.travelTo(level, body);
        setDrive(drive);

        return true;
    }

    public boolean isTravelling() {
        OrbitalStation station = OrbitalStation.getStationFromPosition(worldPosition.getX(), worldPosition.getZ());

        return station.state != StationState.ORBIT;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.orbitalStationComputer");
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileEntityOrbitalStationComputer be) {
        if (!level.isClientSide) {
            boolean had = be.hasDrive;
            be.hasDrive = !be.getDrive().isEmpty();
            if (had != be.hasDrive || level.getGameTime() % 20 == 0) be.sendUpdateToClient(); // networkPackNT(50)
        }
    }

    @Override
    public boolean hasPermission(Player player) {
        return true;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("name")) {
            OrbitalStation station = OrbitalStation.getStationFromPosition(worldPosition.getX(), worldPosition.getZ());
            station.name = data.getString("name");
            station.markDirty();
        }

        if (data.contains("gravity")) {
            OrbitalStation station = OrbitalStation.getStationFromPosition(worldPosition.getX(), worldPosition.getZ());
            station.gravityMultiplier = data.getBoolean("gravity") ? 1 : 0;
            station.markDirty();
        }
    }

    // ── NBT / Sync ───────────────────────────────────────────────────────────────────────────

    @Override
    protected void writeNbtData(CompoundTag tag, @Nullable net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putBoolean("hasDrive", hasDrive);
    }

    @Override
    protected void readNbtData(CompoundTag tag, @Nullable net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        hasDrive = tag.getBoolean("hasDrive");
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return true;
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ContainerOrbitalStationComputer(id, inv, this);
    }

    /** Original {@code getRenderBoundingBox}: x..x+1, y..y+2, z..z+1. */
    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                worldPosition.getX() + 1, worldPosition.getY() + 2, worldPosition.getZ() + 1);
    }
}
