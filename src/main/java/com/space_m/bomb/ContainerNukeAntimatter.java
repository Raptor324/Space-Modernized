package com.space_m.bomb;

import com.hbm_m.inventory.ModItemStackHandlerContainer;
import com.hbm_m.platform.DummyItemStackHandler;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 1:1 NTM Space {@code ContainerNukeAntimatter}: Bleiteilchen (26,15)/(134,14), Antimaterie-Kern (81,15),
 * Hafnium (26,44)/(134,43), Spielerinventar bei y=140/198. Schnellverschieben nur aus Index &lt;= 2 (Original).
 * Original-Klasse: ContainerNukeAntimatter
 */
public class ContainerNukeAntimatter extends AbstractContainerMenu {

    public final TileEntityAntimatter be;

    public ContainerNukeAntimatter(int id, Inventory playerInv, FriendlyByteBuf extraData) {
        this(id, playerInv, getBlockEntity(playerInv, extraData));
    }

    private static TileEntityAntimatter getBlockEntity(Inventory playerInv, FriendlyByteBuf extraData) {
        if (extraData == null) return null;
        BlockEntity blockEntity = playerInv.player.level().getBlockEntity(extraData.readBlockPos());
        if (blockEntity instanceof TileEntityAntimatter tile) return tile;
        if (playerInv.player.level().isClientSide) return null;
        throw new IllegalStateException("BlockEntity is not a TileEntityAntimatter");
    }

    public ContainerNukeAntimatter(int id, Inventory inventory, TileEntityAntimatter blockEntity) {
        super(SpaceBombs.ANTIMATTER_MENU.get(), id);
        this.be = blockEntity;

        var container = this.be != null
                ? this.be
                : new ModItemStackHandlerContainer(new DummyItemStackHandler(TileEntityAntimatter.SLOTS), () -> {});

        addSlot(new Slot(container, 0, 26, 15));
        addSlot(new Slot(container, 1, 134, 14));
        addSlot(new Slot(container, 2, 81, 15));
        addSlot(new Slot(container, 3, 26, 44));
        addSlot(new Slot(container, 4, 134, 43));

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                addSlot(new Slot(inventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18 + 56));
            }
        }
        for (int i = 0; i < 9; i++) {
            addSlot(new Slot(inventory, i, 8 + i * 18, 142 + 56));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return be != null && be.stillValid(player);
    }

    /** Original {@code transferStackInSlot}: nur {@code par2 <= 2}, Ziel ab Index 2 (sic). */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack var3 = ItemStack.EMPTY;
        Slot var4 = this.slots.get(index);

        if (var4 != null && var4.hasItem()) {
            ItemStack var5 = var4.getItem();
            var3 = var5.copy();

            if (index <= 2) {
                if (!this.moveItemStackTo(var5, 2, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                return ItemStack.EMPTY;
            }

            if (var5.isEmpty()) {
                var4.set(ItemStack.EMPTY);
            } else {
                var4.setChanged();
            }
        }

        return var3;
    }
}
