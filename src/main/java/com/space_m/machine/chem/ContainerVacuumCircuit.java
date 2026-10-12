package com.space_m.machine.chem;

import com.space_m.machine.chem.recipe.VacuumCircuitRecipe;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 NTM Space {@code ContainerVacuumCircuit}: Eingaben 2x2 ab 26|18 (0-1 Halbleiter, 2-3 Platinen), Ausgabe 98|27,
 * Batterie 143|72, Aufwertungen 89|72 und 107|72, Spielerinventar 8|122, Leiste 180.
 * Original-Klasse: ContainerVacuumCircuit
 */
public class ContainerVacuumCircuit extends ChemMenu<TileEntityMachineVacuumCircuit> {

    public ContainerVacuumCircuit(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, readBe(inv, buf, TileEntityMachineVacuumCircuit.class));
    }

    public ContainerVacuumCircuit(int id, Inventory inv, TileEntityMachineVacuumCircuit tile) {
        super(ChemMachines.VACUUM_CIRCUIT_MENU.get(), id, tile);

        // Inputs (SlotNonRetarded)
        for (int i = 0; i < 2; i++) for (int j = 0; j < 2; j++) machine(new Slot(container, i * 2 + j, 26 + j * 18, 18 + i * 18));
        // Output
        machine(new TakeOnly(container, 4, 98, 27));
        // Battery
        machine(new Slot(container, 5, 143, 72));
        // Upgrades
        machine(new Upgrade(container, 6, 89, 72));
        machine(new Upgrade(container, 7, 107, 72));

        playerInv(inv, 8, 122, 180);
    }

    @Override
    protected boolean moveToMachine(ItemStack stack) {
        if (com.hbm_m.api.energy.ItemEnergyAccess.isEnergySource(stack)) return merge(stack, 5, 6);
        if (stack.getItem() instanceof com.hbm_m.item.industrial.ItemMachineUpgrade) return merge(stack, 6, 8);
        if (VacuumCircuitRecipe.isWafer(be.getLevel(), stack)) return merge(stack, 0, 2);
        if (VacuumCircuitRecipe.isPcb(be.getLevel(), stack)) return merge(stack, 2, 4);
        return false;
    }
}
