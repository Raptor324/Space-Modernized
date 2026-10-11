package com.space_m.material;

import static com.hbm_m.inventory.material.MaterialShapes.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.hbm_m.inventory.material.MaterialShapes;
import com.hbm_m.inventory.material.Mats;
import com.hbm_m.inventory.material.Mats.MaterialStack;
import com.hbm_m.inventory.material.NTMMaterial;
import com.space_m.dim.PlanetOres;
import com.space_m.dim.minmus.MinmusBlocks;
import com.space_m.dim.duna.DunaBlocks;
import com.space_m.item.SpaceMaterialItems;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

/**
 * R4-B: Giesserei-Materialien von NTM Space ({@code com.hbm.inventory.material.Mats}, Space-Zuwachs) im
 * Materialsystem von HBM-Modernized ({@link Mats#orderedList}/{@code matById}, oeffentliche Fabriken
 * {@code makeSmeltable/makeNonSmeltable/makeAdditive}). IDs, Namen (= Ore-Dictionary-Namen), Farben und Autogen-Formen
 * 1:1; nur mit installiertem Addon vorhanden.
 *
 * <p>Schmelzwerte der Addon-Gegenstaende ({@code Mats.getMaterialsFromItem}): HBM loest Formen-Items nur unter
 * {@code hbm_m:} bzw. ueber {@code forge:}-Tags auf; deshalb traegt das Addon seine Formen (Barren 72, Nugget 8 ...)
 * und die {@code MatDistribution}-Zusaetze des Originals direkt in {@link Mats#materialEntries} ein. Giessformen
 * ({@code Mats.getItemForShape}) koennen Addon-Items noch nicht ausgeben -> Wunsch W-R4b-1.
 *
 * <p>Nicht hier: zusaetzliche {@code FRAGMENT}-Autogen-Formen bestehender Materialien (Bedrock-Erz-Fragmente, R8).
 */
public final class SpaceMats {

    private SpaceMats() {}

    public static final int _EX = 24_000;

    private static String[] n(String... names) { return names; }

    //Vanilla and vanilla-like
    public static final NTMMaterial MAT_GLOWSTONE    = Mats.makeNonSmeltable(Mats._VS + 5, n("Glowstone"), 0xFFFF00, 0x535300, 0xFFFF00).setAutogen(FRAGMENT).n();
    public static final NTMMaterial MAT_CONGLOMERATE = Mats.makeAdditive(2993, n("Conglomerate"), 0x797979, 0x797979, 0x797979).m();
    //Base metals / Space
    public static final NTMMaterial MAT_NICKEL       = Mats.makeSmeltable(2800, n("NickelPure"), 0xE8D1C7, 0x87756E, 0xAE9572).setAutogen(FRAGMENT, NUGGET, DUST, BLOCK).m();
    public static final NTMMaterial MAT_GALLIUM      = Mats.makeSmeltable(3100, n("Gallium"), 0x52687F, 0x52687F, 0x52687F).setAutogen(FRAGMENT, NUGGET, DUST, DUSTTINY).m();
    public static final NTMMaterial MAT_ZINC         = Mats.makeSmeltable(3000, n("Zinc"), 0xD7CBDA, 0x7A7277, 0xA79DA8).setAutogen(FRAGMENT, NUGGET, DUST, WIRE).m();
    public static final NTMMaterial MAT_BROMINE      = Mats.makeNonSmeltable(3500, n("Bromine"), 0xFF642B, 0x720000, 0xFF642B).setAutogen(FRAGMENT).m();
    public static final NTMMaterial MAT_IODINE       = Mats.makeNonSmeltable(5300, n("Iodine"), 0x7A8796, 0x3F3049, 0x7A8796).setAutogen(FRAGMENT).m();
    public static final NTMMaterial MAT_HAFNIUM      = Mats.makeSmeltable(7200, n("Hafnium"), 0xFFF8C7, 0x2E1600, 0xFFF8C7).setAutogen(FRAGMENT, DUST).m();
    public static final NTMMaterial MAT_IRIDIUM      = Mats.makeSmeltable(7700, n("Iridium"), 0xB8D0FF, 0xB8D0FF, 0xB8D0FF).setAutogen(INGOT).m();
    //Space extension alloys
    public static final NTMMaterial MAT_GAAS         = Mats.makeSmeltable(_EX + 0, n("GalliumArsenide"), 0x6F4A57, 0x6F4A57, 0x6F4A57).setAutogen(NUGGET, BILLET).m();
    public static final NTMMaterial MAT_STAINLESS    = Mats.makeSmeltable(_EX + 1, n("StainlessSteel"), 0xD8D8D8, 0x474747, 0x4A4A4A).setAutogen(PLATE, WELDEDPLATE, CASTPLATE).m();
    public static final NTMMaterial MAT_RICH_MAGMA   = Mats.makeSmeltable(_EX + 2, n("RichMagma"), 0x7F7F7F, 0x353555, 0xFF6212).n();
    public static final NTMMaterial MAT_SEMTEX       = Mats.makeNonSmeltable(_EX + 3, n("Semtex"), 0xEDAA28, 0x825D16, 0xF0B090).setAutogen(FRAGMENT).n();

