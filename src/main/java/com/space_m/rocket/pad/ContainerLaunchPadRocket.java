package com.space_m.rocket.pad;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.inventory.ModItemStackHandlerContainer;
import com.hbm_m.platform.DummyItemStackHandler;
import com.space_m.inventory.SpaceMenus;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 1:1 NTM Space {@code ContainerLaunchPadRocket}: Rakete (37/21), Laufwerk (37/39), Batterie (167/90) - diese drei wie
 * {@code SlotNonRetarded} mit Gueltigkeitspruefung -, Kanister rein (77/21) / raus (77/39); Spielerinventar ab 14/154,
 * Schnellleiste 14/212. Umschaltklick wie {@code ContainerBase}.
 */
public class ContainerLaunchPadRocket extends AbstractContainerMenu {

    private static final int TE_SLOTS = TileEntityLaunchPadRocket.SLOT_COUNT;

    @Nullable public final TileEntityLaunchPadRocket machine;

    public ContainerLaunchPadRocket(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public ContainerLaunchPadRocket(int id, Inventory inv, @Nullable BlockEntity entity) {
        super(SpaceMenus.LAUNCH_PAD_ROCKET.get(), id);
        this.machine = entity instanceof TileEntityLaunchPadRocket pad ? pad : null;

        Container container = this.machine != null
                ? new ModItemStackHandlerContainer(this.machine.getInventory(), this.machine::setChanged)
                : new ModItemStackHandlerContainer(new DummyItemStackHandler(TE_SLOTS), () -> {});

        addSlot(new Checked(container, 0, 37, 21)); // Rocket slot
        addSlot(new Checked(container, 1, 37, 39)); // Drive slot

        addSlot(new Checked(container, 2, 167, 90)); // Battery slot

        addSlot(new Slot(container, 3, 77, 21)); // Input
        addSlot(new Slot(container, 4, 77, 39)); // Output

        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 9; j++)
                addSlot(new Slot(inv, j + i * 9 + 9, 14 + j * 18, 154 + i * 18)); // Player inventory

        for (int i = 0; i < 9; i++)
            addSlot(new Slot(inv, i, 14 + i * 18, 212)); // Player hotbar
    }

    /** Original {@code SlotNonRetarded}. */
    private class Checked extends Slot {
        Checked(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return machine == null || machine.isItemValid(getContainerSlot(), stack);
        }
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return result;

        ItemStack stack = slot.getItem();
        result = stack.copy();

        if (index < TE_SLOTS) {
            if (!moveItemStackTo(stack, TE_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, TE_SLOTS, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return result;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        if (machine == null || machine.isRemoved()) return false;
        return machine.hasPermission(player);
    }
}
