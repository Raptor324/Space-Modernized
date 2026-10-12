package com.space_m.machine.misc;

import com.space_m.inventory.SpaceMenus;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.world.inventory.MenuType;

/**
 * R7-A Menues (Original {@code ContainerStardar}, {@code ContainerMachineWarController}); oeffnen mit
 * {@code MenuRegistry.openExtendedMenu(player, be, buf -> buf.writeBlockPos(pos))}.
 */
public final class MiscMenus {

    private MiscMenus() {}

    public static final RegistrySupplier<MenuType<ContainerStardar>> STARDAR =
            SpaceMenus.extended("machine_stardar", ContainerStardar::new);

    public static final RegistrySupplier<MenuType<ContainerMachineWarController>> WAR_CONTROLLER =
            SpaceMenus.extended("war_controller", ContainerMachineWarController::new);

    /** Aus {@code SpaceMenus.init()} vor {@code MENUS.register()} (Klasse laden). */
    public static void init() { }
}
