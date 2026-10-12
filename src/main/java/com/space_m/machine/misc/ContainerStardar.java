package com.space_m.machine.misc;

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
 * 1:1 NTM Space {@code ContainerStardar} ({@code ContainerBase}): Laufwerk-Slot (150,124), Spielerinventar ab (8,174),
 * Schnellleiste (8,232). Umschalt-Klick wie {@code ContainerBase.transferStackInSlot}: aus der Maschine ins
 * Spielerinventar, aus dem Spielerinventar in die Maschine (Stapelgrenze 1). Benutzbar, solange der StarDar steht
 * (Original {@code isUseableByPlayer} = immer true).
 * Original-Klasse: ContainerStardar
 */
public class ContainerStardar extends AbstractContainerMenu {

    @Nullable public final TileEntityMachineStardar stardar;
    private static final int TILE_SIZE = 1;

    public ContainerStardar(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public ContainerStardar(int id, Inventory player, @Nullable BlockEntity entity) {
        super(MiscMenus.STARDAR.get(), id);
        this.stardar = entity instanceof TileEntityMachineStardar s ? s : null;

        Container container = stardar != null
                ? new ModItemStackHandlerContainer(stardar.getInventory(), stardar::setChanged)
                : new ModItemStackHandlerContainer(new DummyItemStackHandler(TILE_SIZE), () -> {});

        this.addSlot(new Slot(container, 0, 150, 124) {
            @Override
            public int getMaxStackSize() {
                return 1; // getInventoryStackLimit
            }
        });

        playerInv(player, 8, 174, 232);
    }

    /** Original {@code ContainerBase.playerInv(invPlayer, playerInvX, playerInvY, playerHotbarY)}. */
    private void playerInv(Inventory invPlayer, int playerInvX, int playerInvY, int playerHotbarY) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(invPlayer, j + i * 9 + 9, playerInvX + j * 18, playerInvY + i * 18));
            }
        }

        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(invPlayer, i, playerInvX + i * 18, playerHotbarY));
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
            } else if (this.slots.get(0).hasItem() // Stapelgrenze 1: Vanillas Zusammenfuehren kennt sie nicht
                    || !this.moveItemStackTo(slotStack, 0, TILE_SIZE, false)) {
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
        return stardar != null && !stardar.isRemoved() && stardar.hasPermission(player);
    }
}
