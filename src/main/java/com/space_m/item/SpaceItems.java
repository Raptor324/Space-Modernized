package com.space_m.item;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import com.space_m.SpaceMod;
import com.space_m.main.SpaceCreativeTabs;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;

/**
 * Items des Addons (Original: die 187 neuen Felder in {@code com.hbm.items.ModItems} von NTM Space).
 *
 * <p>Registry-ID = Original-{@code setUnlocalizedName("...")}. BlockItems legt {@link com.space_m.block.SpaceBlocks} an.
 * {@link #basic(String, Supplier, SpaceCreativeTabs.Tab)} markiert das Item fuer ein generiertes Item-Modell
 * ({@code item/generated}, Textur {@code space_m:item/<id>}).
 */
public final class SpaceItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(SpaceMod.MOD_ID, Registries.ITEM);

    private static final List<RegistrySupplier<Item>> GENERATED_MODELS = new ArrayList<>();

    // ── Eintraege (je Runde/Agent ein eigener, kommentierter Abschnitt) ─────────────────────────

    // R1-D Zeit: Original wand_time (consumableTab, setMaxStackSize(1))
    public static final RegistrySupplier<com.space_m.time.ItemWandTime> WAND_TIME = basic("wand_time",
            () -> new com.space_m.time.ItemWandTime(new Item.Properties().stacksTo(1)), SpaceCreativeTabs.Tab.CONSUMABLE);

    // R2-B Lebenserhaltung (Original ModItems; ItemArmorMod: consumableTab, Stapel 1)
    public static final RegistrySupplier<com.space_m.lifesupport.ItemModOxy> OXY_PLSS = basic("oxy_plss",
            () -> new com.space_m.lifesupport.ItemModOxy(new Item.Properties().stacksTo(1),
                    () -> com.hbm_m.inventory.fluid.ModFluids.OXYGEN.getSource(), 16000, 10, 1), SpaceCreativeTabs.Tab.CONSUMABLE);
    public static final RegistrySupplier<com.hbm_m.item.liquids.InfiniteFluidItem> OXY_PINWHEEL = basic("oxy_pinwheel",
            () -> new com.hbm_m.item.liquids.InfiniteFluidItem(new Item.Properties(),
                    com.hbm_m.inventory.fluid.ModFluids.OXYGEN.getSource(), 1), SpaceCreativeTabs.Tab.PARTS);
    public static final RegistrySupplier<com.space_m.lifesupport.ItemLoxTank> LOX_TANK = basic("lox_tank",
            () -> new com.space_m.lifesupport.ItemLoxTank(new Item.Properties().stacksTo(16)), SpaceCreativeTabs.Tab.CONSUMABLE);
    public static final RegistrySupplier<com.space_m.lifesupport.ItemAtmosphereScanner> ATMOSPHERE_SCANNER = basic("atmosphere_scanner",
            () -> new com.space_m.lifesupport.ItemAtmosphereScanner(new Item.Properties().stacksTo(1)), SpaceCreativeTabs.Tab.CONSUMABLE);
    public static final RegistrySupplier<com.space_m.lifesupport.ItemModHeavyBoots> HEAVY_BOOTS = basic("heavy_boots",
            () -> new com.space_m.lifesupport.ItemModHeavyBoots(new Item.Properties().stacksTo(1)), SpaceCreativeTabs.Tab.CONSUMABLE);

    // R5-C Transporter-Linker (Original transporter_linker: controlTab, setMaxStackSize(1))
    public static final RegistrySupplier<com.space_m.rocket.transport.ItemTransporterLinker> TRANSPORTER_LINKER = basic("transporter_linker",
            () -> new com.space_m.rocket.transport.ItemTransporterLinker(new Item.Properties().stacksTo(1)), SpaceCreativeTabs.Tab.CONTROL);

    private SpaceItems() {}

    public static <T extends Item> RegistrySupplier<T> register(String name, Supplier<T> item, SpaceCreativeTabs.Tab tab) {
        RegistrySupplier<T> reg = ITEMS.register(name, item);
        if (tab != null) SpaceCreativeTabs.add(tab, reg);
        return reg;
    }

    @SuppressWarnings("unchecked")
    public static <T extends Item> RegistrySupplier<T> basic(String name, Supplier<T> item, SpaceCreativeTabs.Tab tab) {
        RegistrySupplier<T> reg = register(name, item, tab);
        GENERATED_MODELS.add((RegistrySupplier<Item>) (RegistrySupplier<?>) reg);
        return reg;
    }

    public static List<RegistrySupplier<Item>> generatedModels() { return Collections.unmodifiableList(GENERATED_MODELS); }

    public static void init() {
        // R4-B: Space-Materialien/-Items (eigene Klasse, muss vor ITEMS.register() geladen sein)
        SpaceMaterialItems.init();
        // R9-B: Space-Watz-Pellets, Curium-RBMK-Staebe, laser_crystal_iron, Schallplatten
        SpaceNuclearItems.init();
        // R5-A: Raketenteile, Rakete, Ziel-Laufwerk
        com.space_m.rocket.SpaceRocketItems.init();
        // R6-C: Space-Satelliten (Dyson-Relais, Railkanone, Praezisionslaser, Detektor, Strahlscanner)
        com.space_m.satellite.SpaceSatelliteItems.init();
        // R8-B: Spawn-Eier Siege-UFO / War-Behemoth (Original EntityMappings.addMob mit Eifarben)
        com.space_m.war.WarEntities.initItems();
        // E11/E12: Fork-Kassetten und NITAN-Ambrosia
        com.space_m.fork.ForkItems.init();
        ITEMS.register();
    }
}
