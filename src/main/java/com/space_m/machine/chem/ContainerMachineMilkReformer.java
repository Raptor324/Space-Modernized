package com.space_m.machine.chem;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 NTM Space {@code ContainerMachineMilkReformer}: Batterie 79|8, Milch-Kanister 45|88 (aus 45|106), Ausgabe-Kanister
 * 95/122/149 | 88 (aus 106), Spielerinventar 8|156, Leiste 214. Abweichung: Umschalt-Klick einer Batterie geht nach
 * Platz 0 (Original {@code mergeItemStack(.., 1, 0, ..)} = leerer Bereich, die Batterie blieb liegen).
 * Original-Klasse: ContainerMachineMilkReformer
 */
public class ContainerMachineMilkReformer extends ChemMenu<TileEntityMachineMilkReformer> {

    public ContainerMachineMilkReformer(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, readBe(inv, buf, TileEntityMachineMilkReformer.class));
    }

    public ContainerMachineMilkReformer(int id, Inventory inv, TileEntityMachineMilkReformer tedf) {
        super(ChemMachines.MILK_REFORMER_MENU.get(), id, tedf);

        // Battery
        machine(new Slot(container, 0, 79, 8));
        // Canister Input / Output
        machine(new Slot(container, 1, 45, 88));
        machine(new TakeOnly(container, 2, 45, 106));
        // Reformate Input / Output
        machine(new Slot(container, 3, 95, 88));
        machine(new TakeOnly(container, 4, 95, 106));
        // Gas Input / Output
        machine(new Slot(container, 5, 122, 88));
        machine(new TakeOnly(container, 6, 122, 106));
        // Hydrogen Input / Output
        machine(new Slot(container, 7, 149, 88));
        machine(new TakeOnly(container, 8, 149, 106));

        playerInv(inv, 8, 156, 214);
    }

    @Override
    protected boolean moveToMachine(ItemStack stack) {
        if (com.hbm_m.api.energy.ItemEnergyAccess.isEnergySource(stack)) return merge(stack, 0, 1);
        return merge(stack, 1, 2) || merge(stack, 3, 4) || merge(stack, 5, 6) || merge(stack, 7, 8);
    }
}
