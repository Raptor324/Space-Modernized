package com.space_m.rocket.transport;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.platform.StackNbt;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

//? if forge {
import net.minecraftforge.items.IItemHandler;
//?} else {
/*import net.neoforged.neoforge.items.IItemHandler;
*///?}

/**
 * Fremdinventar-Zugriff der Transporter-Rampe (Original {@code tryLoad}/{@code tryUnload} ueber
 * {@code IInventory}/{@code ISidedInventory}). Heute: Item-Capability des Nachbarn von der angeschlossenen Seite
 * (Forge {@code ForgeCapabilities.ITEM_HANDLER}, NeoForge ueber HBMs {@code HbmCaps}); der Handler setzt die
 * Seitenregeln ({@code getAccessibleSlotsFromSide}/{@code canExtractItem}/{@code canInsertItem}) selbst um.
 */
public final class TransportInventory {
    private TransportInventory() {}

    @Nullable
    static IItemHandler at(Level level, BlockPos pos, Direction side) {
        BlockEntity te = level.getBlockEntity(pos);
        if (te == null) return null;
        //? if forge {
        return te.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER, side).resolve().orElse(null);
        //?} else {
        /*return com.hbm_m.platform.HbmCaps.get(te, com.hbm_m.platform.HbmCap.ITEM_HANDLER, side).orElse(null);
        *///?}
    }

    /**
     * Original {@code tryLoad}: das erste entnehmbare Teil des Nachbarn wird einzeln in die Eingabe gelegt - zuerst
     * auf einen passenden, nicht vollen Stapel, sonst in einen leeren Platz. Hoechstens ein Teil je Aufruf.
     */
    static void tryLoad(Level level, BlockPos pos, Direction side, TransporterBlockEntityBase self) {
        IItemHandler inv = at(level, pos, side);
        if (inv == null) return;

        for (int slot = 0; slot < inv.getSlots(); slot++) {
            ItemStack stack = inv.getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            if (inv.extractItem(slot, 1, true).isEmpty()) continue;

            for (int j = 0; j < self.inputSlotMax; j++) {
                ItemStack own = self.getStack(j);
                if (!own.isEmpty() && own.getCount() < own.getMaxStackSize() && StackNbt.sameItemSameTags(own, stack)) {
                    inv.extractItem(slot, 1, false);
                    own.grow(1);
                    self.setStack(j, own);
                    return;
                }
            }

            for (int j = 0; j < self.inputSlotMax; j++) {
                if (self.getStack(j).isEmpty()) {
                    ItemStack copy = stack.copy();
                    copy.setCount(1);
                    inv.extractItem(slot, 1, false);
                    self.setStack(j, copy);
                    return;
                }
            }
        }
    }

    /**
     * Original {@code tryUnload}: ein Teil aus den Ausgabeplaetzen in den Nachbarn - zuerst auf einen passenden Stapel,
     * dann in einen leeren Platz. Hoechstens ein Teil je Aufruf.
     */
    static void tryUnload(Level level, BlockPos pos, Direction side, TransporterBlockEntityBase self) {
        IItemHandler inv = at(level, pos, side);
        if (inv == null) return;

        for (int i = self.inputSlotMax; i < self.outputSlotMax; i++) {
            ItemStack out = self.getStack(i);
            if (out.isEmpty()) continue;

            ItemStack one = out.copy();
            one.setCount(1);

            for (int j = 0; j < inv.getSlots(); j++) {
                ItemStack target = inv.getStackInSlot(j);
                if (target.isEmpty() || !StackNbt.sameItemSameTags(out, target)) continue;
                if (target.getCount() >= Math.min(target.getMaxStackSize(), inv.getSlotLimit(j))) continue;
                if (inv.insertItem(j, one, false).isEmpty()) {
                    out.shrink(1);
                    self.setStack(i, out);
                    return;
                }
            }

            for (int j = 0; j < inv.getSlots(); j++) {
                if (!inv.getStackInSlot(j).isEmpty()) continue;
                if (inv.insertItem(j, one, false).isEmpty()) {
                    out.shrink(1);
                    self.setStack(i, out);
                    return;
                }
            }
        }
    }
}
