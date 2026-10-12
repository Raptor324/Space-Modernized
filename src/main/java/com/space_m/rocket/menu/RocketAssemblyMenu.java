package com.space_m.rocket.menu;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.inventory.ModItemStackHandlerContainer;
import com.hbm_m.platform.DummyItemStackHandler;
import com.space_m.inventory.SpaceMenus;
import com.space_m.rocket.MachineRocketAssemblyBlockEntity;
import com.space_m.rocket.RocketPart.PartType;
import com.space_m.rocket.RocketStruct;
import com.space_m.rocket.menu.SlotRocket.SlotCapsule;
import com.space_m.rocket.menu.SlotRocket.SlotDrive;
import com.space_m.rocket.menu.SlotRocket.SlotRocketPart;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Menue der VAB, 1:1 NTM Space {@code ContainerMachineRocketAssembly}: Kapsel (18,13), je Stufe Rumpf/Beine/Triebwerk
 * (18,44/62/80, nur die gewaehlte Stufe sichtbar), Ergebnis (42,91), je Stufe Start-/Ziel-Laufwerk (161,54)/(170,87),
 * Spielerinventar ab (8,142), Schnellleiste (8,200). Laufwerk-Slots sind Geisterslots (Klick legt eine Kopie mit Anzahl 1
 * hinein, der getragene Stapel bleibt); Umschalt-Klick aus dem Spielerinventar ist wie im Original gesperrt.
 * Original-Klasse: ContainerMachineRocketAssembly
 */
public class RocketAssemblyMenu extends AbstractContainerMenu {

    @Nullable public final MachineRocketAssemblyBlockEntity machine;
    private final int tileSize = MachineRocketAssemblyBlockEntity.SLOT_COUNT;

    public RocketAssemblyMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public RocketAssemblyMenu(int id, Inventory invPlayer, @Nullable BlockEntity entity) {
        super(SpaceMenus.ROCKET_ASSEMBLY.get(), id);
        this.machine = entity instanceof MachineRocketAssemblyBlockEntity m ? m : null;

        Container container = machine != null
                ? new ModItemStackHandlerContainer(machine.getInventory(), machine::setChanged)
                : new ModItemStackHandlerContainer(new DummyItemStackHandler(tileSize), () -> {});

        int slotId = 0;

        // Capsule slot
        addSlot(new SlotCapsule(container, slotId++, 18, 13));

        // Stages
        for (int i = 0; i < RocketStruct.MAX_STAGES; i++) {
            addSlot(new SlotRocketPart(container, slotId++, 18, 44, i, PartType.FUSELAGE));
            addSlot(new SlotRocketPart(container, slotId++, 18, 62, i, PartType.FINS));
            addSlot(new SlotRocketPart(container, slotId++, 18, 80, i, PartType.THRUSTER));
        }

        // Result
        addSlot(new SlotRocket(container, slotId++, 42, 91));

        // Drives
        for (int i = 0; i < RocketStruct.MAX_STAGES; i++) {
            addSlot(new SlotDrive(container, machine, slotId++, 161, 54, i));
            addSlot(new SlotDrive(container, machine, slotId++, 170, 87, i));
        }

        // Player inventory
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 9; x++) {
                addSlot(new Slot(invPlayer, x + y * 9 + 9, 8 + x * 18, 142 + y * 18));
            }
        }
        // Player hotbar
        for (int x = 0; x < 9; x++) {
            addSlot(new Slot(invPlayer, x, 8 + x * 18, 200));
        }
    }

    /** Original {@code slotClick}: Laufwerk-Geisterslots und gesperrter Umschalt-Klick aus dem Spielerinventar. */
    @Override
    public void clicked(int index, int button, ClickType clickType, Player player) {
        if (index >= tileSize && clickType == ClickType.QUICK_MOVE) {
            return;
        }

        if (index < 0 || index < tileSize - RocketStruct.MAX_STAGES * 2 || index >= tileSize) {
            super.clicked(index, button, clickType, player);
            return;
        }

        Slot slot = this.getSlot(index);

        ItemStack held = getCarried();

        if (!held.isEmpty() && !slot.mayPlace(held)) return;

        ItemStack put = held.isEmpty() ? ItemStack.EMPTY : held.copy();
        if (!put.isEmpty()) put.setCount(1);
        slot.set(put);

        slot.setChanged();
    }

    /** Original {@code ContainerBase.transferStackInSlot}: aus der Maschine ins Spielerinventar. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack ret = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem() && index < tileSize - RocketStruct.MAX_STAGES * 2) {
            ItemStack stack = slot.getItem();
            ret = stack.copy();

            if (!this.moveItemStackTo(stack, tileSize, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            slot.onTake(player, stack);
        }

        return ret;
    }

    @Override
    public boolean stillValid(Player player) {
        // Original isUseableByPlayer: nur "Blockentity steht noch" (keine 8-Block-Grenze - man steht auf den Buehnen)
        if (machine == null || machine.isRemoved() || machine.getLevel() == null) return false;
        return machine.getLevel().getBlockEntity(machine.getBlockPos()) == machine;
    }
}
