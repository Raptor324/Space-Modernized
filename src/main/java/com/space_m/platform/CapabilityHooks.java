package com.space_m.platform;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Item-Capabilities fremder/HBM-Block-Entities, loaderneutral mit Vanilla-Typen.
 * Forge 1.20.1: {@code ForgeCapabilities.ITEM_HANDLER}; NeoForge 1.21.1: HBMs Bruecke
 * {@code com.hbm_m.platform.HbmCaps}/{@code HbmCap.ITEM_HANDLER} (eigene HBM-BEs direkt, fremde ueber die Block-Capability).
 *
 * <p>Energie laeuft ueber HBMs eigene Schnittstellen ({@code IEnergyReceiver}/{@code IEnergyProvider}), Fluessigkeiten
 * ueber das HBM-Fluidnetz - dafuer keine Capability-Fassade noetig. Eigene Addon-BEs erben von
 * {@code BaseHbmBlockEntity} und bekommen ihre Capabilities von HBM ({@code ModCapabilities}) bzw. melden sie in der
 * jeweiligen Runde an.
 */
public final class CapabilityHooks {
    private CapabilityHooks() {}

    //? if forge {
    @Nullable
    private static net.minecraftforge.items.IItemHandler items(@Nullable BlockEntity be, @Nullable Direction side) {
        if (be == null) return null;
        return be.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER, side).resolve().orElse(null);
    }
    //?} else {
    /*@Nullable
    private static net.neoforged.neoforge.items.IItemHandler items(@Nullable BlockEntity be, @Nullable Direction side) {
        if (be == null) return null;
        return com.hbm_m.platform.HbmCaps.get(be, com.hbm_m.platform.HbmCap.ITEM_HANDLER, side).orElse(null);
    }
    *///?}

    public static boolean hasItemHandler(@Nullable BlockEntity be, @Nullable Direction side) {
        return items(be, side) != null;
    }

    public static int slots(@Nullable BlockEntity be, @Nullable Direction side) {
        var h = items(be, side);
        return h == null ? 0 : h.getSlots();
    }

    public static ItemStack getStackInSlot(@Nullable BlockEntity be, @Nullable Direction side, int slot) {
        var h = items(be, side);
        return h == null || slot < 0 || slot >= h.getSlots() ? ItemStack.EMPTY : h.getStackInSlot(slot);
    }

    /** Fuegt {@code stack} in beliebige Slots ein; Rueckgabe = Rest (leer = alles untergebracht). */
    public static ItemStack insert(@Nullable BlockEntity be, @Nullable Direction side, ItemStack stack, boolean simulate) {
        var h = items(be, side);
        if (h == null || stack.isEmpty()) return stack;
        ItemStack rest = stack.copy();
        for (int i = 0; i < h.getSlots() && !rest.isEmpty(); i++) {
            rest = h.insertItem(i, rest, simulate);
        }
        return rest;
    }

    public static ItemStack extract(@Nullable BlockEntity be, @Nullable Direction side, int slot, int amount, boolean simulate) {
        var h = items(be, side);
        if (h == null || slot < 0 || slot >= h.getSlots()) return ItemStack.EMPTY;
        return h.extractItem(slot, amount, simulate);
    }
}
