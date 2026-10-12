package com.space_m.inventory;

import com.space_m.SpaceMod;

import dev.architectury.registry.menu.MenuRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

/**
 * Menues (Container) des Addons (Original: 13 neue Container / 16 GUIs von NTM Space).
 *
 * <p>Menues mit BlockPos o.ae. ueber {@link #extended(String, MenuRegistry.ExtendedMenuTypeFactory)}
 * (Architectury, oeffnen mit {@code MenuRegistry.openExtendedMenu}); Screens meldet
 * {@link com.space_m.client.SpaceClient} an.
 */
public final class SpaceMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(SpaceMod.MOD_ID, Registries.MENU);

    // ── Eintraege (je Runde/Agent ein eigener, kommentierter Abschnitt) ─────────────────────────

    // R5-C Transporter-Rampe (Original ContainerTransporterRocket)
    public static final RegistrySupplier<MenuType<com.space_m.rocket.transport.TransporterRocketMenu>> TRANSPORTER_ROCKET =
            extended("transporter_rocket", com.space_m.rocket.transport.TransporterRocketMenu::new);

    // R5-A Raketenmontage (Original ContainerMachineRocketAssembly)
    public static final RegistrySupplier<MenuType<com.space_m.rocket.menu.RocketAssemblyMenu>> ROCKET_ASSEMBLY =
            extended("rocket_assembly", com.space_m.rocket.menu.RocketAssemblyMenu::new);

    // R5-B Raketen-Startrampe (Original ContainerLaunchPadRocket)
    public static final RegistrySupplier<MenuType<com.space_m.rocket.pad.ContainerLaunchPadRocket>> LAUNCH_PAD_ROCKET =
            extended("launch_pad_rocket", com.space_m.rocket.pad.ContainerLaunchPadRocket::new);

    private SpaceMenus() {}

    public static <T extends AbstractContainerMenu> RegistrySupplier<MenuType<T>> extended(
            String name, MenuRegistry.ExtendedMenuTypeFactory<T> factory) {
        return MENUS.register(name, () -> MenuRegistry.ofExtended(factory));
    }

    public static void init() {
        com.space_m.orbit.propulsion.PropulsionMenus.init(); // R6-B (Klasse laden)
        com.space_m.machine.misc.MiscMenus.init(); // R7-A (Klasse laden)
        MENUS.register();
    }
}
