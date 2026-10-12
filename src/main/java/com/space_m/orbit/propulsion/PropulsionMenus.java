package com.space_m.orbit.propulsion;

import com.space_m.inventory.SpaceMenus;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.world.inventory.MenuType;

/**
 * R6-B: Menues (Original {@code ContainerOrbitalStationComputer}, {@code ContainerOrbitalStationLauncher}); oeffnen mit
 * {@code MenuRegistry.openExtendedMenu(player, be, buf -> buf.writeBlockPos(pos))}.
 */
public final class PropulsionMenus {

    private PropulsionMenus() {}

    public static final RegistrySupplier<MenuType<ContainerOrbitalStationComputer>> ORBITAL_STATION_COMPUTER =
            SpaceMenus.extended("orbital_station_computer", ContainerOrbitalStationComputer::new);

    public static final RegistrySupplier<MenuType<ContainerOrbitalStationLauncher>> ORBITAL_STATION_LAUNCHER =
            SpaceMenus.extended("orbital_station_launcher", ContainerOrbitalStationLauncher::new);

    /** Aus {@code SpaceMenus.init()} vor {@code MENUS.register()} (Klasse laden). */
    public static void init() { }
}
