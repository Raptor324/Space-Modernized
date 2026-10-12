package com.space_m.machine.misc;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.inventory.ModItemStackHandlerContainer;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 1:1 NTM Space {@code ContainerMachineWarController}: Slots 0 (6,52), 1 (29,11), 2 (49,11), 3 (29,31); Spielerinventar
 * ab (8,103), Schnellleiste (8,162). Umschalt-Klick: Maschine -> Spieler (von hinten), Spieler -> Maschine.
 * Original-Klasse: ContainerMachineWarController
 */
public class ContainerMachineWarController extends AbstractContainerMenu {

    @Nullable public final TileEntityMachineWarController shill;

    public ContainerMachineWarController(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public ContainerMachineWarController(int id, Inventory invPlayer, @Nullable BlockEntity entity) {
        super(MiscMenus.WAR_CONTROLLER.get(), id);
        this.shill = entity instanceof TileEntityMachineWarController w ? w : null;

        Container tedf = shill != null
                ? new ModItemStackHandlerContainer(shill.getInventory(), shill::setChanged)
                : new SimpleContainer(4);

        // 4 Slots (shifted upwards by modifying y values)
        this.addSlot(new Slot(tedf, 0, 6, 52));
        this.addSlot(new Slot(tedf, 1, 29, 11));
        this.addSlot(new Slot(tedf, 2, 49, 11));
        this.addSlot(new Slot(tedf, 3, 29, 31));

        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 9; x++) {
                this.addSlot(new Slot(invPlayer, x + y * 9 + 9, 8 + x * 18, 103 + y * 18));
            }
        }
        for (int x = 0; x < 9; x++) {
            this.addSlot(new Slot(invPlayer, x, 8 + x * 18, 162));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();

            // TileEntity Slots to Player Inventory
            if (index < 4) {
                if (!this.moveItemStackTo(itemstack1, 4, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemstack1, 0, 4, false)) {
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        return shill != null && shill.hasPermission(player);
    }
}
