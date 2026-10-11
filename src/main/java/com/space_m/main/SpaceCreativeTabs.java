package com.space_m.main;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import dev.architectury.registry.CreativeTabRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

/**
 * Creative-Tabs des Originals. NTM Space fuehrt <b>keine eigenen Tabs</b> ein, sondern sortiert seine Inhalte
 * in die neun Tabs von {@code MainRegistry} (1.7.10) ein: partsTab, controlTab, templateTab, blockTab,
 * machineTab, nukeTab, missileTab, weaponTab, consumableTab. HBM-Modernized bildet genau diese neun nach
 * ({@code com.hbm_m.main.ModCreativeTabs}); das Addon haengt seine Eintraege dort an (Architectury
 * {@link CreativeTabRegistry#modify}, beide Loader). Reihenfolge = Reihenfolge der {@link #add}-Aufrufe.
 */
public final class SpaceCreativeTabs {

    /** Original-Tab (1.7.10 {@code MainRegistry.xTab}) -> Registry-ID des Tabs in HBM-Modernized. */
    public enum Tab {
        /** partsTab "tabParts": ingots, nuggets, wires, machine parts */
        PARTS("ntm_parts_tab"),
        /** controlTab "tabControl": items that belong in machines, fuels, etc */
        CONTROL("ntm_control_tab"),
        /** templateTab "tabTemplate": templates, siren tracks */
        TEMPLATE("ntm_template_tab"),
        /** blockTab "tabBlocks": ore and mineral blocks */
        BLOCKS("ntm_blocks_tab"),
        /** machineTab "tabMachine": machines, structure parts */
        MACHINE("ntm_machine_tab"),
        /** nukeTab "tabNuke": bombs */
        NUKE("ntm_nuke_tab"),
        /** missileTab "tabMissile": missiles, satellites */
        MISSILE("ntm_missile_tab"),
        /** weaponTab "tabWeapon": turrets, weapons, ammo */
        WEAPON("ntm_weapon_tab"),
        /** consumableTab "tabConsumable": drinks, kits, tools */
        CONSUMABLE("ntm_consumable_tab");

        public final String hbmTabId;

        Tab(String hbmTabId) {
            this.hbmTabId = hbmTabId;
        }

        public ResourceLocation id() {
            return ResourceLocation.fromNamespaceAndPath("hbm_m", hbmTabId);
        }
    }

    private static final Map<Tab, List<Supplier<ItemStack>>> ENTRIES = new EnumMap<>(Tab.class);
    private static boolean initialized;

    private SpaceCreativeTabs() {}

    /** Haengt ein Item/einen Block an den Original-Tab an. */
    public static void add(Tab tab, Supplier<? extends ItemLike> item) {
        addStack(tab, () -> new ItemStack(item.get()));
    }

    /** Haengt einen Stack (z.B. mit NBT/Meta-Variante) an den Original-Tab an. */
    public static void addStack(Tab tab, Supplier<ItemStack> stack) {
        ENTRIES.computeIfAbsent(tab, t -> new ArrayList<>()).add(stack);
    }

    public static List<Supplier<ItemStack>> entries(Tab tab) {
        return ENTRIES.getOrDefault(tab, List.of());
    }

    /** Einmal im Mod-Konstruktor: meldet je Tab einen Modify-Listener an (Eintraege werden erst beim Fuellen gelesen). */
    public static void init() {
        if (initialized) return;
        initialized = true;
        for (Tab tab : Tab.values()) {
            CreativeTabRegistry.modify(CreativeTabRegistry.defer(tab.id()), (flags, output, canUseOps) -> {
                for (Supplier<ItemStack> s : entries(tab)) {
                    ItemStack st = s.get();
                    if (!st.isEmpty()) output.accept(st);
                }
            });
        }
    }
}
