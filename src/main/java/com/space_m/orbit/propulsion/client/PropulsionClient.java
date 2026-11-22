package com.space_m.orbit.propulsion.client;

import com.space_m.orbit.propulsion.PropulsionMenus;

import dev.architectury.registry.menu.MenuRegistry;

/**
 * Client-Anmeldung des Stationsantriebs (Original {@code ClientProxy}: {@code RenderHTR3}, {@code RenderHTRF4},
 * {@code RenderHTRNeo}, {@code RenderXenonThruster}, {@code RenderOrbitalComputer}, {@code RenderOrbitalStation} fuer den
 * Startport; GUIs {@code GUIOrbitalStationComputer}, {@code GUIOrbitalStationLauncher}).
 */
public final class PropulsionClient {

    private PropulsionClient() {}

    /** Aus {@code SpaceClient.setup} (Client-Setup). */
    public static void setup() {
        // BERs migrated to Nucleus declarative specs in SpaceMachineRenderers

        MenuRegistry.registerScreenFactory(PropulsionMenus.ORBITAL_STATION_COMPUTER.get(), GUIOrbitalStationComputer::new);
        MenuRegistry.registerScreenFactory(PropulsionMenus.ORBITAL_STATION_LAUNCHER.get(), GUIOrbitalStationLauncher::new);
    }
}
