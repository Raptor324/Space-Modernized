package com.space_m.rocket.transport;

import com.hbm_m.inventory.menu.MultiblockMenuReach;
import com.space_m.inventory.SpaceMenus;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 NTM Space {@code ContainerTransporterRocket}: 8 Eingabe- (8|34) und 8 Ausgabeplaetze (98|34), 4
 * Fluid-Identifikatoren (8|106), 2 Treibstoff-Identifikatoren (188|90), Spielerinventar (8|154), Schnellleiste (8|212).
 * Wie im Original nehmen alle Plaetze in der GUI jedes Item an (1.7.10-{@code Slot} ohne Pruefung); die
 * Automatisierungsregeln gelten nur fuer Rohre/Trichter.
 * Original-Klasse: ContainerTransporterRocket
 */
public class TransporterRocketMenu extends AbstractContainerMenu {

    public final TransporterRocketBlockEntity transporter;
    private final Container transporterInventory;

    public TransporterRocketMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, (TransporterRocketBlockEntity) inv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public TransporterRocketMenu(int id, Inventory invPlayer, TransporterRocketBlockEntity transporter) {
        super(SpaceMenus.TRANSPORTER_ROCKET.get(), id);
        this.transporter = transporter;
        this.transporterInventory = new BeContainer(transporter);

        addSlots(transporterInventory, 0, 8, 34, 2, 4); // Input slots
        addSlots(transporterInventory, 8, 98, 34, 2, 4); // Output slots

        addSlots(transporterInventory, 16, 8, 106, 1, 4); // Input Fluid ID slots
        addSlots(transporterInventory, 20, 188, 90, 1, 2); // Fuel Fluid ID slots

        addSlots(invPlayer, 9, 8, 154, 3, 9); // Player inventory
        addSlots(invPlayer, 0, 8, 212, 1, 9); // Player hotbar
    }

    // I'm gonna make a farken helper function for this shit, why was it done the old way for 9 whole ass years?
    private void addSlots(Container inv, int from, int x, int y, int rows, int cols) {
        int slotSize = 18;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                this.addSlot(new Slot(inv, col + row * cols + from, x + col * slotSize, y + row * slotSize));
            }
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();

            int size = transporterInventory.getContainerSize();
            if (index <= size - 1) {
                if (!this.moveItemStackTo(stack, size, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, 0, size, false)) {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            slot.onTake(player, stack);
        }

        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return transporter != null && MultiblockMenuReach.stillValidCore(transporter, player, 128.0D);
    }

    /** Direkter Zugriff auf die BE-Plaetze (ohne die Automatisierungsregeln des Item-Handlers). */
    private static final class BeContainer implements Container {
        private final TransporterBlockEntityBase be;

        BeContainer(TransporterBlockEntityBase be) { this.be = be; }

        @Override public int getContainerSize() { return be.getSlotCount(); }

        @Override
        public boolean isEmpty() {
            for (int i = 0; i < getContainerSize(); i++) if (!be.getStack(i).isEmpty()) return false;
            return true;
        }

        @Override public ItemStack getItem(int slot) { return be.getStack(slot); }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            ItemStack stack = be.getStack(slot);
            if (stack.isEmpty() || amount <= 0) return ItemStack.EMPTY;
            ItemStack split = stack.split(amount);
            be.setStack(slot, stack);
            return split;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            ItemStack stack = be.getStack(slot);
            be.setStack(slot, ItemStack.EMPTY);
            return stack;
        }

        @Override public void setItem(int slot, ItemStack stack) { be.setStack(slot, stack); }
        @Override public void setChanged() { be.setChanged(); }
        @Override public boolean stillValid(Player player) { return true; }

        @Override
        public void clearContent() {
            for (int i = 0; i < getContainerSize(); i++) be.setStack(i, ItemStack.EMPTY);
        }
    }
}
