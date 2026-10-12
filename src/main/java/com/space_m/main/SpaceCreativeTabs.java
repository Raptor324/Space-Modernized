package com.space_m.main;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.hbm_m.api.fluids.HbmFluidRegistry;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.liquids.FluidDuctItem;
import com.hbm_m.item.liquids.FluidIdentifierItem;
import com.hbm_m.item.liquids.ItemFluidTank;
import com.space_m.SpaceMod;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenCustomHashSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackLinkedSet;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluid;

/**
 * Creative-Tabs des Originals. NTM Space fuehrt <b>keine eigenen Tabs</b> ein, sondern sortiert seine Inhalte
 * in die neun Tabs von {@code MainRegistry} (1.7.10) ein: partsTab, controlTab, templateTab, blockTab,
 * machineTab, nukeTab, missileTab, weaponTab, consumableTab. HBM-Modernized bildet genau diese neun nach
 * ({@code com.hbm_m.main.ModCreativeTabs}); das Addon haengt seine Eintraege dort an (Architectury
 * {@link CreativeTabRegistry#modify}, beide Loader). Reihenfolge = Reihenfolge der {@link #add}-Aufrufe.
 * <p>
 * Zusaetzlich (Nutzerwunsch, kein Original): ein eigener Sammel-Tab {@code space_m:ntm_space_tab} "NTM Space",
 * der automatisch <b>alle</b> {@code space_m}-Items zeigt - siehe {@link #fillSpaceTab}. Die Einordnung in die
 * HBM-Tabs bleibt unveraendert.
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

    /** Reihenfolge der HBM-Tab-Eintraege im Sammel-Tab: Bloecke/Erze, Materialien, Maschinenteile, Ausruestung, Maschinen, Raketen. */
    private static final Tab[] SPACE_TAB_ORDER = {
            Tab.BLOCKS, Tab.PARTS, Tab.CONTROL, Tab.CONSUMABLE, Tab.WEAPON, Tab.MACHINE, Tab.TEMPLATE, Tab.MISSILE, Tab.NUKE };

    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(SpaceMod.MOD_ID, Registries.CREATIVE_MODE_TAB);

    /** Eigener Sammel-Tab "NTM Space" (Lang {@code itemGroup.space_m.ntm_space_tab}). */
    public static final RegistrySupplier<CreativeModeTab> NTM_SPACE_TAB = TABS.register("ntm_space_tab",
            () -> CreativeTabRegistry.create(b -> b
                    .title(Component.translatable("itemGroup.space_m.ntm_space_tab"))
                    .icon(() -> new ItemStack(com.space_m.rocket.SpaceRocketItems.ROCKET_CUSTOM.get()))
                    .displayItems((params, output) -> fillSpaceTab(st -> output.accept(st, CreativeModeTab.TabVisibility.PARENT_TAB_ONLY)))));

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
        TABS.register();
        for (Tab tab : Tab.values()) {
            CreativeTabRegistry.modify(CreativeTabRegistry.defer(tab.id()), (flags, output, canUseOps) -> {
                for (Supplier<ItemStack> s : entries(tab)) {
                    ItemStack st = s.get();
                    if (!st.isEmpty()) output.accept(st);
                }
            });
        }
    }

    /**
     * Inhalt des Sammel-Tabs, zur Fuellzeit berechnet (neue Items erscheinen automatisch):
     * <ol>
     * <li>alle Eintraege, die das Addon in die HBM-Tabs haengt (inkl. NBT-Varianten wie Laufwerk je Koerper,
     * Satelliten), in der Reihenfolge {@link #SPACE_TAB_ORDER};</li>
     * <li>HBM-Fluidbehaelter/-kennungen/-rohre, die mit einem {@code space_m}-Fluid gefuellt sind (wie in den HBM-Tabs);</li>
     * <li>alle uebrigen {@code space_m}-Items (inkl. BlockItems) in Registrierungsreihenfolge.</li>
     * </ol>
     * Doppelte Stapel (gleiches Item + gleiche Daten) werden verworfen.
     */
    public static void fillSpaceTab(Consumer<ItemStack> out) {
        Set<ItemStack> seen = new ObjectLinkedOpenCustomHashSet<>(ItemStackLinkedSet.TYPE_AND_TAG);
        Set<Item> seenItems = new HashSet<>();
        Consumer<ItemStack> add = st -> {
            if (st == null || st.isEmpty() || !seen.add(st)) return;
            seenItems.add(st.getItem());
            out.accept(st);
        };
        for (Tab tab : SPACE_TAB_ORDER) {
            for (Supplier<ItemStack> s : entries(tab)) add.accept(s.get());
        }
        addSpaceFluidContainers(add);
        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
            if (!SpaceMod.MOD_ID.equals(key.getNamespace()) || seenItems.contains(item)) continue;
            add.accept(new ItemStack(item));
        }
    }

    private static boolean isSpaceFluid(Fluid f) {
        return f != null && SpaceMod.MOD_ID.equals(BuiltInRegistries.FLUID.getKey(f).getNamespace());
    }

    /** HBM-Behaelter je {@code space_m}-Fluid; gleiche Auswahl wie die HBM-Tabs (getSubItems / hasNoID). */
    private static void addSpaceFluidContainers(Consumer<ItemStack> add) {
        for (ModFluids.FluidEntry entry : HbmFluidRegistry.getOrderedFluids()) {
            Fluid f = entry.getSource();
            if (!isSpaceFluid(f) || FluidType.forFluid(f).hasNoID()) continue;
            ItemStack id = new ItemStack(ModItems.FLUID_IDENTIFIER.get());
            FluidIdentifierItem.setType(id, HbmFluidRegistry.getFluidName(f), true);
            add.accept(id);
        }
        for (RegistrySupplier<Item> tank : List.of(ModItems.CANISTER_FULL, ModItems.GAS_FULL, ModItems.FLUID_TANK_FULL,
                ModItems.FLUID_TANK_LEAD_FULL, ModItems.FLUID_BARREL_FULL, ModItems.FLUID_PACK_FULL)) {
            if (!(tank.get() instanceof ItemFluidTank t)) continue;
            for (ItemStack st : t.getSubItems()) {
                if (isSpaceFluid(ItemFluidTank.getFluid(st))) add.accept(st);
            }
        }
        for (ModFluids.FluidEntry entry : HbmFluidRegistry.getOrderedFluids()) {
            Fluid f = entry.getSource();
            if (!isSpaceFluid(f) || FluidType.forFluid(f).hasNoID()) continue;
            add.accept(FluidDuctItem.createStack(ModItems.FLUID_DUCT.get(), entry));
            add.accept(FluidDuctItem.createStack(ModItems.FLUID_DUCT_COLORED.get(), entry));
            add.accept(FluidDuctItem.createStack(ModItems.FLUID_DUCT_SILVER.get(), entry));
        }
    }
}
