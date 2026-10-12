package com.space_m.satellite;

import java.util.function.Supplier;

import com.space_m.item.SpaceItems;
import com.space_m.main.SpaceCreativeTabs;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * R6-C: die fuenf Space-neuen Satelliten (Original {@code ModItems}, Registry-ID = {@code setUnlocalizedName}):
 * {@code sat_dyson_relay} (32 t), {@code sat_war} (128 t, kein Creative-Tab, Textur sat_gerald), {@code sat_precision_laser}
 * (42 t), {@code sat_detector} (24 t), {@code sat_ray_scan} (34 t) - Massen in R5-As Teile-Tabelle. missileTab, Stapel 1.
 * Creative-Stapel mit Satellitendaten (Original {@code getSubItems} -> {@code ensureItemData}).
 */
public final class SpaceSatelliteItems {

    private SpaceSatelliteItems() {}

    private static RegistrySupplier<Item> register(String id, Supplier<Item> item, boolean tab) {
        RegistrySupplier<Item> reg = SpaceItems.register(id, item, null);
        if (tab) SpaceCreativeTabs.addStack(SpaceCreativeTabs.Tab.MISSILE, () -> creativeStack(reg.get()));
        return reg;
    }

    private static ItemStack creativeStack(Item item) {
        ItemStack stack = new ItemStack(item);
        SatelliteItemData.ensureItemData(stack);
        return stack;
    }

    public static final RegistrySupplier<Item> SAT_DYSON_RELAY = register("sat_dyson_relay", () -> new ItemSpaceSatellite(new Item.Properties()), true);
    public static final RegistrySupplier<Item> SAT_WAR = register("sat_war", () -> new ItemSatWar(new Item.Properties()), false);
    public static final RegistrySupplier<Item> SAT_PRECISION_LASER = register("sat_precision_laser", () -> new ItemSpaceSatellite(new Item.Properties()), true);
    public static final RegistrySupplier<Item> SAT_DETECTOR = register("sat_detector", () -> new ItemSpaceSatellite(new Item.Properties()), true);
    public static final RegistrySupplier<Item> SAT_RAY_SCAN = register("sat_ray_scan", () -> new ItemSpaceSatellite(new Item.Properties()), true);

    /** Aus {@code SpaceItems.init()} vor {@code ITEMS.register()}. */
    public static void init() { }
}
