package com.space_m.machine.chem;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;
import com.hbm_m.inventory.menu.MultiblockMenuReach;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Gemeinsamer Unterbau der R7-B-Container (Original {@code Container}/{@code ContainerBase}): Maschinenplaetze ueber
 * HBMs {@link ModItemStackHandlerContainer}, Spielerinventar ({@code playerInv}), Reichweite 128 vom Kern
 * ({@code isUseableByPlayer}) und Schnellverschieben nach dem Muster {@code transferStackInSlot}.
 */
public abstract class ChemMenu<T extends BaseMachineBlockEntity> extends AbstractContainerMenu {

    protected final T be;
    protected final Container container;
    protected int machineSlots;

    protected ChemMenu(MenuType<?> type, int id, T be) {
        super(type, id);
        this.be = be;
        this.container = new ModItemStackHandlerContainer(be.getInventory(), be::setChanged);
    }

    @SuppressWarnings("unchecked")
    protected static <B> B readBe(Inventory inv, FriendlyByteBuf buf, Class<B> cls) {
        BlockPos pos = buf.readBlockPos();
        var be = inv.player.level().getBlockEntity(pos);
        if (cls.isInstance(be)) return (B) be;
        throw new IllegalStateException("No " + cls.getSimpleName() + " at " + pos);
    }

    public T getBlockEntity() {
        return be;
    }

    /** Maschinenplatz; zaehlt {@link #machineSlots} mit. */
    protected Slot machine(Slot slot) {
        machineSlots++;
        return addSlot(slot);
    }

    /** Original {@code ContainerBase.playerInv(inv, x, y, hotbarY)} bzw. die ausgeschriebenen Schleifen. */
    protected void playerInv(Inventory inv, int x, int y, int hotbarY) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                addSlot(new Slot(inv, j + i * 9 + 9, x + j * 18, y + i * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            addSlot(new Slot(inv, i, x + i * 18, hotbarY));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return MultiblockMenuReach.stillValidCore(be, player, 128.0D);
    }

    /**
     * Original {@code transferStackInSlot}: aus der Maschine ins Spielerinventar ({@code mergeItemStack(.., n, size, true)}),
     * sonst {@link #moveToMachine}. Rueckgabe EMPTY, wenn nichts verschoben werden konnte.
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();

            if (index < machineSlots) {
                if (!this.moveItemStackTo(stack, machineSlots, this.slots.size(), true)) return ItemStack.EMPTY;
            } else {
                if (!moveToMachine(stack)) return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            if (stack.getCount() == result.getCount()) return ItemStack.EMPTY;
            slot.onTake(player, stack);
        }
        return result;
    }

    /** Spielerstapel in die Maschine (Original-Reihenfolge); false = nichts moeglich. */
    protected boolean moveToMachine(ItemStack stack) {
        return false;
    }

    /** {@code mergeItemStack(stack, from, to, false)}. */
    protected boolean merge(ItemStack stack, int from, int to) {
        return moveItemStackTo(stack, from, to, false);
    }

    /** Original {@code SlotTakeOnly}/{@code SlotCraftingOutput}: nur entnehmen. */
    public static class TakeOnly extends Slot {
        public TakeOnly(Container c, int id, int x, int y) { super(c, id, x, y); }
        @Override public boolean mayPlace(ItemStack stack) { return false; }
    }

    /** Original {@code SlotUpgrade}: nur Maschinen-Aufwertungen, Stapelgroesse 1. */
    public static class Upgrade extends Slot {
        public Upgrade(Container c, int id, int x, int y) { super(c, id, x, y); }
        @Override public boolean mayPlace(ItemStack stack) {
            return stack.getItem() instanceof com.hbm_m.item.industrial.ItemMachineUpgrade;
        }
        @Override public int getMaxStackSize() { return 1; }
    }
}
