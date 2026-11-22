package com.space_m.machine.misc.client;

import com.space_m.machine.misc.MiscMenus;

import dev.architectury.registry.menu.MenuRegistry;

/**
 * Client-Anmeldung R7-A Sonstiges (GUIs {@code GUIMachineStardar}, {@code GUIWarController}).
 * BERs and item models migrated to Nucleus declarative specs in SpaceMachineRenderers.
 */
public final class MiscClient {

    private MiscClient() {}

    /** Aus {@code SpaceClient.setup} (Client-Setup). */
    public static void setup() {
        MenuRegistry.registerScreenFactory(MiscMenus.STARDAR.get(), GUIMachineStardar::new);
        MenuRegistry.registerScreenFactory(MiscMenus.WAR_CONTROLLER.get(), GUIWarController::new);
    }
}
