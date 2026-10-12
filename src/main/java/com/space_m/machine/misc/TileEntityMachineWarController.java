package com.space_m.machine.misc;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.tile.IControlReceiver;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.item.ISatChip;
import com.hbm_m.satellite.Satellite;
import com.space_m.celestial.CelestialBody;
import com.space_m.celestial.SolarSystem;
import com.space_m.rocket.ItemVOTVdrive;
import com.space_m.satellite.SatelliteWar;
import com.space_m.satellite.SpaceSatellites;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 NTM Space {@code TileEntityMachineWarController}: 4 Slots (0 frei, 1 Ziel-Laufwerk {@code hard_drive_full},
 * 2 Satelliten-ID-Chip, 3 frei). Jeden Tick: ist ein volles Laufwerk eingelegt und kreist auf der Chip-Frequenz ein
 * Kriegssatellit um den Koerper ({@code SatelliteSavedData.getData(world, x, z)}), wird dessen Ziel auf den Koerper des
 * Laufwerks gesetzt ({@code SatelliteWar.setTarget}). GUI-Befehl {@code setpos x z} schreibt die Laufwerkskoordinaten
 * ({@code xcoord}/{@code zcoord}). Strom: im Original ungenutzt (alle Werte 0, nie geladen) - hier ebenso ohne Anschluss.
 * Original-Klasse: TileEntityMachineWarController
 */
public class TileEntityMachineWarController extends BaseMachineBlockEntity implements IControlReceiver {

    public int id;

    public TileEntityMachineWarController(BlockPos pos, BlockState state) {
        super(MiscBlockEntities.WAR.get(), pos, state, 4, 0L, 0L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, TileEntityMachineWarController be) {
        if (level.isClientSide) return;

        be.id = ISatChip.getFreqS(be.inventory.getStackInSlot(2));
        ItemStack drive = be.inventory.getStackInSlot(1);
        if (drive.isEmpty() || !(drive.getItem() instanceof ItemVOTVdrive)) return;

        SolarSystem.Body target = ItemVOTVdrive.getDestination(drive).body();
        CelestialBody body = target.getBody();

        Satellite sat = SpaceSatellites.getData((ServerLevel) level, pos.getX(), pos.getZ()).getSatFromFreq(be.id);

        if (sat instanceof SatelliteWar satelliteWar) {
            satelliteWar.setTarget(body);
        }
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("xcoord") && data.contains("zcoord")) {
            updateDriveCoords(data.getInt("xcoord"), data.getInt("zcoord"));
        }
    }

    private void updateDriveCoords(int x, int z) {
        ItemStack drive = inventory.getStackInSlot(1);
        if (drive.isEmpty() || !(drive.getItem() instanceof ItemVOTVdrive)) return;

        ItemVOTVdrive.setCoordinates(drive, x, z);
        inventory.setStackInSlot(1, drive);

        this.setChanged();
    }

    @Override
    public boolean hasPermission(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64;
    }

    /** Original {@code getPower()/getMaxPower()} = 0, {@code isLoaded()} = false. */
    @Override
    public boolean canConnectEnergy(Direction side) {
        return false;
    }

    @Override protected boolean isItemValidForSlot(int slot, ItemStack stack) { return true; }

    /** Original {@code getName() = "container.warController"}. */
    @Override protected Component getDefaultName() { return Component.translatable("container.warController"); }
    @Override public Component getDisplayName() { return getDefaultName(); }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ContainerMachineWarController(id, inv, this);
    }
}
