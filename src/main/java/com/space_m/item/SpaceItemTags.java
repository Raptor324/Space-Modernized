package com.space_m.item;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

import com.space_m.dim.PlanetOres;
import com.space_m.dim.minmus.MinmusBlocks;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;

/**
 * R4-B: Ore-Dictionary-Eintraege von NTM Space ({@code OreDictManager}, Space-Zuwachs) als Item-Tags im Schema von
 * HBM-Modernized: {@code hbm_m:oredict/<praefix>/<material>} (Praefix/Material in snake_case, z. B.
 * {@code plateTripleStainlessSteel} -> {@code hbm_m:oredict/plate_triple/stainless_steel}). Jeder DictFrame-Name
 * zaehlt (Curium-242 = {@code curium242} + {@code cm242}). Erzeugt im Datagen ({@code SpaceTagProviders}).
 */
public final class SpaceItemTags {

    private SpaceItemTags() {}

    private static BiConsumer<TagKey<Item>, Item> out;

    public static TagKey<Item> oredict(String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("hbm_m", "oredict/" + path));
    }


    /**
     * Kompatibilitaets-Ordner der {@code forge:}-Formen-Tags je Ore-Dictionary-Praefix (= {@code MaterialShapes.tagFolders}
     * von HBM; auf NeoForge 1.21 benennt processResources {@code forge:} in {@code c:} um). W-R4b-1: HBM erkennt die
     * Addon-Formen direkt ({@code Mats.registerShapeItem}), die Tags zaehlen daher nicht doppelt.
     */
    private static final java.util.Map<String, String> COMPAT_FOLDERS = java.util.Map.of(
            "nugget", "nuggets", "ingot", "ingots", "dust", "dusts", "plate", "plates", "wire_fine", "wires_fine",
            "dust_tiny", "tiny_dusts", "gem", "gems", "block", "storage_blocks", "ore", "ores", "bolt", "bolts");

    public static TagKey<Item> forgeTag(String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("forge", path));
    }

    /** Eintraege {@code <praefix>/<name>} fuer alle Namen (+ {@code forge:<ordner>/<name>} fuer Formen-Praefixe). */
    private static void od(String prefix, String[] names, Supplier<? extends ItemLike> item) {
        String folder = COMPAT_FOLDERS.get(prefix);
        for (String n : names) {
            out.accept(oredict(prefix + "/" + n), item.get().asItem());
            if (folder != null) out.accept(forgeTag(folder + "/" + n), item.get().asItem());
        }
    }

    private static String[] n(String... s) { return s; }

    public static void addItemTags(BiConsumer<TagKey<Item>, Item> consumer) {
        out = consumer;

        // ── Actinide (nugget registriert auch "tiny") ──
        actinide(n("curium242", "cm242"), SpaceMaterialItems.NUGGET_CM242, SpaceMaterialItems.BILLET_CM242, SpaceMaterialItems.INGOT_CM242);
        actinide(n("curium243", "cm243"), SpaceMaterialItems.NUGGET_CM243, SpaceMaterialItems.BILLET_CM243, SpaceMaterialItems.INGOT_CM243);
        actinide(n("curium244", "cm244"), SpaceMaterialItems.NUGGET_CM244, SpaceMaterialItems.BILLET_CM244, SpaceMaterialItems.INGOT_CM244);
        actinide(n("curium245", "cm245"), SpaceMaterialItems.NUGGET_CM245, SpaceMaterialItems.BILLET_CM245, SpaceMaterialItems.INGOT_CM245);
        actinide(n("curium246", "cm246"), SpaceMaterialItems.NUGGET_CM246, SpaceMaterialItems.BILLET_CM246, SpaceMaterialItems.INGOT_CM246);
        actinide(n("curium247", "cm247"), SpaceMaterialItems.NUGGET_CM247, SpaceMaterialItems.BILLET_CM247, SpaceMaterialItems.INGOT_CM247);
        actinide(n("curium_rg"), SpaceMaterialItems.NUGGET_CM_MIX, SpaceMaterialItems.BILLET_CM_MIX, SpaceMaterialItems.INGOT_CM_MIX);
        actinide(n("curium_fuel"), SpaceMaterialItems.NUGGET_CM_FUEL, SpaceMaterialItems.BILLET_CM_FUEL, SpaceMaterialItems.INGOT_CM_FUEL);
        actinide(n("berkelium247", "bk247"), SpaceMaterialItems.NUGGET_BK247, SpaceMaterialItems.BILLET_BK247, SpaceMaterialItems.INGOT_BK247);
        actinide(n("californium251", "cf251"), SpaceMaterialItems.NUGGET_CF251, SpaceMaterialItems.BILLET_CF251, SpaceMaterialItems.INGOT_CF251);
        actinide(n("californium252", "cf252"), SpaceMaterialItems.NUGGET_CF252, SpaceMaterialItems.BILLET_CF252, SpaceMaterialItems.INGOT_CF252);
        actinide(n("einsteinium253", "es253"), SpaceMaterialItems.NUGGET_ES253, SpaceMaterialItems.BILLET_ES253, SpaceMaterialItems.INGOT_ES253);
        od("ingot", n("einsteinium255", "es255"), SpaceMaterialItems.INGOT_ES255);
        String[] cn = n("chinesium989", "cn989");
        actinide(cn, SpaceMaterialItems.NUGGET_CN989, SpaceMaterialItems.BILLET_CN989, SpaceMaterialItems.INGOT_CN989);
        od("dust", cn, SpaceMaterialItems.POWDER_CN989);
        od("plate", cn, SpaceMaterialItems.PLATE_CN989);
        actinide(n("menthol"), SpaceMaterialItems.NUGGET_MENTHOL, SpaceMaterialItems.BILLET_MENTHOL, SpaceMaterialItems.INGOT_MENTHOL);

        // ── Metalle ──
        String[] ni = n("nickel_pure");
        od("ingot", ni, SpaceMaterialItems.INGOT_NICKEL);
        od("dust", ni, SpaceMaterialItems.POWDER_NICKEL);
        od("plate", ni, SpaceMaterialItems.PLATE_NICKEL);
        nugget(ni, SpaceMaterialItems.NUGGET_NICKEL);
        // NIM "Nickel": dust = chunk_ore PENTLANDITE ("dust selected for compat reasons"); PENTLANDITE crystal
        od("dust", n("nickel"), SpaceMaterialItems.CHUNK_ORE_PENTLANDITE);
        od("crystal", n("pentlandite"), SpaceMaterialItems.CHUNK_ORE_PENTLANDITE);
        String[] st = n("stainless_steel");
        od("ingot", st, SpaceMaterialItems.INGOT_STAINLESS);
        od("plate", st, SpaceMaterialItems.PLATE_STAINLESS);
        od("plate_triple", st, SpaceMaterialItems.PLATE_CAST_STAINLESS);   // Autogen CASTPLATE
        od("plate_sextuple", st, SpaceMaterialItems.PLATE_WELDED_STAINLESS); // Autogen WELDEDPLATE
        String[] zi = n("zinc");
        nugget(zi, SpaceMaterialItems.NUGGET_ZINC);
        od("ingot", zi, SpaceMaterialItems.INGOT_ZINC);
        od("dust", zi, SpaceMaterialItems.POWDER_ZINC);
        od("wire_fine", zi, SpaceMaterialItems.WIRE_ZINC);                 // Autogen WIRE (aot wire_zinc)
        String[] ga = n("gallium");
        nugget(ga, SpaceMaterialItems.NUGGET_GALLIUM);
        od("ingot", ga, SpaceMaterialItems.INGOT_GALLIUM);
        od("dust", ga, SpaceMaterialItems.POWDER_GALLIUM);
        od("dust_tiny", ga, SpaceMaterialItems.POWDER_GALLIUM_TINY);
        String[] gaas = n("gallium_arsenide");
        nugget(gaas, SpaceMaterialItems.NUGGET_GAAS);
        od("ingot", gaas, SpaceMaterialItems.INGOT_GAAS);
        od("billet", gaas, SpaceMaterialItems.BILLET_GAAS);
        nugget(n("hafnium"), SpaceMaterialItems.NUGGET_HAFNIUM);
        od("ingot", n("hafnium"), SpaceMaterialItems.INGOT_HAFNIUM);
        od("ingot", n("iridium"), SpaceMaterialItems.INGOT_IRIDIUM);
        od("ingot", n("rich_magma"), SpaceMaterialItems.INGOT_MAGMA);
        od("ingot", n("tasmanite"), SpaceMaterialItems.INGOT_AUSTRALIUM_LESSER);
        od("ingot", n("ayerite"), SpaceMaterialItems.INGOT_AUSTRALIUM_GREATER);
        od("billet", n("mingrade"), SpaceMaterialItems.BILLET_RED_COPPER);
        nugget(n("lanthanum"), SpaceMaterialItems.NUGGET_LANTHANIUM);      // LA.nugget(nugget_lanthanium, fragment_lanthanium)
        od("dust", n("rubber"), SpaceMaterialItems.POWDER_RUBBER);
        od("dust", n("pvc"), SpaceMaterialItems.POWDER_PVC);
        // Koks-Pulver: COALCOKE/PETCOKE/LIGCOKE .dust, ANY_COKE .dust(all), ANY_COAL_COKE (Gruppe + DUST-Praefix)
        od("dust", n("coal_coke", "any_coke", "any_coal_coke"), SpaceMaterialItems.POWDER_COKE_COAL);
        od("dust", n("pet_coke", "any_coke", "any_coal_coke"), SpaceMaterialItems.POWDER_COKE_PETROLEUM);
        od("dust", n("lignite_coke", "any_coke", "any_coal_coke"), SpaceMaterialItems.POWDER_COKE_LIGNITE);
        out.accept(oredict("dust/glowstone"), Items.GLOWSTONE_DUST);           // GLOWSTONE.dust(Items.glowstone_dust)

        // ── Erze (oreAll: alle Koerper-Varianten) ──
        for (PlanetOres.Variant v : PlanetOres.variants()) {
            switch (v.ore()) {
                case "ore_zinc" -> od("ore", zi, v.block());
                case "ore_nickel" -> od("ore", ni, v.block());
                default -> { }
            }
        }
        od("ore", n("conglomerate"), MinmusBlocks.STONE_RESOURCE_CONGLOMERATE);

        // ── Lebensmittel (KEY_FLOUR/BUTTER/STRAWBERRY_ICECREAM/STRAWBERRY_MILKSHAKE) ──
        out.accept(oredict("food_flour"), SpaceMaterialItems.FLOUR.get());
        out.accept(oredict("food_butter"), SpaceMaterialItems.BUTTER.get());
        out.accept(oredict("food_strawberryicecream"), SpaceMaterialItems.S_CREAM.get());
        out.accept(oredict("food_strawberrymilkshake"), SpaceMaterialItems.GLASS_SMILK.get());
        // KEY_STICK
        out.accept(oredict("stick_wood"), SpaceMaterialItems.STICK_PVC.get());
        out.accept(oredict("stick_wood"), SpaceMaterialItems.STICK_VINYL.get());

        // forge:-Formen-Tags (od, COMPAT_FOLDERS): seit W-R4b-1 kennt HBM die Addon-Formen direkt (Mats.registerShapeItem),
        // Schmelzwert bleibt einfach (72 Quanten/Barren).
    }


    private static void nugget(String[] names, Supplier<? extends ItemLike> item) {
        od("nugget", names, item);
        od("tiny", names, item);
    }

    private static void actinide(String[] names, Supplier<? extends ItemLike> nug, Supplier<? extends ItemLike> bil, Supplier<? extends ItemLike> ing) {
        nugget(names, nug);
        od("billet", names, bil);
        od("ingot", names, ing);
    }

    /** Block-Tags: Gummi-Ackerboden (Material.sand -> Schaufel), Feldfruechte. */
    public static void addBlockTags(BiConsumer<TagKey<Block>, Block> consumer) {
        consumer.accept(BlockTags.MINEABLE_WITH_SHOVEL, SpaceCrops.RUBBER_FARMLAND.get());
        consumer.accept(BlockTags.CROPS, SpaceCrops.CROP_MINT.get());
        consumer.accept(BlockTags.CROPS, SpaceCrops.CROP_COFFEE.get());
        consumer.accept(BlockTags.CROPS, SpaceCrops.CROP_TEA.get());
        consumer.accept(BlockTags.CROPS, SpaceCrops.CROP_PARAFFIN.get());
        consumer.accept(BlockTags.MAINTAINS_FARMLAND, SpaceCrops.CROP_MINT.get());
        consumer.accept(BlockTags.MAINTAINS_FARMLAND, SpaceCrops.CROP_COFFEE.get());
        consumer.accept(BlockTags.MAINTAINS_FARMLAND, SpaceCrops.CROP_TEA.get());
        consumer.accept(BlockTags.MAINTAINS_FARMLAND, SpaceCrops.CROP_PARAFFIN.get());
    }
}
