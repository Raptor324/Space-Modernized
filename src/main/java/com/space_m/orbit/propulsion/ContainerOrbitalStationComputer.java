package com.space_m.orbit.propulsion;

import org.jetbrains.annotations.Nullable;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 1:1 NTM Space {@code ContainerOrbitalStationComputer}: Menue ohne Slots ({@code canInteractWith} immer true); traegt
 * nur den Computer fuer die GUI (Name/Schwerkraft).
 * Original-Klasse: ContainerOrbitalStationComputer
 */
public class ContainerOrbitalStationComputer extends AbstractContainerMenu {

    @Nullable public final TileEntityOrbitalStationComputer computer;

    public ContainerOrbitalStationComputer(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public ContainerOrbitalStationComputer(int id, Inventory inv, @Nullable BlockEntity entity) {
        super(PropulsionMenus.ORBITAL_STATION_COMPUTER.get(), id);
        this.computer = entity instanceof TileEntityOrbitalStationComputer c ? c : null;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
