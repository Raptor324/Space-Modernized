package com.space_m.orbit.propulsion.client;

import com.space_m.orbit.propulsion.PropulsionBlockEntities;
import com.space_m.orbit.propulsion.PropulsionMenus;

import dev.architectury.registry.client.rendering.BlockEntityRendererRegistry;
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
        BlockEntityRendererRegistry.register(PropulsionBlockEntities.HTR3.get(), RenderHTR3::new);
        BlockEntityRendererRegistry.register(PropulsionBlockEntities.HTRF4.get(), RenderHTRF4::new);
        BlockEntityRendererRegistry.register(PropulsionBlockEntities.HTRF4_NEO.get(), RenderHTRNeo::new);
        BlockEntityRendererRegistry.register(PropulsionBlockEntities.XENON_THRUSTER.get(), RenderXenonThruster::new);
        BlockEntityRendererRegistry.register(PropulsionBlockEntities.ORBITAL_STATION_COMPUTER.get(), RenderOrbitalComputer::new);
        BlockEntityRendererRegistry.register(PropulsionBlockEntities.ORBITAL_STATION_LAUNCHER.get(), RenderOrbitalStationLauncher::new);

        MenuRegistry.registerScreenFactory(PropulsionMenus.ORBITAL_STATION_COMPUTER.get(), GUIOrbitalStationComputer::new);
        MenuRegistry.registerScreenFactory(PropulsionMenus.ORBITAL_STATION_LAUNCHER.get(), GUIOrbitalStationLauncher::new);
    }
}
