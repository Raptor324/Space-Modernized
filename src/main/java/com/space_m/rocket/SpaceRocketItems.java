package com.space_m.rocket;

import com.space_m.item.SpaceItems;
import com.space_m.main.SpaceCreativeTabs;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.world.item.Item;

/**
 * R5-A: Raketen-Items (Original {@code ModItems}: Space-neue {@code mp_*}/{@code rp_*}, {@code rocket_custom},
 * {@code full_drive}). Registry-ID = Original-{@code setUnlocalizedName}. Die Teilewerte stehen in {@link RocketParts}.
 */
public final class SpaceRocketItems {

    private SpaceRocketItems() {}

    /** Raketenteil (missileTab); {@code single} = Original {@code setMaxStackSize(1)}. */
    private static RegistrySupplier<Item> register(String id, float a, float b, boolean single) {
        return SpaceItems.register(id, () -> new ItemRocketPart(single ? new Item.Properties().stacksTo(1) : new Item.Properties(), a, b),
                SpaceCreativeTabs.Tab.MISSILE);
    }

    // Triebwerke (makeThruster: Verbrauch 1F, Nutzlast 1_500 bzw. 100_000)
    public static final RegistrySupplier<Item> MP_THRUSTER_20_METHALOX = register("mp_thruster_20_methalox", 1F, 100_000F, false);
    public static final RegistrySupplier<Item> MP_THRUSTER_20_METHALOX_DUAL = register("mp_thruster_20_methalox_dual", 1F, 100_000F, false);
    public static final RegistrySupplier<Item> MP_THRUSTER_20_METHALOX_TRIPLE = register("mp_thruster_20_methalox_triple", 1F, 100_000F, false);
    public static final RegistrySupplier<Item> MP_THRUSTER_20_HYDROGEN = register("mp_thruster_20_hydrogen", 1F, 100_000F, false);
    public static final RegistrySupplier<Item> MP_THRUSTER_20_HYDROGEN_DUAL = register("mp_thruster_20_hydrogen_dual", 1F, 100_000F, false);
    public static final RegistrySupplier<Item> MP_THRUSTER_20_HYDROGEN_TRIPLE = register("mp_thruster_20_hydrogen_triple", 1F, 100_000F, false);
    public static final RegistrySupplier<Item> MP_THRUSTER_20_HYDRAZINE = register("mp_thruster_20_hydrazine", 1F, 100_000F, false);
    public static final RegistrySupplier<Item> MP_THRUSTER_10_HYDRAZINE = register("mp_thruster_10_hydrazine", 1F, 1_500F, false);

    // Rumpfe (makeFuselage)
    public static final RegistrySupplier<Item> MP_FUSELAGE_10_HYDRAZINE = register("mp_fuselage_10_hydrazine", 0F, 0F, false);
    /** Original-Feld {@code rp_fuselage_20_12_hydrazine}. */
    public static final RegistrySupplier<Item> RP_FUSELAGE_20_12_HYDRAZINE = register("mp_fuselage_20_hydrazine", 0F, 0F, false);
    public static final RegistrySupplier<Item> RP_FUSELAGE_20_12 = register("rp_f_20_12", 0F, 0F, false);
    public static final RegistrySupplier<Item> RP_FUSELAGE_20_6 = register("rp_f_20_6", 0F, 0F, false);
    public static final RegistrySupplier<Item> RP_FUSELAGE_20_3 = register("rp_f_20_3", 0F, 0F, false);
    public static final RegistrySupplier<Item> RP_FUSELAGE_20_1 = register("rp_f_20_1", 0F, 0F, false);

    // Landebeine (makeStability(0, SIZE_20), Stapel 1)
    public static final RegistrySupplier<Item> RP_LEGS_20 = register("rp_l_20", 0F, 0F, true);

    // Nutzlasten (makeWarhead APOLLO/SATELLITE, Stapel 1)
    public static final RegistrySupplier<Item> RP_CAPSULE_20 = register("rp_c_20", 0F, 0F, true);
    public static final RegistrySupplier<Item> RP_STATION_CORE_20 = register("rp_sc_20", 0F, 0F, true);
    public static final RegistrySupplier<Item> RP_POD_20 = register("rp_pod_20", 0F, 0F, true);

    /** Original {@code rocket_custom}: Stapel 1, kein Creative-Tab, Textur missile_custom. */
    public static final RegistrySupplier<Item> ROCKET_CUSTOM = SpaceItems.register("rocket_custom",
            () -> new ItemCustomRocket(new Item.Properties().stacksTo(1)), null);

    /** Original {@code full_drive} = {@code hard_drive_full} (controlTab, Stapel 1, nicht reparierbar). */
    public static final RegistrySupplier<Item> FULL_DRIVE = SpaceItems.register("hard_drive_full",
            () -> new ItemVOTVdrive(new Item.Properties()), null);

    /** Aus {@code SpaceItems.init()} vor {@code ITEMS.register()}. */
    public static void init() {
        SpaceCreativeTabs.Tab tab = SpaceCreativeTabs.Tab.CONTROL;
        for (int i = 0; i < com.space_m.celestial.SolarSystem.Body.values().length; i++) {
            final int idx = i;
            SpaceCreativeTabs.addStack(tab, () -> ItemVOTVdrive.creativeStacks().get(idx));
        }
    }
}