    /** Laedt die Klasse (Materialien beim Mod-Start anmelden, wie der statische Initialisierer von {@code Mats}). */
    public static void init() {}

    private static boolean entriesDone;

    /**
     * Schmelzwerte der Addon-Gegenstaende ({@code Mats.materialEntries}); erst wenn die Items registriert sind
     * (Common-Setup). Original: Ore-Dictionary-Formen der Materialien + {@code MatDistribution.registerDefaults}.
     */
    public static synchronized void registerEntries() {
        if (entriesDone) return;
        entriesDone = true;

        // Formen (nur schmelzbar/Zusatz: Original getMaterialsFromItem prueft SMELTABLE/ADDITIVE)
        shape(SpaceMaterialItems.INGOT_NICKEL, MAT_NICKEL, INGOT);
        shape(SpaceMaterialItems.NUGGET_NICKEL, MAT_NICKEL, NUGGET);
        shape(SpaceMaterialItems.POWDER_NICKEL, MAT_NICKEL, DUST);
        shape(SpaceMaterialItems.PLATE_NICKEL, MAT_NICKEL, PLATE);
        shape(SpaceMaterialItems.INGOT_ZINC, MAT_ZINC, INGOT);
        shape(SpaceMaterialItems.NUGGET_ZINC, MAT_ZINC, NUGGET);
        shape(SpaceMaterialItems.POWDER_ZINC, MAT_ZINC, DUST);
        shape(SpaceMaterialItems.WIRE_ZINC, MAT_ZINC, WIRE);
        shape(SpaceMaterialItems.INGOT_GALLIUM, MAT_GALLIUM, INGOT);
        shape(SpaceMaterialItems.NUGGET_GALLIUM, MAT_GALLIUM, NUGGET);
        shape(SpaceMaterialItems.POWDER_GALLIUM, MAT_GALLIUM, DUST);
        shape(SpaceMaterialItems.POWDER_GALLIUM_TINY, MAT_GALLIUM, DUSTTINY);
        shape(SpaceMaterialItems.INGOT_HAFNIUM, MAT_HAFNIUM, INGOT);
        shape(SpaceMaterialItems.NUGGET_HAFNIUM, MAT_HAFNIUM, NUGGET);
        shape(SpaceMaterialItems.INGOT_IRIDIUM, MAT_IRIDIUM, INGOT);
        shape(SpaceMaterialItems.INGOT_GAAS, MAT_GAAS, INGOT);
        shape(SpaceMaterialItems.NUGGET_GAAS, MAT_GAAS, NUGGET);
        shape(SpaceMaterialItems.BILLET_GAAS, MAT_GAAS, BILLET);
        shape(SpaceMaterialItems.INGOT_STAINLESS, MAT_STAINLESS, INGOT);
        shape(SpaceMaterialItems.PLATE_STAINLESS, MAT_STAINLESS, PLATE);
        shape(SpaceMaterialItems.PLATE_CAST_STAINLESS, MAT_STAINLESS, CASTPLATE);
        shape(SpaceMaterialItems.PLATE_WELDED_STAINLESS, MAT_STAINLESS, WELDEDPLATE);
        shape(SpaceMaterialItems.INGOT_MAGMA, MAT_RICH_MAGMA, INGOT);

        // MatDistribution.registerDefaults (Space-Zusaetze)
        entry(SpaceMaterialItems.BLADE_SYNGAS, new MaterialStack(MAT_STAINLESS, INGOT.q(3)));
        // registerOre(ZI.ore(), MAT_ZINC, INGOT.q(2), MAT_STONE, QUART.q(1)) - alle Koerper-Varianten (oreAll)
        for (PlanetOres.Variant v : PlanetOres.variants()) {
            if (v.ore().equals("ore_zinc")) {
                entry(v.block()::get, new MaterialStack(MAT_ZINC, INGOT.q(2)), new MaterialStack(Mats.MAT_STONE, QUART.q(1)));
            }
        }
        // registerOre(CONGLOMERATE.ore(), MAT_CONGLOMERATE, INGOT.q(4))
        entry(MinmusBlocks.STONE_RESOURCE_CONGLOMERATE::get, new MaterialStack(MAT_CONGLOMERATE, INGOT.q(4)));
        // registerEntry(stone_resource CALCIUM, MAT_FLUX, DUST.q(12))
        entry(DunaBlocks.STONE_RESOURCE_CALCIUM::get, new MaterialStack(Mats.MAT_FLUX, DUST.q(12)));
        // registerEntry(ModItems.ring_starmetal, MAT_STAR, INGOT.q(4))
        Item ring = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("hbm_m", "ring_starmetal"));
        if (ring != Items.AIR && !Mats.materialEntries.containsKey(ring)) {
            Mats.materialEntries.put(ring, new ArrayList<>(List.of(new MaterialStack(Mats.MAT_STAR, INGOT.q(4)))));
        }
    }

    private static void shape(Supplier<? extends ItemLike> item, NTMMaterial mat, MaterialShapes shape) {
        entry(item, new MaterialStack(mat, shape.q(1)));
    }

    private static void entry(Supplier<? extends ItemLike> item, MaterialStack... stacks) {
        Item it = item.get().asItem();
        List<MaterialStack> list = new ArrayList<>(List.of(stacks));
        Mats.materialEntries.put(it, list);
    }
}
