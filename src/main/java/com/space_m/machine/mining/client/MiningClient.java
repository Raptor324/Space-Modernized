package com.space_m.machine.mining.client;

import com.space_m.machine.mining.MiningMachines;

import dev.architectury.registry.menu.MenuRegistry;

/**
 * Client-Anmeldung R7-B Bergbau (GUI {@code GUIMachineDriveProcessor}).
 * BERs and item models migrated to Nucleus declarative specs in SpaceMachineRenderers.
 * Aus {@code SpaceClient.setup}.
 */
public final class MiningClient {
    private MiningClient() {}

    public static void setup() {
        MenuRegistry.registerScreenFactory(MiningMachines.DRIVE_PROCESSOR_MENU.get(), GUIMachineDriveProcessor::new);
    }
}
