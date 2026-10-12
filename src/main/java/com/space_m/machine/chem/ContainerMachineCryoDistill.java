package com.space_m.machine.chem;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 NTM Space {@code ContainerMachineCryoDistill}: Batterie (26|90), Fluid-Identifikator (26|108), Kanister der vier
 * Ausgaenge (80/98/116/134 | 90 ein, 108 aus), Spielerinventar 8|156, Leiste 214.
 * Abweichung: das Original verschiebt per Umschalt-Klick aus der Maschine ab Index 8 und Identifikatoren nach 11
 * (Indexfehler); hier ins Spielerinventar bzw. in Platz 7.
 * Original-Klasse: ContainerMachineCryoDistill
 */
public class ContainerMachineCryoDistill extends ChemMenu<TileEntityMachineCryoDistill> {

    public ContainerMachineCryoDistill(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, readBe(inv, buf, TileEntityMachineCryoDistill.class));
    }

    public ContainerMachineCryoDistill(int id, Inventory inv, TileEntityMachineCryoDistill tedf) {
        super(ChemMachines.CRYO_DISTILL_MENU.get(), id, tedf);

        machine(new Slot(container, 0, 26, 90));
        machine(new Slot(container, 1, 80, 90));
        machine(new TakeOnly(container, 2, 80, 108));
        machine(new Slot(container, 3, 98, 90));
        machine(new TakeOnly(container, 4, 98, 108));
        machine(new Slot(container, 5, 116, 90));
        machine(new TakeOnly(container, 6, 116, 108));
        machine(new Slot(container, 7, 26, 108));
        machine(new Slot(container, 8, 134, 90));
        machine(new TakeOnly(container, 9, 134, 108));

        playerInv(inv, 8, 156, 214);
    }

    @Override
    protected boolean moveToMachine(ItemStack stack) {
        if (com.hbm_m.api.energy.ItemEnergyAccess.isEnergySource(stack)) return merge(stack, 0, 1);
        if (stack.getItem() instanceof com.hbm_m.interfaces.IItemFluidIdentifier) return merge(stack, 7, 8);
        return merge(stack, 1, 2) || merge(stack, 3, 4) || merge(stack, 5, 6) || merge(stack, 8, 9);
    }
}
