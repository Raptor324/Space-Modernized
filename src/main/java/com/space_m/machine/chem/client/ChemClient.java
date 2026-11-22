package com.space_m.machine.chem.client;

import com.space_m.machine.chem.ChemMachines;

import dev.architectury.registry.menu.MenuRegistry;

/**
 * Client-Anmeldung R7-B Chemie (GUIs {@code GUIMachineCryoDistill}, {@code GUIVacuumCircuit},
 * {@code GUIMilkReformer}, {@code GUIHydroponic}).
 * BlockEntityRenderers and item models migrated to Nucleus declarative specs and loaders.
 */
public final class ChemClient {
    private ChemClient() {}

    public static void setup() {
        MenuRegistry.registerScreenFactory(ChemMachines.CRYO_DISTILL_MENU.get(), GUIMachineCryoDistill::new);
        MenuRegistry.registerScreenFactory(ChemMachines.VACUUM_CIRCUIT_MENU.get(), GUIVacuumCircuit::new);
        MenuRegistry.registerScreenFactory(ChemMachines.MILK_REFORMER_MENU.get(), GUIMilkReformer::new);
        MenuRegistry.registerScreenFactory(ChemMachines.HYDROBAY_MENU.get(), GUIHydroponic::new);
    }
}
