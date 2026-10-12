package com.space_m.machine.chem;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 NTM Space {@code ContainerHydroponic} ({@code ContainerBase}): Saat 67|18, Duenger 67|54, Batterie 147|54, Ernte
 * 111|18/36/54 (nur entnehmen), Spielerinventar 8|104, Leiste 162. Umschalt-Klick wie {@code ContainerBase}: in die
 * ersten passenden Maschinenplaetze.
 * Original-Klasse: ContainerHydroponic
 */
public class ContainerHydroponic extends ChemMenu<TileEntityHydroponic> {

    public ContainerHydroponic(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, readBe(inv, buf, TileEntityHydroponic.class));
    }

    public ContainerHydroponic(int id, Inventory inv, TileEntityHydroponic hydro) {
        super(ChemMachines.HYDROBAY_MENU.get(), id, hydro);

        // Inputs
        machine(new Slot(container, 0, 67, 18));
        machine(new Slot(container, 1, 67, 54));
        // Battery
        machine(new Slot(container, 2, 147, 54));
        // Outputs
        for (int i = 0; i < 3; i++) machine(new TakeOnly(container, i + 3, 111, 18 + i * 18));

        playerInv(inv, 8, 104, 162);
    }

    @Override
    protected boolean moveToMachine(ItemStack stack) {
        return merge(stack, 0, 3);
    }
}
