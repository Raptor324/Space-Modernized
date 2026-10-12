package com.space_m.machine.mining;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.inventory.ModItemStackHandlerContainer;
import com.hbm_m.platform.DummyItemStackHandler;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 1:1 NTM Space {@code ContainerDriveProcessor} ({@code ContainerBase}): 0 aktives Laufwerk (30,18), 1 Klonziel (50,38),
 * 2 Prozessor (81,24), 3 Batterie (134,72); Spielerinventar {@code playerInv(inv, 8, 125, 183)}. Umschalt-Klick wie
 * {@code ContainerBase.transferStackInSlot} (Maschine -> Spieler, Spieler -> Maschine); Stapelgrenze 1.
 * Original-Klasse: ContainerDriveProcessor
 */
public class ContainerDriveProcessor extends AbstractContainerMenu {

    private static final int TILE_SIZE = 4;

    @Nullable public final TileEntityMachineDriveProcessor machine;

    public ContainerDriveProcessor(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public ContainerDriveProcessor(int id, Inventory invPlayer, @Nullable BlockEntity entity) {
        super(MiningMachines.DRIVE_PROCESSOR_MENU.get(), id);
        this.machine = entity instanceof TileEntityMachineDriveProcessor m ? m : null;

        Container container = machine != null
                ? new ModItemStackHandlerContainer(machine.getInventory(), machine::setChanged)
                : new ModItemStackHandlerContainer(new DummyItemStackHandler(TILE_SIZE), () -> {});

        // 0 - active drive slot
        // 1 - cloning drive slot

        // 2 - upgrade slot

        // 3 - battery slot

        addSlot(new One(container, 0, 30, 18));
        addSlot(new One(container, 1, 50, 38));

        addSlot(new One(container, 2, 81, 24));

        addSlot(new One(container, 3, 134, 72));

        playerInv(invPlayer, 8, 125, 183);
    }

    /** Original {@code ContainerBase.playerInv(invPlayer, x, y, hotbarY)}. */
    private void playerInv(Inventory invPlayer, int playerInvX, int playerInvY, int playerHotbarY) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                addSlot(new Slot(invPlayer, j + i * 9 + 9, playerInvX + j * 18, playerInvY + i * 18));
            }
        }

        for (int i = 0; i < 9; i++) {
            addSlot(new Slot(invPlayer, i, playerInvX + i * 18, playerHotbarY));
        }
    }

    /** {@code Slot} mit der Stapelgrenze der Maschine ({@code getInventoryStackLimit() = 1}). */
    private static class One extends Slot {
        One(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return 1;
        }
    }

    /** Original {@code ContainerBase.transferStackInSlot}. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack slotOriginal = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            slotOriginal = slotStack.copy();

            if (index < TILE_SIZE) {
                if (!this.moveItemStackTo(slotStack, TILE_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(slotStack, 0, TILE_SIZE, false)) {
                return ItemStack.EMPTY;
            }

            if (slotStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            slot.onTake(player, slotStack);
        }

        return slotOriginal;
    }

    @Override
    public boolean stillValid(Player player) {
        if (machine == null || machine.isRemoved()) return false;
        return machine.hasPermission(player);
    }
}
